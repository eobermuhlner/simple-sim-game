The economy simulation of this game is becoming complex.                                                                                                                                                                   
find a solution to run the simulation effectively (no UI) and test the balance of configurations.                                                                                                                            
The output of a simulation run should provide output about what should be re-balanced. 

## Solution

Created `SimulationRunner` - a headless simulation runner that tests balance configurations.

### Run Commands

```bash
# Run simulation (default 500 ticks)
./gradlew core:runSimulation

# Run via test (quick 200 tick run)
./gradlew core:test --tests "ch.obermuhlner.sim.SimulationRunnerTest"
```

### What It Tests

The simulation runner:
1. Creates 4 settlements with different specializations (Logging, Mining, Farming, Default)
2. Builds roads between them to enable trade
3. Runs the economy simulation for specified ticks
4. Tracks balance metrics per settlement:
   - Food production vs consumption
   - Population growth/starvation
   - Resource production rates
   - Price volatility
   - Gold income/expenses

### Output

The output shows balance issues and recommendations:

```
=== BALANCE ANALYSIS ===

--- Farming Village ---
  [ISSUE] Food deficit: avg -0.36/tick
    -> Food production insufficient for population
    -> Consider: increase GRASS_FOOD, adjust growth_rate, or add food bonuses
  [ISSUE] Starvation: 200 ticks with starvation
    -> Population dying frequently
    -> Consider: reduce starvation_rate or increase food production

=== CONFIGURATION RECOMMENDATIONS ===

To adjust in application.yml:

# Food balance:
simulation:
  food_demand_per_pop: 0.15
  growth_rate: 0.01
  starvation_rate: 0.02
  terrain_production:
    GRASS_FOOD: 0.5
```

### Files Created

- `core/src/main/java/ch/obermuhlner/sim/SimulationRunner.java` - Main simulation runner
- `core/src/test/java/ch/obermuhlner/sim/SimulationRunnerTest.java` - JUnit test wrapper 
