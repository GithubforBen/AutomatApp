package de.schnorrenbergers.automat.manager;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class ConfigurationManager {
    private final Map<String, Object> objMap;
    public ConfigurationManager() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("config.yaml");
        objMap = yaml.load(inputStream);
        System.out.println(objMap);
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
