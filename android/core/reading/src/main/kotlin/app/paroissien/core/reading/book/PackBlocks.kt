package app.paroissien.core.reading.book

import androidx.compose.ui.graphics.ImageBitmap
import app.paroissien.core.content.PackBlock
import app.paroissien.core.model.text.Inlines
import app.paroissien.core.model.text.RichText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Les blocs qui se mettent en regard quand ils ont les deux langues. */
private val FACING_TYPES = setOf("para", "verse", "versicle", "heading", "rubric", "quote", "stanza", "item")

/**
 * Blocs d'une unité de paquet → blocs de lecture, dans la [language] voulue (sinon [fallback], sinon la première).
 * Le titre courant réunit le titre de l'unité et le dernier titre de niveau 1 ou 2 (« Livre premier · Chapitre III »).
 * Un type inconnu s'affiche comme un paragraphe (ADR 003) ; les notes attendent leur panneau. [facing] : un bloc qui a
 * du latin et du français se compose en deux colonnes. [image] donne l'image d'un bloc `image` (masque d'encre).
 */
fun packToReadingBlocks(
    blocks: List<PackBlock>,
    unitTitle: String,
    language: String,
    fallback: String,
    facing: Boolean = false,
    image: (String) -> ImageBitmap? = { null },
): List<ReadingBlock> {
    var head = unitTitle

    fun one(block: PackBlock, lang: String, text: RichText): ReadingBlock {
        val blockLang = BlockLang.of(lang)
        fun attr(key: String) = block.attrs[key]?.jsonPrimitive?.contentOrNull
        return when (block.type) {
            "heading" -> {
                val level = attr("level")?.toIntOrNull() ?: 1
                val plain = Inlines.plainText(text)
                // Un titre en marge du fil (consigne du servant dans un livret de messe) ne change pas le titre courant.
                val aside = attr("label") != null
                val running = if (level <= 2 && !aside && plain != unitTitle) "$unitTitle · $plain".also { head = it } else null
                ReadingBlock.Heading(block.id, text, runningHead = running ?: head.takeIf { level <= 2 && !aside }, minor = level >= 2, lang = blockLang)
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

    return blocks.mapNotNull { block ->
        val lang = listOf(language, fallback).firstOrNull { it in block.texts } ?: block.texts.keys.firstOrNull()
        val text = lang?.let { block.texts[it] }
        when (block.type) {
            "separator" -> ReadingBlock.Ornament(block.id)
            "note" -> null
            "image" -> block.attrs["href"]?.jsonPrimitive?.contentOrNull?.let(image)?.let { ReadingBlock.Vignette(block.id, it) }
            "slot" -> text?.let {
                fun ids(key: String) = (block.attrs[key] as? JsonArray)?.mapNotNull { e -> e.jsonPrimitive.contentOrNull }.orEmpty()
                ReadingBlock.Slot(block.id, it, block.attrs["slot"]?.jsonPrimitive?.contentOrNull, ids("replaces"), ids("omit"))
            }
            else -> {
                val la = block.texts["la"]
                val fr = block.texts["fr"]
                when {
                    text == null -> null
                    facing && la != null && fr != null && block.type in FACING_TYPES -> {
                        val left = one(block, "la", la)
                        // Le titre courant suit le français : le titre de droite est composé en dernier.
                        ReadingBlock.Facing(block.id, left, one(block, "fr", fr))
                    }
                    else -> one(block, lang, text)
                }
            }
        }
    }
}
