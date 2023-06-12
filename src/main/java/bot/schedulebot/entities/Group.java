package bot.schedulebot.entities;

import bot.schedulebot.enums.Role;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@jakarta.persistence.Entity
@Getter
@Setter
@Table(name = "groups")
@EqualsAndHashCode
public class Group implements Serializable, Entity {
    @Id
    //@GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "code")
    private String code;

    @OneToMany(mappedBy = "group", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<Task> tasks;

    @OneToMany(mappedBy = "group", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<Notification> repeatedNotifications;

    @OneToMany(mappedBy = "group", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<Announcement> announcements;

    @OneToMany(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "groups_impinf",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "Impinf_id", unique = true))
    private List<Announcement> importantInfo;

    @OneToMany(mappedBy = "group", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<Subject> subjects;

    @ElementCollection()
    @Enumerated(EnumType.STRING)
    @MapKeyJoinColumn(name = "users_id", referencedColumnName = "id")
    @CollectionTable(name = "groups_user_roles")
    private Map<User, Role> userRoles;

    @ManyToMany(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "groups_users",
            inverseJoinColumns = @JoinColumn(name = "users_id"),
            joinColumns = @JoinColumn(name = "groups_id"))
    private List<User> users;

    @OneToMany(mappedBy = "group", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "group", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @EqualsAndHashCode.Exclude
    private List<UnappointedTask> unappointedTasks;
}
