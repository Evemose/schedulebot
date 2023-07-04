package bot.schedulebot.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@jakarta.persistence.Entity
@EqualsAndHashCode
@Table(name = "announcements")
public class Announcement implements Serializable, Entity {
    @Id
    @Column(name = "id")
    private int id;
    @Column(name = "name")
    private String title;
    @Column(name = "description")
    private String text;
    @Column(name = "image")
    @Lob
    private String image;
    @OneToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "announcements_files",
            inverseJoinColumns = @JoinColumn(name = "file_id"),
            joinColumns = @JoinColumn(name = "announcement_id"))
    private File file;
    @ManyToOne(cascade = {CascadeType.REMOVE, CascadeType.REFRESH, CascadeType.MERGE})
    @JoinTable(name = "groups_announcements",
            inverseJoinColumns = @JoinColumn(name = "group_id"),
            joinColumns = @JoinColumn(name = "announcements_id"))
    private Group group;

    public String toString() {
        return "*Title:* " + title
               + "\n\n*Announcement:* " + text;
    }
}
