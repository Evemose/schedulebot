package bot.schedulebot.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@jakarta.persistence.Entity
@EqualsAndHashCode
@Table(name = "appointments")
public class Appointment implements Serializable, Entity {
    @Id
    //@GeneratedValue
    @Column(name = "id")
    private int id;

    @JoinColumn(name = "task_id")
    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private Task task;

    @Column(name = "appointed_date")
    private LocalDate date;

    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "groups_appointments",
            joinColumns = @JoinColumn(name = "appointment_id", unique = true),
            inverseJoinColumns = @JoinColumn(name = "group_id"))
    private Group group;

    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinColumn(name = "user_id")
    @JoinTable(name = "users_appointments",
            joinColumns = @JoinColumn(name = "appointment_id", unique = true),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private User user;

    public Appointment() {
    }

    public Appointment(UnappointedTask unappointedTask) {
        this.task = unappointedTask.getTask();
        this.group = unappointedTask.getTask().getGroup();
        this.user = unappointedTask.getUser();
    }

    public String toString() {
        return task.toString() +
                "\n\n*Appointed on:* " + date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
