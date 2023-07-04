package bot.schedulebot.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Component
public class Converter {

    private Converter() {
    }

    public String convertFileToJsonString(File file) {
        String imageString;
        try {
            imageString = Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("file_name", file.getName());
        jsonObject.addProperty("file_size", file.length());
        jsonObject.addProperty("file_data", imageString);

        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }

    public File convertJsonStringToFile(String jsonString) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);

        String filename = jsonObject.get("file_name").getAsString();
        String content = jsonObject.get("file_data").getAsString();

        Path tempFile;
        try {
            tempFile = Files.createTempFile(filename, "");
            Files.write(tempFile, Base64.getDecoder().decode(content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return tempFile.toFile();
    }


}
