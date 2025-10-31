package de.hype.bingonet.shared.tutorials

/**
 * Pure data interface for tutorial conditions.
 * Logic for evaluating conditions is in TutorialConditionLogic in at.hannibal2 package.
 */
interface TutorialCondition

/**
 * Helper function to create a NotTutorialCondition
 */
fun TutorialCondition.not(): NotTutorialCondition {
    return NotTutorialCondition(this)
}

/**
 * Pure data class: Condition that inverts another condition
 */
class NotTutorialCondition(
    val condition: TutorialCondition,
) : TutorialCondition

/**
 * Pure data class: Condition that requires all sub-conditions to be true
 */
class AndTutorialCondition(
    val conditions: List<TutorialCondition>,
) : TutorialCondition {
    constructor(vararg conditions: TutorialCondition) : this(conditions.toList())
}

/**
 * Pure data class: Condition that requires any sub-condition to be true
 */
class OrTutorialCondition(
    val conditions: List<TutorialCondition>,
) : TutorialCondition {
    constructor(vararg conditions: TutorialCondition) : this(conditions.toList())
}

/**
 * Pure data class: Condition that requires all nodes to be complete
 */
class AllOfTutorialCondition(
    val conditions: List<TutorialNode>,
) : TutorialCondition {
    constructor(vararg conditions: TutorialNode) : this(conditions.toList())
}

/**
 * Pure data class: Condition that requires any node to be complete
 */
class AnyOfTutorialCondition(
    val conditions: List<TutorialNode>,
) : TutorialCondition {
    constructor(vararg conditions: TutorialNode) : this(conditions.toList())
}
