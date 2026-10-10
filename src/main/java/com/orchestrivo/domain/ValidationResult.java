package com.orchestrivo.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidationResult {
    private final boolean isValid;
    private final List<String> errors;

    public ValidationResult(boolean isValid, List<String> errors) {
        this.isValid = isValid;
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    }
    
    public static ValidationResult failure(String error) {
        return new ValidationResult(false, Collections.singletonList(error));
    }

    public boolean isValid() {
        return isValid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
