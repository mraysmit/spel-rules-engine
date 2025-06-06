package dev.mars.rulesengine.core.service.validation;

/**
 * Abstract base class for validators.
 * Provides common functionality for all validators.
 * 
 * @param <T> The type of object this validation can validate
 */
public abstract class AbstractValidator<T> implements Validator<T> {
    private final String name;
    private final Class<T> type;

    /**
     * Create a new AbstractValidator with the specified name and type.
     * 
     * @param name The name of the validation
     * @param type The class of objects this validation can validate
     */
    protected AbstractValidator(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the type of objects this validation can validate.
     * 
     * @return The class of objects this validation can validate
     */
    public Class<T> getType() {
        return type;
    }

}
