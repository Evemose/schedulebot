package bot.schedulebot.repositories;

import bot.schedulebot.entities.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository extends bot.schedulebot.repositories.Repository<Task> {
    @Autowired
    protected TaskRepository() {}
}
