package bot.schedulebot.util;

import bot.schedulebot.entities.Entity;
import bot.schedulebot.entities.File;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@Component
@Getter
public class ClassFieldsStorage {
    private final Map<Class<?>, Field[]> entitiesToAddPropertiesNotCollections;
    private final ClassesStorage classesStorage;

    private ClassFieldsStorage(ClassesStorage classesStorage) {
        entitiesToAddPropertiesNotCollections = new HashMap<>();
        this.classesStorage = classesStorage;
    }

    @PostConstruct
    public void init() {
        classesStorage.getClassesInPackages()
                .get(Entity.class.getPackage())
                .forEach(clazz -> {
                    Field[] fields = Arrays.stream(clazz.getDeclaredFields())
                            .filter(getEntityPropertiesFilter())
                            .sorted(getEntityPropertiesOrderComparator()).toArray(Field[]::new);
                    entitiesToAddPropertiesNotCollections.put(clazz, fields);
                    });
        entitiesToAddPropertiesNotCollections.forEach((aClass, fields) -> {
            System.out.print(aClass.getSimpleName() + ": ");
            for (Field field : fields) {
                System.out.print(field.getName() + " ");
            }
            System.out.println();
        });
    }

    private static Predicate<Field> getEntityPropertiesFilter() {
        return field ->
                (field.getType().isPrimitive() && !field.getName().equals("id") && !field.getType().equals(boolean.class))
                        || field.getType().equals(String.class)
                        || field.getType().equals(LocalDate.class)
                        || field.getType().equals(LocalTime.class)
                        || field.getType().equals(File.class);
    }

    private static Comparator<Field> getEntityPropertiesOrderComparator() {
        return Comparator.comparing(field -> field.getType().isPrimitive()
                || field.getType().equals(String.class) && !field.getName().equals("image")
                ? 1 : (field.getType().equals(LocalDate.class)
                || field.getType().equals(LocalTime.class) ? 0 : -1));
    }
}
