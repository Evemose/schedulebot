package bot.schedulebot.exceptions;

public class NoFileInUpdateException extends Exception {
    public NoFileInUpdateException(String filetype) {
        super("No " + filetype + " in message you have sent. Try again");
    }
}
