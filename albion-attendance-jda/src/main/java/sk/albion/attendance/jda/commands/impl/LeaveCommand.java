package sk.albion.attendance.jda.commands.impl;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.albion.attendance.jda.commands.Command;
import sk.albion.attendance.jda.holder.DiscordDataHolder;

@Slf4j
@Component
public class LeaveCommand implements Command {

    private final DiscordDataHolder discordDataHolder;

    @Autowired
    public LeaveCommand(DiscordDataHolder discordDataHolder) {
        this.discordDataHolder = discordDataHolder;
    }

    @Value("${discord.commands.leave}")
    private String leaveCommand;

    @Override
    public String getCommandDefinition() {
        return leaveCommand;
    }

    @Override
    public void executeCommand(Event rawEvent) {
        GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) rawEvent;
        TextChannel channel = event.getChannel();

        String caller = channel.getGuild().getSelfMember().getUser().getName();
        log.info("Received leave request from {}", caller);

        VoiceChannel controlledChannel = discordDataHolder.getControlledChannel();
        try {
            if (controlledChannel == null) {
                channel.sendMessage("I am not connected to a voice channel!").queue();
                log.warn("{} is not connected to a voice channel!", caller);
                return;
            }

            // Disconnect
            event.getGuild().getAudioManager().closeAudioConnection();

            channel.sendMessage("Disconnected from the voice channel!").queue();
            log.info("Bot successfully disconnected channel {} !", controlledChannel.getName());
        } finally {
            discordDataHolder.getConnectedMembers().clear();
            discordDataHolder.setControlledChannel(null);
        }
    }
}
