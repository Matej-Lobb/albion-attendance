package sk.albion.attendance.jda.events;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import sk.albion.attendance.jda.commands.impl.JoinCommand;
import sk.albion.attendance.jda.commands.impl.LeaveCommand;
import sk.albion.attendance.jda.commands.impl.SaveCommand;
import sk.albion.attendance.jda.commands.impl.ShowCommand;
import sk.albion.attendance.jda.event.UserJoinServerEvent;

@Slf4j
public class AttendanceEvent extends ListenerAdapter {

    private final UserJoinServerEvent joinServerEvent;
    private final LeaveCommand leaveCommand;
    private final SaveCommand saveCommand;
    private final JoinCommand joinCommand;
    private final ShowCommand showCommand;

    private static final Permission USAGE_RIGHT = Permission.ADMINISTRATOR;

    public AttendanceEvent(JoinCommand joinCommand, SaveCommand saveCommand, LeaveCommand leaveCommand,
                           ShowCommand showCommand, UserJoinServerEvent joinServerEvent) {
        this.joinServerEvent = joinServerEvent;
        this.leaveCommand = leaveCommand;
        this.showCommand = showCommand;
        this.joinCommand = joinCommand;
        this.saveCommand = saveCommand;
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.getMember().hasPermission(USAGE_RIGHT)) {
            return;
        }

        if (event.getMessage().getContentRaw().equals(joinCommand.getCommandDefinition())) {
            joinCommand.executeCommand(event);
        } else if (event.getMessage().getContentRaw().equals(leaveCommand.getCommandDefinition())) {
            leaveCommand.executeCommand(event);
        } else if (event.getMessage().getContentRaw().equals(showCommand.getCommandDefinition())) {
            showCommand.executeCommand(event);
        } else if (event.getMessage().getContentRaw().equals(saveCommand.getCommandDefinition())) {
            saveCommand.executeCommand(event);
        }
    }

    public void onGenericGuildVoice(GenericGuildVoiceEvent event) {
        joinServerEvent.onGenericGuildVoice(event);
    }
}
