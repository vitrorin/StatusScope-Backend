package com.itesm.interfaces.rest;

import java.util.Map;

public class OutbreakImportResultDto {
    public boolean success;
    public String message;
    public OutbreakImportSummaryDto municipal;
    public OutbreakImportSummaryDto state;

    public OutbreakImportResultDto() {
    }

    public OutbreakImportResultDto(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static OutbreakImportResultDto error(String message) {
        return new OutbreakImportResultDto(false, message);
    }

    public static OutbreakImportResultDto success(String message,
                                                   OutbreakImportSummaryDto municipal,
                                                   OutbreakImportSummaryDto state) {
        OutbreakImportResultDto result = new OutbreakImportResultDto(true, message);
        result.municipal = municipal;
        result.state = state;
        return result;
    }

    public static class OutbreakImportSummaryDto {
        public int created;
        public int updated;
        public int unchanged;
        public int ended;
        public int activeRows;

        public OutbreakImportSummaryDto() {
        }

        public OutbreakImportSummaryDto(int created, int updated, int unchanged, int ended, int activeRows) {
            this.created = created;
            this.updated = updated;
            this.unchanged = unchanged;
            this.ended = ended;
            this.activeRows = activeRows;
        }
    }
}