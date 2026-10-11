package app.paroissien.core.reading.book

import app.paroissien.core.content.PackBlock
import app.paroissien.core.model.text.Inlines
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * Blocs d'une unité de paquet → blocs de lecture, dans la [language] voulue (sinon [fallback], sinon la première).
 * Le titre courant réunit le titre de l'unité et le dernier titre de niveau 1 ou 2 (« Livre premier · Chapitre III »).
 * Un type inconnu s'affiche comme un paragraphe (ADR 003) ; les notes attendent leur panneau.
 */
fun packToReadingBlocks(blocks: List<PackBlock>, unitTitle: String, language: String, fallback: String): List<ReadingBlock> {
    var head = unitTitle
    return blocks.mapNotNull { block ->
        val lang = listOf(language, fallback).firstOrNull { it in block.texts } ?: block.texts.keys.firstOrNull()
        val text = lang?.let { block.texts[it] }
        val blockLang = BlockLang.of(lang ?: language)
        fun attr(key: String) = block.attrs[key]?.jsonPrimitive?.contentOrNull
        when (block.type) {
            "separator" -> ReadingBlock.Ornament(block.id)
            "note", "image", "slot" -> null
            else -> if (text == null) null else when (block.type) {
                "heading" -> {
                    val level = attr("level")?.toIntOrNull() ?: 1
                    val plain = Inlines.plainText(text)
                    val running = if (level <= 2 && plain != unitTitle) "$unitTitle · $plain".also { head = it } else null
                    ReadingBlock.Heading(block.id, text, runningHead = running ?: head.takeIf { level <= 2 }, minor = level >= 2, lang = blockLang)
                }
                "rubric" -> ReadingBlock.Rubric(block.id, text)
                "versicle" -> ReadingBlock.Versicle(block.id, text, blockLang)
                "verse" -> ReadingBlock.Paragraph(block.id, text, blockLang, tight = attr("p") != "true")
                else -> ReadingBlock.Paragraph(
                    block.id, text, blockLang,
                    dropCap = attr("dropCap") == "true",
                    role = when {
                        attr("style") == "centered" -> TextRole.CENTERED
                        attr("style") in setOf("small", "intro", "epigraph", "signature") || block.type == "quote" -> TextRole.SMALL
                        else -> TextRole.BODY
                    },
                )
            }
        }
    }
}
