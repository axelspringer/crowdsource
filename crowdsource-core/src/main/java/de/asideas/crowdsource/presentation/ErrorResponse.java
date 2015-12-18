package de.asideas.crowdsource.presentation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    public String getErrorCode() {
        return this.errorCode;
    }

    public Map<String, String> getFieldViolations() {
        return this.fieldViolations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ErrorResponse that = (ErrorResponse) o;
        return Objects.equals(errorCode, that.errorCode) &&
                Objects.equals(fieldViolations, that.fieldViolations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(errorCode, fieldViolations);
    }
}
