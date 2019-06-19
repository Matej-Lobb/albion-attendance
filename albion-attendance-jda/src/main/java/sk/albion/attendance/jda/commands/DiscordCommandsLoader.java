package sk.albion.attendance.jda.commands;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DiscordCommandsLoader {

    @Value("${discord.commands.join}")
    private String joinCommand;

    @Value("${discord.commands.show}")
    private String showCommand;

    @Value("${discord.commands.save}")
    private String saveCommand;

    @Value("${discord.commands.leave}")
    private String leaveCommand;

    public String getJoinCommand() {
        return joinCommand;
    }

    public String getShowCommand() {
        return showCommand;
    }

    public String getSaveCommand() {
        return saveCommand;
    }

    public String getLeaveCommand() {
        return leaveCommand;
    }
}
