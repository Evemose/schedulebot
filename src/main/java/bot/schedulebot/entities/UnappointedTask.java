package bot.schedulebot.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "unappointed_tasks")
@EqualsAndHashCode
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
}
