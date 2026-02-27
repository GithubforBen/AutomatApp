package de.schnorrenbergers.automat.spring.mappings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingMapping {
    public PingMapping() {
        System.out.println("PingMapping");
    }

    @GetMapping({"/ping", "/api/ping"})
    public String ping() {
        System.out.println("Ping");
        return "pong";
    }

    @org.springframework.web.bind.annotation.PostMapping({"/ping", "/api/ping"})
    public String pingPost(@org.springframework.web.bind.annotation.RequestBody String body) {
        System.out.println("Ping Post: " + body);
        return "pong post";
    }
}
