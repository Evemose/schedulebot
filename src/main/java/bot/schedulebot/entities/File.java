package bot.schedulebot.entities;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Getter
@Setter
public class File implements Serializable, bot.schedulebot.entities.Entity {
    @Id
    @Column(name = "id")
    private int id;
    @Column(name = "file_type")
    private String fileType;
    @Lob
    private byte[] file;

    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "announcements_files",
            joinColumns = @JoinColumn(name = "file_id"),
            inverseJoinColumns = @JoinColumn(name = "announcement_id"))
    private Announcement announcement;

    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "tasks_files",
            joinColumns = @JoinColumn(name = "file_id"),
            inverseJoinColumns = @JoinColumn(name = "task_id"))
    private Task task;
}
