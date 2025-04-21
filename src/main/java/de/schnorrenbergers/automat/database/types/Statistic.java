package de.schnorrenbergers.automat.database.types;

import de.schnorrenbergers.automat.database.types.types.StatisticType;
import jakarta.persistence.*;

import java.util.Objects;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StatisticType getType() {
        return type;
    }

    public void setType(StatisticType type) {
        this.type = type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Statistic statistic = (Statistic) o;
        return Objects.equals(getId(), statistic.getId()) && getType() == statistic.getType() && Objects.equals(getTimestamp(), statistic.getTimestamp()) && Objects.equals(getData(), statistic.getData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getType(), getTimestamp(), getData());
    }

    @Override
    public String toString() {
        return "Statistic{" +
                "id=" + id +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", data='" + data + '\'' +
                '}';
    }
}
