package de.asideas.crowdsource.presentation;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ErrorResponse {

    private String errorCode;

    private Map<String, String> fieldViolations = new HashMap<>();

    private ErrorResponse() {
    }

    public ErrorResponse(String errorCode) {
        this.errorCode = errorCode;
    }

    public ErrorResponse addConstraintViolation(String fieldName, String violation) {
        fieldViolations.put(fieldName, violation);
        return this;
    }
}
