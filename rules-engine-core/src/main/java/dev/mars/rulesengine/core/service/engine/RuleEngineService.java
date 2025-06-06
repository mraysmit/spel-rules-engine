package dev.mars.rulesengine.core.service.engine;

import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import org.springframework.expression.EvaluationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for evaluating business rules using SpEL expressions.
 * This class handles rule evaluation and result reporting.
 */
public class RuleEngineService {
    private static final Logger LOGGER = Logger.getLogger(RuleEngineService.class.getName());
    private final ExpressionEvaluatorService evaluatorService;
    private boolean printResults = true;

    public RuleEngineService(ExpressionEvaluatorService evaluatorService) {
        LOGGER.info("Initializing RuleEngineService");
        this.evaluatorService = evaluatorService;
        LOGGER.fine("Using evaluator service: " + evaluatorService.getClass().getSimpleName());
    }

    /**
     * Set whether to print results to the console.
     * 
     * @param printResults True to print results, false to suppress output
     * @return This service for method chaining
     */
    public RuleEngineService setPrintResults(boolean printResults) {
        LOGGER.fine("Setting printResults to: " + printResults);
        this.printResults = printResults;
        return this;
    }

    /**
     * Evaluates a list of rules against the given context and returns the results.
     * 
     * @param rules The rules to evaluate
     * @param context The evaluation context
     * @return A list of RuleResult objects, one for each rule that was evaluated
     */
    public List<RuleResult> evaluateRules(List<Rule> rules, EvaluationContext context) {
        LOGGER.info("Evaluating " + (rules != null ? rules.size() : 0) + " rules");
        List<RuleResult> results = new ArrayList<>();

        if (rules == null || rules.isEmpty()) {
            LOGGER.info("No rules to evaluate");
            return results;
        }

        for (Rule rule : rules) {
            LOGGER.fine("Evaluating rule: " + rule.getName());
            try {
                // Use evaluateWithResult instead of evaluateQuietly for better error handling
                RuleResult baseResult = evaluatorService.evaluateWithResult(rule.getCondition(), context, Object.class);

                // Create a proper RuleResult with the rule name and message
                RuleResult ruleResult;
                if (baseResult.getResultType() == RuleResult.ResultType.MATCH) {
                    ruleResult = RuleResult.match(rule.getName(), rule.getMessage());
                } else if (baseResult.getResultType() == RuleResult.ResultType.ERROR) {
                    ruleResult = RuleResult.error(rule.getName(), baseResult.getMessage());
                    // Print to System.err for test verification
                    System.err.println("Error evaluating rule '" + rule.getName() + "': " + baseResult.getMessage());
                } else {
                    ruleResult = RuleResult.noMatch();
                }

                results.add(ruleResult);
                LOGGER.fine("Rule '" + rule.getName() + "' evaluated, result type: " + ruleResult.getResultType());

                if (printResults) {
                    LOGGER.info(rule.getName() + ": " + rule.getMessage());
                    LOGGER.info("Result type: " + ruleResult.getResultType());
                    // Also print to System.out for test verification
                    System.out.println(rule.getName() + ": " + rule.getMessage());
                    System.out.println("Result: " + (ruleResult.isTriggered() ? "true" : "false"));
                }
            } catch (Exception e) {
                RuleResult errorResult = RuleResult.error(rule.getName(), e.getMessage());
                results.add(errorResult);
                LOGGER.log(Level.WARNING, "Error evaluating rule '" + rule.getName() + "': " + e.getMessage(), e);
                // Also print to System.err for test verification
                System.err.println("Error evaluating rule '" + rule.getName() + "': " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        LOGGER.info("Evaluated " + results.size() + " rules successfully");
        return results;
    }
}
