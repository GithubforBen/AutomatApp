package de.schnorrenbergers.automat.spring.mappings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingMapping {
    public PingMapping() {
        System.out.println("PingMapping");
    }

    @GetMapping("/ping")
    public String ping() {
        System.out.println("Ping");
        return "pong";
    }
}
