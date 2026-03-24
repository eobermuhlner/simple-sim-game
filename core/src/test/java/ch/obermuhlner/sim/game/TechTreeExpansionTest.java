package ch.obermuhlner.sim.game;

import org.junit.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class TechTreeExpansionTest {

    private static final GameConfig CONFIG = new GameConfig(new GameConfig.Root());

    @Test
    public void testConditionEvaluationSettlements() {
        TechTree techTree = new TechTree();
        Settlement s = new Settlement("S", 0, 0, CONFIG);
        
        assertTrue("6 >= 3 should be true", 
            techTree.evaluateCondition("settlements >= 3", Arrays.asList(s, s, s, s, s, s), 0));
        assertTrue("3 >= 3 should be true", 
            techTree.evaluateCondition("settlements >= 3", Arrays.asList(s, s, s), 0));
        assertFalse("2 >= 3 should be false", 
            techTree.evaluateCondition("settlements >= 3", Arrays.asList(s, s), 0));
    }

    @Test
    public void testConditionEvaluationPopulation() {
        TechTree techTree = new TechTree();
        
        // Use raw evaluateCondition with empty settlements to test the variable parsing
        // The actual value will be 0 since settlements is empty
        assertFalse("0 >= 500 should be false", 
            techTree.evaluateCondition("total_population >= 500", Arrays.asList(), 0));
        assertTrue("0 >= 0 should be true", 
            techTree.evaluateCondition("total_population >= 0", Arrays.asList(), 0));
    }

    @Test
    public void testConditionEvaluationTradeRoutes() {
        TechTree techTree = new TechTree();
        Settlement s = new Settlement("S", 0, 0, CONFIG);
        
        assertTrue("5 >= 5 should be true", 
            techTree.evaluateCondition("active_trade_routes >= 5", Arrays.asList(s), 5));
        assertTrue("10 >= 5 should be true", 
            techTree.evaluateCondition("active_trade_routes >= 5", Arrays.asList(s), 10));
        assertFalse("3 >= 5 should be false", 
            techTree.evaluateCondition("active_trade_routes >= 5", Arrays.asList(s), 3));
    }

    @Test
    public void testConditionComparisonOperators() {
        TechTree techTree = new TechTree();
        Settlement s = new Settlement("S", 0, 0, CONFIG);
        
        // Test > operator
        assertTrue("5 > 3 should be true", 
            techTree.evaluateCondition("settlements > 3", Arrays.asList(s, s, s, s, s, s), 0));
        assertFalse("3 > 3 should be false", 
            techTree.evaluateCondition("settlements > 3", Arrays.asList(s, s, s), 0));
        
        // Test < operator
        assertTrue("3 < 5 should be true", 
            techTree.evaluateCondition("settlements < 5", Arrays.asList(s, s, s), 0));
        assertFalse("5 < 3 should be false", 
            techTree.evaluateCondition("settlements < 3", Arrays.asList(s, s, s, s, s), 0));
        
        // Test == operator
        assertTrue("3 == 3 should be true", 
            techTree.evaluateCondition("settlements == 3", Arrays.asList(s, s, s), 0));
        assertFalse("3 == 5 should be false", 
            techTree.evaluateCondition("settlements == 3", Arrays.asList(s, s, s, s, s), 0));
        
        // Test >= operator
        assertTrue("3 >= 3 should be true", 
            techTree.evaluateCondition("settlements >= 3", Arrays.asList(s, s, s), 0));
        
        // Test <= operator
        assertTrue("3 <= 3 should be true", 
            techTree.evaluateCondition("settlements <= 3", Arrays.asList(s, s, s), 0));
        assertTrue("2 <= 3 should be true", 
            techTree.evaluateCondition("settlements <= 3", Arrays.asList(s, s), 0));
    }

    @Test
    public void testConditionInvalidCondition() {
        TechTree techTree = new TechTree();
        
        // Invalid condition should return true (treat as available)
        assertTrue("Invalid condition should return true", 
            techTree.evaluateCondition("invalid", Arrays.asList(), 0));
        assertTrue("Empty condition should return true", 
            techTree.evaluateCondition("", Arrays.asList(), 0));
    }

    @Test
    public void testConditionUnknownVariable() {
        TechTree techTree = new TechTree();
        
        // Unknown variable should return true (treat as available)
        assertTrue("Unknown variable should return true", 
            techTree.evaluateCondition("unknown >= 5", Arrays.asList(), 0));
    }

    @Test
    public void testCrossSpecializationCanResearch() {
        TechTree techTree = new TechTree();
        
        // Test with only one required specialization
        GameConfig.CrossSpecializationTechConfig config = new GameConfig.CrossSpecializationTechConfig();
        config.id = "TEST_TECH";
        config.requires = new ArrayList<>();
        config.requires.add("LOGGING_CAMP");
        
        // Create settlement at Town level to allow specialization
        Settlement loggingCamp = new Settlement("Logging", 0, 0, CONFIG);
        loggingCamp.setPopulation(50); // Reach Village max to trigger specialization choice
        loggingCamp.specialize(Specialization.LOGGING_CAMP);
        
        List<Settlement> withLogging = new ArrayList<>();
        withLogging.add(loggingCamp);
        
        assertTrue("Should be able to research with required specialization",
            techTree.canResearchCrossSpecialization(config, withLogging));
        
        // Test without required specialization
        Settlement miningTown = new Settlement("Mining", 5, 5, CONFIG);
        miningTown.setPopulation(50);
        miningTown.specialize(Specialization.MINING_TOWN);
        
        List<Settlement> withoutLogging = new ArrayList<>();
        withoutLogging.add(miningTown);
        
        assertFalse("Should NOT be able to research without required specialization",
            techTree.canResearchCrossSpecialization(config, withoutLogging));
    }

    @Test
    public void testCrossSpecializationLockHint() {
        TechTree techTree = new TechTree();
        
        GameConfig.CrossSpecializationTechConfig config = new GameConfig.CrossSpecializationTechConfig();
        config.id = "TEST_TECH";
        config.requires = new ArrayList<>();
        config.requires.add("LOGGING_CAMP");
        config.requires.add("TRADE_HUB");
        
        // Only Logging Camp
        Settlement loggingCamp = new Settlement("Logging", 0, 0, CONFIG);
        loggingCamp.specialize(Specialization.LOGGING_CAMP);
        
        List<Settlement> onlyLogging = new ArrayList<>();
        onlyLogging.add(loggingCamp);
        
        String hint = techTree.getCrossSpecializationLockHint(config, onlyLogging);
        assertNotNull("Lock hint should not be null", hint);
        // The hint should mention the missing specialization
        assertTrue("Hint should contain something about requirement: " + hint, 
            hint.contains("Req"));
    }

    @Test
    public void testCrossSpecializationAlreadyResearched() {
        TechTree techTree = new TechTree();
        
        GameConfig.CrossSpecializationTechConfig config = new GameConfig.CrossSpecializationTechConfig();
        config.id = "TEST_TECH";
        config.requires = Arrays.asList("LOGGING_CAMP");
        
        // Mark as researched
        techTree.startResearch("TEST_TECH");
        
        assertFalse("Already researched tech should return false",
            techTree.canResearchCrossSpecialization(config, Arrays.asList()));
    }

    @Test
    public void testConditionalCanResearch() {
        TechTree techTree = new TechTree();
        
        GameConfig.ConditionalTechConfig config = new GameConfig.ConditionalTechConfig();
        config.id = "TEST_TECH";
        config.condition = "settlements >= 3";
        
        Settlement s1 = new Settlement("S1", 0, 0, CONFIG);
        Settlement s2 = new Settlement("S2", 5, 5, CONFIG);
        Settlement s3 = new Settlement("S3", 10, 10, CONFIG);
        
        assertTrue("Should be able to research with 3 settlements",
            techTree.canResearchConditional(config, Arrays.asList(s1, s2, s3), 0));
        
        assertFalse("Should NOT be able to research with 2 settlements",
            techTree.canResearchConditional(config, Arrays.asList(s1, s2), 0));
    }

    @Test
    public void testConditionalLockHint() {
        TechTree techTree = new TechTree();
        
        GameConfig.ConditionalTechConfig config = new GameConfig.ConditionalTechConfig();
        config.id = "TEST_TECH";
        config.condition = "settlements >= 3";
        
        Settlement s1 = new Settlement("S1", 0, 0, CONFIG);
        
        String hint = techTree.getConditionalLockHint(config, Arrays.asList(s1), 0);
        assertNotNull("Lock hint should not be null when condition not met", hint);
        assertTrue("Hint should mention the condition", hint.contains("settlements"));
    }

    @Test
    public void testConditionalAlreadyResearched() {
        TechTree techTree = new TechTree();
        
        GameConfig.ConditionalTechConfig config = new GameConfig.ConditionalTechConfig();
        config.id = "TEST_TECH";
        config.condition = "settlements >= 3";
        
        // Mark as researched (simulated)
        techTree.startResearch("TEST_TECH");
        
        assertFalse("Already researched tech should return false",
            techTree.canResearchConditional(config, Arrays.asList(), 0));
    }
}
