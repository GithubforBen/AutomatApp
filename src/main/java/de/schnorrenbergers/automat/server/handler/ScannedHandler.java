package de.schnorrenbergers.automat.server.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import de.schnorrenbergers.automat.Main;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScannedHandler extends CustomHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
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
        respond(exchange, "success");
    }
}
