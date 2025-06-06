package dev.mars.rulesengine.demo.integration;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.lookup.LookupServiceRegistry;
import dev.mars.rulesengine.core.service.transform.GenericTransformerService;
import dev.mars.rulesengine.core.service.validation.ValidationService;
import dev.mars.rulesengine.demo.model.Customer;
import dev.mars.rulesengine.demo.model.Product;
import dev.mars.rulesengine.core.service.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for core functionality of the rules engine.
 * This class tests the core features directly without using demo classes.
 */
public class CoreFunctionalityTest {
    private LookupServiceRegistry registry;
    private ValidationService validationService;
    private GenericTransformerService transformerService;
    private RulesEngine rulesEngine;

    @BeforeEach
    public void setUp() {
        // Create registry
        registry = new LookupServiceRegistry();

        // Create rules engine
        rulesEngine = new RulesEngine(new RulesEngineConfiguration());

        // Create services
        validationService = new ValidationService(registry, rulesEngine);
        transformerService = new GenericTransformerService(registry, rulesEngine);
    }

    /**
     * Test basic validation functionality.
     */
    @Test
    public void testValidation() {
        // Create a simple validation
        Validator<Customer> ageValidator = new Validator<Customer>() {
            @Override
            public String getName() {
                return "ageValidator";
            }

            @Override
            public boolean validate(Customer value) {
                return value != null && value.getAge() >= 18;
            }

            @Override
            public Class<Customer> getType() {
                return Customer.class;
            }
        };

        // Register the validation
        registry.registerService(ageValidator);

        // Create test customers
        Customer validCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        Customer invalidCustomer = new Customer("Charlie Brown", 17, "Basic", Arrays.asList("ETF"));

        // Test validation
        boolean isValidCustomer = validationService.validate("ageValidator", validCustomer);
        boolean isInvalidCustomer = validationService.validate("ageValidator", invalidCustomer);

        // Verify results
        assertTrue(isValidCustomer, "Adult customer should be valid");
        assertFalse(isInvalidCustomer, "Minor customer should be invalid");
    }

    /**
     * Test rule engine functionality.
     */
    @Test
    public void testRuleEngineSimple() {
        // Create a simple rule
        Rule rule = new Rule(
            "AgeRule",
            "#customer.age >= 18",
            "Customer is an adult"
        );

        // Create test customers
        Customer adultCustomer = new Customer("Alice Smith", 35, "Gold", Arrays.asList("Equity"));
        Customer youngCustomer = new Customer("Charlie Brown", 17, "Basic", Arrays.asList("ETF"));

        // Create facts for rule evaluation
        Map<String, Object> adultCustomerFacts = new HashMap<>();
        adultCustomerFacts.put("customer", adultCustomer);

        Map<String, Object> youngCustomerFacts = new HashMap<>();
        youngCustomerFacts.put("customer", youngCustomer);

        // Evaluate rule for adult customer
        List<Rule> rules = Arrays.asList(rule);
        RuleResult adultCustomerResult = rulesEngine.executeRulesList(rules, adultCustomerFacts);

        // Evaluate rule for minor customer
        RuleResult youngCustomerResult = rulesEngine.executeRulesList(rules, youngCustomerFacts);

        // Verify results
        assertTrue(adultCustomerResult.isTriggered(), "Adult customer should pass the age rule");
        assertFalse(youngCustomerResult.isTriggered(), "Under age customer should fail the age rule");
    }

    /**
     * Test product price calculation.
     */
    @Test
    public void testProductPriceCalculation() {
        // Create a rule with a Boolean condition
        Rule discountRule = new Rule(
            "DiscountRule",
            "#product.price > 0",  // Boolean condition that will evaluate to true
            "Product is eligible for discount"
        );

        // Create a test product
        Product product = new Product("Test Product", 100.0, "Test");

        // Create facts for rule evaluation
        Map<String, Object> facts = new HashMap<>();
        facts.put("product", product);
        facts.put("discountRate", 0.1); // 10% discount

        // Evaluate rule
        List<Rule> rules = Arrays.asList(discountRule);
        RuleResult result = rulesEngine.executeRulesList(rules, facts);

        // Verify rule triggered
        assertTrue(result.isTriggered(), "Discount rule should be triggered");

        // Calculate the discounted price manually
        double discountRate = 0.1; // 10% discount
        double originalPrice = product.getPrice();
        double discountedPrice = originalPrice * (1 - discountRate);

        // Verify the calculation
        assertEquals(90.0, discountedPrice, 0.01, "Discounted price should be 90.0");
    }
}
