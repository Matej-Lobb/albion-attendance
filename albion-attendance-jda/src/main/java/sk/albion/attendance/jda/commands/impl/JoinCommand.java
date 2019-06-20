package sk.albion.attendance.jda.commands.impl;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.albion.attendance.jda.commands.Command;
import sk.albion.attendance.jda.holder.DiscordDataHolder;

@Slf4j
@Component
public class JoinCommand implements Command {

    @Value("${discord.commands.join}")
    private String joinCommand;

    private final DiscordDataHolder discordDataHolder;

    @Autowired
    public JoinCommand(DiscordDataHolder discordDataHolder) {
        this.discordDataHolder = discordDataHolder;
    }

    @Override
    public String getCommandDefinition() {
        return joinCommand;
    }

    @Override
    public void executeCommand(Event rawEvent) {
        GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) rawEvent;
        TextChannel channel = event.getChannel();

        String caller = channel.getGuild().getSelfMember().getUser().getName();
        log.info("Received join request from {}", caller);

        if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT)) {
            channel.sendMessage("I do not have permissions to join a voice channel!").queue();
            log.warn("Unable to join the channel. No permissions to join a voice channel!");
            return;
        }

        VoiceChannel controlledChannel = event.getMember().getVoiceState().getChannel();
        if (controlledChannel == null) {
            channel.sendMessage("You are not connected to a voice channel!").queue();
            log.warn("{} is not connected to a voice channel!", caller);
            return;
        }

        discordDataHolder.setControlledChannel(controlledChannel);

        AudioManager audioManager = event.getGuild().getAudioManager();
        if (audioManager.isAttemptingToConnect()) {
            channel.sendMessage("The bot is already trying to connect! Wait for it please").queue();
            return;
        }

        // Connect
        audioManager.openAudioConnection(controlledChannel);

        log.info("Connected to the voice channel {} !", controlledChannel.getName());
        channel.sendMessage("Connected to the voice channel!").queue();
    }
}
