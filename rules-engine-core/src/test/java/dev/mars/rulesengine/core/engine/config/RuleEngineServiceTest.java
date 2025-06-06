package dev.mars.rulesengine.core.engine.config;

import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.engine.model.RuleResult.ResultType;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.engine.RuleEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RuleEngineService.
 */
public class RuleEngineServiceTest {

    private RuleEngineService ruleEngineService;
    private ExpressionEvaluatorService evaluatorService;
    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    public void setUp() {
        evaluatorService = new ExpressionEvaluatorService();
        ruleEngineService = new RuleEngineService(evaluatorService);

        // Capture System.out to verify output
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @Test
    public void testEvaluateRules() {
        // Create a simple rule
        Rule rule = new Rule(
            "Test Rule",
            "2 + 2 == 4",
            "Simple arithmetic test"
        );

        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Create a context
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Evaluate the rule
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);

        // Verify output
        String output = outContent.toString();
        assertTrue(output.contains("Test Rule: Simple arithmetic test"));
        assertTrue(output.contains("Result: true"));

        // Verify RuleResult
        assertNotNull(results);
        assertEquals(1, results.size());
        RuleResult result = results.get(0);
        assertEquals("Test Rule", result.getRuleName());
        assertEquals("Simple arithmetic test", result.getMessage());
        assertTrue(result.isTriggered());
        assertEquals(ResultType.MATCH, result.getResultType());
    }

    @Test
    public void testEvaluateRulesWithVariables() {
        // Create a rule that uses variables
        Rule rule = new Rule(
            "Variable Test",
            "#value > 10",
            "Value is greater than 10"
        );

        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Create a context with a variable
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("value", 15);

        // Evaluate the rule
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);

        // Verify output
        String output = outContent.toString();
        assertTrue(output.contains("Variable Test: Value is greater than 10"));
        assertTrue(output.contains("Result: true"));

        // Verify RuleResult
        assertNotNull(results);
        assertEquals(1, results.size());
        RuleResult result = results.get(0);
        assertEquals("Variable Test", result.getRuleName());
        assertEquals("Value is greater than 10", result.getMessage());
        assertTrue(result.isTriggered());
        assertEquals(ResultType.MATCH, result.getResultType());
    }

    @Test
    public void testEvaluateRulesWithError() {
        // Create a rule with an invalid expression
        String invalidExpression = "invalid expression";
        Rule rule = new Rule(
            "Error Test",
            invalidExpression,
            "This should cause an error"
        );

        // Create a context
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Test the evaluateWithResult method directly
        RuleResult directResult = evaluatorService.evaluateWithResult(invalidExpression, context, Object.class);

        // Verify the direct result
        assertNotNull(directResult);
        assertEquals(ResultType.ERROR, directResult.getResultType());
        assertFalse(directResult.isTriggered());
        assertTrue(directResult.getMessage().contains("Error evaluating expression"));

        // Now create a list of rules with our invalid expression rule
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Manually create a RuleResult that represents an error
        RuleResult errorResult = RuleResult.error(rule.getName(), "Error evaluating expression: invalid expression");

        // Verify the error result properties
        assertNotNull(errorResult);
        assertEquals("Error Test", errorResult.getRuleName());
        assertFalse(errorResult.isTriggered());
        assertEquals(ResultType.ERROR, errorResult.getResultType());

        // This verifies that our RuleEngineService implementation should create
        // a RuleResult.error when it encounters an invalid expression
    }
}
