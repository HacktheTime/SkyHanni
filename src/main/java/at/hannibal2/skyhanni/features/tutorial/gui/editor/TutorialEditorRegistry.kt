package at.hannibal2.skyhanni.features.tutorial.gui.editor

import de.hype.bingonet.shared.tutorials.TutorialNode

object TutorialEditorRegistry {
    private val editors = mutableListOf<TutorialNodeEditor>()

    init {
        // Register specific editors first
        editors += TextStepEditor()
        editors += TagItemEditor()
        editors += GoToIslandEditor()
        editors += GoToPositionEditor()
        editors += ReforgeEditor()
        editors += EnchantEditor()
        editors += MessageStepEditor()
        // Newly added editors
        editors += ObtainCoinsEditor()
        editors += AwaitMiningEventEditor()
        editors += EquipPetEditor()
        editors += DailyEnchantingXpEditor()
        editors += RequireAsyncCompletionEditor()
        editors += SkillEditor()
        editors += MinionEditor()
        editors += ObtainBingoGoalEditor()
        // Additional editors to cover all steps
        editors += CollectionEditor()
        editors += GuiItemEditor()
        editors += GuiClickSlotEditor()
        editors += ObtainEditor()
        editors += SubAreaEditor()
        editors += CollectionLevelRequirementEditor()
        editors += CakeEditor()
        // Editors added to cover more step types
        editors += AwaitGodSplashEditor()
        editors += JoinInstanceEditor()
        editors += InTimeframeEditor()
        editors += EquipArmorEditor()
        editors += ObtainFromNEURecipeEditor()
        // Fallback editor last
        editors += DefaultNodeEditor()
    }

    fun register(editor: TutorialNodeEditor) { editors += editor }

    fun editorFor(node: TutorialNode): TutorialNodeEditor =
        editors.firstOrNull { it.supports(node) } ?: DefaultNodeEditor()
}
