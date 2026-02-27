package de.schnorrenbergers.automat.spring.controllers;

import de.schnorrenbergers.automat.Main;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/energetics")
public class EnergeticsController {

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> energetics() {
        Main.getInstance().getScreenSaver().setLastMove(0);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("200");
    }
}
