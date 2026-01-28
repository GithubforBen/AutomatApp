package de.schnorrenbergers.automat.utils;

import de.schnorrenbergers.automat.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class CustomRequest {

    private final String urlString;
    private final REVIVER reviver;

    public CustomRequest(String url, REVIVER reviver) {
        this.reviver = reviver;
        this.urlString = Main.getInstance().getUrl(reviver) + "/" + url;
    }

    public String execute() throws IOException {
        if (!urlString.contains("ping")) {
            if (!isOnline()) return null;
        }
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout(1000);
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(sb::append);
        br.close();
        return sb.toString();
    }

    public boolean isOnline() {
        try {
            URL url = new URL(Main.getInstance().getUrl(reviver) + "/ping");
            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(1000);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder sb = new StringBuilder();
            br.lines().forEach(sb::append);
            br.close();
        } catch (Exception e) {
            Main.getInstance().loadScene("hello-view.fxml");
            return false;
        }
        return true;
    }

    public String executeComplex(String data) throws IOException {
        if (!isOnline()) return null;
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");
        urlConnection.setDoOutput(true);
        urlConnection.setDoInput(true);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Accept", "application/json");
        urlConnection.setConnectTimeout(1000);
        OutputStream outputStream = urlConnection.getOutputStream();
        outputStream.write(data.getBytes());
        outputStream.flush();
        outputStream.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        urlConnection.disconnect();
        return response.toString();
    }

    public enum REVIVER {
        WEBSITE, DISPENSER, SCANNER, STATION
    }
}