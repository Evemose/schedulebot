package bot.schedulebot.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
}
