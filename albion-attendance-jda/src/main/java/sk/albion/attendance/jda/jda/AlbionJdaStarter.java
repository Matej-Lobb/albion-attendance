package sk.albion.attendance.jda.jda;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import sk.albion.attendance.jda.events.AttendanceEvent;
import sk.albion.attendance.jda.google.GoogleService;

@Slf4j
@Component
public class AlbionJdaStarter {

    @Value("${discord.token}")
    private String token;

    private final GoogleService googleService;

    @Autowired
    public AlbionJdaStarter(GoogleService googleService) {
        this.googleService = googleService;
    }

    @EventListener
    public void discordInitializer(ContextRefreshedEvent event) throws Exception {
        log.info("Starting Albion-Attendance BOT ...");
        log.debug("Discord Token: {}", token);
        JDA jda = new JDABuilder(token).build();
        jda.addEventListener(new AttendanceEvent(googleService));
    }
}