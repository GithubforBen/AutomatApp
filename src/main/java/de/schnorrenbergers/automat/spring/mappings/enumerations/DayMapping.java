package de.schnorrenbergers.automat.spring.mappings.enumerations;

import de.schnorrenbergers.automat.database.types.types.Day;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
public class DayMapping {

    @GetMapping({"/days", "/api/days"})
    public String ping() {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(Day.values()).map(Enum::toString).forEach((x) -> {
            sb.append(x);
            sb.append('\n');
        });
        return sb.toString();
    }
}
