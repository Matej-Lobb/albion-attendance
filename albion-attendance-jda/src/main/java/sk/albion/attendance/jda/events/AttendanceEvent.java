package sk.albion.attendance.jda.events;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import sk.albion.attendance.jda.commands.Command;
import sk.albion.attendance.jda.commands.impl.JoinCommand;
import sk.albion.attendance.jda.commands.impl.LeaveCommand;
import sk.albion.attendance.jda.commands.impl.SaveCommand;
import sk.albion.attendance.jda.commands.impl.ShowCommand;
import sk.albion.attendance.jda.event.UserJoinServerEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class AttendanceEvent extends ListenerAdapter {

    private final UserJoinServerEvent joinServerEvent;
    private final Map<String, Command> commands;

    private static final Permission USAGE_RIGHT = Permission.ADMINISTRATOR;

    public AttendanceEvent(Map<String, Command> commands, UserJoinServerEvent joinServerEvent) {
        this.joinServerEvent = joinServerEvent;
        this.commands = commands;
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.getMember().hasPermission(USAGE_RIGHT)) {
            return;
        }

        Set<String> commands = this.commands.keySet();
        commands.forEach(command -> {
            if (event.getMessage().getContentRaw().contains(command)) {
                this.commands.get(command).executeCommand(event);
            }
        });
    }

    public void onGenericGuildVoice(GenericGuildVoiceEvent event) {
        joinServerEvent.onGenericGuildVoice(event);
    }
}
