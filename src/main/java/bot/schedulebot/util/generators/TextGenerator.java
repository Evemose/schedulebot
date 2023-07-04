package bot.schedulebot.util.generators;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.*;
import bot.schedulebot.repositories.TaskRepository;
import bot.schedulebot.repositories.UserRepository;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TextGenerator {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;


    private TextGenerator(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    public String getUsersWithTask(int taskId) {
        StringBuilder res = new StringBuilder();
        Session session = HibernateConfig.getSession();
        List<User> users = userRepository.getAll(session);
        for (User user : users) {
            if (user.getAppointments().stream().anyMatch(appointment -> appointment.getTask().getId() == taskId)
                    || user.getUnappointedTasks().stream().anyMatch(unappointedTask -> unappointedTask.getTask().getId() == taskId)) {
                res.append(user.getName()).append("(@").append(user.getTag()).append(")\n");
            }
        }
        return res.toString();
    }

    public String getMessageTextFromAppointmentsList(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return "*You dont have appointments there*";
        }
        StringBuilder text = new StringBuilder("*Your appointments*\n\n");

        for (Appointment appointment :
                appointments) {
            text.append(appointment.getTask().getName()).append("\nAppointed on: ").append(appointment.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n\n");
        }

        return text.toString();
    }

    public String getMessageTextFromAnnouncementsList(List<Announcement> announcements) {
        if (announcements.isEmpty()) {
            return "*No announcements in this group*";
        }
        StringBuilder text = new StringBuilder("*Group announcements*\n\n");

        for (Announcement announcement :
                announcements) {
            text.append(announcement.getTitle()).append("\n");
        }

        return text.toString();
    }

    public String getMessageTextFromUnappointedTasksList(List<UnappointedTask> unappointedTasks) {
        if (unappointedTasks.isEmpty()) {
            return "*You dont have unappointed tasks there*";
        }
        StringBuilder text = new StringBuilder("*Your unappointed tasks*\n\n");

        for (UnappointedTask unappointedTask :
                unappointedTasks) {
            text.append(unappointedTask.getTask().getName()).append("\nDeadline on: ").append(unappointedTask.getTask().getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))).append("\n\n");
        }

        return text.toString();
    }

    public String getMessageTextFromSubjectsList(List<Subject> subjects) {
        StringBuilder text = new StringBuilder();

        if (subjects.isEmpty()) {
            return "*You dont have subjects there*";
        }

        for (Subject subject :
                subjects) {
            text.append(subject.getName()).append("\n\n");
        }

        return text.toString();
    }

    public String getAppointmentsForTodayText(TodayTasksInfo todayTasksInfo) {
        return (todayTasksInfo.getAppointmentsForToday().size() > 0 ? "*Your appointments for today*" : "*You have no appointments for today*");
    }

    public String getAppointmentsWithDeadlineTodayText(TodayTasksInfo todayTasksInfo) {
        return (todayTasksInfo.getAppointmentsForToday().size() > 0 ? "*Your appointments with deadline today*" : "*You have no appointments with deadline today*");
    }

    public String getOutdatedAppointmentsText(TodayTasksInfo todayTasksInfo) {
        return (todayTasksInfo.getAppointmentsForToday().size() > 0 ? "*Your outdated appointments" : "*You have no outdated appointments*");
    }

    public String getOutdatedUnappointedTasksText(TodayTasksInfo todayTasksInfo) {
        return (todayTasksInfo.getAppointmentsForToday().size() > 0 ? "*Your outdated unappointed tasks" : "*You have no outdated unappointed tasks*");
    }

    public String getUnappointedTasksWithDeadlineTodayText(TodayTasksInfo todayTasksInfo) {
        return (todayTasksInfo.getAppointmentsForToday().size() > 0 ? "*Your unappointed tasks with deadline today*" : "*You have no unappointed tasks with deadline today*");
    }

    public String getMessageTextFromSubject(Subject subject) {
        StringBuilder res = new StringBuilder("Name: " + subject.toString() + "\n\n");
        res.append("Tasks: ");
        for (Task task :
                subject.getTasks()) {
            res.append(task.getName()).append(", ");
        }
        return res.substring(0, res.length()-2) + '\n';
    }
}
