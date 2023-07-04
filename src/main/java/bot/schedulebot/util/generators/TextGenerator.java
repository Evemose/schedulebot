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

    public String getAnswerTheQuestionRequest() {
        return "Please, answer question above or stop addition process by pressing any other button";
    }

    public String getStringOfUsersWithTask(int taskId) {
        String res = "";
        Session session = HibernateConfig.getSession();
        List<User> users = userRepository.getAll(session);
        for (User user : users) {
            if (user.getAppointments().stream().anyMatch(appointment -> appointment.getTask().getId() == taskId)
                    || user.getUnappointedTasks().stream().anyMatch(unappointedTask -> unappointedTask.getTask().getId() == taskId)) {
                res += user.getName() + "(@" + user.getTag() + ")\n";
            }
        }
        return res;
    }

    public String getMessageTextFromAppointmentsList(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return "*You dont have appointments there*";
        }
        String text = "*Your appointments*\n\n";

        for (Appointment appointment :
                appointments) {
            text += appointment.getTask().getName() + "\nAppointed on: " + appointment.getAppointedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n";
        }

        return text;
    }

    public String getMessageTextFromAnnouncementsList(List<Announcement> announcements) {
        if (announcements.isEmpty()) {
            return "*No announcements in this group*";
        }
        String text = "*Group announcements*\n\n";

        for (Announcement announcement :
                announcements) {
            text += announcement.getTitle() + "\n";
        }

        return text;
    }

    public String getMessageTextFromUnappointedTasksList(List<UnappointedTask> unappointedTasks) {
        if (unappointedTasks.isEmpty()) {
            return "*You dont have unappointed tasks there*";
        }
        String text = "*Your unappointed tasks*\n\n";

        for (UnappointedTask unappointedTask :
                unappointedTasks) {
            text += unappointedTask.getTask().getName() + "\nDeadline on: " + unappointedTask.getTask().getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "\n\n";
        }

        return text;
    }

    public String getMessageTextFromSubjectsList(List<Subject> subjects) {
        String text = "";

        if (subjects.isEmpty()) {
            return "*You dont have subjects there*";
        }

        for (Subject subject :
                subjects) {
            text += subject.getName() + "\n\n";
        }

        return text;
    }

    public String getMessageTextFromTodayTasksInfo(TodayTasksInfo todayTasksInfo) {
        return ("For today you have:\n\n"
                + todayTasksInfo.getAppointmentsForToday().size() + " appointment" + (todayTasksInfo.getAppointmentsForToday().size() != 1 ? "s" : "") + " for today\n\n"
                + todayTasksInfo.getAppointmentsWithDeadlineToday().size() + " task" + (todayTasksInfo.getAppointmentsForToday().size() != 1 ? "s" : "") + " that you have appointed for later, but " + (todayTasksInfo.getAppointmentsForToday().size() != 1 ? "their" : "its") + " deadline is today\n\n"
                + todayTasksInfo.getUnappointedTasksWithDeadlineToday().size() + " task" + (todayTasksInfo.getAppointmentsForToday().size() != 1 ? "s" : "") + ", that you have not appointed for any date, but their deadline is today\n\n"
                + todayTasksInfo.getOutdatedUnappointedTasks().size() + " outdated task" + (todayTasksInfo.getAppointmentsForToday().size() != 1 ? "s" : "") + ", that you have not appointed for any date\n\n")
                + todayTasksInfo.getOutdatedAppointments().size() + " outdated appointment" + (todayTasksInfo.getAppointmentsForToday().size() != 1 ? "s" : "");
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

    public String getAppointmentMenuText(Appointment appointment) {
        Session session = HibernateConfig.getSession();
        Task task = taskRepository.get(appointment.getTask().getId(), session);
        String res = "*Name:* " + task.getName() +
                "\n\n*Subject:* " + task.getSubject().getName() +
                "\n\n*Description:* " + task.getDescription() +
                "\n\n*Deadline:* " + task.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) +
                "\n\n*Appointed on:* " + appointment.getAppointedDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        session.close();
        return res;
    }

    public String getTaskMenuText(int taskId) {
        Session session = HibernateConfig.getSession();
        Task task = taskRepository.get(taskId, session);
        String res = "*Name:* " + task.getName() +
                "\n\n*Subject:* " + task.getSubject().getName() +
                "\n\n*Description:* " + task.getDescription() +
                "\n\n*Deadline:* " + task.getDeadline().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        session.close();
        return res;
    }

    public String getNotificationMenuText(Notification notification) {
        String res = "*Text:* " + notification.getText().replace("*", "\\*") +
                "\n\n*Time:* " + notification.getTime() +
                "\n\n*Frequency:* " + notification.getFrequency() +
                "\n\n*Next notification date:* " + notification.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        return res;
    }

    public String getMessageTextFromSubject(Subject subject) {
        StringBuilder res = new StringBuilder("Name: ");
        res.append(subject.getName()).append("\n\n");
        res.append("Tasks: ");
        for (Task task :
                subject.getTasks()) {
            res.append(task.getName()).append(", ");
        }
        return res.substring(0, res.length()-2) + '\n';
    }
}
