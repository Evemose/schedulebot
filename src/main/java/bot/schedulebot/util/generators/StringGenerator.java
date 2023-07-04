package bot.schedulebot.util.generators;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class StringGenerator {
    private StringGenerator() {

    }

    public String generateRandomString() {
        StringBuilder str = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            str.append((char) random.nextInt(97, 122));
        }
        return str.toString();
    }

}
