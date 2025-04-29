package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ScannedHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
        BufferedReader br = new BufferedReader(isr);
        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
            buf.append((char) b);
        }
        JSONObject json = getJSON(exchange);
        List<Integer> byteAdresses = new ArrayList<>();
        for (Object rfid : json.getJSONArray("rfid")) {
            byteAdresses.add(Integer.parseInt(rfid.toString()));
        }
        int[] arr = new int[byteAdresses.size()];
        for (int i = 0; i < byteAdresses.size(); i++) {
            arr[i] = byteAdresses.get(i);
        }
        Main.getInstance().setLastScan(arr);
        respond(exchange, buf.toString(), 200);
    }
}
