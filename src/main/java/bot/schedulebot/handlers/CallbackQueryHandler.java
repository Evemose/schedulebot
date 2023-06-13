package bot.schedulebot.handlers;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.entities.TodayTasksInfo;
import bot.schedulebot.entities.User;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.enums.TodayTasksInfoMode;
import bot.schedulebot.objectsunderconstruction.AppointmentsUnderConstruction;
import bot.schedulebot.repositories.TodayTasksInfoRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.services.*;
import bot.schedulebot.storages.menustorages.GroupMenuStorage;
import bot.schedulebot.storages.menustorages.MenuStorage;
import bot.schedulebot.util.ParseUtil;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@Controller
public class CallbackQueryHandler {
    private final UserRepository userRepository;
    private final MenuStorage menuStorage;
    private final MainService mainService;
    private final ParseUtil parseUtil;
    private final AppointmentsUnderConstruction appointmentsUnderConstruction;
    private final BotConfig botConfig;
    private final GroupService groupService;
    private final AppointmentService appointmentService;
    private final TaskService taskService;
    private final AnnouncementService announcementService;
    private final TodayTasksInfoRepository todayTasksInfoRepository;
    private final NotificationService notificationService;
    private final SubjectService subjectService;
    private final GroupMenuStorage groupMenuStorage;

    public CallbackQueryHandler(UserRepository userRepository, MenuStorage menuStorage, MainService mainService, ParseUtil parseUtil, AppointmentsUnderConstruction appointmentsUnderConstruction, GroupService groupService, AppointmentService appointmentService, TaskService taskService, AnnouncementService announcementService, TodayTasksInfoRepository todayTasksInfoRepository, NotificationService notificationService, SubjectService subjectService, GroupMenuStorage groupMenuStorage) {
        this.userRepository = userRepository;
        this.menuStorage = menuStorage;
        this.mainService = mainService;
        this.parseUtil = parseUtil;
        this.appointmentsUnderConstruction = appointmentsUnderConstruction;
        this.botConfig = new BotConfig();
        this.groupService = groupService;
        this.appointmentService = appointmentService;
        this.taskService = taskService;
        this.announcementService = announcementService;
        this.todayTasksInfoRepository = todayTasksInfoRepository;
        this.notificationService = notificationService;
        this.subjectService = subjectService;
        this.groupMenuStorage = groupMenuStorage;
    }

    List<Message> handleCallbackQuery(Update update) {
        List<Message> resultMessagesList = new ArrayList<>();
        String callbackData = update.getCallbackQuery().getData();
        User u = userRepository.get(update.getCallbackQuery().getFrom().getUserName());
        if (!(callbackData.startsWith("Task") || (callbackData.startsWith("Announcement")))) {
            u.setInstanceAdditionStage(InstanceAdditionStage.NONE);
            userRepository.update(u);
        }
        switch (callbackData) {
            case "Show groups menu" -> {
                resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUPS_MENU, update, userRepository.get(parseUtil.getTag(update)).getId()));
            }
            case "Show users groups" -> {
                resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUPS_INDEX_MENU, update));
            }
            case "Create group" -> {
                resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.GROUP_START, update, null));
            }
            case "Show group join menu" -> {
                groupMenuStorage.handleGroupJoinMenuProvision(update, resultMessagesList);
            }
            case "Task appointment yes" -> {
                resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.APPOINTMENT_DATE, update, null));
            }
            case "Task appointment no" -> {
                appointmentsUnderConstruction.getObjectsUnderConstructions().get(update.getCallbackQuery().getFrom().getUserName()).setId(-1);
                resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.APPOINTMENT_START, update, null));
            }
            case "Task set image no" -> {
                resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.TASK_NAME_NO_IMAGE, update, null));
            }
            case "Task set image yes" -> {
                taskService.handleTaskSetImageYes(resultMessagesList, u);
            }
            case "Task set document yes" -> {
                taskService.handleTaskSetDocumentYes(resultMessagesList, u);
            }
            case "Task set document no" -> {
                resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.TASK_IMAGE_NO_FILE, update, null));
            }
            case "Announcement set image no", "Announcement set document no", "Announcement set document yes", "Announcement set image yes" -> {
                mainService.handleAddition(InstanceAdditionStage.ANNOUNCEMENT_TITLE, update, null);
            }
            case "Delete this" -> {
                botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
            }
            default -> {
                if (callbackData.matches("Show group \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_MENU, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Show announcements menu in group \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_ANNOUNCEMENTS_MENU_MANAGE, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Show main menu of \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.MAIN_MENU, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Show people in group \\d+")) {
                    u.setGroupMode(true);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_USERS_LIST, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Show appointments in group \\d+")) {
                    u.setGroupMode(true);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_APPOINTMENTS_MENU, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Show unappointed tasks in group \\d+")) {
                    u.setGroupMode(true);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_UNAPPOINTED_TASKS_MENU, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.startsWith("Add subject")) {
                    subjectService.handleAdditionStart(update);
                } else if (callbackData.matches("Add task to group \\d+")) {
                    taskService.handleTaskAdditionToGroup(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Add appointment to \\d+")) {
                    appointmentService.handleAppointmentAdditionInitiation(update, resultMessagesList, u);
                } else if (callbackData.matches("Show subjects in group \\d+")) {
                    u.setGroupMode(true);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.GET_GROUP_SUBJECTS, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.startsWith("Show list to delete subject")) {
                    u.setGroupMode(callbackData.matches("Show list to delete subject in group \\d+"));
                    userRepository.update(u);
                    if (u.isGroupMode()) {
                        resultMessagesList.add(menuStorage.getMenu(MenuMode.DELETE_GROUP_SUBJECT, update, parseUtil.getTargetId(callbackData)));
                    } else {
                        resultMessagesList.add(menuStorage.getMenu(MenuMode.DELETE_USER_SUBJECT, update));
                    }
                } else if (callbackData.startsWith("Delete subject")) {
                    subjectService.handleSubjectDeletion(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Add admin to \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.LIST_TO_ADD_ADMIN, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Add admin \\d+ to \\d+")) {
                    groupService.handleAdminAddition(update, resultMessagesList, callbackData);
                } else if (callbackData.matches("Remove admin \\d+ to \\d+")) {
                    groupService.handleAdminRemoval(update, resultMessagesList, callbackData);
                } else if (callbackData.matches("Kick user from \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.KICK_USER, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Kick user \\d+ to \\d+")) {
                    groupService.handleUserKick(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Transfer ownership in \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.TRANSFER_OWNERSHIP, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Transfer ownership of \\d+ to \\d+")) {
                    groupService.handleOwnershipTransfer(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Set subject \\d+")) {
                    resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.TASK_SUBJECT, update, null));
                } else if (callbackData.matches("Leave group \\d+")) {
                    groupService.handleGroupLeave(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Show group code \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_GROUP_CODE, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.startsWith("Show unappointed task ")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_UNAPPOINTED_TASK, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Appoint unappointed task in group \\d+")) {
                    u.setGroupMode(true);
                    userRepository.update(u);
                    resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.APPOINTMENT_START, update, null));
                } else if (callbackData.matches("Appoint unappointed task of user \\d+")) {
                    u.setGroupMode(false);
                    userRepository.update(u);
                    resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.APPOINTMENT_START, update, null));
                } else if (callbackData.matches("Show unappointed tasks of \\d+")) {
                    u.setGroupMode(false);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.UNAPPOINTED_TASKS_MENU, update, 0));
                } else if (callbackData.matches("Show appointments of \\d+")) {
                    u.setGroupMode(false);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.USER_APPOINTMENTS_LIST, update, u.getId()));
                } else if (callbackData.startsWith("Show appointment ")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_APPOINTMENT, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Mark appointment as done \\d+")) {
                    appointmentService.handleMarkAppointmentAsDone(update, resultMessagesList, callbackData, u);
                } else if (callbackData.startsWith("Change appointed date")) {
                    appointmentService.handleAppointmentDateChange(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Show subjects of \\d+")) {
                    u.setGroupMode(false);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_SUBJECTS_OF_USER, update));
                } else if (callbackData.matches("Show tasks in group \\d+")) {
                    u.setGroupMode(true);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_GROUP_TASKS, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Show task \\d+.*")) {
                    if (callbackData.contains("deletethis")) {
                        botConfig.deleteMessage(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId());
                    }
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.TASK_MENU, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.startsWith("Edit")) {
                    menuStorage.handleEditMenuProvision(update, resultMessagesList, callbackData);
                } else if (callbackData.matches("Delete task \\d+")) {
                    taskService.handleTaskDeletion(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Force delete task \\d+")) {
                    taskService.handleTaskForceDeletion(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Add personal task in group \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.ADD_PERSONAL_TASK, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Show announcements in group \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_ANNOUNCEMENTS_MENU_VIEW, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Add personal task to user \\d+ to \\d+")) {
                    taskService.handlePersonalTaskAddition(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Add announcement to group \\d+")) {
                    announcementService.handleAdditionStart(u, update);
                } else if (callbackData.startsWith("Show announcement")) {
                    menuStorage.handleAnnouncementMenuProvision(update, resultMessagesList, callbackData);
                } else if (callbackData.matches("Delete announcement \\d+")) {
                    announcementService.handleAnnouncementDeletion(update, resultMessagesList, callbackData, u);
                } else if (callbackData.startsWith("Change")) {
                    mainService.handlePropertyChange(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Mark announcement as important \\d+")) {
                    announcementService.handleMarkAnnouncementAsImportant(update, resultMessagesList, callbackData);
                } else if (callbackData.matches("Show important info in group \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.GROUP_IMPORTANT_INFO, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Remove announcement from important \\d+")) {
                    announcementService.removeAnnouncementFromImportant(parseUtil.getTargetId(callbackData));
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.ANNOUNCEMENT_MENU_MANAGE, update, parseUtil.getTargetId(callbackData)));
                    Message message = new Message();
                    message.setText("Announcement removed from important");
                    resultMessagesList.add(0, message);
                } else if (callbackData.matches("Show appointments for today \\d+")) {
                    TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(parseUtil.getTargetId(callbackData));
                    todayTasksInfo.setMode(TodayTasksInfoMode.APPOINTMENTS_FOR_TODAY);
                    todayTasksInfoRepository.update(todayTasksInfo);
                    menuStorage.getMenu(MenuMode.APPOINMENTS_FOR_TODAY, update, parseUtil.getTargetId(callbackData));
                } else if (callbackData.matches("Show appointments with deadline today \\d+")) {
                    TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(parseUtil.getTargetId(callbackData));
                    todayTasksInfo.setMode(TodayTasksInfoMode.APPOINTMENTS_WITH_DEADLINE_TODAY);
                    todayTasksInfoRepository.update(todayTasksInfo);
                    menuStorage.getMenu(MenuMode.APPOINTMENTS_WITH_DEADLINE_TODAY, update, parseUtil.getTargetId(callbackData));
                } else if (callbackData.matches("Show tasks for today \\d+")) {
                    TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(parseUtil.getTargetId(callbackData));
                    todayTasksInfo.setMode(TodayTasksInfoMode.MAIN);
                    todayTasksInfoRepository.update(todayTasksInfo);
                    menuStorage.getMenu(MenuMode.TASKS_FOR_TODAY, update, parseUtil.getTargetId(callbackData));
                } else if (callbackData.matches("Show unappointed tasks with deadline today \\d+")) {
                    TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(parseUtil.getTargetId(callbackData));
                    todayTasksInfo.setMode(TodayTasksInfoMode.UNAPPOINTED_TASKS_WITH_DEADLINE_TODAY);
                    todayTasksInfoRepository.update(todayTasksInfo);
                    menuStorage.getMenu(MenuMode.UNAPPOINTED_TASKS_WITH_DEADLINE_TODAY, update, parseUtil.getTargetId(callbackData));
                } else if (callbackData.matches("Show outdated unappointed tasks \\d+")) {
                    TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(parseUtil.getTargetId(callbackData));
                    todayTasksInfo.setMode(TodayTasksInfoMode.OUTDATED_UNAPPOINTED_TASKS);
                    todayTasksInfoRepository.update(todayTasksInfo);
                    menuStorage.getMenu(MenuMode.OUTDATED_UNAPPOINTED_TASKS, update, parseUtil.getTargetId(callbackData));
                } else if (callbackData.matches("Show outdated appointments \\d+")) {
                    TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(parseUtil.getTargetId(callbackData));
                    todayTasksInfo.setMode(TodayTasksInfoMode.OUTDATED_APPOINTMENTS);
                    todayTasksInfoRepository.update(todayTasksInfo);
                    menuStorage.getMenu(MenuMode.OUTDATED_APPOINTMENTS, update, parseUtil.getTargetId(callbackData));
                } else if (callbackData.startsWith("Appoint for deadline")) {
                    u.setGroupMode(callbackData.contains("(group)"));
                    userRepository.update(u);
                    appointmentService.appointTaskForDeadline(resultMessagesList, update);
                } else if (callbackData.startsWith("Postpone for tomorrow")) {
                    u.setGroupMode(callbackData.contains("(group)"));
                    userRepository.update(u);
                    appointmentService.appointTaskForTomorrow(update);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.SET_APPOINTMENT_DATE, update));
                } else if (callbackData.matches("Show notifications in group \\d+")) {
                    u.setGroupMode(true);
                    userRepository.update(u);
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_NOTIFICATIONS_IN_GROUP_MENU, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Add notification to \\d+")) {
                    u.setGroupMode(true);
                    userRepository.update(u);
                    resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.NOTIFICATION_START, update, null));
                } else if (callbackData.matches("Notification \\d{4}-\\d{2}-\\d{2}")) {
                    resultMessagesList.addAll(mainService.handleAddition(InstanceAdditionStage.NOTIFICATION_DATE, update, null));
                } else if (callbackData.matches("Show notification \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.SHOW_NOTIFICATION_MENU, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Delete notification \\d+")) {
                    notificationService.handleNotificationDelete(callbackData, update, resultMessagesList, u);
                } else if (callbackData.matches("Turn off notifications for \\d+")) {
                    u.setWantToGenNotifications(false);
                    userRepository.update(u);
                    botConfig.editMessageText(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId(), menuStorage.getMenu(MenuMode.MAIN_MENU, update, u.getId()));
                } else if (callbackData.matches("Turn on notifications for \\d+")) {
                    u.setWantToGenNotifications(true);
                    userRepository.update(u);
                    botConfig.editMessageText(u.getChatId(), update.getCallbackQuery().getMessage().getMessageId(), menuStorage.getMenu(MenuMode.MAIN_MENU, update, u.getId()));
                } else if (callbackData.matches("Delete group \\d+")) {
                    groupService.handleGroupDelete(update, resultMessagesList, callbackData, u);
                } else if (callbackData.matches("Remove admin from \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.LIST_TO_REMOVE_ADMIN, update, parseUtil.getTargetId(callbackData)));
                } else if (callbackData.matches("Manage admins \\d+")) {
                    resultMessagesList.add(menuStorage.getMenu(MenuMode.MANAGE_ADMINS_MENU, update, parseUtil.getTargetId(callbackData)));
                } else throw new RuntimeException("Wrong inline keyboard command:" + callbackData);
            }
        }
        return resultMessagesList;
    }
}
