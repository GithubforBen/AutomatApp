package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.StatisticType;
import jakarta.persistence.*;

@Entity
@Table(name = "statistic")
public class Statistic {

    @Id
    @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private StatisticType type;

    @Column(name = "time")
    private Long timestamp;

    @Column(name = "data")
    private String data;

    public Statistic(String data, StatisticType type) {
        this.data = data;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public Statistic() {

    }
}
