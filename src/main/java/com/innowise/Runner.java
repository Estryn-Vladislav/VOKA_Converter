package com.innowise;

import com.innowise.google.SheetsHandler;

public class Runner {
    public static void main(String[] args) throws Exception {
        new SheetsHandler().generateJsonFilesFromSheets();
//        new DriveHandler().uploadFilesToDrive();
    }
}