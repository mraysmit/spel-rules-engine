package dev.mars.rulesengine.demo.integration;

import dev.mars.rulesengine.core.engine.config.RulesEngine;
import dev.mars.rulesengine.core.engine.config.RulesEngineConfiguration;
import dev.mars.rulesengine.core.engine.model.Rule;
import dev.mars.rulesengine.core.engine.model.RuleResult;
import dev.mars.rulesengine.core.service.engine.ExpressionEvaluatorService;
import dev.mars.rulesengine.core.service.engine.RuleEngineService;
import dev.mars.rulesengine.core.service.engine.TemplateProcessorService;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.mars.rulesengine.core.service.lookup.LookupService;
import dev.mars.rulesengine.demo.service.providers.PricingServiceDemo;
import dev.mars.rulesengine.core.service.data.DataServiceManager;

/**
 * This class demonstrates advanced features of SpEL for dynamic evaluation.
 * It focuses on collection manipulation, array operations, and complex expressions.
 * 
 * This is a test/demo class that uses test data to demonstrate the rules engine functionality.
 */
public class SpelAdvancedFeaturesDemo {
    // Config and services
    private final SpelAdvancedFeaturesDemoConfig config;
    private final ExpressionEvaluatorService evaluatorService;
    private final RuleEngineService ruleEngineService;
    private final TemplateProcessorService templateProcessorService;
    private final DataServiceManager dataServiceManager;

    /**
     * Constructor with dependency injection.
     * 
     * @param config The configuration for this demo
     */
    public SpelAdvancedFeaturesDemo(SpelAdvancedFeaturesDemoConfig config) {
        this.config = config;
        this.evaluatorService = config.getEvaluatorService();
        this.ruleEngineService = config.getRuleEngineService();
        this.templateProcessorService = config.getTemplateProcessorService();

        // Initialize DataServiceManager with mock data
        this.dataServiceManager = new DataServiceManager();
        this.dataServiceManager.initializeWithMockData();
    }

    public static void main(String[] args) {
        // Create services
        ExpressionEvaluatorService evaluatorService = new ExpressionEvaluatorService();
        RuleEngineService ruleEngineService = new RuleEngineService(evaluatorService);
        TemplateProcessorService templateProcessorService = new TemplateProcessorService(evaluatorService);
        RulesEngine rulesEngine = new RulesEngine(new RulesEngineConfiguration());
        PricingServiceDemo pricingService = new PricingServiceDemo();

        // Create config with injected services
        SpelAdvancedFeaturesDemoConfig config = new SpelAdvancedFeaturesDemoConfig(
            rulesEngine,
            evaluatorService,
            ruleEngineService,
            templateProcessorService
        );

        // Create main class with injected config
        SpelAdvancedFeaturesDemo spelAdvancedFeaturesDemo = new SpelAdvancedFeaturesDemo(config);

        // Run demonstrations
        spelAdvancedFeaturesDemo.demonstrateCollectionOperations();
        spelAdvancedFeaturesDemo.demonstrateAdvancedRuleEngine();
        spelAdvancedFeaturesDemo.demonstrateDynamicMethodExecution(pricingService);
        spelAdvancedFeaturesDemo.demonstrateTemplateExpressions();
        spelAdvancedFeaturesDemo.demonstrateXmlTemplateExpressions();
        spelAdvancedFeaturesDemo.demonstrateJsonTemplateExpressions();
        spelAdvancedFeaturesDemo.demonstrateDynamicLookupService();
        spelAdvancedFeaturesDemo.demonstrateRuleResultFeatures();
    }

    /**
     * Demonstrates collection operations using SpEL.
     */
    private void demonstrateCollectionOperations() {
        System.out.println("\n=== Financial Instrument Collection Operations ===");

        // Get context from config
        StandardEvaluationContext context = config.createContext();

        // Add price threshold variable
        context.setVariable("priceThreshold", 500.0);

        // Collection selection - filter fixed income products
        RuleResult result1 = evaluatorService.evaluateWithResult("#products.?[category == 'FixedIncome']", context, List.class);
        System.out.println("Rule result: " + (result1.isTriggered() ? "Triggered" : "Not triggered"));

        // Collection projection - get all product names
        RuleResult result2 = evaluatorService.evaluateWithResult("#products.![name]", context, List.class);
        System.out.println("Rule result: " + (result2.isTriggered() ? "Triggered" : "Not triggered"));

        // Combining selection and projection - names of equity products
        RuleResult result3 = evaluatorService.evaluateWithResult("#products.?[category == 'Equity'].![name]", context, List.class);
        System.out.println("Rule result: " + (result3.isTriggered() ? "Triggered" : "Not triggered"));

        // First and last elements
        RuleResult result4 = evaluatorService.evaluateWithResult("#products.^[price > #priceThreshold].name", context, String.class);
        System.out.println("First expensive product: " + result4.getMessage());

        RuleResult result5 = evaluatorService.evaluateWithResult("#products.$[price < 200].name", context, String.class);
        System.out.println("Last cheap product: " + result5.getMessage());
    }

    /**
     * Demonstrates advanced rule engine with collection filtering.
     */
    private void demonstrateAdvancedRuleEngine() {
        System.out.println("\n=== Advanced Rule Engine with Collection Filtering ===");

        // Get context from config
        StandardEvaluationContext context = config.createContext();

        // Get rules from config
        List<Rule> rules = config.createInvestmentRules();

        // Evaluate rules
        ruleEngineService.evaluateRules(rules, context);
    }

    /**
     * Demonstrates dynamic method resolution and execution.
     */
    private void demonstrateDynamicMethodExecution(PricingServiceDemo pricingService) {
        System.out.println("\n=== Dynamic Method Resolution and Execution ===");

        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("pricingService", pricingService);
        context.setVariable("basePrice", 100.0);

        // Dynamic method call based on pricing strategy
        String[] pricingStrategies = {"Standard", "Premium", "Sale", "Clearance"};
        for (String strategy : pricingStrategies) {
            String methodName = "calculate" + strategy + "Price";
            String expression = "#pricingService." + methodName + "(#basePrice)";

            Double price = evaluatorService.evaluate(expression, context, Double.class);
            System.out.println(strategy + " price: $" + price);
        }
    }

    /**
     * Demonstrates template expressions with placeholders.
     */
    private void demonstrateTemplateExpressions() {
        System.out.println("\n=== Template Expressions with Placeholders ===");

        // Get context from config
        StandardEvaluationContext context = config.createTemplateContext();

        // Process template with customer information
        String template = "Dear #{#customer.name},\n\n" +
                "Thank you for being a valued #{#customer.membershipLevel} member for #{#customer.age} years.\n" +
                "We have selected some investment opportunities in #{#customer.preferredCategories[0]} " +
                "and #{#customer.preferredCategories[1]} that might interest you.\n\n" +
                "Your current discount: #{#customer.membershipLevel == 'Gold' ? '20%' : '10%'}\n\n" +
                "Sincerely,\nInvestment Team";

        String result = templateProcessorService.processTemplate(template, context);
        System.out.println(result);
    }

    /**
     * Demonstrates XML template expressions with placeholders.
     */
    private void demonstrateXmlTemplateExpressions() {
        System.out.println("\n=== XML Template Expressions with Placeholders ===");

        // Get context from config
        StandardEvaluationContext context = config.createTemplateContext();

        // Process XML template with customer and product information
        String xmlTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<investment-recommendations>\n" +
                "    <customer>\n" +
                "        <name>#{#customer.name}</name>\n" +
                "        <membership>#{#customer.membershipLevel}</membership>\n" +
                "        <age>#{#customer.age}</age>\n" +
                "    </customer>\n" +
                "    <preferred-categories>\n" +
                "        <category>#{#customer.preferredCategories[0]}</category>\n" +
                "        <category>#{#customer.preferredCategories[1]}</category>\n" +
                "    </preferred-categories>\n" +
                "    <recommended-products>\n" +
                "        <product>\n" +
                "            <name>#{#products[0].name}</name>\n" +
                "            <price>#{#products[0].price}</price>\n" +
                "            <category>#{#products[0].category}</category>\n" +
                "        </product>\n" +
                "        <product>\n" +
                "            <name>#{#products[1].name}</name>\n" +
                "            <price>#{#products[1].price}</price>\n" +
                "            <category>#{#products[1].category}</category>\n" +
                "        </product>\n" +
                "    </recommended-products>\n" +
                "</investment-recommendations>";

        String result = templateProcessorService.processXmlTemplate(xmlTemplate, context);
        System.out.println(result);
    }

    /**
     * Demonstrates JSON template expressions with placeholders.
     */
    private void demonstrateJsonTemplateExpressions() {
        System.out.println("\n=== JSON Template Expressions with Placeholders ===");

        // Get context from config
        StandardEvaluationContext context = config.createTemplateContext();

        // Process JSON template with customer and product information
        String jsonTemplate = "{\n" +
                "  \"customer\": {\n" +
                "    \"name\": \"#{#customer.name}\",\n" +
                "    \"membership\": \"#{#customer.membershipLevel}\",\n" +
                "    \"age\": #{#customer.age},\n" +
                "    \"preferredCategories\": [\n" +
                "      \"#{#customer.preferredCategories[0]}\",\n" +
                "      \"#{#customer.preferredCategories[1]}\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"recommendedProducts\": [\n" +
                "    {\n" +
                "      \"name\": \"#{#products[0].name}\",\n" +
                "      \"price\": #{#products[0].price},\n" +
                "      \"category\": \"#{#products[0].category}\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"#{#products[1].name}\",\n" +
                "      \"price\": #{#products[1].price},\n" +
                "      \"category\": \"#{#products[1].category}\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"discountPercentage\": #{#customer.membershipLevel == 'Gold' ? 20 : 10}\n" +
                "}";

        String result = templateProcessorService.processJsonTemplate(jsonTemplate, context);
        System.out.println(result);
    }

    /**
     * Demonstrates dynamic lookup service.
     */
    private void demonstrateDynamicLookupService() {
        System.out.println("\n=== Dynamic Lookup Service ===");

        // Get context from config
        StandardEvaluationContext context = config.createContext();

        // Get lookup services from data service
        List<LookupService> lookupServices = dataServiceManager.requestData("lookupServices");
        context.setVariable("lookupServices", lookupServices);

        // Dynamically find lookup service by name
        String lookupName = "InstrumentTypes";
        context.setVariable("lookupName", lookupName);

        RuleResult result = evaluatorService.evaluateWithResult(
            "#lookupServices.?[name == #lookupName][0]", context, LookupService.class);

        if (result.isTriggered()) {
            LookupService lookupService = evaluatorService.evaluate(
                "#lookupServices.?[name == #lookupName][0]", context, LookupService.class);

            if (lookupService != null) {
                System.out.println("Found lookup service: " + lookupService.getName());
                System.out.println("Values: " + lookupService.getLookupValues());

                // Test validation
                String testValue = "Equity";
                context.setVariable("testValue", testValue);

                Boolean isValid = evaluatorService.evaluate(
                    "#lookupServices.?[name == #lookupName][0].validate(#testValue)", 
                    context, Boolean.class);

                System.out.println("Is '" + testValue + "' valid? " + isValid);
            }
        } else {
            System.out.println("Lookup service not found: " + lookupName);
        }
    }

    /**
     * Demonstrates RuleResult features and capabilities.
     * This method showcases the different result types, properties, and how to use
     * RuleResult for conditional rule execution.
     */
    private void demonstrateRuleResultFeatures() {
        System.out.println("\n=== RuleResult Features and Capabilities ===");

        // Get context from config
        StandardEvaluationContext context = config.createContext();

        // Add additional variables for this demonstration
        context.setVariable("investmentAmount", 150000);
        context.setVariable("accountType", "retirement");
        context.setVariable("clientRiskScore", 8);
        context.setVariable("marketVolatility", 0.25);
        context.setVariable("kycVerified", false);

        System.out.println("\n1. Demonstrating RuleResult Types:");

        // 1. MATCH result type
        RuleResult matchResult = evaluatorService.evaluateWithResult(
            "#investmentAmount > 100000", context, Boolean.class);
        System.out.println("\nMATCH Result:");
        printRuleResultDetails(matchResult);

        // 2. NO_MATCH result type
        RuleResult noMatchResult = evaluatorService.evaluateWithResult(
            "#investmentAmount < 50000", context, Boolean.class);
        System.out.println("\nNO_MATCH Result:");
        printRuleResultDetails(noMatchResult);

        // 3. ERROR result type
        RuleResult errorResult = evaluatorService.evaluateWithResult(
            "#undefinedVariable > 100", context, Boolean.class);
        System.out.println("\nERROR Result:");
        printRuleResultDetails(errorResult);

        // 4. NO_RULES result type (create manually since it's hard to trigger naturally)
        RuleResult noRulesResult = RuleResult.noRules();
        System.out.println("\nNO_RULES Result:");
        printRuleResultDetails(noRulesResult);

        System.out.println("\n2. Using RuleResult for Conditional Rule Execution:");

        // Get rules from config
        List<Rule> ruleResultRules = config.createRuleResultRules();

        // Extract specific rules for demonstrations
        Rule highValueRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("HighValueCustomerRule"))
            .findFirst()
            .orElse(new Rule(
                "High-Value Investment",
                "#investmentAmount > 100000",
                "High-value investment detected"
            ));

        Rule retirementAccountRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("InitialAssessmentRule"))
            .findFirst()
            .orElse(new Rule(
                "Retirement Account",
                "#accountType == 'retirement'",
                "Retirement account detected"
            ));

        Rule highRiskClientRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("CustomerCategoryRule"))
            .findFirst()
            .orElse(new Rule(
                "High-Risk Client",
                "#clientRiskScore > 7",
                "High-risk client detected"
            ));

        Rule volatileMarketRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("MidAgeInvestorRule"))
            .findFirst()
            .orElse(new Rule(
                "Volatile Market",
                "#marketVolatility > 0.2",
                "Volatile market conditions detected"
            ));

        Rule kycVerificationRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("SeniorInvestorRule"))
            .findFirst()
            .orElse(new Rule(
                "KYC Verification",
                "!#kycVerified",
                "KYC verification required"
            ));

        // Demonstrate conditional execution based on triggered status
        System.out.println("\nConditional Execution Based on Triggered Status:");
        executeRuleWithConditionalFollowup(highValueRule, retirementAccountRule, context);

        // Demonstrate execution based on result type
        System.out.println("\nExecution Based on Result Type:");
        executeRuleBasedOnResultType(highRiskClientRule, context);

        // Demonstrate rule chaining using result message
        System.out.println("\nRule Chaining Using Result Message:");
        executeRuleChain(volatileMarketRule, kycVerificationRule, context);

        // Demonstrate dynamic rule selection
        System.out.println("\nDynamic Rule Selection:");
        executeDynamicRuleSelection(context);
    }

    /**
     * Helper method to print details of a RuleResult.
     */
    private void printRuleResultDetails(RuleResult result) {
        System.out.println("  - ID: " + result.getId());
        System.out.println("  - Rule Name: " + result.getRuleName());
        System.out.println("  - Message: " + result.getMessage());
        System.out.println("  - Triggered: " + result.isTriggered());
        System.out.println("  - Result Type: " + result.getResultType());
        System.out.println("  - Timestamp: " + result.getTimestamp());
    }

    /**
     * Demonstrates conditional execution based on triggered status.
     */
    private void executeRuleWithConditionalFollowup(Rule rule1, Rule rule2, StandardEvaluationContext context) {
        // Create a list with the first rule
        List<Rule> rules = new ArrayList<>();
        rules.add(rule1);

        // Evaluate the first rule
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
        RuleResult result = results.get(0);

        System.out.println("Evaluated rule: " + rule1.getName());
        System.out.println("Result: " + (result.isTriggered() ? "Triggered" : "Not triggered"));

        // Conditional execution based on the result
        if (result.isTriggered()) {
            System.out.println("First rule triggered, executing second rule...");

            // Create a list with the second rule
            List<Rule> followupRules = new ArrayList<>();
            followupRules.add(rule2);

            // Evaluate the second rule
            List<RuleResult> followupResults = ruleEngineService.evaluateRules(followupRules, context);
            RuleResult followupResult = followupResults.get(0);

            System.out.println("Evaluated rule: " + rule2.getName());
            System.out.println("Result: " + (followupResult.isTriggered() ? "Triggered" : "Not triggered"));
        } else {
            System.out.println("First rule not triggered, skipping second rule.");
        }
    }

    /**
     * Demonstrates execution based on result type.
     */
    private void executeRuleBasedOnResultType(Rule rule, StandardEvaluationContext context) {
        // Create a list with the rule
        List<Rule> rules = new ArrayList<>();
        rules.add(rule);

        // Evaluate the rule
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
        RuleResult result = results.get(0);

        System.out.println("Evaluated rule: " + rule.getName());

        // Execute different actions based on result type
        switch (result.getResultType()) {
            case MATCH:
                System.out.println("MATCH result: Executing actions for successful match...");
                System.out.println("Action: Flagging client for enhanced due diligence");
                break;
            case NO_MATCH:
                System.out.println("NO_MATCH result: Executing default actions...");
                System.out.println("Action: Proceeding with standard processing");
                break;
            case ERROR:
                System.out.println("ERROR result: Executing error handling...");
                System.out.println("Action: Logging error and notifying administrator");
                break;
            case NO_RULES:
                System.out.println("NO_RULES result: No rules to execute");
                break;
        }
    }

    /**
     * Demonstrates rule chaining using result message.
     */
    private void executeRuleChain(Rule rule1, Rule rule2, StandardEvaluationContext context) {
        // Create a list with the first rule
        List<Rule> rules = new ArrayList<>();
        rules.add(rule1);

        // Evaluate the first rule
        List<RuleResult> results = ruleEngineService.evaluateRules(rules, context);
        RuleResult result = results.get(0);

        System.out.println("Evaluated rule: " + rule1.getName());

        // Use the message from the first rule to determine the next action
        if (result.isTriggered()) {
            System.out.println("First rule triggered with message: " + result.getMessage());

            // The message indicates market volatility, so we check KYC verification
            if (result.getMessage().contains("Volatile market")) {
                System.out.println("Market volatility detected, checking KYC verification status...");

                // Create a list with the second rule
                List<Rule> followupRules = new ArrayList<>();
                followupRules.add(rule2);

                // Evaluate the second rule
                List<RuleResult> followupResults = ruleEngineService.evaluateRules(followupRules, context);
                RuleResult followupResult = followupResults.get(0);

                System.out.println("Evaluated rule: " + rule2.getName());

                if (followupResult.isTriggered()) {
                    System.out.println("KYC verification required. Investment on hold until verification complete.");
                } else {
                    System.out.println("KYC verification complete. Proceeding with investment despite market volatility.");
                }
            }
        } else {
            System.out.println("Market conditions stable, proceeding with standard investment process.");
        }
    }

    /**
     * Demonstrates dynamic rule selection.
     */
    private void executeDynamicRuleSelection(StandardEvaluationContext context) {
        // Get rules from config
        List<Rule> ruleResultRules = config.createRuleResultRules();

        // Create a map of rules that can be selected dynamically
        Map<String, Rule> ruleRepository = new HashMap<>();

        // Extract rules from the config and add them to the repository
        Rule youngInvestorRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("YoungInvestorRule"))
            .findFirst()
            .orElse(null);
        if (youngInvestorRule != null) {
            ruleRepository.put("Rule-HighValue", youngInvestorRule);
        } else {
            ruleRepository.put("Rule-HighValue", new Rule(
                "High-Value Investment",
                "#investmentAmount > 100000",
                "High-value investment detected"
            ));
        }

        Rule midAgeInvestorRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("MidAgeInvestorRule"))
            .findFirst()
            .orElse(null);
        if (midAgeInvestorRule != null) {
            ruleRepository.put("Rule-Retirement", midAgeInvestorRule);
        } else {
            ruleRepository.put("Rule-Retirement", new Rule(
                "Retirement Account",
                "#accountType == 'retirement'",
                "Retirement account detected"
            ));
        }

        Rule seniorInvestorRule = ruleResultRules.stream()
            .filter(r -> r.getName().equals("SeniorInvestorRule"))
            .findFirst()
            .orElse(null);
        if (seniorInvestorRule != null) {
            ruleRepository.put("Rule-HighRisk", seniorInvestorRule);
        } else {
            ruleRepository.put("Rule-HighRisk", new Rule(
                "High-Risk Client",
                "#clientRiskScore > 7",
                "High-risk client detected"
            ));
        }

        // Start with a rule to determine the investment type
        Rule investmentTypeRule = new Rule(
            "Investment Type Determination",
            "#investmentAmount > 100000 ? 'HighValue' : (#accountType == 'retirement' ? 'Retirement' : 'Standard')",
            "Determining investment type"
        );

        // Evaluate the rule to determine which rule to execute next
        RuleResult result = evaluatorService.evaluateWithResult(
            investmentTypeRule.getCondition(), context, String.class);

        System.out.println("Evaluated rule: " + investmentTypeRule.getName());

        if (result.isTriggered()) {
            // Use the result to dynamically select the next rule
            String investmentType = evaluatorService.evaluate(
                investmentTypeRule.getCondition(), context, String.class);

            System.out.println("Investment type determined: " + investmentType);

            String nextRuleName = "Rule-" + investmentType;
            Rule nextRule = ruleRepository.get(nextRuleName);

            if (nextRule != null) {
                System.out.println("Dynamically selected rule: " + nextRule.getName());

                // Create a list with the selected rule
                List<Rule> selectedRules = new ArrayList<>();
                selectedRules.add(nextRule);

                // Evaluate the selected rule
                List<RuleResult> selectedResults = ruleEngineService.evaluateRules(selectedRules, context);
                RuleResult selectedResult = selectedResults.get(0);

                System.out.println("Result: " + (selectedResult.isTriggered() ? "Triggered" : "Not triggered"));
                if (selectedResult.isTriggered()) {
                    System.out.println("Message: " + selectedResult.getMessage());
                }
            } else {
                System.out.println("No rule found for investment type: " + investmentType);
            }
        } else {
            System.out.println("Could not determine investment type.");
        }
    }
}
