# Tutorial Handler Implementation Status

## Overview
This document tracks the implementation status of tutorial step and fork handlers after separating data and logic layers.

## Completed Handlers: 13/34 (38%)

### Fork Handlers: 6/6 (100% ✅)
All fork handlers are complete and registered:

1. ✅ **AsyncTutorialForkHandler** - Async node processing
   - File: `handlers/forks/AsyncTutorialForkHandler.kt`
   - Logic: Returns all pathNodes, async=true, checks all nodes complete
   
2. ✅ **HiddenOptionalImprovementForkHandler** - Conditional paths
   - File: `handlers/forks/HiddenOptionalImprovementForkHandler.kt`
   - Logic: Condition-based path selection (improved vs default)
   
3. ✅ **OptionalTutorialForkHandler** - Optional paths
   - File: `handlers/forks/OptionalTutorialForkHandler.kt`
   - Logic: Any path completion satisfies, async=true
   
4. ✅ **SelectPathTutorialForkHandler** - User selection
   - File: `handlers/forks/SelectPathTutorialForkHandler.kt`
   - Logic: User selects from multiple paths
   
5. ✅ **WhileTutorialNodeHandler** - Loop nodes
   - File: `handlers/forks/WhileTutorialNodeHandler.kt`
   - Logic: Repeats while condition true, auto-completes on condition match
   
6. ✅ **RequireAsyncCompletionTutorialStepHandler** - Await completion
   - File: `handlers/steps/RequireAsyncCompletionTutorialStepHandler.kt`
   - Logic: Waits for referenced node completion

### Step Handlers: 7/28 (25%)

#### Basic Steps: 0/3
- ❌ **CollectionTutorialStep** - Collection amount tracking
- ❌ **MinionTutorialStep** - Minion placement
- ❌ **AwaitMiningEvent** - Mining event detection

#### GUI Steps: 0/3
- ❌ **GUIBasedTutorialStep** - Abstract base for GUI steps
- ❌ **GuiClickSlotTutorialStep** - Slot click detection
- ❌ **GuiItemTutorialStep** - Item inspection in GUI

#### Item Steps: 0/3
- ❌ **TagItemTutorialStep** - Item tagging
- ❌ **ReforgeTutorialStep** - Item reforging
- ❌ **EnchantTutorialStep** - Item enchanting

#### Location Steps: 4/4 (100% ✅)
1. ✅ **GoToIslandTutorialStepHandler** - Island navigation
   - Events: IslandChangeEvent
   - Active check: Current island on activation
   - Passive check: Island change event
   - Features: Warp suggestion chat prompt
   
2. ✅ **GoToPositionTutorialStepHandler** - Position navigation
   - Events: SecondPassedEvent
   - Active check: Distance to target
   - Passive check: Position polling every second
   
3. ✅ **JoinInstanceTutorialStepHandler** - Instance joining
   - Events: IslandChangeEvent
   - Active check: Server ID match on activation
   - Passive check: Server change detection
   
4. ✅ **SubAreaTutorialStepHandler** - Sub-area detection
   - Events: IslandChangeEvent
   - Active check: Current area on activation
   - Passive check: Area change event

#### Message/Text Steps: 2/2 (100% ✅)
1. ✅ **MessageTutorialStepHandler** - Chat message matching
   - Events: SkyHanniChatEvent
   - Active check: None
   - Passive check: Pattern matching on chat
   - Features: Case-insensitive regex matching
   
2. ✅ **TextTutorialStepHandler** - Text display
   - No events (manual skip only)
   - Simple text display step

#### Misc Steps: 0/6
- ❌ **AwaitGodSplashTutorialStep** - God splash detection
- ❌ **CakeTutorialStep** - Cake year tracking
- ❌ **DailyEnchantingXpTutorialStep** - Daily enchanting XP
- ❌ **EquipPetTutorialStep** - Pet equipment
- ❌ **InTimeframeTutorialStep** - Time-based completion
- ❌ **ObtainBingoGoalTutorialStep** - Bingo goal completion

#### Requirement Steps: 0/4
- ❌ **CollectionLevelRequirement** - Collection level checking
- ❌ **NeuRecipeRequirement** - Recipe unlock checking
- ❌ **ObtainCoinsTutorialStep** - Coin amount checking
- ❌ **SkillTutorialStep** - Skill level checking

#### Storage Steps: 0/3
- ❌ **EquipArmorTutorialStep** - Armor equipment
- ❌ **ObtainFromNEURecipeTutorialStep** - NEU recipe-based obtaining
- ❌ **ObtainTutorialStep** - Item obtaining

## Implementation Pattern

Each handler follows this structure:

```kotlin
class SomeStepHandler : TutorialStepHandler<SomeStep> {
    // Display methods
    override fun getStepName(step, tutorial): String
    override fun getStepDescription(step, tutorial): String?
    override fun getRequirements(step): List<TutorialNode>
    
    // Active check (on activation)
    override fun check(step, tutorial): Boolean
    
    // Lifecycle hooks
    override fun onActivate(step, tutorial) {
        activeSteps.add(step)  // Enable passive checks
    }
    override fun onDeactivate(step, tutorial) {
        activeSteps.remove(step)  // Disable passive checks
    }
    
    // Passive check (event-driven)
    @HandleEvent
    fun onSomeEvent(event: SomeEvent) {
        activeSteps.forEach { step ->
            if (conditionMet) {
                TutorialStepLogic.complete(step)
            }
        }
    }
    
    companion object {
        // Track active steps for efficient event processing
        private val activeSteps = mutableSetOf<SomeStep>()
    }
}
```

## Registration

All handlers must be registered in `TutorialHandlers.init()`:

```kotlin
TutorialHandlerRegistry.registerStepHandler(SomeStep::class, SomeStepHandler())
```

## Next Steps

### Priority 1: Core Functionality (6 handlers)
These are essential for basic tutorial functionality:
1. CollectionTutorialStep - Core mechanic
2. ObtainTutorialStep - Core mechanic  
3. TagItemTutorialStep - Core mechanic
4. EnchantTutorialStep - Core mechanic
5. ReforgeTutorialStep - Core mechanic
6. SkillTutorialStep - Common requirement

### Priority 2: Advanced Features (9 handlers)
7. GUI-based steps (3 handlers)
8. Misc steps (6 handlers)

### Priority 3: Specialized (6 handlers)
9. Remaining requirement steps (3 handlers)
10. Remaining storage steps (2 handlers)
11. Basic steps (1 handler - Mining, Minion)

## Estimation

- **Time per handler**: 15-20 minutes (logic extraction + implementation + testing)
- **Remaining handlers**: 21
- **Estimated time**: 5-7 hours

## Implementation Checklist

For each remaining handler:
- [ ] Extract original logic from git history (commit 8dc9a5c^)
- [ ] Implement display methods (getStepName, getStepDescription, getRequirements)
- [ ] Implement active check (check method) if needed
- [ ] Implement lifecycle hooks (onActivate, onDeactivate, onReset)
- [ ] Implement event handlers with @HandleEvent
- [ ] Add active step tracking (companion object Set)
- [ ] Register in TutorialHandlers.init()
- [ ] Test completion flow

## Architecture Benefits

✅ **Data Layer**: Pure data classes in `de.hype.bingonet.shared` with zero dependencies  
✅ **Behavior Layer**: All logic in `at.hannibal2.skyhanni.features.tutorial.handlers`  
✅ **Efficient Processing**: Only active steps processed per event  
✅ **Type-Safe Dispatch**: KClass-based handler routing  
✅ **Event-Driven**: Passive checks via @HandleEvent  
✅ **Maintainable**: Clear separation of concerns  
✅ **Sync-Ready**: Data classes can be synced externally  
