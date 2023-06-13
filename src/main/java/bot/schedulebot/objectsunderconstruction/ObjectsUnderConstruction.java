package bot.schedulebot.objectsunderconstruction;


import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Exchanger;

@Getter
@Setter
public abstract class ObjectsUnderConstruction<T> {
    private Map<String, T> objectsUnderConstructions;
    private Map<String, Exchanger<Update>> exchangers;
    public ObjectsUnderConstruction() {
        this.objectsUnderConstructions = new HashMap<>();
        this.exchangers = new HashMap<>();
    }
}
