package de.schnorrenbergers.automat.spring;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication(scanBasePackages = "de.schnorrenbergers.automat")
public class SpringApi {
    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        System.out.println("Spring boot Started!");
    }
}
