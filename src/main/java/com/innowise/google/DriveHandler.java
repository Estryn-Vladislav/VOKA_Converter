package com.innowise.google;

import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class DriveHandler extends GoogleBasics {

    private static final List<String> DRIVE_SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String DRIVE_CREDENTIALS_PATH = "/drive-credentials.json";
    private static final String DRIVE_TOKEN_PATH = "tokens/drive";
    private static final String CONTENT_TYPE = "application/json";
    private static final String EN_FILE_NAME = "en.json";
    private static final String DE_FILE_NAME = "de.json";
    private static final String RU_FILE_NAME = "ru.json";
    private Drive service;
    private List<File> filesFromDriveNullable;

    public DriveHandler() throws GeneralSecurityException, IOException {
        service = new Drive.Builder(httpTransport, JSON_FACTORY, getCredentials(DRIVE_CREDENTIALS_PATH, DRIVE_SCOPES, DRIVE_TOKEN_PATH))
                .setApplicationName(APPLICATION_NAME)
                .build();
        filesFromDriveNullable = service.files().list().execute().getFiles();
    }

    public void uploadFilesToDrive() throws IOException {
        uploadFile(EN_FILE_NAME, new java.io.File("src/main/resources/json-results/Acquired heart diseases_en.json"));
        uploadFile(DE_FILE_NAME, new java.io.File("src/main/resources/json-results/Congenital heart defects_en.json"));
        uploadFile(RU_FILE_NAME, new java.io.File("src/main/resources/json-results/Vascular pathology_en.json"));
    }

    private void uploadFile(String name, java.io.File filePath) throws IOException {
        if (!filesFromDriveNullable.isEmpty()) {
            for (File file : filesFromDriveNullable) {
                if (file.getName().equals(name)) {
                    service.files().delete(file.getId()).execute();
                }
            }
        }
        File fileMeta = new File();
        fileMeta.setName(name);
        FileContent content = new FileContent(CONTENT_TYPE, filePath);
        service.files().create(fileMeta, content).setFields("id").execute();
    }

}