package sk.albion.attendance.jda.events;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.entities.impl.VoiceChannelImpl;
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sk.albion.attendance.google.GoogleService;
import sk.albion.attendance.jda.commands.DiscordCommandsLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

@Slf4j
@Component
public class AttendanceEvent extends ListenerAdapter {

    private final GoogleService googleService;
    private final DiscordCommandsLoader discordCommandsLoader;

    private VoiceChannel controlledChannel;
    private List<Member> connectedMembers = new ArrayList<>();

    private static final Permission USAGE_RIGHT = Permission.ADMINISTRATOR;

    @Autowired
    public AttendanceEvent(GoogleService googleService, DiscordCommandsLoader discordCommandsLoader) {
        this.discordCommandsLoader = discordCommandsLoader;
        this.googleService = googleService;
    }

    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        if (!event.getMember().hasPermission(USAGE_RIGHT)) {
            return;
        }

        if (event.getMessage().getContentRaw().equals(discordCommandsLoader.getJoinCommand())) {
            join(event);
        } else if (event.getMessage().getContentRaw().equals(discordCommandsLoader.getLeaveCommand())) {
            leave(event);
        } else if (event.getMessage().getContentRaw().equals(discordCommandsLoader.getShowCommand())) {
            showUsers(event);
        } else if (event.getMessage().getContentRaw().equals(discordCommandsLoader.getSaveCommand())) {
            save(event);
        }
    }

    public void onGenericGuildVoice(GenericGuildVoiceEvent event) {
        VoiceChannel channelJoined = event.getMember().getVoiceState().getChannel();
        if (controlledChannel != null && channelJoined != null) {
            if (channelJoined.getName().equals(controlledChannel.getName())) {
                log.info("User {} joined to controlled channel!", event.getMember().getUser().getName());
                addMemberIfNotExists(event.getMember());
            }
        }
    }

    private void save(GuildMessageReceivedEvent event) {
        TextChannel channel = event.getChannel();

        String caller = channel.getGuild().getSelfMember().getUser().getName();
        log.info("Received save request from {}", caller);

        try {
            if (controlledChannel == null) {
                channel.sendMessage("I am not connected to a voice channel!").queue();
                log.warn("{} is not connected to a voice channel!", caller);
                return;
            }

            String spreadSheetId = googleService.createInitialSpreadSheet();

            Calendar cal = Calendar.getInstance();
            String month = new SimpleDateFormat("MMM").format(cal.getTime());

            googleService.writeFirstDataToSpreadSheet(spreadSheetId, month, "ZVZ", connectedMembers);
            log.info("Data successfully updated in SpreadSheet: {}",
                    String.format("https://docs.google.com/spreadsheets/d/%s/", spreadSheetId));

            channel.sendMessage(String.format("Data successfully updated in SpreadSheet: %s",
                    String.format("https://docs.google.com/spreadsheets/d/%s/", spreadSheetId))).queue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to save data to Google Spreadsheet!");
        }
    }

    private void leave(GuildMessageReceivedEvent event) {
        TextChannel channel = event.getChannel();

        String caller = channel.getGuild().getSelfMember().getUser().getName();
        log.info("Received leave request from {}", caller);

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
            controlledChannel = null;
            connectedMembers = new ArrayList<>();
        }
    }

    private void join(GuildMessageReceivedEvent event) {
        TextChannel channel = event.getChannel();

        String caller = channel.getGuild().getSelfMember().getUser().getName();
        log.info("Received join request from {}", caller);

        if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.VOICE_CONNECT)) {
            channel.sendMessage("I do not have permissions to join a voice channel!").queue();
            log.warn("Unable to join the channel. No permissions to join a voice channel!");
            return;
        }

        controlledChannel = event.getMember().getVoiceState().getChannel();
        if (controlledChannel == null) {
            channel.sendMessage("You are not connected to a voice channel!").queue();
            log.warn("{} is not connected to a voice channel!", caller);
            return;
        }

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

    private void showUsers(GuildMessageReceivedEvent event) {
        TextChannel channel = event.getChannel();

        String caller = channel.getGuild().getSelfMember().getUser().getName();
        log.info("Received show request from {}", caller);

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
            rawMember.forEach(this::addMemberIfNotExists);
        } else {
            channel.sendMessage("You are not connected to a voice channel!").queue();
            log.warn("{} is not connected to a voice channel!", caller);
            return;
        }

        StringBuilder members = new StringBuilder();

        for (Member member : connectedMembers) {
            members.append(" User: ").append(member.getUser().getName());
            if (!Strings.isNullOrEmpty(member.getNickname())) {
                members.append(" / Nickname: ").append(member.getNickname());
            }
        }
        log.info("Show returned: {}", members.toString());
        channel.sendMessage("Connected Members: " + members.toString()).queue();
    }

    private void addMemberIfNotExists(Object object) {
        Member member = (Member) object;
        if (connectedMembers.contains(member)) {
            return;
        }
        if (member.getUser().isBot()) {
            return;
        }
        log.debug("Adding user {}/{}  to list!", member.getUser().getName(), member.getNickname());
        connectedMembers.add(member);
    }
}
