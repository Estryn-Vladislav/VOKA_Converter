package com.innowise.google;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;

@Deprecated
public class SheetsHandlerFirstVersion extends GoogleBasics {

    private static final String SHEETS_ID = "1yKLY63xyFbEu77c3NF0ZwGAPecBKv7Mw42MBeRZIXDw";
    private static final List<String> SHEETS_SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String SHEETS_CREDENTIALS_PATH = "/sheets-credentials.json";
    private static final String SHEETS_TOKEN_PATH = "tokens/sheets";

    public SheetsHandlerFirstVersion() throws GeneralSecurityException, IOException {
    }

    public void convertSheetsToJsonAndSaveLocally() throws IOException {
        Sheets sheetsService = new Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(SHEETS_CREDENTIALS_PATH, SHEETS_SCOPES, SHEETS_TOKEN_PATH))
                .setApplicationName(APPLICATION_NAME)
                .build();
        Spreadsheet sheets = sheetsService.spreadsheets().get(SHEETS_ID).execute();
        Map<String, Integer> sheetsMap = sheets.getSheets().stream()
                .map(Sheet::getProperties)
                .collect(Collectors.toMap(SheetProperties::getTitle, SheetProperties::getIndex));

        Map<String, GridRange> namedRanges = sheets.getNamedRanges().stream()
                .collect(Collectors.toMap(NamedRange::getName, NamedRange::getRange));
        List<Object> pathologysEn = new LinkedList<>();
        List<Object> pathologysRu = new LinkedList<>();
        List<Object> pathologysDe = new LinkedList<>();
        long sourceIndex = 1L;
        for (Map.Entry<String, Integer> sheet : sheetsMap.entrySet()) {
            try {
                ValueRange response = null;
                String rangeName = sheet.getKey().replace(" ", "");
                if (!namedRanges.containsKey(rangeName)) {
                    continue; // go to next sheet if range wasn't provided.
                }
                String range = getRange(namedRanges, sheet, rangeName);
                response = sheetsService.spreadsheets().values()
                        .get(SHEETS_ID, range)
                        .execute();

                List<List<Object>> values = response.getValues();

                if (values == null || values.isEmpty()) {
                    System.out.println("No data found.");
                } else {

                    List<Object> typesEn = null;
                    List<Object> typesDe = null;
                    List<Object> typesRu = null;

                    for (List row : values) {
                        if (!EMPTY_STRING.equals(row.get(0))) {
                            Map<String, Object> pathologyEn = new HashMap<>();
                            Map<String, Object> pathologyRu = new HashMap<>();
                            Map<String, Object> pathologyDe = new HashMap<>();
                            pathologysEn.add(pathologyEn);
                            pathologysDe.add(pathologyDe);
                            pathologysRu.add(pathologyRu);
                            fillPathology(sourceIndex, row, pathologyEn, pathologyRu, pathologyDe);

                            typesEn = new LinkedList<>();
                            typesDe = new LinkedList<>();
                            typesRu = new LinkedList<>();
                            pathologyEn.put("types", typesEn);
                            pathologyDe.put("types", typesDe);
                            pathologyRu.put("types", typesRu);
                            addType(row, typesEn, typesDe, typesRu);
                        } else {
                            addType(row, typesEn, typesDe, typesRu);
                        }
                        sourceIndex++;
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        save(pathologysEn, pathologysRu, pathologysDe);
    }

    private static void save(List<Object> pathologysEn, List<Object> pathologysRu, List<Object> pathologysDe) throws IOException {
        BufferedWriter writerEn = new BufferedWriter(new FileWriter("src/main/resources/json-results/en.json"));
        BufferedWriter writerRu = new BufferedWriter(new FileWriter("src/main/resources/json-results/ru.json"));
        BufferedWriter writerDe = new BufferedWriter(new FileWriter("src/main/resources/json-results/de.json"));
        writerEn.write(JSON_FACTORY.toPrettyString(pathologysEn));
        writerDe.write(JSON_FACTORY.toPrettyString(pathologysDe));
        writerRu.write(JSON_FACTORY.toPrettyString(pathologysRu));

        writerEn.close();
        writerDe.close();
        writerRu.close();

    }

    private static void fillPathology(long sourceIndex, List row, Map<String, Object> pathologyEn, Map<String, Object> pathologyRu, Map<String, Object> pathologyDe) {
        pathologyEn.put("name", row.get(0));
        pathologyDe.put("name", row.get(2));
        pathologyRu.put("name", row.get(4));
        pathologyEn.put("sourceIndex", sourceIndex);
        pathologyRu.put("sourceIndex", sourceIndex);
        pathologyDe.put("sourceIndex", sourceIndex);
    }

    private static String getRange(Map<String, GridRange> namedRanges, Map.Entry<String, Integer> sheet, String rangeName) {
        return sheet.getKey()
                + "!A" + (namedRanges.get(rangeName).getStartRowIndex() + 1)
                + ":I" + namedRanges.get(rangeName).getEndRowIndex();
    }

    private static void addType(List row, List typesEn, List typesDe, List typesRu) {
        Map<String, Object> typeMapEn = new HashMap<>();
        Map<String, Object> typeMapDe = new HashMap<>();
        Map<String, Object> typeMapRu = new HashMap<>();
        typeMapEn.put("type", row.get(1));
        typeMapDe.put("type", row.get(3));
        typeMapRu.put("type", row.get(5));

        List<Object> sketchfabs = new LinkedList<>();
        typeMapEn.put("sketchfabs", sketchfabs);
        typeMapDe.put("sketchfabs", sketchfabs);
        typeMapRu.put("sketchfabs", sketchfabs);

        fillModel(row, sketchfabs);
        fillCut(row, sketchfabs);
        fillHemo(row, sketchfabs);

        typesEn.add(typeMapEn);
        typesDe.add(typeMapDe);
        typesRu.add(typeMapRu);
    }

    private static void fillHemo(List row, List<Object> sketchfabs) {
        if (row.size() > 8 && !EMPTY_STRING.equals(row.get(8))) {
            Map<String, Object> hemo = new HashMap<>();
            hemo.put("type", "hemo");
            String link = ((String) row.get(8)).split("\\?")[0];
            fillLinks(hemo, link);
            sketchfabs.add(hemo);
        }
    }

    private static void fillCut(List row, List<Object> sketchfabs) {
        if (row.size() > 7 && !EMPTY_STRING.equals(row.get(7))) {
            Map<String, Object> cut = new HashMap<>();
            cut.put("type", "cut");
            String link = ((String) row.get(7)).split("\\?")[0];
            fillLinks(cut, link);
            sketchfabs.add(cut);
        }
    }

    private static void fillModel(List row, List<Object> sketchfabs) {
        if (row.size() > 6 && !EMPTY_STRING.equals(row.get(6))) { //here we need check size.
            Map<String, Object> model = new HashMap<>();
            String link = ((String) row.get(6)).split("\\?")[0];
            model.put("type", "model");
            fillLinks(model, link);
            sketchfabs.add(model);
        }
    }

    private static void fillLinks(Map<String, Object> model, String link) {
        model.put("link", link);
        model.put("thumbnail", getThumbnail(link));
    }

    private static String getThumbnail(String link) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet("https://sketchfab.com/oembed?url=" + link);
            CloseableHttpResponse result = httpClient.execute(request);

            String json = EntityUtils.toString(result.getEntity(), "UTF-8");
            try {
                JSONParser parser = new JSONParser();
                return (String) ((JSONObject) parser.parse(json)).get("thumbnail_url");
            } catch (ParseException e) {
                System.out.println(e.getMessage());
            }
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return null;
    }
}