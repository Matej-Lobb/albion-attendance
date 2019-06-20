package sk.albion.attendance.jda.commands.impl;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.entities.impl.VoiceChannelImpl;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.albion.attendance.jda.commands.Command;
import sk.albion.attendance.jda.holder.DiscordDataHolder;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ShowCommand implements Command {

    private final DiscordDataHolder discordDataHolder;

    @Value("${discord.commands.show}")
    private String showCommand;

    @Autowired
    public ShowCommand(DiscordDataHolder discordDataHolder) {
        this.discordDataHolder = discordDataHolder;
    }

    @Override
    public String getCommandDefinition() {
        return showCommand;
    }

    @Override
    public void executeCommand(Event rawEvent) {
        GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) rawEvent;
        TextChannel channel = event.getChannel();

        String caller = event.getMessage().getAuthor().getName();
        log.info("Received show request from {}", caller);

        VoiceChannel controlledChannel = discordDataHolder.getControlledChannel();
        if (controlledChannel == null) {
            channel.sendMessage("You are not connected to a voice channel!").queue();
            log.warn("{} is not connected to a voice channel!", caller);
            return;
        }

        AudioManager audioManager = event.getGuild().getAudioManager();
        if (audioManager.isAttemptingToConnect()) {
            channel.sendMessage("The bot is already trying to connect!").queue();
            return;
        }

        if (controlledChannel instanceof VoiceChannelImpl) {
            VoiceChannelImpl voice = (VoiceChannelImpl) controlledChannel;
            List<Object> rawMember = Arrays.asList(voice.getConnectedMembersMap().values());
            rawMember.forEach(discordDataHolder::addMemberIfNotExists);
        } else {
            channel.sendMessage("You are not connected to a voice channel!").queue();
            log.warn("{} is not connected to a voice channel!", caller);
            return;
        }

        StringBuilder members = new StringBuilder();

        for (Member member : discordDataHolder.getConnectedMembers()) {
            members.append(" User: ").append(member.getUser().getName());
            if (!Strings.isNullOrEmpty(member.getNickname())) {
                members.append(" / Nickname: ").append(member.getNickname());
            }
        }
        log.info("Show returned: {}", members.toString());
        channel.sendMessage("Connected Members: " + members.toString()).queue();
    }
}
