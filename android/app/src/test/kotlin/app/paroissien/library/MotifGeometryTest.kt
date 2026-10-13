package app.paroissien.library

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** L'atelier des motifs : placer, agrandir, tourner, toucher ; les motifs d'avant les formes se relisent tels quels. */
class MotifGeometryTest {

    private val fraise = MotifStroke(listOf(40f, 50f), width = 4f, color = 0xFFB0332B, stamp = "fraise", sx = 10f, sy = 10f)

    @Test
    fun placedElementsMoveGrowAndTurnAroundTheirCenter() {
        val e = MotifGeometry.transform(fraise, Offset(5f, -3f), 2f, 30f)
        assertEquals(Offset(45f, 47f), MotifGeometry.center(e))
        assertEquals(20f, e.sx, 1e-4f)
        assertEquals(30f, e.rotation, 1e-4f)
    }

    @Test
    fun freehandStrokesTurnAroundTheirCentroid() {
        val line = MotifStroke(listOf(0f, 0f, 10f, 0f), width = 2f, color = 0xFF000000)
        val turned = MotifGeometry.transform(line, Offset.Zero, 1f, 90f)
        assertEquals(5f, turned.points[0], 1e-3f)
        assertEquals(-5f, turned.points[1], 1e-3f)
        assertEquals(5f, turned.points[2], 1e-3f)
        assertEquals(5f, turned.points[3], 1e-3f)
    }

    @Test
    fun aTouchFindsTheElementUnderTheFinger() {
        assertTrue(MotifGeometry.hit(fraise, Offset(46f, 55f), 2f))
        assertFalse(MotifGeometry.hit(fraise, Offset(70f, 50f), 2f))
    }

    @Test
    fun everyStampAndShapeHasAFigure() {
        MotifStamps.all.forEach { assertTrue(it.id, it.figure.subs.isNotEmpty()) }
        MotifShape.entries.forEach { assertTrue(it.name, it.figure.subs.isNotEmpty()) }
        assertNotNull(MotifGeometry.figure(fraise))
    }

    /** Un motif enregistré avant les formes (12/10/2026) se relit, et ses encres gardent leurs noms. */
    @Test
    fun oldMotifsStillRead() {
        val json = """{"id":"motif-a","name":"Vieux","period":140.0,"strokes":[{"points":[1.0,2.0,3.0,4.0],"width":6.0,"color":4290346074}],"mirrorU":true,"mirrorV":false,"ground":null}"""
        val m = requireNotNull(CustomMotif.decode(json))
        assertEquals(1, m.strokes.size)
        assertEquals(null, m.strokes[0].stamp)
        assertEquals(setOf("motif-a.c0"), FrameComposer.motifInks(m).keys)
    }

    @Test
    fun metalStrokesGetAMetalInk() {
        val m = CustomMotif("motif-b", "Or", strokes = listOf(fraise.copy(metal = "GOLD")), groundMetal = "SILVER")
        assertEquals(setOf("motif-b.m.GOLD", "motif-b.ground"), FrameComposer.motifMetals(m).keys)
    }
}
