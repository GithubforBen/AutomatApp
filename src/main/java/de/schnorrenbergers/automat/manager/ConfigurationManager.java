package de.schnorrenbergers.automat.manager;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class ConfigurationManager {
    private Map<String, Object> objMap;
    public ConfigurationManager() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("config.yaml");
        objMap = yaml.load(inputStream);
        System.out.println(objMap);
    }

    public Object get(String key) {
        return objMap.get(key);
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
}
