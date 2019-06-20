package sk.albion.attendance.jda.commands;

import net.dv8tion.jda.core.events.Event;

public interface Command {

    String getCommandDefinition();

    void executeCommand(Event event);
}
