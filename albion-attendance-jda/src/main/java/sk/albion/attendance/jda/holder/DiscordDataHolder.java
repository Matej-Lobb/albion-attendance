package sk.albion.attendance.jda.holder;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Data
@Component
public class DiscordDataHolder {

    private VoiceChannel controlledChannel = null;
    private List<Member> connectedMembers = new ArrayList<>();

    public void addMemberIfNotExists(Object object) {
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

