package sk.albion.attendance.google.sheet;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sk.albion.attendance.google.auth.GoogleAuthService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class GoogleSheetService {

    private static final String APPLICATION_NAME = "Albion-Attendance";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final Sheets sheets;
    private final GoogleAuthService googleAuthService;

    @Autowired
    public GoogleSheetService(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
        sheets = getSheetsService();
    }

    private Sheets getSheetsService() {
        if (sheets == null) {
            try {
                log.info("Loading Spreadsheet API ...");
                NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
                return new Sheets.Builder(netHttpTransport, JSON_FACTORY,
                        googleAuthService.getCredentials(netHttpTransport))
                        .setApplicationName(APPLICATION_NAME).build();
            } catch (Exception e) {
                throw new IllegalArgumentException("Fail to load Google API");
            }
        } else {
            return sheets;
        }
    }

    public String createSpreadSheet(String title) throws IOException {
        log.info("Creating new Spreadsheet with name {} ...", title);
        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties()
                        .setTitle(title));
        spreadsheet = sheets.spreadsheets().create(spreadsheet)
                .setFields("spreadsheetId")
                .execute();
        String spreadSheetId = spreadsheet.getSpreadsheetId();
        log.info("Spreadsheet {} created! Spreadsheet ID: ", title, spreadSheetId);
        return spreadSheetId;
    }

    public BatchUpdateSpreadsheetResponse addSheetToSpreadSheet(String spreadSheetId, String sheetName)
            throws IOException {
        log.info("Adding Sheet {} to Spreadsheet with ID: {}", sheetName, spreadSheetId);
        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest();
        Request addRequest = new Request();
        AddSheetRequest addSheetRequest = new AddSheetRequest();
        SheetProperties sheetProperties = new SheetProperties();
        sheetProperties.setTitle(sheetName);
        addSheetRequest.setProperties(sheetProperties);
        addRequest.setAddSheet(addSheetRequest);
        request.setRequests(Collections.singletonList(addRequest));

        BatchUpdateSpreadsheetResponse response = sheets.spreadsheets().batchUpdate(spreadSheetId, request).execute();
        log.info("Response: {}", response);
        return response;
    }

    public void deleteInitialSheet(String spreadSheetId, String existingSheet) throws IOException {
        log.info("Deleting initial Sheet from Spreadsheet with ID: {}", spreadSheetId);
        List<Sheet> sheetProperties = getSheetProperties(spreadSheetId);
        for (Sheet sheet: sheetProperties) {
            if (sheet.getProperties().getTitle().equals(existingSheet)) {
                continue;
            }
            deleteSheetFromSpreadSheet(spreadSheetId, sheet.getProperties().getTitle());
        }
    }

    public BatchUpdateSpreadsheetResponse deleteSheetFromSpreadSheet(String spreadSheetId, String title)
            throws IOException {
        log.info("Deleting Sheet {} from Spreadsheet with ID: {}",title, spreadSheetId);

        BatchUpdateSpreadsheetRequest request = new BatchUpdateSpreadsheetRequest();
        DeleteSheetRequest deleteSheetRequest = new DeleteSheetRequest();
        Request deleteRequest = new Request();
        deleteSheetRequest.setSheetId(getSheetIdByTitle(spreadSheetId, title));
        deleteRequest.setDeleteSheet(deleteSheetRequest);
        request.setRequests(Collections.singletonList(deleteRequest));
        BatchUpdateSpreadsheetResponse response = sheets.spreadsheets().batchUpdate(spreadSheetId, request).execute();
        log.info("Response: {}", response);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Sheet> getSheetProperties(String spreadSheetId) throws IOException {
        return (List<Sheet>) sheets.spreadsheets().get(spreadSheetId).execute().get("sheets");
    }

    private Integer getSheetIdByTitle(String spreadSheetId, String title) throws IOException {
        List<Sheet> sheetProperties = getSheetProperties(spreadSheetId);
        for (Sheet sheet : sheetProperties) {
            if (sheet.getProperties().getTitle().equals(title)) {
                return sheet.getProperties().getSheetId();
            }
        }
        throw new IllegalArgumentException(String.format("Sheet with title %s not found!", title));
    }

    public ValueRange getDataFromSpreadSheet(String spreadSheetId, String range) throws IOException {
        return sheets.spreadsheets().values().get(spreadSheetId, range).execute();
    }

    public UpdateValuesResponse writeDataToSpreadSheet(String spreadsheetId, String range, List<List<Object>> values)
            throws IOException {
        UpdateValuesResponse response = sheets.spreadsheets().values()
                .update(spreadsheetId, range, new ValueRange().setValues(values))
                .setValueInputOption("RAW")
                .execute();
        log.info("Response: {}", response);
        return response;
    }
}
