package ch.obermuhlner.sim.game;

import com.badlogic.gdx.files.FileHandle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Global tech tree state: tracks researched techs and active research progress.
 * Research is fueled by gold drained from settlements each tick.
 */
public class TechTree {

    private final Set<String> researchedTechs = new LinkedHashSet<>();
    private String activeResearchId = null;
    private float researchProgress = 0f;

    // ---- Queries ----

    public boolean isResearched(String techId) {
        return researchedTechs.contains(techId);
    }

    public boolean hasActiveResearch() {
        return activeResearchId != null;
    }

    public String getActiveResearchId() {
        return activeResearchId;
    }

    public float getResearchProgress() {
        return researchProgress;
    }

    public Set<String> getResearchedTechs() {
        return Collections.unmodifiableSet(researchedTechs);
    }

    // ---- Mutators ----

    /** Start researching a tech. Resets progress if switching to a different tech. */
    public void startResearch(String techId) {
        if (techId.equals(activeResearchId)) return;
        activeResearchId = techId;
        researchProgress = 0f;
    }

    /** Mark a cross-specialization tech as researched (completed automatically when conditions are met). */
    public void researchCrossSpecialization(String techId) {
        researchedTechs.add(techId);
    }

    /** Mark a conditional tech as researched (completed automatically when conditions are met). */
    public void researchConditional(String techId) {
        researchedTechs.add(techId);
    }

    /**
     * Invest gold toward the active research. Returns gold actually consumed.
     * Completes research automatically when cost is reached.
     */
    public float addProgress(float goldAmount, GameConfig config) {
        if (activeResearchId == null) return 0f;
        GameConfig.TechConfig tech = config.getTech(activeResearchId);
        if (tech == null) {
            activeResearchId = null;
            return 0f;
        }
        float needed = tech.cost - researchProgress;
        float consumed = Math.min(goldAmount, needed);
        researchProgress += consumed;
        if (researchProgress >= tech.cost) {
            researchedTechs.add(activeResearchId);
            activeResearchId = null;
            researchProgress = 0f;
        }
        return consumed;
    }

    // ---- Effect aggregation ----

    /** Returns the summed value of an effect key across all researched techs. */
    public float getEffectTotal(String effectKey, GameConfig config) {
        float total = 0f;
        
        // Regular techs
        for (String id : researchedTechs) {
            GameConfig.TechConfig tech = config.getTech(id);
            if (tech != null) {
                total += tech.effects.getOrDefault(effectKey, 0f);
            }
        }
        
        // Cross-specialization techs
        for (GameConfig.CrossSpecializationTechConfig csTech : config.getAllCrossSpecializationTechs()) {
            if (isResearched(csTech.id)) {
                total += csTech.effects.getOrDefault(effectKey, 0f);
            }
        }
        
        // Conditional techs
        for (GameConfig.ConditionalTechConfig condTech : config.getAllConditionalTechs()) {
            if (isResearched(condTech.id)) {
                total += condTech.effects.getOrDefault(effectKey, 0f);
            }
        }
        
        return total;
    }

    // ---- Allow / Deny checks ----

    /**
     * Returns true if at least one researched tech has {@code allow[category]} containing {@code name}.
     * Use for gated items that are not available by default.
     */
    public boolean isAllowed(String category, String name, GameConfig config) {
        String cat = category.toLowerCase();
        String upper = name.toUpperCase();
        List<String> initial = config.getInitiallyAvailable(cat);
        if (initial != null && initial.contains(upper)) return true;
        
        // Check regular techs
        for (String id : researchedTechs) {
            GameConfig.TechConfig tech = config.getTech(id);
            if (tech == null) continue;
            List<String> list = tech.allow.get(cat);
            if (list != null && list.contains(upper)) return true;
        }
        
        // Check cross-specialization techs
        for (GameConfig.CrossSpecializationTechConfig csTech : config.getAllCrossSpecializationTechs()) {
            if (isResearched(csTech.id)) {
                List<String> list = csTech.unlocks.get(cat);
                if (list != null && list.contains(upper)) return true;
            }
        }
        
        // Check conditional techs
        for (GameConfig.ConditionalTechConfig condTech : config.getAllConditionalTechs()) {
            if (isResearched(condTech.id)) {
                List<String> list = condTech.unlocks.get(cat);
                if (list != null && list.contains(upper)) return true;
            }
        }
        
        return false;
    }

    /**
     * Returns true if at least one researched tech has {@code deny[category]} containing {@code name}.
     * Deny overrides allow; always-on items are only hidden when explicitly denied.
     */
    public boolean isDenied(String category, String name, GameConfig config) {
        String cat = category.toLowerCase();
        String upper = name.toUpperCase();
        for (String id : researchedTechs) {
            GameConfig.TechConfig tech = config.getTech(id);
            if (tech == null) continue;
            List<String> list = tech.deny.get(cat);
            if (list != null && list.contains(upper)) return true;
        }
        return false;
    }

    // ---- Availability checks ----

    /**
     * Returns true if the tech can be started:
     * not already researched, prerequisite met, level requirement met,
     * and (for specialization branches) a matching settlement exists.
     */
    public boolean canResearch(GameConfig.TechConfig tech, List<Settlement> settlements, SettlementLevel maxLevel, GameConfig config) {
        if (isResearched(tech.id)) return false;
        if (tech.id.equals(activeResearchId)) return false;
        if (!tech.required_tech.isEmpty() && !isResearched(tech.required_tech)) return false;
        SettlementLevel req = config.getLevelById(tech.required_level);
        if (req != null && maxLevel.ordinal() < req.ordinal()) return false;
        if (!"GENERAL".equals(tech.branch)) {
            boolean found = false;
            for (Settlement s : settlements) {
                if (s.specialization.name().equals(tech.branch)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /**
     * Returns a hint explaining why a tech is locked.
     * Returns null if the tech is available or already researched.
     */
    public String getLockHint(GameConfig.TechConfig tech, List<Settlement> settlements, SettlementLevel maxLevel, GameConfig config) {
        if (isResearched(tech.id)) return null;
        if (!tech.required_tech.isEmpty() && !isResearched(tech.required_tech)) {
            return "Req: " + tech.required_tech.replace('_', ' ');
        }
        SettlementLevel req = config.getLevelById(tech.required_level);
        if (req != null && maxLevel.ordinal() < req.ordinal()) return "Req: " + req.getId();
        if (!"GENERAL".equals(tech.branch)) {
            boolean found = false;
            for (Settlement s : settlements) {
                if (s.specialization.name().equals(tech.branch)) { found = true; break; }
            }
            if (!found) return "Req: " + tech.branch.replace('_', ' ');
        }
        return null;
    }

    // ---- Cross-Specialization Tech ----

    /**
     * Check if a cross-specialization tech can be researched.
     */
    public boolean canResearchCrossSpecialization(GameConfig.CrossSpecializationTechConfig tech, List<Settlement> settlements) {
        if (isResearched(tech.id)) return false;
        if (tech.id.equals(activeResearchId)) return false;
        
        // Check if all required specializations exist
        for (String required : tech.requires) {
            boolean found = false;
            for (Settlement s : settlements) {
                if (s.specialization.name().equals(required)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /**
     * Get lock hint for cross-specialization tech.
     */
    public String getCrossSpecializationLockHint(GameConfig.CrossSpecializationTechConfig tech, List<Settlement> settlements) {
        if (isResearched(tech.id)) return null;
        
        for (String required : tech.requires) {
            boolean found = false;
            for (Settlement s : settlements) {
                if (s.specialization.name().equals(required)) {
                    found = true;
                    break;
                }
            }
            if (!found) return "Req: " + required.replace('_', ' ');
        }
        return null;
    }

    // ---- Conditional Tech ----

    private static final Pattern CONDITION_PATTERN = Pattern.compile("(\\w+)\\s*(>=|<=|>|<|==)\\s*(\\d+)");

    /**
     * Evaluate a conditional tech's condition based on current game state.
     */
    public boolean evaluateCondition(String condition, List<Settlement> settlements, int activeTradeRoutes) {
        if (condition == null || condition.isEmpty()) return true;
        
        Matcher matcher = CONDITION_PATTERN.matcher(condition);
        if (!matcher.matches()) return true; // Invalid condition treated as true
        
        String variable = matcher.group(1).toLowerCase();
        String operator = matcher.group(2);
        int threshold = Integer.parseInt(matcher.group(3));
        
        int value = 0;
        switch (variable) {
            case "settlements":
                value = settlements.size();
                break;
            case "total_population":
                value = settlements.stream().mapToInt(s -> s.population).sum();
                break;
            case "active_trade_routes":
                value = activeTradeRoutes;
                break;
            default:
                return true; // Unknown variable treated as true
        }
        
        switch (operator) {
            case ">=": return value >= threshold;
            case "<=": return value <= threshold;
            case ">": return value > threshold;
            case "<": return value < threshold;
            case "==": return value == threshold;
            default: return true;
        }
    }

    /**
     * Check if a conditional tech can be researched.
     */
    public boolean canResearchConditional(GameConfig.ConditionalTechConfig tech, List<Settlement> settlements, int activeTradeRoutes) {
        if (isResearched(tech.id)) return false;
        if (tech.id.equals(activeResearchId)) return false;
        
        return evaluateCondition(tech.condition, settlements, activeTradeRoutes);
    }

    /**
     * Get lock hint for conditional tech.
     */
    public String getConditionalLockHint(GameConfig.ConditionalTechConfig tech, List<Settlement> settlements, int activeTradeRoutes) {
        if (isResearched(tech.id)) return null;
        
        if (!evaluateCondition(tech.condition, settlements, activeTradeRoutes)) {
            return "Condition: " + tech.condition;
        }
        return null;
    }

    // ---- Persistence ----

    public void save(FileHandle file) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(researchedTechs.size());
            for (String id : researchedTechs) dos.writeUTF(id);
            dos.writeUTF(activeResearchId != null ? activeResearchId : "");
            dos.writeFloat(researchProgress);
            dos.flush();
            file.writeBytes(baos.toByteArray(), false);
        } catch (Exception e) {
            // ignore save failures
        }
    }

    public void load(FileHandle file) {
        if (!file.exists()) return;
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(file.readBytes()));
            int count = dis.readInt();
            researchedTechs.clear();
            for (int i = 0; i < count; i++) researchedTechs.add(dis.readUTF());
            String active = dis.readUTF();
            activeResearchId = active.isEmpty() ? null : active;
            researchProgress = dis.readFloat();
        } catch (Exception e) {
            // ignore load failures — start fresh
        }
    }
}
