/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 * Ported from org.aph.monarchblocks.monarch_utils.BrailleUtil (monarch-dotsandboxes/MonarchBlocks).
 */

package org.aph.plantwave.monarch_utils

import com.humanware.keysoftsdk.translator.BrailleGrade
import com.humanware.keysoftsdk.translator.Translator
import com.humanware.keysoftsdk.translator.aidl.TranslationQuery

class BrailleUtil {
    companion object{
        private val cache = HashMap<String, String>()

        /**
         * Translates plain text string into unicode braille string
         *
         * @param input String object which contains the plain text to be translated.
         * @param translator HumanWare Translator object that performs the translation.
         * @param setBack (Optional) Boolean when set to true it will convert unicode braille back to plain text. Default is false.
         *
         * @return Unicode Braille String of the original string that was passed in.
         */
        fun translate(input: String, translator: Translator, setBack:Boolean = false): String {
            val key = if (setBack) "B:$input" else input
            cache[key]?.let { return it }

            var res = ""
            val query = TranslationQuery()
            //query.grade = BrailleGrade.GRADE_UNCONTRACTED //If you want to specify the braille grade to translate into.
            query.isBack = setBack
            val lines = input.lines()
            for (line in lines) {
                query.original = line
                translator.translate(query)
                res += query.translated + "\n"
            }
            res= res.trimEnd('\n') //no trailing \n

            cache[key] = res
            return res
        }


        /**
         * Gets the current braille grade.
         *
         * @param translator HumanWare Translator object that performs the translation.
         *
         * @return An Integer indicating Uncontracted/Contracted. Compare to BrailleGrade.GRADE_UNCONTRACTED, BrailleGrade.GRADE_CONTRACTED, BrailleGrade.GRADE_COMPUTER_BRAILLE, or BrailleGrade.UNSPECIFIED.
         */
        fun getBrailleGrade(translator: Translator): Int{
            val translationQuery = translator.forwardTranslate("a", 0)
            var brailleGrade = BrailleGrade.GRADE_UNSPECIFIED

            if(translationQuery != null){
                brailleGrade = translationQuery.grade
            }

            return brailleGrade
        }
    }
}
