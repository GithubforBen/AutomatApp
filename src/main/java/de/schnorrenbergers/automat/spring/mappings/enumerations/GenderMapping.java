package de.schnorrenbergers.automat.spring.mappings.enumerations;

import de.schnorrenbergers.automat.database.types.types.Gender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
public class GenderMapping {

    @GetMapping({"/genders", "/api/genders"})
    public String ping() {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(Gender.values()).map(Enum::toString).forEach((x) -> {
            sb.append(x);
            sb.append('\n');
        });
        return sb.toString();
    }
}
