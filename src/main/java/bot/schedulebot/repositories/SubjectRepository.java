package bot.schedulebot.repositories;

import bot.schedulebot.entities.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SubjectRepository extends bot.schedulebot.repositories.Repository<Subject> {
    @Autowired
    protected SubjectRepository() {}
}
