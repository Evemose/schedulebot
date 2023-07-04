package bot.schedulebot.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "unappointed_tasks")
@EqualsAndHashCode
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class UnappointedTask implements Entity, Serializable {
    @Id
    //@GeneratedValue
    @Column(name = "id")
    private int id;

    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "tasks_unappointed_tasks",
            inverseJoinColumns = @JoinColumn(name = "task_id"),
            joinColumns = @JoinColumn(name = "unappointed_task_id", unique = true))
    @EqualsAndHashCode.Exclude
    private Task task;

    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "groups_unappointed_tasks",
            inverseJoinColumns = @JoinColumn(name = "group_id"),
            joinColumns = @JoinColumn(name = "unappointed_task_id", unique = true))
    private Group group;

    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "users_unappointed_tasks",
            joinColumns = @JoinColumn(name = "unappointed_task_id", unique = true),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private User user;

    public UnappointedTask() {
    }

    public UnappointedTask(Appointment appointment) {
        this.task = appointment.getTask();
        this.group = appointment.getGroup();
        this.user = appointment.getUser();
    }

    public UnappointedTask(Task task) {
        this.task = task;
        this.group = task.getGroup();
    }

    public String toString() {
        return task.toString();
    }
}
