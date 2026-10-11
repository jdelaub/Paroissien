package app.paroissien.core.model.text

import java.text.Normalizer

/**
 * Césure par motifs de Liang (format TeX hyph-utf8). Android ne coupe pas les mots latins accentués
 * (rapport 07) : on calcule les points de coupure à l'affichage et on insère des U+00AD que le moteur
 * de texte honore. Le texte canonique, lui, n'en contient jamais (ADR 004).
 */
class LiangHyphenator(patterns: Sequence<String>, private val leftMin: Int = 2, private val rightMin: Int = 2) {

    private val table = HashMap<String, IntArray>()
    private val maxLength: Int

    init {
        var longest = 0
        for (raw in patterns) {
            val pattern = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC)
            if (pattern.isEmpty() || pattern.startsWith("%")) continue
            val letters = StringBuilder()
            val values = ArrayList<Int>()
            var pending = 0
            for (c in pattern) {
                if (c.isDigit()) {
                    pending = c - '0'
                } else {
                    values += pending
                    pending = 0
                    letters.append(c)
                }
            }
            values += pending
            table[letters.toString()] = values.toIntArray()
            longest = maxOf(longest, letters.length)
        }
        maxLength = longest
    }

    /** Positions (indices dans [word]) devant lesquelles une coupure est permise. */
    fun breakPoints(word: String): List<Int> {
        val n = word.length
        if (n < leftMin + rightMin) return emptyList()
        // Minuscule caractère par caractère : les indices restent alignés sur [word].
        val dotted = ".${String(CharArray(n) { word[it].lowercaseChar() })}."
        val points = IntArray(dotted.length + 1)
        for (i in dotted.indices) {
            for (j in i + 1..minOf(dotted.length, i + maxLength)) {
                val values = table[dotted.substring(i, j)] ?: continue
                for (k in values.indices) points[i + k] = maxOf(points[i + k], values[k])
            }
        }
        // points[p + 1] concerne la coupure entre word[p - 1] et word[p] (décalage du point initial).
        return (leftMin..n - rightMin).filter { p -> points[p + 1] % 2 == 1 && !isMark(word[p]) }
    }

    /** Insère des césures conditionnelles (U+00AD) dans chaque mot de [text], en NFC. */
    fun insertSoftHyphens(text: String): String {
        val nfc = Normalizer.normalize(text, Normalizer.Form.NFC)
        val out = StringBuilder(nfc.length + nfc.length / 4)
        var i = 0
        while (i < nfc.length) {
            if (!isWordChar(nfc[i])) {
                out.append(nfc[i++])
                continue
            }
            val start = i
            while (i < nfc.length && isWordChar(nfc[i])) i++
            val word = nfc.substring(start, i)
            var last = 0
            for (p in breakPoints(word)) {
                out.append(word, last, p).append(SOFT_HYPHEN)
                last = p
            }
            out.append(word, last, word.length)
        }
        return out.toString()
    }

    private fun isWordChar(c: Char) = c.isLetter() || isMark(c)

    private fun isMark(c: Char) = Character.getType(c) == Character.NON_SPACING_MARK.toInt()

    companion object {
        const val SOFT_HYPHEN = '­'

        /** Latin liturgique (Solesmes, Beccari, Roux ; MIT), minima 2/2. */
        val latinLiturgical: LiangHyphenator by lazy {
            val stream = requireNotNull(LiangHyphenator::class.java.getResourceAsStream("/hyphenation/la-x-liturgic.pat.txt")) {
                "Motifs de césure latins absents des ressources"
            }
            stream.bufferedReader(Charsets.UTF_8).useLines { LiangHyphenator(it.toList().asSequence()) }
        }
    }
}
