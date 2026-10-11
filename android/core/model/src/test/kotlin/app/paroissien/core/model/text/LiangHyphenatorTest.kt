package app.paroissien.core.model.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LiangHyphenatorTest {

    private val latin = LiangHyphenator.latinLiturgical

    private fun hyphenated(word: String) = latin.insertSoftHyphens(word).replace(LiangHyphenator.SOFT_HYPHEN, '-')

    /** Mots de contrôle de la porte A (revue 15, §5.2), relus contre la syllabation liturgique. */
    @Test
    fun controlWordsBreakAsExpected() {
        val failures = CONTROL.mapNotNull { expected ->
            val actual = hyphenated(expected.replace("-", ""))
            if (actual == expected) null else "$actual (attendu $expected)"
        }
        assertEquals(failures.joinToString("\n"), 0, failures.size)
    }

    @Test
    fun keepsTwoLettersOnEachSide() {
        assertEquals("Amen", hyphenated("Amen"))
        assertEquals("gló-ria", hyphenated("glória")) // jamais « gló-ri-a »
    }

    @Test
    fun hyphenatesRunningTextWithoutTouchingPunctuation() {
        val text = "Glória in excélsis Deo. Et in terra pax homínibus bonæ voluntátis."
        val out = latin.insertSoftHyphens(text)
        assertEquals("Gló-ria in ex-cél-sis Deo. Et in ter-ra pax ho-mí-ni-bus bo-næ vo-lun-tá-tis.", out.replace(LiangHyphenator.SOFT_HYPHEN, '-'))
        assertEquals(text, out.replace(LiangHyphenator.SOFT_HYPHEN.toString(), ""))
    }

    @Test
    fun searchFoldingIgnoresInsertedHyphens() {
        val shown = latin.insertSoftHyphens("Dóminus")
        assertEquals("dominus", Fold.fold(shown))
        assertFalse(Fold.fold(shown).contains(LiangHyphenator.SOFT_HYPHEN))
    }

    private companion object {
        val CONTROL = listOf(
            "Dó-mi-nus", "gló-ria", "ex-cél-sis", "Spí-ri-tus", "Sanc-tus", "Ký-rie", "elé-i-son", "Chris-te",
            "om-ní-pot-ens", "sǽ-cu-la", "sæ-cu-ló-rum", "cæ-lum", "Be-ne-dí-ci-mus", "Glo-ri-fi-cá-mus",
            "de-pre-ca-ti-ó-nem", "mi-se-ri-cór-di-am", "In-tro-í-bo", "al-tá-re", "læ-tí-fi-cat", "ju-ven-tú-tem",
            "Jú-di-ca", "di-scér-ne", "in-í-quo", "do-ló-so", "for-ti-tú-do", "re-pu-lís-ti", "in-cé-do",
            "af-flí-git", "in-i-mí-cus", "pec-cá-ta", "mi-se-ré-re", "sú-sci-pe", "déx-te-ram", "uni-gé-ni-te",
            "Al-tís-si-mus", "vo-lun-tá-tis", "ho-mí-ni-bus", "grá-ti-as", "ági-mus", "cæ-lés-tis", "Agnus",
            "tol-lis", "Cre-do", "vi-si-bí-li-um", "in-vi-si-bí-li-um", "con-sub-stan-ti-á-lem", "in-car-ná-tus",
            "cru-ci-fí-xus", "re-sur-rec-ti-ó-nem", "Amen", "imi-ta-ti-ó-ne", "va-ni-tá-tum", "sa-pi-én-tia",
            "Ho-sán-na", "be-ne-díc-tus", "Je-su", "Evan-gé-li-um",
        )
    }
}
