package de.schnorrenbergers.automat.types;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class CustomRequest {
    String urlString;
    public CustomRequest(String url) {
        this.urlString = url;
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
}
