package com.ssafy.backend.game.dto;

public class ValidationResultDto {
    private final boolean valid;
    private final String errorMessage;

    private ValidationResultDto(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static ValidationResultDto valid() {
        return new ValidationResultDto(true, null);
    }

    public static ValidationResultDto invalid(String errorMessage) {
        return new ValidationResultDto(false, errorMessage);
    }

    public boolean isValid() { return valid; }
    public String getErrorMessage() { return errorMessage; }
}
