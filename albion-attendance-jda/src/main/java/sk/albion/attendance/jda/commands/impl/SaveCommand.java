package sk.albion.attendance.jda.commands.impl;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sk.albion.attendance.google.GoogleService;
import sk.albion.attendance.jda.commands.Command;
import sk.albion.attendance.jda.holder.DiscordDataHolder;

import java.text.SimpleDateFormat;
import java.util.Calendar;

@Slf4j
@Component
public class SaveCommand implements Command {

    private final GoogleService googleService;
    private final DiscordDataHolder discordDataHolder;

    @Autowired
    public SaveCommand(GoogleService googleService, DiscordDataHolder discordDataHolder) {
        this.discordDataHolder = discordDataHolder;
        this.googleService = googleService;
    }

    @Value("${discord.commands.save}")
    private String saveCommand;

    @Override
    public String getCommandDefinition() {
        return saveCommand;
    }

    @Override
    public void executeCommand(Event rawEvent) {
        GuildMessageReceivedEvent event = (GuildMessageReceivedEvent) rawEvent;
        TextChannel channel = event.getChannel();

        String caller = channel.getGuild().getSelfMember().getUser().getName();
        log.info("Received save request from {}", caller);

        try {
            VoiceChannel controlledChannel = discordDataHolder.getControlledChannel();

            if (controlledChannel == null) {
                channel.sendMessage("I am not connected to a voice channel!").queue();
                log.warn("{} is not connected to a voice channel!", caller);
                return;
            }

            String spreadSheetId = googleService.createInitialSpreadSheet();

            Calendar cal = Calendar.getInstance();
            String month = new SimpleDateFormat("MMM").format(cal.getTime());

            googleService.writeFirstDataToSpreadSheet(spreadSheetId, month, "ZVZ",
                    discordDataHolder.getConnectedMembers());
            log.info("Data successfully updated in SpreadSheet: {}",
                    String.format("https://docs.google.com/spreadsheets/d/%s/", spreadSheetId));

            channel.sendMessage(String.format("Data successfully updated in SpreadSheet: %s",
                    String.format("https://docs.google.com/spreadsheets/d/%s/", spreadSheetId))).queue();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to save data to Google Spreadsheet!");
        }
    }
}
