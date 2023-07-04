package bot.schedulebot.boot;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.TodayTasksInfo;
import bot.schedulebot.entities.User;
import bot.schedulebot.repositories.TodayTasksInfoRepository;
import bot.schedulebot.repositories.UserRepository;
import bot.schedulebot.services.TodayTasksInfoService;
import bot.schedulebot.util.DateTimeUtil;
import bot.schedulebot.util.Notificator;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class NotificationsBoot {
    private final UserRepository userRepository;
    private final TodayTasksInfoRepository todayTasksInfoRepository;
    private final Notificator notificator;
    private final DateTimeUtil dateTimeUtil;
    private final TodayTasksInfoService todayTasksInfoService;

    public NotificationsBoot(UserRepository userRepository, TodayTasksInfoRepository todayTasksInfoRepository, Notificator notificator, DateTimeUtil dateTimeUtil, TodayTasksInfoService todayTasksInfoService) {
        this.userRepository = userRepository;
        this.todayTasksInfoService = todayTasksInfoService;
        this.todayTasksInfoRepository = todayTasksInfoRepository;
        this.notificator = notificator;
        this.dateTimeUtil = dateTimeUtil;
    }

    public void initializeRepeatedNotifications() {
        Timer timer = new Timer();
        timer.schedule(getAppointmentNotificationsTimerTask(), dateTimeUtil.getMidnightOfNextDay(), 1000 * 60 * 60 * 24);
    }

    private TimerTask getAppointmentNotificationsTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                Session session = HibernateConfig.getSession();
                List<User> users = userRepository.getAll(session);
                todayTasksInfoService.deleteAllTasksInfoMessages();
                users.stream().filter(user -> user.isWantToGenNotifications()).forEach(user -> {
                    TodayTasksInfo todayTasksInfo;
                    if (user.getTodayTasksInfo() == null) {
                        todayTasksInfo = new TodayTasksInfo();
                        todayTasksInfo.setUser(user);
                    } else {
                        todayTasksInfo = user.getTodayTasksInfo();
                    }

                    notificator.sendEverydayTasksNotification(user.getChatId(), todayTasksInfo, session);
                    todayTasksInfoRepository.update(todayTasksInfo, session);
                });
                session.close();
            }
        };
    }
}
