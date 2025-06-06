package dev.mars.rulesengine.core.service.transform;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.TransformerRule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.common.NamedService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic transformer that can transform any type of object based on rules and field mappings.
 * This transformer contains no business logic but receives as parameters a set of rules and
 * a set of fields and values for both positive and negative rule results.
 *
 * @param <T> The type of object this transformer can transform
 */
public class GenericTransformer<T> implements NamedService {
    private static final Logger LOGGER = Logger.getLogger(GenericTransformer.class.getName());

    private final String name;
    private final Class<T> type;
    private final RulesEngine rulesEngine;
    private final List<TransformerRule<T>> transformerRules;

    /**
     * Create a new GenericTransformer with the specified parameters.
     *
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     * @param rulesEngine The rules engine to use for transformation
     * @param transformerRules The transformation rules to apply
     */
    public GenericTransformer(String name, Class<T> type, RulesEngine rulesEngine, List<TransformerRule<T>> transformerRules) {
        this.name = name;
        this.type = type;
        this.rulesEngine = rulesEngine != null ? rulesEngine : new RulesEngine(new RulesEngineConfiguration());
        this.transformerRules = transformerRules != null ? new ArrayList<>(transformerRules) : new ArrayList<>();
    }

    /**
     * Create a new GenericTransformer with the specified parameters.
     *
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     * @param transformerRules The transformation rules to apply
     */
    public GenericTransformer(String name, Class<T> type, List<TransformerRule<T>> transformerRules) {
        this(name, type, null, transformerRules);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the type of objects this transformer can transform.
     * 
     * @return The class of objects this transformer can transform
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * Add a transformation rule to this transformer.
     *
     * @param transformerRule The transformation rule to add
     */
    public void addTransformationRule(TransformerRule<T> transformerRule) {
        if (transformerRule != null) {
            this.transformerRules.add(transformerRule);
        }
    }

    /**
     * Transform a value of type T.
     * 
     * @param value The value to transform
     * @return The transformed value
     */
    public T transform(T value) {
        if (value == null) {
            return null;
        }

        try {
            // Create a copy of the object to transform
            T transformedValue = createCopy(value);

            // Apply each transformation rule
            for (TransformerRule<T> transformerRule : transformerRules) {
                applyTransformationRule(transformerRule, value, transformedValue);
            }

            return transformedValue;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error transforming value: " + e.getMessage(), e);
            return value; // Return the original value if transformation fails
        }
    }

    /**
     * Transform a value and return a RuleResult.
     *
     * @param value The value to transform
     * @return A RuleResult containing the transformation outcome
     */
    public RuleResult transformWithResult(T value) {
        if (value == null) {
            return RuleResult.error(getName(), "Value is null");
        }

        try {
            T transformedValue = transform(value);

            // Check if any transformation was applied
            boolean transformed = !value.equals(transformedValue);

            if (transformed) {
                return RuleResult.match(getName(), "Value transformed successfully");
            } else {
                return RuleResult.noMatch();
            }
        } catch (Exception e) {
            return RuleResult.error(getName(), "Error transforming value: " + e.getMessage());
        }
    }

    /**
     * Apply a transformation rule to a value.
     *
     * @param transformerRule The transformation rule to apply
     * @param originalValue The original value
     * @param transformedValue The value to transform
     */
    private void applyTransformationRule(TransformerRule<T> transformerRule, T originalValue, T transformedValue) {
        // Set up facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("value", originalValue);

        // Add any additional facts from the transformation rule
        if (transformerRule.getAdditionalFacts() != null) {
            facts.putAll(transformerRule.getAdditionalFacts());
        }

        // Evaluate the rule
        RuleResult ruleResult = rulesEngine.executeRulesList(List.of(transformerRule.getRule()), facts);

        // Apply the appropriate field transformation action based on the rule result
        if (ruleResult.isTriggered()) {
            // Rule was triggered, apply positive action
            for (FieldTransformerAction<T> action : transformerRule.getPositiveActions()) {
                applyFieldTransformationAction(action, originalValue, transformedValue, facts);
            }
        } else {
            // Rule was not triggered, apply negative action
            for (FieldTransformerAction<T> action : transformerRule.getNegativeActions()) {
                applyFieldTransformationAction(action, originalValue, transformedValue, facts);
            }
        }
    }

    /**
     * Apply a field transformation action to a value.
     *
     * @param action The field transformation action to apply
     * @param originalValue The original value
     * @param transformedValue The value to transform
     * @param facts The facts for rule evaluation
     */
    private void applyFieldTransformationAction(FieldTransformerAction<T> action, T originalValue, T transformedValue, Map<String, Object> facts) {
        try {
            // Get the field value from the original object
            Object fieldValue = action.getFieldValueExtractor().apply(originalValue);

            // Calculate the new field value
            Object newFieldValue = action.getFieldValueTransformer().apply(fieldValue, facts);

            // Set the new field value on the transformed object
            action.getFieldValueSetter().accept(transformedValue, newFieldValue);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error applying field transformation action: " + e.getMessage(), e);
        }
    }

    /**
     * Create a copy of an object.
     *
     * @param value The object to copy
     * @return A copy of the object
     * @throws Exception If an error occurs during copying
     */
    @SuppressWarnings("unchecked")
    private T createCopy(T value) throws Exception {
        // Try to use a copy constructor if available
        try {
            return (T) value.getClass().getConstructor(value.getClass()).newInstance(value);
        } catch (NoSuchMethodException e) {
            // No copy constructor, try to use the default constructor and copy fields
            T copy = (T) value.getClass().getConstructor().newInstance();

            // Copy all fields
            for (Field field : value.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                field.set(copy, field.get(value));
            }

            return copy;
        }
    }
}
