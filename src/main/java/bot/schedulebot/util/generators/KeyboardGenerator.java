package bot.schedulebot.util.generators;

import bot.schedulebot.entities.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardGenerator {
    private KeyboardGenerator() {

    }

    public List<List<InlineKeyboardButton>> getKeyboardFromAppointmentsList(List<Appointment> appointments, int id, boolean isGroupMode) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        appointments.stream().forEach(appointment -> {
            keyboard.add(createSingleButtonRow(
                    appointment.getTask().getName(),
                    "Show appointment " + appointment.getId()));
        });
        keyboard.add(createSingleButtonRow(
                "Back",
                (isGroupMode ? "Show group " : "Show main menu of ") + id));
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getKeyboardFromUnappointedTasksList(List<UnappointedTask> unappointedTasks, int id, boolean isGroup) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        unappointedTasks.stream().forEach(unappointedTask -> {
            keyboard.add(createSingleButtonRow(
                    unappointedTask.getTask().getName(),
                    "Show unappointed task " + unappointedTask.getId()));
        });
        keyboard.add(createSingleButtonRow(
                "Back",
                (isGroup ? "Show group " : "Show main menu of ") + id));
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getKeyboardFromSubjectsList(List<Subject> subjects, int id, boolean isGroup) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        subjects.stream().forEach(subject -> {
            keyboard.add(createSingleButtonRow(
                    subject.getName(),
                    "Show subject " + subject.getId()));
        });
        keyboard.add(createSingleButtonRow(
                "Back",
                (isGroup ? "Show group " : "Show main menu of ") + id));
        return keyboard;
    }

    public List<InlineKeyboardButton> createSingleButtonRow(String text, String callbackData) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton groupButton = new InlineKeyboardButton(text);
        groupButton.setCallbackData(callbackData);
        row.add(groupButton);
        return row;
    }

    public List<InlineKeyboardButton> createManyButtonsRow(List<String> text, List<String> callbackData) {
        List<InlineKeyboardButton> row = createSingleButtonRow(text.get(0), callbackData.get(0));
        for (int i = 1; i < text.size(); i++) {
            InlineKeyboardButton groupButton = new InlineKeyboardButton(text.get(i));
            groupButton.setCallbackData(callbackData.get(i));
            row.add(groupButton);
        }
        return row;
    }

    public List<List<InlineKeyboardButton>> getEverydayTaskNotificationKeyboard(TodayTasksInfo todayTasksInfo) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createSingleButtonRow("See appointments for today", "Show appointments for today " + todayTasksInfo.getId()));
        keyboard.add(createSingleButtonRow("See appointments with deadline today", "Show appointments with deadline today " + todayTasksInfo.getId()));
        keyboard.add(createSingleButtonRow("See unappointed tasks with deadline today", "Show unappointed tasks with deadline today " + todayTasksInfo.getId()));
        keyboard.add(createSingleButtonRow("See outdated unappointed tasks", "Show outdated unappointed tasks " + todayTasksInfo.getId()));
        keyboard.add(createSingleButtonRow("See outdated appointments", "Show outdated appointments " + todayTasksInfo.getId()));

        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getAppointmentsForTodayKeyboard(TodayTasksInfo todayTasksInfo) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        todayTasksInfo.getAppointmentsForToday().stream().forEach(appointment -> {
            keyboard.add(createSingleButtonRow(appointment.getTask().getName(), "Show appointment (noback) " + appointment.getId()));
        });
        keyboard.add(createSingleButtonRow("Back", "Show tasks for today " + todayTasksInfo.getId()));

        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getAppointmentsWithDeadlineTodayKeyboard(TodayTasksInfo todayTasksInfo) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        todayTasksInfo.getAppointmentsWithDeadlineToday().stream().forEach(appointment -> {
            keyboard.add(createSingleButtonRow(appointment.getTask().getName(), "Show appointment (noback) " + appointment.getId()));
        });
        keyboard.add(createSingleButtonRow("Back", "Show tasks for today " + todayTasksInfo.getId()));

        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getOutdatedAppointmentsKeyboard(TodayTasksInfo todayTasksInfo) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        todayTasksInfo.getOutdatedAppointments().stream().forEach(appointment -> {
            keyboard.add(createSingleButtonRow(appointment.getTask().getName(), "Show appointment (noback) " + appointment.getId()));
        });
        keyboard.add(createSingleButtonRow("Back", "Show tasks for today " + todayTasksInfo.getId()));

        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getUnappointedTaskKeyboard(UnappointedTask unappointedTask, boolean hasBackButton) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow("Appoint", "Appoint unappointed task " +
                (unappointedTask.getUser().isGroupMode() ? "in group " : "of user ") + unappointedTask.getId()));
        keyboard.add(createSingleButtonRow("Appoint for deadline", "Appoint for deadline " +
                (unappointedTask.getUser().isGroupMode() ? "(group) " : "(user) ") + unappointedTask.getId()));
        if (hasBackButton)
            keyboard.add(createSingleButtonRow("Back", "Show unappointed tasks " + (unappointedTask.getUser().isGroupMode() ? "in group " + unappointedTask.getGroup().getId() : "of " + unappointedTask.getUser().getId())));
        else keyboard.addAll(getDeleteButtonKeyboard());
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getUnappointedTasksWithDeadlineTodayKeyboard(TodayTasksInfo todayTasksInfo) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        todayTasksInfo.getUnappointedTasksWithDeadlineToday().stream().forEach(unappointedTask -> {
            keyboard.add(createSingleButtonRow(unappointedTask.getTask().getName(), "Show unappointed task (noback) " + unappointedTask.getId()));
        });
        keyboard.add(createSingleButtonRow("Back", "Show tasks for today " + todayTasksInfo.getId()));

        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getForceDeleteTaskKeyboard(Task task) {
        List<String> text = new ArrayList<>();
        List<String> callback = new ArrayList<>();
        text.add("Yes");
        text.add("No");
        callback.add("Force delete task " + task.getId());
        callback.add("Show task " + task.getId() + " (deletethis)");
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createManyButtonsRow(text, callback));
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getNextSevenDaysKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        String dayName;
        for (int i = 0; i < 7; i++) {
            dayName = LocalDate.now().plusDays(i).getDayOfWeek().toString();
            dayName = dayName.toLowerCase();
            dayName = dayName.replaceFirst(Character.toString(dayName.charAt(0)), Character.toString(dayName.charAt(0)).toUpperCase());
            keyboard.add(createSingleButtonRow(dayName,
                    "Notification " + LocalDate.now().plusDays(i)));
        }
        keyboard.get(0).get(0).setText(keyboard.get(0).get(0).getText() + " (today)");
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getAppointmentKeyboard(Appointment appointment, boolean hasBackButton, boolean isGroupMode) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        keyboard.add(createSingleButtonRow("Mark as done", "Mark appointment as done " + appointment.getId()));
        keyboard.add(createSingleButtonRow("Change appointed date", "Change appointed date " + (appointment.getUser().isGroupMode() ? "(group) " : "(user) ") + appointment.getId()));
        if (!appointment.getTask().getDeadline().isBefore(LocalDate.now()))
            keyboard.add(createSingleButtonRow("Appoint for deadline", "Appoint for deadline " + (appointment.getUser().isGroupMode() ? "(group) " : "(user) ") + appointment.getId()));
        keyboard.add(createSingleButtonRow((appointment.getAppointedDate().equals(LocalDate.now()) ? "Postpone " : "Appoint ") + "for tomorrow", "Postpone for tomorrow (group) " + appointment.getId()));
        if (hasBackButton)
            keyboard.add(createSingleButtonRow("Back", "Show appointments " + (appointment.getUser().isGroupMode() ? "in group " + appointment.getGroup().getId() : "of " + appointment.getUser().getId())));
        else keyboard.addAll(getDeleteButtonKeyboard());
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getTaskEditKeyboard(int taskId) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow("Change name", "Change task name " + taskId));
        keyboard.add(createSingleButtonRow("Change description", "Change task description " + taskId));
        keyboard.add(createSingleButtonRow("Change task deadline", "Change task deadline " + taskId));
        keyboard.add(createSingleButtonRow("Change task image", "Change task image " + taskId));
        keyboard.add(createSingleButtonRow("Change task file", "Change task file " + taskId));
        keyboard.add(createSingleButtonRow("Back", "Show task " + taskId));
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getNotificationEditKeyboard(int id) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow("Edit text", "Change notification text " + id));
        keyboard.add(createSingleButtonRow("Edit time", "Change notification time " + id));
        keyboard.add(createSingleButtonRow("Edit frequency", "Change notification frequency " + id));
        keyboard.add(createSingleButtonRow("Edit next date", "Change notification date " + id));
        keyboard.add(createSingleButtonRow("Back", "Show notification " + id));
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getNotificationMenuKeyboard(Notification notification) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow("Edit", "Edit notification " + notification.getId()));
        keyboard.add(createSingleButtonRow("Delete", "Delete notification " + notification.getId()));
        keyboard.add(createSingleButtonRow("Back", "Show notifications in group " + notification.getGroup().getId()));
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getDeleteButtonKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(createSingleButtonRow("Delete this message", "Delete this"));
        return keyboard;
    }

    public List<List<InlineKeyboardButton>> getOutdatedUnappointedTasksKeyboard(TodayTasksInfo todayTasksInfo) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        todayTasksInfo.getOutdatedUnappointedTasks().stream().forEach(unappointedTask -> {
            keyboard.add(createSingleButtonRow(unappointedTask.getTask().getName(), "Show appointment (noback) " + unappointedTask.getId()));
        });
        keyboard.add(createSingleButtonRow("Back", "Show tasks for today " + todayTasksInfo.getId()));

        return keyboard;
    }
}
