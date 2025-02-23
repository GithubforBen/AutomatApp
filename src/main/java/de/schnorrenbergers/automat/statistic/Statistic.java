package de.schnorrenbergers.automat.statistic;

import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class Statistic {
    File file = new File("./stats.json");
    HashMap<Integer, Integer> stats = new HashMap<>();
    public Statistic() throws IOException {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
            for (int i = 0; i < 8; i++) {
                stats.put(i, 0);
            }
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
        for (int i = 0; i < obj.getInt("length"); i++) {
            stats.put(i, obj.getInt("d:" + i));
        }
    }

    public int getStat(int i) {
        return stats.get(i);
    }

    public void save() throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("length", stats.size());
        stats.forEach((key, value) -> {
            obj.put("d:" + key, value);
        });
        file.deleteOnExit();
        file.createNewFile();
        FileWriter writer = new FileWriter(file);
        writer.write(obj.toString());
        writer.flush();
        writer.close();
    }

    public void addOne(int sweet) {
        Integer orDefault = stats.getOrDefault(sweet, -1);
        if (orDefault == -1) {
             stats.put(sweet, 1);
        } else {
            stats.replace(sweet, orDefault+1);
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
