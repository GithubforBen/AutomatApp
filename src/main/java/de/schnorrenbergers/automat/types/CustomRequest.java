package de.schnorrenbergers.automat.types;

import de.schnorrenbergers.automat.Main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class CustomRequest {
    String urlString;
    public CustomRequest(String url) {
        this.urlString = Main.getInstance().getUrl() + "/" + url;
    }

    public String execute() throws IOException {
            URL url = new URL(urlString);
            url.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder sb = new StringBuilder();
            br.lines().forEach(sb::append);
            br.close();
            return sb.toString();
    }

    public String executeComplex(String data) throws IOException {
        URL url = new URL(urlString);
        URLConnection urlConnection = url.openConnection();
        urlConnection.getOutputStream().write(data.getBytes());
        BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder sb = new StringBuilder();
        br.lines().forEach(sb::append);
        br.close();
        return sb.toString();

    }
}
