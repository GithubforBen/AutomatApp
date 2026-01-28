package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.utils.CustomRequest;

import java.io.IOException;
import java.util.Arrays;

public class DispensationManager {

    private static Thread dispenser;

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

    public static void dispense(int number, KontenManager kontenManager, ConfigurationManager configurationManager) {
        if (canDispense()) dispenser = new Thread(() -> {
            try {
                Main.getInstance().setLastScan(null);
                kontenManager.withdraw(configurationManager.getInt("sweets._" + number + ".kost"));
                Main.getInstance().kost();
                new StatisticManager().persistDispense(number);
                new CustomRequest("dispense", CustomRequest.REVIVER.DISPENSER).executeComplex("{\"nr\":" + map(number) + ",\"cost\":" + configurationManager.getInt("sweets._" + number + ".kost") + ",\"usr\":" + Arrays.toString(Main.getInstance().getLastScan()) + "}");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            dispenser = null;
        });
        dispenser.start();
    }

    public static boolean canDispense() {
        return dispenser == null;
    }
}
