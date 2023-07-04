package bot.schedulebot.objectsunderconstruction;


import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Exchanger;

@Getter
public abstract class ObjectsUnderConstruction<T> {
    protected Map<String, T> objectsUnderConstructions;
    protected Map<String, Exchanger<Update>> exchangers;
    protected Map<String, Exchanger<Update>> editExchangers;
    protected Map<String, Integer> messageIds;
    public ObjectsUnderConstruction() {
        this.objectsUnderConstructions = new HashMap<>();
        this.exchangers = new HashMap<>();
        this.editExchangers = new HashMap<>();
        this.messageIds = new HashMap<>();
    }
}
