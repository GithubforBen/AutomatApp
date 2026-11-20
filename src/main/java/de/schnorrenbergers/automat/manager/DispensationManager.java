package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.controller.MainController;
import de.schnorrenbergers.automat.utils.CustomRequest;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DispensationManager {

    private static final Logger LOG = Logger.getLogger(DispensationManager.class.getName());

    /**
     * So lange darf eine einzelne Ausgabe dauern, bis die Ausgabe (ESP32) ihren
     * Erfolg gemeldet haben muss: Motor ca. 11 s plus Meldung ans Backend.
     */
    private static final long SUCCESS_TIMEOUT_MS = 30_000;
    /**
     * Zusätzliche Wartezeit je Ausgabe, die vorher noch in der Warteschlange der
     * Ausgabe steht - sie arbeitet eine nach der anderen ab.
     */
    private static final long QUEUE_DELAY_MS = 15_000;

    private static Thread dispenser;

    /**
     * Ausgaben, die an die Ausgabe geschickt wurden und noch auf deren
     * Erfolgsmeldung ({@code /dispense/success}) warten - in Ausgabe-Reihenfolge.
     */
    private static final List<Pending> pending = new ArrayList<>();
    private static final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "dispense-timeout");
        thread.setDaemon(true);
        return thread;
    });

    private record Pending(int type, ScheduledFuture<?> timeout) {
    }

    private static int map(int i) {
        return switch (i) {
            case 0 -> 5;
            case 1 -> 2;
            case 2 -> 1;
            case 3 -> 4;
            case 4 -> 3;
            case 5 -> 6;
            case 6 -> 7;
            default -> 0;
        } - 1;
    }

    /**
     * Inverse of {@link #map(int)}: converts the ESP32-reported dispense slot (`nr`)
     * back to the corresponding Sweet type / button index.
     */
    public static int unmap(int nr) {
        return switch (nr) {
            case 0 -> 2;
            case 1 -> 1;
            case 2 -> 4;
            case 3 -> 3;
            case 4 -> 0;
            case 5 -> 5;
            case 6 -> 6;
            default -> -1;
        };
    }

    public static void dispense(int number, KontenManager kontenManager, ConfigurationManager configurationManager) {
        if (!canDispense()) return;
        int cost = configurationManager.getInt("sweets._" + number + ".kost");
        int[] card = Main.getInstance().getLastScan();
        dispenser = new Thread(() -> {
            try {
                // Erst den Auftrag abgeben - abgebucht wird nur, was die Ausgabe
                // auch angenommen hat.
                String answer = null;
                try {
                    answer = new CustomRequest("dispense", CustomRequest.REVIVER.DISPENSER).executeComplex("{\"nr\":" + map(number) + ",\"cost\":" + cost + ",\"usr\":" + Arrays.toString(card) + "}");
                } catch (IOException e) {
                    LOG.warning("Ausgabe nicht erreichbar: " + e.getMessage());
                }
                if (!"success".equals(answer)) {
                    // Nicht erreichbar oder abgelehnt: keine Süßigkeit, also nichts
                    // abbuchen und das Fach nicht sperren - es liegt nicht am Fach.
                    LOG.warning("Ausgabe hat den Auftrag nicht angenommen (Antwort: " + answer + ")");
                    if (answer == null) {
                        Main.getInstance().showReconnect();
                    } else if (MainController.getMainController() != null) {
                        MainController.getMainController().setText("Ausgabe nicht bereit", Color.RED, true);
                    }
                    return;
                }
                Main.getInstance().setLastScan(null);
                new AvailabilityManager().addSweet(number, -1);
                // Lehrkräfte und Admins haben unbegrenzt Stunden - nichts abziehen.
                if (!kontenManager.hasUnlimitedHours()) {
                    kontenManager.withdraw(cost);
                }
                new StatisticManager().persistDispense(number);
                awaitSuccess(number);
                Main.getInstance().kost();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                dispenser = null;
            }
        });
        dispenser.start();
    }

    public static boolean canDispense() {
        return dispenser == null;
    }

    /**
     * Merkt sich eine angenommene Ausgabe. Meldet die Ausgabe ihren Erfolg nicht
     * rechtzeitig, gilt sie als fehlgeschlagen (siehe {@link #failed(int, String)}).
     */
    private static void awaitSuccess(int type) {
        synchronized (pending) {
            long delay = SUCCESS_TIMEOUT_MS + QUEUE_DELAY_MS * pending.size();
            Pending[] entry = new Pending[1];
            ScheduledFuture<?> timeout = timer.schedule(() -> {
                boolean wasPending;
                synchronized (pending) {
                    wasPending = pending.remove(entry[0]);
                }
                if (wasPending) failed(type, "keine Erfolgsmeldung der Ausgabe");
            }, delay, TimeUnit.MILLISECONDS);
            entry[0] = new Pending(type, timeout);
            pending.add(entry[0]);
        }
    }

    /**
     * Die Ausgabe hat gemeldet, dass eine Süßigkeit des Typs durch die Lichtschranke
     * gefallen ist.
     *
     * @return ob dazu eine offene Ausgabe gefunden wurde
     */
    public static boolean succeeded(int type) {
        Pending done = take(type);
        if (done == null) {
            LOG.info("Erfolgsmeldung für Fach " + type + " ohne offene Ausgabe (vermutlich schon abgelaufen)");
            return false;
        }
        done.timeout().cancel(false);
        return true;
    }

    /**
     * Die Ausgabe hat gemeldet, dass die Lichtschranke nichts gesehen hat.
     */
    public static void reportedFailed(int type) {
        Pending done = take(type);
        if (done != null) done.timeout().cancel(false);
        failed(type, "Lichtschranke hat nichts erkannt");
    }

    private static Pending take(int type) {
        synchronized (pending) {
            for (Pending p : pending) {
                if (p.type() == type) {
                    pending.remove(p);
                    return p;
                }
            }
            return null;
        }
    }

    /**
     * Eine Ausgabe ist fehlgeschlagen. Ist "Fächer automatisch sperren" an, wird
     * das Fach gesperrt, bis es im Admin-Bereich reaktiviert wird.
     */
    private static void failed(int type, String reason) {
        if (!Main.getInstance().isAutoLock()) {
            LOG.warning("Ausgabe von Fach " + type + " fehlgeschlagen (" + reason + ") - automatisches Sperren ist aus");
            return;
        }
        LOG.warning("Ausgabe von Fach " + type + " fehlgeschlagen (" + reason + ") - Fach wird gesperrt");
        new AvailabilityManager().disableSweet(type);
        if (MainController.getMainController() != null) {
            try {
                Main.getInstance().kost();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
