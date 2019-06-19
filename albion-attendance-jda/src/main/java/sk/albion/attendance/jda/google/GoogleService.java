package sk.albion.attendance.jda.google;

import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import net.dv8tion.jda.core.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static sk.albion.attendance.jda.constants.GoogleConstants.SPREAD_SHEET_NAME;

@Component
public class GoogleService {

    private final GoogleSheetService googleSheetService;

    @Autowired
    public GoogleService(GoogleSheetService googleSheetService) {
        this.googleSheetService = googleSheetService;
    }

    public String createInitialSpreadSheet() throws IOException {
        String spreadSheetId = googleSheetService.createSpreadSheet(SPREAD_SHEET_NAME);
        // Get Actual Month Name
        String month = createSpreadSheetForActualMonth(spreadSheetId);

        // Remove default sheet
        // Spreadsheet have to have always at leas one sheet
        googleSheetService.deleteInitialSheet(spreadSheetId, month);

        return spreadSheetId;
    }

    public String createSpreadSheetForActualMonth(String spreadSheetId) throws IOException {
        String month = getActualMonthName();
        googleSheetService.addSheetToSpreadSheet(spreadSheetId, month);
        return month;
    }

    public void writeFirstDataToSpreadSheet(String spreadSheetId, String range, String actionName,
                                           List<Member> members) throws IOException {
        List<List<Object>> writeData = new ArrayList<>();
        writeData.add(constructFirstLine(actionName));
        for (Member member : members) {
            List<Object> line = new ArrayList<>();
            line.add(" ");
            line.add(member.getUser().getName());
            line.add(member.getNickname());
            writeData.add(line);
        }
        UpdateValuesResponse updateValuesResponse = googleSheetService.writeDataToSpreadSheet(spreadSheetId, range,
                writeData);
        System.out.println(updateValuesResponse);
    }

    private List<Object> constructFirstLine(String title) {
        List<Object> firstLine = new ArrayList<>();
        firstLine.add(title + " " + new Date());
        firstLine.add("Discord User");
        firstLine.add("Discord Nickname");
        return firstLine;
    }

    private static String getActualMonthName() {
        Calendar cal = Calendar.getInstance();
        return new SimpleDateFormat("MMM").format(cal.getTime());
    }
}
