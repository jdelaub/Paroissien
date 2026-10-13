package app.paroissien.core.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Export des données de l'utilisateur (ADR 008, rapport 09) : positions, rubans, images posées et images pieuses
 * personnelles (leur description ; les fichiers voyagent dans la sauvegarde complète), en JSON versionné.
 * Rien ne part dans le nuage ; l'utilisateur garde ce fichier où il veut. Les intentions et le journal viendront en V1.
 */
@Serializable
data class UserDataExport(
    val formatVersion: Int = FORMAT_VERSION,
    val exportedAt: Long,
    val positions: List<ExportedPosition>,
    val ribbons: List<ExportedRibbon>,
    /** Ajouté en 0.5.0 : absent des fichiers plus anciens. */
    val cards: List<ExportedCard> = emptyList(),
    /** Ajouté en 0.5.9 : images pieuses numérisées ou importées (titre, verso). */
    val personalCards: List<ExportedPersonalCard> = emptyList(),
    /** Ajoutés en 0.6.0 : catégories créées par l'utilisateur, et ce qu'il a rangé ou retiré. */
    val categories: List<ExportedCategory> = emptyList(),
    val cardCategories: List<ExportedCardCategory> = emptyList(),
    /** Ajoutés en 0.6.1 : l'habit de chaque livre et les cadres composés (JSON de l'atelier). */
    val bookLooks: List<ExportedJson> = emptyList(),
    val customFrames: List<ExportedJson> = emptyList(),
) {
    companion object {
        const val FORMAT_VERSION = 1
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

        fun encode(export: UserDataExport): String = json.encodeToString(serializer(), export)

        fun decode(text: String): UserDataExport {
            val export = json.decodeFromString(serializer(), text)
            require(export.formatVersion <= FORMAT_VERSION) { "Ce fichier vient d'une version plus récente de Paroissien" }
            return export
        }
    }
}

@Serializable
data class ExportedPosition(val publication: String, val unit: String, val block: String, val charOffset: Int, val blockRev: String? = null, val updatedAt: Long)

@Serializable
data class ExportedRibbon(val publication: String, val slot: Int, val unit: String, val block: String, val charOffset: Int, val blockRev: String? = null, val updatedAt: Long)

/** Une description JSON de l'atelier, telle quelle : [key] est la publication ou l'identifiant du cadre. */
@Serializable
data class ExportedJson(val key: String, val json: String, val updatedAt: Long)

@Serializable
data class ExportedCategory(val id: String, val label: String, val createdAt: Long)

@Serializable
data class ExportedCardCategory(val card: String, val category: String, val removed: Boolean, val updatedAt: Long)

@Serializable
data class ExportedPersonalCard(val id: String, val title: String, val hasBack: Boolean, val createdAt: Long, val meta: String? = null)

@Serializable
data class ExportedCard(val card: String, val publication: String, val unit: String, val block: String, val charOffset: Int, val blockRev: String? = null, val updatedAt: Long)

/** Exporte et réimporte ; à l'import, la donnée la plus récente l'emporte, et rien n'est jamais effacé. */
class UserDataTransfer(private val db: UserDatabase) {

    suspend fun export(now: Long = System.currentTimeMillis()): String = UserDataExport.encode(
        UserDataExport(
            exportedAt = now,
            positions = db.positions().all().map { ExportedPosition(it.publication, it.unit, it.block, it.charOffset, it.blockRev, it.updatedAt) },
            ribbons = db.ribbons().all().map { ExportedRibbon(it.publication, it.slot, it.unit, it.block, it.charOffset, it.blockRev, it.updatedAt) },
            cards = db.cards().all().map { ExportedCard(it.card, it.publication, it.unit, it.block, it.charOffset, it.blockRev, it.updatedAt) },
            personalCards = db.personalCards().all().map { ExportedPersonalCard(it.id, it.title, it.hasBack, it.createdAt, it.meta) },
            categories = db.categories().categories().map { ExportedCategory(it.id, it.label, it.createdAt) },
            cardCategories = db.categories().assignments().map { ExportedCardCategory(it.card, it.category, it.removed, it.updatedAt) },
            bookLooks = db.looks().all().map { ExportedJson(it.publication, it.json, it.updatedAt) },
            customFrames = db.looks().frames().map { ExportedJson(it.id, it.json, it.updatedAt) },
        ),
    )

    /** Renvoie le nombre d'éléments repris. */
    suspend fun import(text: String): Int {
        val data = UserDataExport.decode(text)
        val positions = db.positions().all().associateBy { it.publication }
        val ribbons = db.ribbons().all().associateBy { it.publication to it.slot }
        val cards = db.cards().all().associateBy { it.card }
        val personal = db.personalCards().all().associateBy { it.id }
        var taken = 0
        val categories = db.categories().categories().associateBy { it.id }
        for (c in data.categories) {
            if (c.id !in categories) {
                db.categories().saveCategory(CardCategory(c.id, c.label, c.createdAt))
                taken++
            }
        }
        val assignments = db.categories().assignments().associateBy { it.card to it.category }
        for (a in data.cardCategories) {
            if ((assignments[a.card to a.category]?.updatedAt ?: Long.MIN_VALUE) < a.updatedAt) {
                db.categories().assign(CardInCategory(a.card, a.category, a.removed, a.updatedAt))
                taken++
            }
        }
        val looks = db.looks().all().associateBy { it.publication }
        for (l in data.bookLooks) {
            if ((looks[l.key]?.updatedAt ?: Long.MIN_VALUE) < l.updatedAt) {
                db.looks().save(BookLookRow(l.key, l.json, l.updatedAt))
                taken++
            }
        }
        val frames = db.looks().frames().associateBy { it.id }
        for (f in data.customFrames) {
            if ((frames[f.key]?.updatedAt ?: Long.MIN_VALUE) < f.updatedAt) {
                db.looks().saveFrame(CustomFrameRow(f.key, f.json, f.updatedAt))
                taken++
            }
        }
        // Une image déjà là garde son titre : celui de l'appareil a pu être changé depuis la sauvegarde.
        for (c in data.personalCards) {
            if (c.id !in personal) {
                db.personalCards().save(PersonalCard(c.id, c.title, c.hasBack, c.createdAt, c.meta))
                taken++
            }
        }
        for (p in data.positions) {
            if ((positions[p.publication]?.updatedAt ?: Long.MIN_VALUE) < p.updatedAt) {
                db.positions().save(BookPosition(p.publication, p.unit, p.block, p.charOffset, p.blockRev, p.updatedAt))
                taken++
            }
        }
        for (r in data.ribbons) {
            if (r.slot !in 0..5) continue
            if ((ribbons[r.publication to r.slot]?.updatedAt ?: Long.MIN_VALUE) < r.updatedAt) {
                db.ribbons().place(Ribbon(r.publication, r.slot, r.unit, r.block, r.charOffset, r.blockRev, r.updatedAt))
                taken++
            }
        }
        for (c in data.cards) {
            if ((cards[c.card]?.updatedAt ?: Long.MIN_VALUE) < c.updatedAt) {
                db.cards().place(CardPlacement(c.card, c.publication, c.unit, c.block, c.charOffset, c.blockRev, c.updatedAt))
                taken++
            }
        }
        return taken
    }
}
