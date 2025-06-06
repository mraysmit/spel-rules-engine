package dev.mars.rulesengine.core.service.validation;

import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.common.NamedService;

/**
 * Interface for validation services.
 * @param <T> The type of object this validation can validate
 */
public interface Validator<T> extends NamedService {
    /**
     * Validate a value of type T.
     * 
     * @param value The value to validate
     * @return True if the value is valid, false otherwise
     */
    boolean validate(T value);

    /**
     * Validate a value of type T and return a detailed result.
     * 
     * @param value The value to validate
     * @return A RuleResult containing the validation outcome
     */
    default RuleResult validateWithResult(T value) {
        boolean isValid = validate(value);
        if (isValid) {
            return RuleResult.match(getName(), "Validation successful for " + getName());
        } else {
            return RuleResult.noMatch();
        }
    }

    /**
     * Get the type of objects this validation can validate.
     * 
     * @return The class of objects this validation can validate
     */
    Class<T> getType();
}
