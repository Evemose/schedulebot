package bot.schedulebot.util.generators;

import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Component
public class FileGenerator {
    public File getFileFromByteArray(byte[] array, String name) {
        File file = new File(name);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(array);
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}
