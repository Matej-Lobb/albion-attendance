package sk.albion.attendance.jda.jda;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sk.albion.attendance.jda.commands.Command;
import sk.albion.attendance.jda.event.UserJoinServerEvent;
import sk.albion.attendance.jda.events.AttendanceEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AlbionJdaStarter {

    @Value("${discord.token}")
    private String token;

    private final UserJoinServerEvent joinServerEvent;
    private final List<Command> commands;

    @Autowired
    public AlbionJdaStarter(List<Command> commands, UserJoinServerEvent joinServerEvent) {
        this.joinServerEvent = joinServerEvent;
        this.commands = commands;
    }

    @EventListener
    public void discordInitializer(ContextRefreshedEvent contextRefreshedEvent) throws Exception {
        log.info("Starting Albion-Attendance BOT ...");
        log.debug("Discord Token: {}", token);
        JDA jda = new JDABuilder(token).build();
        jda.addEventListener(new AttendanceEvent(convertListToMap(commands), joinServerEvent));
    }

    private Map<String, Command> convertListToMap(List<Command> commands) {
        return commands.stream().collect(Collectors.toMap(Command::getCommandDefinition, command -> command));
    }
}