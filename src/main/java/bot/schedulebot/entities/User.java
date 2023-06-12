package bot.schedulebot.entities;

import bot.schedulebot.enums.InstanceAdditionStage;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@jakarta.persistence.Entity
@Table(name = "users")
@EqualsAndHashCode
public class User implements Entity, Serializable {
    @Id
    //@GeneratedValue
    @Column(name = "id")
    private int id;

    @Column(name = "chat_id", unique = true)
    private String chatId;

    @Column(name = "want_to_get_notifications")
    private boolean wantToGenNotifications;

    @Column(name = "name")
    private String name;

    @Column(name = "tag", unique = true)
    private String tag;

    @Column(name = "is_group_mode")
    private boolean isGroupMode;

    @Enumerated(EnumType.STRING)
    private InstanceAdditionStage instanceAdditionStage;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<Appointment> appointments;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<UnappointedTask> unappointedTasks;

    @ManyToMany(mappedBy = "users", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<Group> groups;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    private List<Subject> subjects;

    @OneToOne(mappedBy = "user", cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @EqualsAndHashCode.Exclude
    private TodayTasksInfo todayTasksInfo;

    /*@Enumerated(EnumType.STRING)
    @ElementCollection
    @CollectionTable(name = "gif_stages", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "gif_type")
    @Column(name = "stage")
    private Map<GifStickerType, Integer> gifStages;*/

    public User() {
        wantToGenNotifications = true;
        instanceAdditionStage = InstanceAdditionStage.NONE;
    }
}
