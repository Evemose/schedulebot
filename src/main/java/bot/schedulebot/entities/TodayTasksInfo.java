package bot.schedulebot.entities;

import bot.schedulebot.enums.TodayTasksInfoMode;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@jakarta.persistence.Entity
@EqualsAndHashCode
public class TodayTasksInfo implements Entity, Serializable {
    @Id
    private int id;

    @Column(name = "message_id")
    private int messageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode")
    private TodayTasksInfoMode mode;

    @OneToOne
    @JoinTable(name = "users_tasks_info",
            joinColumns = @JoinColumn(name = "tasks_info_id", unique = true),
            inverseJoinColumns = @JoinColumn(name = "user_id", unique = true))
    @EqualsAndHashCode.Exclude
    private User user;

    @OneToMany
    @JoinTable(name = "tasks_info_appointments_for_today",
            joinColumns = @JoinColumn(name = "tasks_info_id"),
            inverseJoinColumns = @JoinColumn(name = "appointment_id", unique = true))
    private List<Appointment> appointmentsForToday;

    @OneToMany
    @JoinTable(name = "tasks_info_appointments_with_deadline_today",
            joinColumns = @JoinColumn(name = "tasks_info_id"),
            inverseJoinColumns = @JoinColumn(name = "appointment_id", unique = true))
    private List<Appointment> appointmentsWithDeadlineToday;

    @OneToMany
    @JoinTable(name = "tasks_info_unappoited_tasks_with_deadline_today",
            joinColumns = @JoinColumn(name = "tasks_info_id"),
            inverseJoinColumns = @JoinColumn(name = "unappointed_task_id", unique = true))
    private List<UnappointedTask> unappointedTasksWithDeadlineToday;

    public TodayTasksInfo() {
        mode = TodayTasksInfoMode.MAIN;
    }

    @OneToMany
    @JoinTable(name = "tasks_info_outdated_unappoited_tasks",
            joinColumns = @JoinColumn(name = "tasks_info_id"),
            inverseJoinColumns = @JoinColumn(name = "unappointed_task_id", unique = true))
    private List<UnappointedTask> outdatedUnappointedTasks;

    @OneToMany
    @JoinTable(name = "tasks_info_outdated_appointments",
            joinColumns = @JoinColumn(name = "tasks_info_id"),
            inverseJoinColumns = @JoinColumn(name = "appointment_id", unique = true))
    private List<Appointment> outdatedAppointments;

    @Override
    public String toString() {
        return appointmentsForToday.size() + " appointment" + (appointmentsForToday.size() != 1 ? "s" : "") + " for today\n\n"
                + appointmentsWithDeadlineToday.size() + " task" + (appointmentsWithDeadlineToday.size() != 1 ? "s" : "") + " that you have appointed for later, but " + (appointmentsForToday.size() != 1 ? "their" : "its") + " deadline is today\n\n"
                + unappointedTasksWithDeadlineToday.size() + " task" + (unappointedTasksWithDeadlineToday.size() != 1 ? "s" : "") + ", that you have not appointed for any date, but their deadline is today\n\n"
                + outdatedUnappointedTasks.size() + " outdated task" + (outdatedUnappointedTasks.size() != 1 ? "s" : "") + ", that you have not appointed for any date\n\n"
                + outdatedAppointments.size() + " outdated appointment" + (outdatedAppointments.size() != 1 ? "s" : "");
    }
}
