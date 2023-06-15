package bot.schedulebot.repositories;

import bot.schedulebot.entities.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class FileRepository extends bot.schedulebot.repositories.Repository<File> {
    @Autowired
    protected FileRepository() {}
}
