package bot.schedulebot.util;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Getter
public class ClassesStorage {
    private final Map<Package, Set<Class<?>>> classesInPackages;

    private ClassesStorage() {
        classesInPackages = new HashMap<>();
    }

    @PostConstruct
    private void init() {
        try {
            addClassesToMap(classesInPackages,
                    Class.forName("bot.schedulebot.EmptyClass").getPackage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        //classesInPackages.forEach((aPackage, classes) -> System.out.println(aPackage + " " + classes.toString()));
    }

    private void addClassesToMap(Map<Package, Set<Class<?>>> map, Package pack) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(ClasspathHelper.forPackage(pack.getName()))
                .setScanners(new SubTypesScanner(false)));

        Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);

        classes.forEach(aClass -> {
            if (map.get(aClass.getPackage()) == null) {
                map.put(aClass.getPackage(), new LinkedHashSet<>(Set.of(aClass)));
            } else {
                map.get(aClass.getPackage()).add(aClass);
            }
        });

    }
}
