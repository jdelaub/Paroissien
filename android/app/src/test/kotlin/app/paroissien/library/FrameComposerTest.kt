package app.paroissien.library

import app.paroissien.core.designsystem.frame.OrnamentalFrames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Un cadre composé de modules de plusieurs cadres : chaque tracé trouve son encre, rien ne disparaît. */
class FrameComposerTest {

    private val theme = setOf("paper", "text", "rubric")

    @Test
    fun chaqueEncreEstDefinie() {
        val spec = CustomFrameSpec(
            id = "perso-essai", name = "Essai",
            top = ModuleRef("matin", "top"), side = ModuleRef("vigne_calice", "side"), bottom = ModuleRef("soir", "bottom"),
            corner = ModuleRef("gothique", "corner"), center = ModuleRef("lis_vox_dei", "sideCenter"),
            ground = "soir", fillets = "cadre", disposition = "ouverte",
        )
        val def = requireNotNull(FrameComposer.build(spec))
        val inks = def.style.base.keys + theme
        val s = def.spec
        val ops = s.top.base + s.top.over + s.side.base + s.side.over + s.bottom.base + s.bottom.over +
            listOfNotNull(s.corner, s.bottomCorner).flatMap { it.ops } + listOfNotNull(s.topCenter, s.sideCenter).flatMap { it.ops }
        assertTrue(ops.isNotEmpty())
        val missing = ops.map { it.ink }.toSet() - inks
        assertEquals("encres sans couleur : $missing", emptySet<String>(), missing)
        s.ground?.let { assertTrue(it in inks) }
        s.rules.forEach { assertTrue(it.ink in inks) }
        assertEquals(false, s.bands.inner)
    }

    @Test
    fun tousLesModulesSeComposent() {
        for (kind in listOf("frise", "coin", "medaillon")) {
            val modules = FrameComposer.modules(kind)
            assertTrue(kind, modules.isNotEmpty())
            modules.forEach { assertNotNull("$kind ${it.frame}/${it.part}", FrameComposer.sample(it, kind)) }
        }
        assertTrue(OrnamentalFrames.isNotEmpty())
    }

    private fun assertInked(def: app.paroissien.core.designsystem.frame.OrnamentalFrameDef) {
        val s = def.spec
        val inks = def.style.base.keys + theme
        val ops = s.top.base + s.top.over + s.side.base + s.side.over + s.bottom.base + s.bottom.over +
            listOfNotNull(s.corner, s.bottomCorner).flatMap { it.ops } + listOfNotNull(s.topCenter, s.sideCenter).flatMap { it.ops }
        val missing = ops.map { it.ink }.toSet() - inks
        assertEquals("${def.name} : encres sans couleur $missing", emptySet<String>(), missing)
        s.ground?.let { assertTrue(it in inks) }
    }

    @Test
    fun compositionsAuHasard() {
        repeat(40) { seed ->
            val spec = FrameComposer.random(seed.toLong(), "perso-$seed", "Essai")
            assertInked(requireNotNull(FrameComposer.build(spec)) { "graine $seed" })
            // La composition tirée survit à l'enregistrement.
            assertEquals(spec, CustomFrameSpec.decode(CustomFrameSpec.encode(spec)))
        }
    }

    @Test
    fun motifDessineDansUnCadre() {
        val motif = CustomMotif(
            id = "motif-essai", name = "Essai", period = 120f, mirrorU = true, mirrorV = true, ground = 0xFF2E4865,
            strokes = listOf(
                MotifStroke(listOf(10f, 20f, 30f, 50f, 55f, 40f), width = 6f, color = 0xFFB89C5A),
                MotifStroke(listOf(60f, 50f), width = 10f, color = 0xFFD8745A, dot = true),
            ),
        )
        FrameComposer.motifs = mapOf(motif.id to motif)
        try {
            val def = requireNotNull(FrameComposer.build(CustomFrameSpec("perso-m", "M", top = ModuleRef(motif.id, "top"), ground = motif.id)))
            assertInked(def)
            // Deux traits, chacun en quatre exemplaires (miroirs gauche-droite et haut-bas).
            assertEquals(8, def.spec.top.base.size)
            assertEquals("motif-essai.ground", def.spec.ground)
            assertTrue(FrameComposer.modules("frise").any { it.frame == motif.id })
            assertEquals(motif, CustomMotif.decode(CustomMotif.encode(motif)))
        } finally {
            FrameComposer.motifs = emptyMap()
        }
    }

    @Test
    fun choixAllerRetour() {
        val choice = BookDressChoice(FrameMode.PER_CHAPTER, chapterFrames = mapOf("u1" to "matin", "u2" to ""), seed = 3)
        assertEquals(choice, BookDressChoice.decode(BookDressChoice.encode(choice)))
        assertEquals(null, BookDressChoice.decode("pas du json"))
    }
}
