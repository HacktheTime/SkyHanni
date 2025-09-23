package de.hype.bingonet.shared.tutorials

interface TutorialCondition{
    fun matches(tutorial: Tutorial): Boolean
}
fun TutorialCondition.not(): NotTutorialCondition {
    return NotTutorialCondition(this)
}
class NotTutorialCondition(
    val condition: TutorialCondition
) : TutorialCondition {
    override fun matches(tutorial: Tutorial): Boolean {
        return !condition.matches(tutorial)
    }
}
class AndTutorialCondition(
    val conditions: List<TutorialCondition>
) : TutorialCondition {
    constructor(vararg conditions: TutorialCondition) : this(conditions.toList())
    override fun matches(tutorial: Tutorial): Boolean {
        return conditions.all { it.matches(tutorial) }
    }
}
class OrTutorialCondition(
    val conditions: List<TutorialCondition>
) : TutorialCondition {
    constructor(vararg conditions: TutorialCondition) : this(conditions.toList())
    override fun matches(tutorial: Tutorial): Boolean {
        return conditions.any { it.matches(tutorial) }
    }
}
class AllOfTutorialCondition(
    val conditions: List<TutorialNode>
) : TutorialCondition {
    constructor(vararg conditions: TutorialNode) : this(conditions.toList())
    override fun matches(tutorial: Tutorial): Boolean {
        return conditions.all { it.isComplete(tutorial) }
    }
}
class AnyOfTutorialCondition(
    val conditions: List<TutorialNode>
) : TutorialCondition {
    constructor(vararg conditions: TutorialNode) : this(conditions.toList())

    override fun matches(tutorial: Tutorial): Boolean {
        return conditions.any { it.isComplete(tutorial) }
    }
}
