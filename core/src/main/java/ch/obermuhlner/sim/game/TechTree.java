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

/**
 * Global tech tree state: tracks researched techs and active research progress.
 * Research is fueled by gold drained from settlements each tick.
 */
public class TechTree {

    private final Set<String> researchedTechs = new LinkedHashSet<>();
    private String activeResearchId = null;
    private float researchProgress = 0f; // gold invested toward activeResearchId

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
        for (String id : researchedTechs) {
            GameConfig.TechConfig tech = config.getTech(id);
            if (tech != null) {
                total += tech.effects.getOrDefault(effectKey, 0f);
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
        for (String id : researchedTechs) {
            GameConfig.TechConfig tech = config.getTech(id);
            if (tech == null) continue;
            List<String> list = tech.allow.get(cat);
            if (list != null && list.contains(upper)) return true;
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
    public boolean canResearch(GameConfig.TechConfig tech, List<Settlement> settlements, SettlementLevel maxLevel) {
        if (isResearched(tech.id)) return false;
        if (tech.id.equals(activeResearchId)) return false;
        if (!tech.required_tech.isEmpty() && !isResearched(tech.required_tech)) return false;
        try {
            SettlementLevel req = SettlementLevel.valueOf(tech.required_level);
            if (maxLevel.ordinal() < req.ordinal()) return false;
        } catch (IllegalArgumentException ignored) {}
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
    public String getLockHint(GameConfig.TechConfig tech, List<Settlement> settlements, SettlementLevel maxLevel) {
        if (isResearched(tech.id)) return null;
        if (!tech.required_tech.isEmpty() && !isResearched(tech.required_tech)) {
            GameConfig.TechConfig prereq = null; // hint will use raw id
            return "Req: " + tech.required_tech.replace('_', ' ');
        }
        try {
            SettlementLevel req = SettlementLevel.valueOf(tech.required_level);
            if (maxLevel.ordinal() < req.ordinal()) return "Req: " + req.name();
        } catch (IllegalArgumentException ignored) {}
        if (!"GENERAL".equals(tech.branch)) {
            boolean found = false;
            for (Settlement s : settlements) {
                if (s.specialization.name().equals(tech.branch)) { found = true; break; }
            }
            if (!found) return "Req: " + tech.branch.replace('_', ' ');
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
