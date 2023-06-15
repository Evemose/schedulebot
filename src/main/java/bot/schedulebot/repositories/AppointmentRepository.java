package bot.schedulebot.repositories;

import bot.schedulebot.config.HibernateConfig;
import bot.schedulebot.entities.Appointment;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AppointmentRepository extends bot.schedulebot.repositories.Repository<Appointment> {
    @Autowired
    protected AppointmentRepository() {}
    public List<Appointment> getAppointmentsOfTask(int taskId) {
        Session session = HibernateConfig.getSession();
        List<Appointment> appointments = getAll(session).stream().filter(appointment -> appointment.getTask().getId() == taskId).toList();
        session.close();
        return appointments;
    }
}
