package bot.schedulebot.util.generators;

import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class StringGenerator {
    public String generateRandomString() {
        String str = "";
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            str += (char) random.nextInt(97, 122);
        }
        return str;
    }

}
