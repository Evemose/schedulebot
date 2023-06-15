package bot.schedulebot.boot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;

import static bot.schedulebot.config.HibernateConfig.startHibernate;

@SpringBootApplication(scanBasePackages = "bot.schedulebot")
public class ScheduleBotApplication {

    private static BotBoot botBoot;
    private static NotificationsBoot notificationsBoot;

    @Autowired
    public ScheduleBotApplication(BotBoot botBoot, NotificationsBoot notificationsBoot) {
        ScheduleBotApplication.notificationsBoot = notificationsBoot;
        ScheduleBotApplication.botBoot = botBoot;
    }

    public static void main(String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(ScheduleBotApplication.class, args);
        startHibernate();
        botBoot.startBot();
        notificationsBoot.initializeRepeatedNotifications();
    }
}
