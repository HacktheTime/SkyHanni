package at.hannibal2.skyhanni.features.misc.massconfiguration

import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.placeholder
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import kotlin.math.max

fun massConfigMultilineText(
    raw: String,
    maxWidth: Int,
    horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
): Renderable {
    if (raw.isBlank()) return Renderable.placeholder(0, 0)
    val width = max(80, maxWidth)
    val paragraphs = raw.split("\n")
    val parts = mutableListOf<Renderable>()
    paragraphs.forEachIndexed { index, paragraph ->
        val lines = paragraph.takeIf { it.isNotEmpty() }?.splitLines(width)?.split("\n") ?: listOf(" ")
        lines.forEach { line ->
            parts.add(Renderable.text(line.ifEmpty { " " }, horizontalAlign = horizontalAlign))
        }
        if (index != paragraphs.lastIndex) {
            parts.add(Renderable.placeholder(0, 4))
        }
    }
    return when (parts.size) {
        0 -> Renderable.placeholder(0, 0)
        1 -> parts.first()
        else -> Renderable.vertical(parts, spacing = 2, horizontalAlign = horizontalAlign)
    }
}
