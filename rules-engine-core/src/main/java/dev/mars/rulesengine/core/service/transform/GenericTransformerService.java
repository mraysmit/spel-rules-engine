package dev.mars.rulesengine.core.service.transform;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.engine.model.TransformerRule;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for generic transformation operations.
 * This service provides functionality to create and apply transformers dynamically
 * based on rules and field mappings.
 */
public class GenericTransformerService {
    private static final Logger LOGGER = Logger.getLogger(GenericTransformerService.class.getName());
    private final LookupServiceRegistry registry;
    private final RulesEngine rulesEngine;
    /**
     * Create a new GenericTransformerService with the specified registry and rules engine.
     * 
     * @param registry The lookup service registry
     * @param rulesEngine The rules engine to use for transformation
     */
    public GenericTransformerService(LookupServiceRegistry registry, RulesEngine rulesEngine) {
        this.registry = registry;
        this.rulesEngine = rulesEngine;
        LOGGER.info("GenericTransformerService initialized with custom RulesEngine");
    }

    /**
     * Create and register a generic transformer with the specified parameters.
     * 
     * @param <T> The type of objects this transformer can transform
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     * @param transformerRules The transformation rules to apply
     * @return The created transformer
     */
    public <T> GenericTransformer<T> createTransformer(String name, Class<T> type, List<TransformerRule<T>> transformerRules) {
        LOGGER.fine("Creating transformer: " + name);
        GenericTransformer<T> transformer = new GenericTransformer<>(name, type, rulesEngine, transformerRules);
        registry.registerService(transformer);
        return transformer;
    }

    /**
     * Create and register a generic transformer with a single rule.
     * 
     * @param <T> The type of objects this transformer can transform
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     * @param rule The rule to apply
     * @param positiveActions The actions to apply if the rule evaluates to true
     * @param negativeActions The actions to apply if the rule evaluates to false
     * @return The created transformer
     */
    public <T> GenericTransformer<T> createTransformer(
            String name, 
            Class<T> type, 
            Rule rule, 
            List<FieldTransformerAction<T>> positiveActions, 
            List<FieldTransformerAction<T>> negativeActions) {

        LOGGER.fine("Creating transformer with single rule: " + name);

        List<TransformerRule<T>> transformerRules = new ArrayList<>();
        transformerRules.add(new TransformerRule<>(rule, positiveActions, negativeActions));

        return createTransformer(name, type, transformerRules);
    }

    /**
     * Create and register a generic transformer with a single rule and additional facts.
     * 
     * @param <T> The type of objects this transformer can transform
     * @param name The name of the transformer
     * @param type The class of objects this transformer can transform
     * @param rule The rule to apply
     * @param positiveActions The actions to apply if the rule evaluates to true
     * @param negativeActions The actions to apply if the rule evaluates to false
     * @param additionalFacts Additional facts to use during rule evaluation
     * @return The created transformer
     */
    public <T> GenericTransformer<T> createTransformer(
            String name, 
            Class<T> type, 
            Rule rule, 
            List<FieldTransformerAction<T>> positiveActions, 
            List<FieldTransformerAction<T>> negativeActions,
            Map<String, Object> additionalFacts) {

        LOGGER.fine("Creating transformer with single rule and additional facts: " + name);

        List<TransformerRule<T>> transformerRules = new ArrayList<>();
        transformerRules.add(new TransformerRule<>(rule, positiveActions, negativeActions, additionalFacts));

        return createTransformer(name, type, transformerRules);
    }

    /**
     * Transform a value using a dynamically created transformer.
     * 
     * @param <T> The type of the value to transform
     * @param value The value to transform
     * @param transformerRules The transformation rules to apply
     * @return The transformed value
     */
    public <T> T transform(T value, List<TransformerRule<T>> transformerRules) {
        if (value == null) {
            return null;
        }

        LOGGER.fine("Transforming value using dynamic transformer");

        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) value.getClass();
        String name = "DynamicTransformer-" + System.currentTimeMillis();

        GenericTransformer<T> transformer = new GenericTransformer<>(name, type, rulesEngine, transformerRules);
        return transformer.transform(value);
    }

    /**
     * Transform a value using a dynamically created transformer and return a RuleResult.
     * 
     * @param <T> The type of the value to transform
     * @param value The value to transform
     * @param transformerRules The transformation rules to apply
     * @return A RuleResult containing the transformation outcome
     */
    public <T> RuleResult transformWithResult(T value, List<TransformerRule<T>> transformerRules) {
        if (value == null) {
            return RuleResult.error("DynamicTransformer", "Value is null");
        }

        LOGGER.fine("Transforming value using dynamic transformer with result");

        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) value.getClass();
        String name = "DynamicTransformer-" + System.currentTimeMillis();

        GenericTransformer<T> transformer = new GenericTransformer<>(name, type, rulesEngine, transformerRules);
        return transformer.transformWithResult(value);
    }

    /**
     * Transform a value using a registered transformer.
     * 
     * @param <T> The type of the value to transform
     * @param transformerName The name of the transformer to use
     * @param value The value to transform
     * @return The transformed value
     */
    @SuppressWarnings("unchecked")
    public <T> T transform(String transformerName, T value) {
        LOGGER.fine("Transforming value using transformer: " + transformerName);

        // Get the transformer from the registry
        GenericTransformer<?> transformer = registry.getService(transformerName, GenericTransformer.class);
        if (transformer == null) {
            LOGGER.warning("Transformer not found: " + transformerName);
            return value;
        }

        // Check if the transformer can handle this type
        if (value != null && !transformer.getType().isInstance(value)) {
            LOGGER.warning("Transformer " + transformerName + " cannot handle type: " + value.getClass().getName());
            return value;
        }

        // Call the transformer with the appropriate type
        GenericTransformer<T> typedTransformer = (GenericTransformer<T>) transformer;
        return typedTransformer.transform(value);
    }

    /**
     * Transform a value using a registered transformer and return a RuleResult.
     * 
     * @param <T> The type of the value to transform
     * @param transformerName The name of the transformer to use
     * @param value The value to transform
     * @return A RuleResult containing the transformation outcome
     */
    public <T> RuleResult transformWithResult(String transformerName, T value) {
        LOGGER.fine("Transforming value using transformer with result: " + transformerName);

        // Get the transformer from the registry
        GenericTransformer<?> transformer = registry.getService(transformerName, GenericTransformer.class);
        if (transformer == null) {
            LOGGER.warning("Transformer not found: " + transformerName);
            return RuleResult.error(transformerName, "Transformer not found");
        }

        // Check if the transformer can handle this type
        if (value != null && !transformer.getType().isInstance(value)) {
            LOGGER.warning("Transformer " + transformerName + " cannot handle type: " + value.getClass().getName());
            return RuleResult.error(transformerName, "Transformer cannot handle type: " + value.getClass().getName());
        }

        try {
            // Cast to the appropriate type and use transformWithResult method
            @SuppressWarnings("unchecked")
            GenericTransformer<T> typedTransformer = (GenericTransformer<T>) transformer;
            return typedTransformer.transformWithResult(value);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error transforming value: " + e.getMessage(), e);
            return RuleResult.error(transformerName, "Error transforming value: " + e.getMessage());
        }
    }

    /**
     * Apply a rule to a value using a registered transformer.
     * If the rule evaluates to true, the value is transformed using the specified transformer.
     * 
     * @param <T> The type of the value
     * @param rule The rule to apply
     * @param value The value to transform
     * @param additionalFacts Additional facts to use during rule evaluation
     * @param transformerName The name of the transformer to use
     * @return The transformed value if the rule evaluates to true, otherwise the original value
     */
    public <T> T applyRule(Rule rule, T value, Map<String, Object> additionalFacts, String transformerName) {
        LOGGER.fine("Applying rule to value: " + rule.getName());

        // Get the transformer from the registry
        GenericTransformer<?> transformer = registry.getService(transformerName, GenericTransformer.class);
        if (transformer == null) {
            LOGGER.warning("Transformer not found: " + transformerName);
            return value;
        }

        // Check if the transformer can handle this type
        if (value != null && !transformer.getType().isInstance(value)) {
            LOGGER.warning("Transformer " + transformerName + " cannot handle type: " + value.getClass().getName());
            return value;
        }

        // Create facts for the rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("value", value);

        // Add any additional facts
        if (additionalFacts != null) {
            facts.putAll(additionalFacts);
        }

        // Create a list of rules
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Execute the rule
        RuleResult result = rulesEngine.executeRulesList(rules, facts);

        // If the rule was triggered, transform the value
        if (result.isTriggered()) {
            LOGGER.fine("Rule triggered, transforming value");
            @SuppressWarnings("unchecked")
            GenericTransformer<T> typedTransformer = (GenericTransformer<T>) transformer;
            return typedTransformer.transform(value);
        } else {
            LOGGER.fine("Rule not triggered, returning original value");
            return value;
        }
    }

    /**
     * Apply a rule condition to a value using a registered transformer.
     * If the condition evaluates to true, the value is transformed using the specified transformer.
     * 
     * @param <T> The type of the value
     * @param ruleCondition The rule condition to apply
     * @param value The value to transform
     * @param additionalFacts Additional facts to use during rule evaluation
     * @param transformerName The name of the transformer to use
     * @return The transformed value if the rule condition evaluates to true, otherwise the original value
     */
    public <T> T applyRuleCondition(String ruleCondition, T value, Map<String, Object> additionalFacts, String transformerName) {
        LOGGER.fine("Applying rule condition to value: " + ruleCondition);

        // Create a rule from the condition
        Rule rule = new Rule(
            "Transformation Rule",
            ruleCondition,
            "Transformation rule with condition: " + ruleCondition
        );

        // Apply the rule
        return applyRule(rule, value, additionalFacts, transformerName);
    }

    /**
     * Apply a rule condition to a value using a registered transformer.
     * If the condition evaluates to true, the value is transformed using the specified transformer.
     * This overload is for compatibility with TransformationEnrichmentService.
     * 
     * @param <T> The type of the value
     * @param ruleCondition The rule condition to apply
     * @param value The value to transform
     * @param lookupData The lookup data to use for transformation
     * @param transformerName The name of the transformer to use
     * @return The transformed value if the rule condition evaluates to true, otherwise the original value
     */
    public <T> T applyRuleCondition(String ruleCondition, T value, Object lookupData, String transformerName) {
        LOGGER.fine("Applying rule condition to value with lookup data: " + ruleCondition);

        // Create a map of additional facts with the lookup data
        Map<String, Object> additionalFacts = new HashMap<>();
        additionalFacts.put("lookupData", lookupData);

        // Create a rule from the condition that uses lookupData instead of additionalFacts
        Rule rule = new Rule(
            "Transformation Rule",
            ruleCondition.replace("#coreData", "#value"),
            "Transformation rule with condition: " + ruleCondition
        );

        // Apply the rule
        return applyRule(rule, value, additionalFacts, transformerName);
    }

    /**
     * Create a field transformer action for a specific field.
     * 
     * @param <T> The type of objects this action can transform
     * @param fieldName The name of the field
     * @param fieldValueExtractor A function to extract the field value
     * @param fieldValueTransformer A function to transform the field value
     * @param fieldValueSetter A function to set the field value
     * @return The created field transformer action
     */
    public <T> FieldTransformerAction<T> createFieldTransformerAction(
            String fieldName,
            Function<T, Object> fieldValueExtractor,
            BiFunction<Object, Map<String, Object>, Object> fieldValueTransformer,
            BiConsumer<T, Object> fieldValueSetter) {

        return new FieldTransformerActionBuilder<T>()
            .withFieldName(fieldName)
            .withFieldValueExtractor(fieldValueExtractor)
            .withFieldValueTransformer(fieldValueTransformer)
            .withFieldValueSetter(fieldValueSetter)
            .build();
    }

    /**
     * Create a copy of an object.
     * This is a utility method that can be used by transformers to create a copy of an object.
     * 
     * @param <T> The type of the object
     * @param value The object to copy
     * @return A copy of the object
     * @throws Exception If an error occurs during copying
     */
    @SuppressWarnings("unchecked")
    public <T> T createCopy(T value) throws Exception {
        if (value == null) {
            return null;
        }

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
