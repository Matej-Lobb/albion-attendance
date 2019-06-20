package sk.albion.attendance.jda.event;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sk.albion.attendance.jda.holder.DiscordDataHolder;

@Slf4j
@Component
public class UserJoinServerEvent {

    private final DiscordDataHolder discordDataHolder;

    @Autowired
    public UserJoinServerEvent(DiscordDataHolder discordDataHolder) {
        this.discordDataHolder = discordDataHolder;
    }

    public void onGenericGuildVoice(GenericGuildVoiceEvent event) {
        VoiceChannel channelJoined = event.getMember().getVoiceState().getChannel();
        VoiceChannel controlledChannel = discordDataHolder.getControlledChannel();
        if (controlledChannel != null && channelJoined != null) {
            if (channelJoined.getName().equals(controlledChannel.getName())) {
                log.info("User {} joined to controlled channel!", event.getMember().getUser().getName());
                discordDataHolder.addMemberIfNotExists(event.getMember());
            }
        }
    }
}
