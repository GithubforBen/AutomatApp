package de.schnorrenbergers.automat.manager;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class ConfigurationManager {
    private final Map<String, Object> objMap;
    private final File file;

    public ConfigurationManager() {
        file = new File("./config.yaml");
        Yaml yaml = new Yaml();
        if (!file.exists()) {
            InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("config.yaml");
            objMap = yaml.load(inputStream);
        } else {
            try {
                objMap = yaml.load(new FileReader(file));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void save() throws IOException {
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(new Yaml().dump(objMap));
        fileWriter.flush();
        fileWriter.close();
    }

    public Object get(String key) {
        String[] split = key.split("\\.");
        if (split.length == 1) return objMap.get(key);
        Map<String, Object> map = objMap;
        for (String s : split) {
            if (map.get(s) instanceof Map) {
                map = (Map<String, Object>) map.get(s);
            } else {
                return map.get(s);
            }
        }
        return null;
    }

    public String getString(String key) {
        return (String) get(key);
    }

    public int getInt(String key) {
        return ((Number) get(key)).intValue();
    }

    public boolean getBoolean(String key) {
        return (boolean) get(key);
    }

    /**
     * Auch für ganze Zahlen in der YAML (die kommen dort als Integer an) - z.B.
     * Preise wie {@code kost: 2} neben {@code kost: 1.5}.
     */
    public double getDouble(String key) {
        return ((Number) get(key)).doubleValue();
    }

    /**
     * Preis einer Süßigkeit in Stunden (auch halbe, z.B. 1.5).
     */
    public double getCost(int sweet) {
        return getDouble("sweets._" + sweet + ".kost");
    }

    /**
     * Preis zum Anzeigen: "2" statt "2.0", "1,5" statt "1.5".
     */
    public static String formatHours(double hours) {
        if (hours == Math.rint(hours)) return String.valueOf((long) hours);
        return String.valueOf(hours).replace('.', ',');
    }
}
