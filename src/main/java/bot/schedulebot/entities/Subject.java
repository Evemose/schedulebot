package bot.schedulebot.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "subjects")
@EqualsAndHashCode
public class Subject implements Entity, Serializable {
    @Id
    //@GeneratedValue
    @Column(name = "id")
    private int id;
    @Column(name = "name")
    private String name;

    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "groups_subjects",
            inverseJoinColumns = @JoinColumn(name = "group_id"),
            joinColumns = @JoinColumn(name = "subjects_id", unique = true))
    private Group group;

    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "users_subjects",
            inverseJoinColumns = @JoinColumn(name = "user_id"),
            joinColumns = @JoinColumn(name = "subjects_id", unique = true))
    private User user;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Task> tasks;

    public String toString() {
        return name;
    }
}
