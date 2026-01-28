package de.schnorrenbergers.automat.manager;

import de.schnorrenbergers.automat.Main;
import de.schnorrenbergers.automat.utils.CustomRequest;

import java.io.IOException;
import java.util.Arrays;

public class DispensationManager {

    private static Thread dispenser;

    public static void dispense(int number, KontenManager kontenManager, ConfigurationManager configurationManager) {
        if (canDispense()) dispenser = new Thread(() -> {
            try {
                Main.getInstance().setLastScan(null);
                kontenManager.withdraw(configurationManager.getInt("sweets._" + number + ".kost"));
                Main.getInstance().kost();
                new StatisticManager().persistDispense(number);
                new CustomRequest("dispense", CustomRequest.REVIVER.DISPENSER).executeComplex("{\"nr\":" + number + ",\"cost\":" + configurationManager.getInt("sweets._" + number + ".kost") + ",\"usr\":" + Arrays.toString(Main.getInstance().getLastScan()) + "}");
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
