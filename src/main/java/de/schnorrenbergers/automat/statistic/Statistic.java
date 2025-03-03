package de.schnorrenbergers.automat.statistic;

import de.schnorrenbergers.automat.Main;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class Statistic {
    File file = new File("./stats.json");
    HashMap<Integer, Integer> stats = new HashMap<>();
    JSONObject settings;

    public Statistic() throws IOException {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
            for (int i = 0; i < 8; i++) {
                stats.put(i, 0);
            }
            settings = new JSONObject();
            settings.put("notNull", 0);
            save();
            return;
        }
        FileReader fileReader = new FileReader(file);
        StringBuilder stringBuilder = new StringBuilder();
        int b = 0;
        while (b != -1) {
            b = fileReader.read();
            if (b == -1) continue;
            stringBuilder.append((char) b);
        }
        fileReader.close();
        System.out.println(stringBuilder.toString());
        JSONObject obj = new JSONObject(stringBuilder.toString());
        settings = obj.getJSONObject("settings");
        for (int i = 0; i < obj.getInt("length"); i++) {
            stats.put(i, obj.getInt("d:" + i));
        }
    }

    public Object getSetting(String adress) {
        return settings.get(adress);
    }

    public Object getSettingOrDefault(String adress, Object defaultValue) {
        try {
            return getSetting(adress);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean setSetting(String adress, Object value) {
        try {
            settings.get(adress);
            settings.remove(adress);
        } catch (Exception _) {
        }
        settings.put(adress, value);
        return true;
    }

    public int getStat(int i) {
        return stats.get(i);
    }

    public void resetStats() {
        HashMap<Integer, Integer> temp = new HashMap<>();
        stats.forEach(new BiConsumer<Integer, Integer>() {
            @Override
            public void accept(Integer integer, Integer integer2) {
                temp.put(integer, 0);
            }
        });
        stats = temp;
    }

    public void save() throws IOException {
        setSetting("logout", Main.getInstance().getLogoutTime());
        JSONObject obj = new JSONObject();
        obj.put("length", stats.size());
        stats.forEach((key, value) -> {
            obj.put("d:" + key, value);
        });
        obj.put("settings", settings);
        PrintWriter printWriter = new PrintWriter(file);
        printWriter.print("");
        printWriter.print(obj.toString());
        printWriter.flush();
        printWriter.close();
    }

    public void addOne(int sweet) {
        Integer orDefault = stats.getOrDefault(sweet, -1);
        if (orDefault == -1) {
            stats.put(sweet, 1);
        } else {
            stats.replace(sweet, orDefault + 1);
        }
        try {
            save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File getFile() {
        return file;
    }
}
