package com.innowise.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.innowise.model.Catalog;
import com.innowise.model.Pathology;
import com.innowise.model.PathologyType;
import com.innowise.model.Sketchfab;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SheetsHandler extends GoogleBasics {

    private static final String SHEETS_ID = "1yKLY63xyFbEu77c3NF0ZwGAPecBKv7Mw42MBeRZIXDw";
    private static final List<String> SHEETS_SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);
    private static final String SHEETS_CREDENTIALS_PATH = "/sheets-credentials.json";
    private static final String SHEETS_TOKEN_PATH = "tokens/sheets";
    private static final String ROOT_PATH_JSON = "src/main/resources/catalogs/";
    private static final String FIRST_COLUMN = "!A";
    private static final String LAST_COLUMN = ":I";
    private static final String CATALOGS_RANGE = "!A2:C2";
    private List<Pathology> pathologiesEn;
    private List<Pathology> pathologiesRu;
    private List<Pathology> pathologiesDe;
    private Sheets sheetsService;
    private ObjectMapper mapper;
    private int dirID = 0;
    private long pathologyId = 0;
    private long typeId = 0;
    private long sketchfabId = 0;

    public SheetsHandler() throws GeneralSecurityException, IOException {
        sheetsService = new Sheets.Builder(httpTransport, JSON_FACTORY, getCredentials(SHEETS_CREDENTIALS_PATH, SHEETS_SCOPES, SHEETS_TOKEN_PATH))
                .setApplicationName(APPLICATION_NAME)
                .build();
        mapper = new ObjectMapper();
    }

    public void generateJsonFilesFromSheets() throws IOException {
        Spreadsheet document = sheetsService.spreadsheets().get(SHEETS_ID).execute();
        List<Sheet> sheets = document.getSheets();
        List<NamedRange> namedRanges = document.getNamedRanges();
        createCatalogJson(sheets);
        for (Sheet sheet : sheets) {
            System.out.println("Handling page: " + sheet.getProperties().getTitle());
            setDataFromSheet(sheet, namedRanges);
            writeDataToJsonFile();
        }
    }

    private void createCatalogJson(List<Sheet> sheets) throws IOException {
        List<Catalog> catalogs = new ArrayList<>();
        long sheetId = 0;
        for (Sheet sheet : sheets) {
            Catalog catalog = new Catalog();
            catalog.setId(sheetId++);
            ValueRange pageValuesFromCatalogsRange = sheetsService.spreadsheets().values()
                    .get(SHEETS_ID, sheet.getProperties().getTitle() + CATALOGS_RANGE)
                    .execute();
            List<List<Object>> rows = pageValuesFromCatalogsRange.getValues();
            catalog.setNameEn((String) rows.get(0).get(0));
            catalog.setNameDe((String) rows.get(0).get(1));
            catalog.setNameRu((String) rows.get(0).get(2));
            //TODO change this hardcore settings in future
            catalog.setState("Available");
            catalog.setIcon("");
            catalogs.add(catalog);
        }

        mapper.writeValue(new File(ROOT_PATH_JSON + "/catalogs.json"), catalogs);
    }

    private void writeDataToJsonFile() throws IOException {
        mapper.writeValue(new File(ROOT_PATH_JSON + dirID + "/en.json"), pathologiesEn);
        mapper.writeValue(new File(ROOT_PATH_JSON + dirID + "/de.json"), pathologiesDe);
        mapper.writeValue(new File(ROOT_PATH_JSON + dirID + "/ru.json"), pathologiesRu);
        dirID++;
    }

    private void setDataFromSheet(Sheet sheet, List<NamedRange> namedRanges) throws IOException {

        pathologiesEn = new ArrayList<>();
        pathologiesDe = new ArrayList<>();
        pathologiesRu = new ArrayList<>();
        Pathology pathologyEn = null;
        Pathology pathologyDe = null;
        Pathology pathologyRu = null;
        List<PathologyType> typesEn = new ArrayList<>();
        List<PathologyType> typesDe = new ArrayList<>();
        List<PathologyType> typesRu = new ArrayList<>();
        PathologyType typeEn;
        PathologyType typeDe;
        PathologyType typeRu;
        List<Sketchfab> sketchfabs = new ArrayList<>();
        Sketchfab sketchfab;
        long currentRow = 0;

        List<List<Object>> rowsFromSheet = getRowsFromSheet(sheet, namedRanges);

        for (List<Object> rows : rowsFromSheet) {
            currentRow++;
            List<Object> fixedRows = fixRow(rows);
            if (!fixedRows.get(0).equals("")) {
                if (pathologyEn != null) {
                    pathologyEn.setTypes(typesEn);
                    pathologyDe.setTypes(typesDe);
                    pathologyRu.setTypes(typesRu);
                    typesEn = new ArrayList<>();
                    typesRu = new ArrayList<>();
                    typesDe = new ArrayList<>();

                    pathologiesEn.add(pathologyEn);
                    pathologiesDe.add(pathologyDe);
                    pathologiesRu.add(pathologyRu);
                }
                pathologyEn = new Pathology();
                pathologyEn.setId(pathologyId);
                pathologyEn.setName((String) fixedRows.get(0));

                pathologyDe = new Pathology();
                pathologyDe.setId(pathologyId);
                pathologyDe.setName((String) fixedRows.get(2));

                pathologyRu = new Pathology();
                pathologyRu.setId(pathologyId);
                pathologyRu.setName((String) fixedRows.get(4));
                pathologyId++;
            }
            typeEn = new PathologyType();
            typeEn.setId(typeId);
            typeEn.setName((String) fixedRows.get(1));

            typeDe = new PathologyType();
            typeDe.setId(typeId);
            typeDe.setName((String) fixedRows.get(3));

            typeRu = new PathologyType();
            typeRu.setId(typeId);
            typeRu.setName((String) fixedRows.get(5));
            typeId++;

            sketchfab = new Sketchfab();
            sketchfab.setId(sketchfabId++);
            sketchfab.setType("model");
            sketchfab.setLink((String) fixedRows.get(6));
            sketchfab.setThumbnail(getThumbnail(sketchfab.getLink()));
            sketchfabs.add(sketchfab);

            sketchfab = new Sketchfab();
            sketchfab.setId(sketchfabId++);
            sketchfab.setType("cut");
            sketchfab.setLink((String) fixedRows.get(7));
            sketchfab.setThumbnail(getThumbnail(sketchfab.getLink()));
            sketchfabs.add(sketchfab);

            sketchfab = new Sketchfab();
            sketchfab.setId(sketchfabId++);
            sketchfab.setType("hemo");
            sketchfab.setLink((String) fixedRows.get(8));
            sketchfab.setThumbnail(getThumbnail(sketchfab.getLink()));
            sketchfabs.add(sketchfab);

            typeEn.setSketchfabs(sketchfabs);
            typeDe.setSketchfabs(sketchfabs);
            typeRu.setSketchfabs(sketchfabs);
            sketchfabs = new ArrayList<>();

            typesEn.add(typeEn);
            typesDe.add(typeDe);
            typesRu.add(typeRu);


            if (currentRow == rowsFromSheet.size()) {
                pathologyEn.setTypes(typesEn);
                pathologyDe.setTypes(typesDe);
                pathologyRu.setTypes(typesRu);

                pathologiesEn.add(pathologyEn);
                pathologiesDe.add(pathologyDe);
                pathologiesRu.add(pathologyRu);
            }
        }
    }

    private List<Object> fixRow(List<Object> row) {
        List<Object> fixedRows = new ArrayList<>();
        for (Object cell : row) {
            if (cell.equals("-")) {
                fixedRows.add("");
            } else {
                fixedRows.add(cell);
            }
        }
        return fixedRows;
    }

    private List<List<Object>> getRowsFromSheet(Sheet sheet, List<NamedRange> namedRanges) throws IOException {
        String currentPageName = sheet.getProperties().getTitle().replace(" ", "");
        GridRange currentPageNamedRange = namedRanges.stream().filter(r -> r.getName().equals(currentPageName)).findFirst().orElseThrow(RuntimeException::new).getRange();

        ValueRange pageValuesFromRange = sheetsService.spreadsheets().values()
                .get(SHEETS_ID, sheet.getProperties().getTitle() + FIRST_COLUMN + (currentPageNamedRange.getStartRowIndex() + 1) + LAST_COLUMN + currentPageNamedRange.getEndRowIndex())
                .execute();

        return pageValuesFromRange.getValues();
    }

    private String getThumbnail(String link) {
        if (link.equals("")) {
            return "";
        }
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet request = new HttpGet("https://sketchfab.com/oembed?url=" + link);
            CloseableHttpResponse result = httpClient.execute(request);

            String json = EntityUtils.toString(result.getEntity(), "UTF-8");

            JSONParser parser = new JSONParser();
            return (String) ((JSONObject) parser.parse(json)).get("thumbnail_url");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}