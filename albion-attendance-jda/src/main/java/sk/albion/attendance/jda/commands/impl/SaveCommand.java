package sk.albion.attendance.jda.commands.impl;

import com.google.common.base.Strings;
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
import sk.albion.db.entity.SpreadsheetEntity;
import sk.albion.db.repository.SpreadsheetRepository;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SaveCommand implements Command {

    private final SpreadsheetRepository spreadsheetRepository;
    private final DiscordDataHolder discordDataHolder;
    private final GoogleService googleService;

    @Autowired
    public SaveCommand(GoogleService googleService, DiscordDataHolder discordDataHolder,
                       SpreadsheetRepository spreadsheetRepository) {
        this.spreadsheetRepository = spreadsheetRepository;
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

        String caller = event.getMessage().getAuthor().getName();
        log.info("Received save request from {}", caller);

        try {
            VoiceChannel controlledChannel = discordDataHolder.getControlledChannel();
            if (controlledChannel == null) {
                channel.sendMessage("I am not connected to a voice channel!").queue();
                log.warn("{} is not connected to a voice channel!", caller);
                return;
            }

            //TODO Refactor
            String message = event.getMessage().getContentRaw();
            String croppedMessage = message.replace(getCommandDefinition(), "");
            if (Strings.isNullOrEmpty(croppedMessage)) {
                showUsage();
                return;
            }
            String[] parts = croppedMessage.split(" ");
            List<String> words = new ArrayList<>();
            for (String part : parts) {
                if (!Strings.isNullOrEmpty(part)) {
                    words.add(part);
                }
            }

            StringBuilder actionName = new StringBuilder();
            if (words.isEmpty()) {
                showUsage();
                return;
            } else if (words.size() > 1) {
                for (String word : words) {
                    if (Strings.isNullOrEmpty(actionName.toString())) {
                        actionName.append(word);
                    } else {
                        actionName.append(" ").append(word);
                    }
                }
            } else {
                actionName = new StringBuilder(words.iterator().next());
            }

            log.info("Action Name: {}", actionName);
            // TODO End

            String spreadSheetId = getOrCreateSpreadsheet(event);

            Calendar cal = Calendar.getInstance();
            String month = new SimpleDateFormat("MMM").format(cal.getTime());

            // TODO check sheet if fit month

            googleService.writeFirstDataToSpreadSheet(spreadSheetId, month, "ZVZ",
                    discordDataHolder.getConnectedMembers());
            log.info("Data successfully updated in SpreadSheet: {}",
                    String.format("https://docs.google.com/spreadsheets/d/%s/", spreadSheetId));

            channel.sendMessage(String.format("Data successfully updated in SpreadSheet: %s",
                    String.format("https://docs.google.com/spreadsheets/d/%s/", spreadSheetId))).queue();
        } catch (Exception e) {
            log.error("Exception during save!", e);
            throw new IllegalArgumentException("Failed to save data to Google Spreadsheet!");
        }
    }

    private void showUsage() {

    }

    private String getOrCreateSpreadsheet(GuildMessageReceivedEvent event) throws IOException {
        String discordId = event.getGuild().getId();
        SpreadsheetEntity spreadsheetEntity = spreadsheetRepository.findByDiscordId(discordId);
        String spreadSheetId;
        if (spreadsheetEntity == null) {
            spreadSheetId = googleService.createInitialSpreadSheet();
            spreadsheetEntity = new SpreadsheetEntity();
            spreadsheetEntity.setDiscordId(discordId);
            spreadsheetEntity.setSpreadSheetId(spreadSheetId);
            spreadsheetRepository.save(spreadsheetEntity);
        } else {
            spreadSheetId = spreadsheetEntity.getSpreadSheetId();
        }
        return spreadSheetId;
    }
}
