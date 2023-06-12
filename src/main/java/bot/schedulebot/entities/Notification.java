package bot.schedulebot.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@jakarta.persistence.Entity
@Getter
@Setter
@EqualsAndHashCode
public class Notification implements Serializable, Entity {
    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "text")
    private String text;

    @Column(name = "title")
    private String title;

    @Column(name = "notif_date")
    private LocalDate notificationDate;

    @Column(name = "notif_time")
    private LocalTime notificationTime;

    @Column(name = "frequency")
    private int frequency;

    @ManyToOne(cascade = {CascadeType.MERGE, CascadeType.REFRESH, CascadeType.REMOVE})
    @JoinTable(name = "groups_notifications",
            inverseJoinColumns = @JoinColumn(name = "group_id"),
            joinColumns = @JoinColumn(name = "notification_id", unique = true))
    private Group group;
}
