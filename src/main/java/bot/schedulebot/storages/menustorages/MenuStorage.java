package bot.schedulebot.storages.menustorages;

import bot.schedulebot.config.BotConfig;
import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.TodayTasksInfo;
import bot.schedulebot.enums.InstanceAdditionStage;
import bot.schedulebot.enums.MenuMode;
import bot.schedulebot.enums.Role;
import bot.schedulebot.repositories.TodayTasksInfoRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.storages.supportingmessagesstorages.InstanceAdditionSupportingMessagesStorage;
import bot.schedulebot.util.ParseUtil;
import org.hibernate.Session;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Controller
public class MenuStorage {
    private final GroupMenuStorage groupMenuStorage;
    private final UnappointedTaskMenuStorage unappointedTaskMenuStorage;
    private final InstanceAdditionSupportingMessagesStorage instanceAdditionSupportingMessagesStorage;
    private final ParseUtil parseUtil;
    private final AppointmentMenuStorage appointmentMenuStorage;
    private final SubjectMenuStorage subjectMenuStorage;
    private final UserRepository userRepository;
    private final TaskMenuStorage taskMenuStorage;
    private final AnnouncementMenuStorage announcementMenuStorage;
    private final TodayTasksInfoRepository todayTasksInfoRepository;
    private final GeneralMenuStorage generalMenuStorage;
    private final NotificationMenuStorage notificationMenuStorage;

    public MenuStorage(GroupMenuStorage groupMenuStorage, UnappointedTaskMenuStorage unappointedTaskMenuStorage, InstanceAdditionSupportingMessagesStorage instanceAdditionSupportingMessagesStorage, ParseUtil parseUtil, AppointmentMenuStorage appointmentMenuStorage, SubjectMenuStorage subjectMenuStorage, UserRepository userRepository, TaskMenuStorage taskMenuStorage, AnnouncementMenuStorage announcementMenuStorage, TodayTasksInfoRepository todayTasksInfoRepository, GeneralMenuStorage generalMenuStorage, NotificationMenuStorage notificationMenuStorage) {
        this.groupMenuStorage = groupMenuStorage;
        this.unappointedTaskMenuStorage = unappointedTaskMenuStorage;
        this.instanceAdditionSupportingMessagesStorage = instanceAdditionSupportingMessagesStorage;
        this.parseUtil = parseUtil;
        this.appointmentMenuStorage = appointmentMenuStorage;
        this.subjectMenuStorage = subjectMenuStorage;
        this.userRepository = userRepository;
        this.taskMenuStorage = taskMenuStorage;
        this.announcementMenuStorage = announcementMenuStorage;
        this.todayTasksInfoRepository = todayTasksInfoRepository;
        this.generalMenuStorage = generalMenuStorage;
        this.notificationMenuStorage = notificationMenuStorage;
    }

    public Message getMenu(MenuMode menuMode, Update update) {
        switch (menuMode) {
            case GROUPS_INDEX_MENU -> {
                return groupMenuStorage.getGroupsIndexMenu(parseUtil.getTag(update));
            }
            case SET_TASK_FILE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_FILE, update);
            }
            case NO_SUBJECTS_FOR_TASK_MENU -> {
                return subjectMenuStorage.getNoSubjectsForTaskMenu(update);
            }
            case NO_SUBJECTS_FOR_PERSONAL_APPOINTMENT -> {
                return subjectMenuStorage.getNoSubjectForPersonalAppointmentMenu(update);
            }
            case SET_SUBJECT_NAME -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.SUBJECT_NAME, update);
            }
            case SUBJECT_START -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.SUBJECT_START, update);
            }
            case CREATE_GROUP_FORM -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.GROUP_START, update);
            }
            case SET_GROUP_NAME -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.GROUP_NAME, update);
            }
            case TASK_START -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_START, update);
            }
            case SET_TASK_IMAGE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_IMAGE, update);
            }
            case SET_TASK_NAME_SKIP_IMAGE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_NAME_NO_IMAGE, update);
            }
            case SET_TASK_NAME -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_NAME, update);
            }
            case SET_TASK_DESCRIPTION -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_DESCRIPTION, update);
            }
            case SET_TASK_SUBJECT -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_SUBJECT, update);
            }
            case SET_TASK_DEADLINE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_DEADLINE, update);
            }
            case JOIN_GROUP -> {
                return groupMenuStorage.getGroupJoinMenu();
            }
            case APPOINT_TASK -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.APPOINTMENT_START, update);
            }
            case SET_APPOINTMENT_DATE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.APPOINTMENT_DATE, update);
            }
            case SHOW_SUBJECTS_OF_USER -> {
                return subjectMenuStorage.getSubjectsMenu(parseUtil.getTag(update));
            }
            case DELETE_USER_SUBJECT -> {
                return subjectMenuStorage.getUserSubjectsListToAlterUser(parseUtil.getTag(update), "Delete subject of user");
            }
            case SET_TASK_IMAGE_SKIP_FILE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.TASK_IMAGE_NO_FILE, update);
            }
            case ANNOUNCEMENT_START -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.ANNOUNCEMENT_START, update);
            }
            case SET_ANNOUNCEMENT_FILE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.ANNOUNCEMENT_FILE, update);
            }
            case SET_ANNOUNCEMENT_IMAGE_SKIP_FILE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.ANNOUNCEMENT_IMAGE_SKIP_FILE, update);
            }
            case SET_ANNOUNCEMENT_IMAGE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.ANNOUNCEMENT_IMAGE, update);
            }
            case SET_ANNOUNCEMENT_TITLE_SKIP_IMAGE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.ANNOUNCEMENT_TITLE_SKIP_IMAGE, update);
            }
            case SET_ANNOUNCEMENT_TITLE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.ANNOUNCEMENT_TITLE, update);
            }
            case SET_ANNOUNCEMENT_TEXT -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.ANNOUNCEMENT_TEXT, update);
            }
            case NOTIFICATION_START -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.NOTIFICATION_START, update);
            }
            case SET_NOTIFICATION_TEXT -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.NOTIFICATION_TEXT, update);
            }
            case SET_NOTIFICATION_DATE -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.NOTIFICATION_DATE, update);
            }
            case SET_NOTIFICATION_TIME -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.NOTIFICATION_TIME, update);
            }
            case SET_NOTIFICATION_FREQUENCY -> {
                return instanceAdditionSupportingMessagesStorage.getMessageByStage(InstanceAdditionStage.NOTIFICATION_FREQUENCY, update);
            }
            case UNHANDLED_MESSAGE -> {
                return generalMenuStorage.getUnhandledMessageReply();
            }
            default -> {
                throw new RuntimeException("Wrong menu option passed");
            }
        }
    }

    public Message getMenu(MenuMode menuMode, Update update, int targetId) {
        switch (menuMode) {
            case MAIN_MENU: {
                return generalMenuStorage.getMainMenu(targetId);
            }
            case USER_APPOINTMENTS_LIST: {
                return appointmentMenuStorage.getAppointmentsMenu(targetId);
            }
            case UNAPPOINTED_TASKS_MENU: {
                return unappointedTaskMenuStorage.getUnappointedTasksMenu(parseUtil.getTag(update));
            }
            case GROUPS_MENU: {
                return groupMenuStorage.getGroupsMenu(targetId);
            }
            case GROUP_MENU: {
                return groupMenuStorage.getGroupMenu(targetId, update);
            }
            case GROUP_USERS_LIST: {
                return groupMenuStorage.getUsersInGroupList(targetId, update);
            }
            case GROUP_APPOINTMENTS_MENU: {
                return groupMenuStorage.getAppointmentsInGroupList(targetId, parseUtil.getTag(update));
            }
            case GROUP_UNAPPOINTED_TASKS_MENU: {
                return groupMenuStorage.getUnappointedTasksInGroupList(targetId, parseUtil.getTag(update));
            }
            case GET_GROUP_SUBJECTS: {
                return groupMenuStorage.getSubjectsMenu(targetId);
            }
            case SET_TASK_SUBJECT_LIST: {
                if (userRepository.get(parseUtil.getTag(update)).isGroupMode()) {
                    return groupMenuStorage.getSubjectsMenuToAlterGroup(targetId, "Set subject");
                } else {
                    return subjectMenuStorage.getUserSubjectsListToAlterUser(parseUtil.getTag(update), "Set subject");
                }
            }
            case DELETE_GROUP_SUBJECT: {
                return groupMenuStorage.getSubjectsMenuToAlterGroup(targetId, "Delete subject in group");
            }
            case LIST_TO_ADD_ADMIN: {
                return groupMenuStorage.getUsersInGroupToAlterUser(targetId, update, "Add admin", "Manage admins", Role.DEFAULT);
            }
            case LIST_TO_REMOVE_ADMIN: {
                return groupMenuStorage.getUsersInGroupToAlterUser(targetId, update, "Remove admin", "Manage admins", Role.ADMIN);
            }
            case KICK_USER: {
                return groupMenuStorage.getUsersInGroupToAlterUser(targetId, update, "Kick user", "Show group", Role.DEFAULT);
            }
            case TRANSFER_OWNERSHIP: {
                return groupMenuStorage.getUsersInGroupToAlterUser(targetId, update, "Transfer ownership of", "Show group", null);
            }
            case SHOW_GROUP_CODE: {
                return groupMenuStorage.getGroupCodeMenu(targetId);
            }
            case SHOW_UNAPPOINTED_TASK: {
                return unappointedTaskMenuStorage.getUnappointedTaskMenu(targetId, update, !update.hasCallbackQuery() || !update.getCallbackQuery().getData().contains("(noback)"));
            }
            case SHOW_APPOINTMENT: {
                return appointmentMenuStorage.getAppointmentMenu(targetId, update, !update.hasCallbackQuery() || !update.getCallbackQuery().getData().contains("(noback)"), userRepository.get(parseUtil.getTag(update)).isGroupMode());
            }
            case MARK_APPOINTMENT_AS_DONE: {
                return appointmentMenuStorage.getMarkAsDoneMenu();
            }
            case SHOW_GROUP_TASKS: {
                return groupMenuStorage.getTasksInGroupMenu(targetId);
            }
            case ADD_PERSONAL_TASK: {
                return groupMenuStorage.getUsersInGroupToAlterUser(targetId, update, "Add personal task in", "Show group", null);
            }
            case GROUP_ANNOUNCEMENTS_MENU_MANAGE: {
                return groupMenuStorage.getAnnouncementsMenu(targetId, true);
            }
            case GROUP_ANNOUNCEMENTS_MENU_VIEW: {
                return groupMenuStorage.getAnnouncementsMenu(targetId, false);
            }
            case TASK_EDIT_MENU: {
                return taskMenuStorage.getTaskEditMenu(targetId, update);
            }
            case TASK_MANAGE_MENU: {
                return taskMenuStorage.getTaskMenu(targetId, update);
            }
            case ANNOUNCEMENT_MANAGE_MENU: {
                return announcementMenuStorage.getAnnouncementMenu(targetId, update, true, false);
            }
            case ANNOUNCEMENT_MENU_VIEW: {
                return announcementMenuStorage.getAnnouncementMenu(targetId, update, false, false);
            }
            case ANNOUNCEMENT_MENU_IMPINF: {
                return announcementMenuStorage.getAnnouncementMenu(targetId, update, false, true);
            }
            case ANNOUNCEMENT_EDIT_MENU: {
                return announcementMenuStorage.getAnnouncementEditMenu(targetId, update);
            }
            case GROUP_IMPORTANT_INFO: {
                return groupMenuStorage.getImportantInfo(targetId);
            }
            case APPOINMENTS_FOR_TODAY: {
                appointmentMenuStorage.getAppointmentsForTodayMenu(targetId);
                return null;
            }
            case APPOINTMENTS_WITH_DEADLINE_TODAY: {
                appointmentMenuStorage.getAppointmentsWithDeadlineTodayMenu(targetId);
                return null;
            }
            case TASKS_FOR_TODAY: {
                Session session = HibernateConfig.getSession();
                TodayTasksInfo todayTasksInfo = todayTasksInfoRepository.get(targetId, session);
                new BotConfig().editMessage(parseUtil.getChatId(update), todayTasksInfo.getMessageId(), generalMenuStorage.getTasksForTodayMenu(todayTasksInfo, session));
                session.close();
                return null;
            }
            case UNAPPOINTED_TASKS_WITH_DEADLINE_TODAY: {
                unappointedTaskMenuStorage.getUnappointedTasksWithDeadlineTodayMenu(targetId);
                return null;
            }
            case OUTDATED_UNAPPOINTED_TASKS: {
                unappointedTaskMenuStorage.getOutdatedUnappointedTasksMenu(targetId);
                return null;
            }
            case OUTDATED_APPOINTMENTS: {
                appointmentMenuStorage.getOutdatedAppointmentsMenu(targetId);
                return null;
            }
            case SHOW_APPOINTMENT_NO_ATTACHEMETS: {
                return appointmentMenuStorage.getAppointmentMenuWithoutAttachments(targetId,
                        !update.hasCallbackQuery() || !update.getCallbackQuery().getData().contains("(noback)"), false);
            }
            case SHOW_NOTIFICATIONS_IN_GROUP_MENU: {
                return groupMenuStorage.getNotificationsInGroupMenu(targetId);
            }
            case SHOW_NOTIFICATION_MENU: {
                return notificationMenuStorage.getNotificationMenu(targetId);
            }
            case NOTIFICATION_EDIT_MENU: {
                return notificationMenuStorage.getNotificationEditMenu(targetId);
            }
            case MANAGE_ADMINS_MENU: {
                return groupMenuStorage.getManageAdminsMenu(targetId);
            }
            case SUBJECT_MANAGE_MENU: {
                return subjectMenuStorage.getSubjectMenu(targetId);
            }
            default: {
                throw new RuntimeException("Wrong menu option(targetID): " + menuMode);
            }
        }
    }

    public void handleAnnouncementMenuProvision(Update update, List<Message> resultMessagesList, String callbackData) {
        if (callbackData.matches("Show announcement \\(manage\\) \\d+")) {
            resultMessagesList.add(getMenu(MenuMode.ANNOUNCEMENT_MANAGE_MENU, update, parseUtil.getTargetId(callbackData)));
        } else if (callbackData.matches("Show announcement \\(impinf\\) \\d+")) {
            resultMessagesList.add(getMenu(MenuMode.ANNOUNCEMENT_MENU_IMPINF, update, parseUtil.getTargetId(callbackData)));
        } else if (callbackData.matches("Show announcement \\d+")) {
            resultMessagesList.add(getMenu(MenuMode.ANNOUNCEMENT_MENU_VIEW, update, parseUtil.getTargetId(callbackData)));
        }
    }
    public void handleEditMenuProvision(Update update, List<Message> resultMessagesList, String callbackData) {
        if (callbackData.substring("Edit ".length()).startsWith("task")) {
            resultMessagesList.add(getMenu(MenuMode.TASK_EDIT_MENU, update, parseUtil.getTargetId(callbackData)));
        } else if (callbackData.substring("Edit ".length()).startsWith("announcement")) {
            resultMessagesList.add(getMenu(MenuMode.ANNOUNCEMENT_EDIT_MENU, update, parseUtil.getTargetId(callbackData)));
        } else if (callbackData.substring("Edit ".length()).startsWith("notification")) {
            resultMessagesList.add(getMenu(MenuMode.NOTIFICATION_EDIT_MENU, update, parseUtil.getTargetId(callbackData)));
        }
    }

}
