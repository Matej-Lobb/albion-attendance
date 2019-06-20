package sk.albion.attendance.jda.jda;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sk.albion.attendance.jda.commands.impl.JoinCommand;
import sk.albion.attendance.jda.commands.impl.LeaveCommand;
import sk.albion.attendance.jda.commands.impl.SaveCommand;
import sk.albion.attendance.jda.commands.impl.ShowCommand;
import sk.albion.attendance.jda.event.UserJoinServerEvent;
import sk.albion.attendance.jda.events.AttendanceEvent;

@Slf4j
@Component
public class AlbionJdaStarter {

    @Value("${discord.token}")
    private String token;

    private final UserJoinServerEvent joinServerEvent;
    private final LeaveCommand leaveCommand;
    private final SaveCommand saveCommand;
    private final JoinCommand joinCommand;
    private final ShowCommand showCommand;

    @Autowired
    public AlbionJdaStarter(JoinCommand joinCommand, SaveCommand saveCommand, LeaveCommand leaveCommand,
                           ShowCommand showCommand, UserJoinServerEvent joinServerEvent) {
        this.joinServerEvent = joinServerEvent;
        this.leaveCommand = leaveCommand;
        this.showCommand = showCommand;
        this.joinCommand = joinCommand;
        this.saveCommand = saveCommand;
    }

    @EventListener
    public void discordInitializer(ContextRefreshedEvent contextRefreshedEvent) throws Exception {
        log.info("Starting Albion-Attendance BOT ...");
        log.debug("Discord Token: {}", token);
        JDA jda = new JDABuilder(token).build();
        jda.addEventListener(new AttendanceEvent(joinCommand, saveCommand, leaveCommand, showCommand, joinServerEvent));
    }
}