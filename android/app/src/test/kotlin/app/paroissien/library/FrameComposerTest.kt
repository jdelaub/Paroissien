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

    @Test
    fun choixAllerRetour() {
        val choice = BookDressChoice(FrameMode.PER_CHAPTER, chapterFrames = mapOf("u1" to "matin", "u2" to ""), seed = 3)
        assertEquals(choice, BookDressChoice.decode(BookDressChoice.encode(choice)))
        assertEquals(null, BookDressChoice.decode("pas du json"))
    }
}
