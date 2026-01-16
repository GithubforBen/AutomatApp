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
        return (int) get(key);
    }

    public boolean getBoolean(String key) {
        return (boolean) get(key);
    }

    public double getDouble(String key) {
        return (double) get(key);
    }
}
