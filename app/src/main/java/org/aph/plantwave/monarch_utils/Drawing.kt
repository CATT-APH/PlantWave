/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 * Ported from org.aph.monarchblocks.monarch_utils.Drawing (monarch-dotsandboxes/MonarchBlocks).
 */

package org.aph.plantwave.monarch_utils

import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix

const val PIN_DOWN: Byte = 0
const val PIN_UP: Byte = 1

class Drawing {
    companion object {

        /**
         * Method converts a Unicode Braille character into a Array<ByteArray> which can be used to make
         * a DotsMatrix object and displayed using the Self Brailling Widget.
         *
         * @param ch: A Unicode braille character.
         *
         * @return Returns an Array<ByteArray> which can be used to display by converting it to a DotsMatrix.
         */
        fun brailleCellFromCharacter(ch: Char): Array<ByteArray> {
            val res = Array(4) { ByteArray(2) {0}} //space
            if ((ch.code < 0x2800) || (ch.code > 0x28ff))
                return res

            res[0][0] = if ((ch.code and 0x01) != 0) { PIN_UP } else { PIN_DOWN }
            res[1][0] = if ((ch.code and 0x02) != 0) { PIN_UP } else { PIN_DOWN }
            res[2][0] = if ((ch.code and 0x04) != 0) { PIN_UP } else { PIN_DOWN }
            res[0][1] = if ((ch.code and 0x08) != 0) { PIN_UP } else { PIN_DOWN }
            res[1][1] = if ((ch.code and 0x10) != 0) { PIN_UP } else { PIN_DOWN }
            res[2][1] = if ((ch.code and 0x20) != 0) { PIN_UP } else { PIN_DOWN }
            res[3][0] = if ((ch.code and 0x40) != 0) { PIN_UP } else { PIN_DOWN }
            res[3][1] = if ((ch.code and 0x80) != 0) { PIN_UP } else { PIN_DOWN }
            return res
        }


        fun freeformBraille(s: String, dm: DotsMatrix, cPos: Position, center:Boolean = false) {
            //draws Braille anywhere -- no concept of cursor, no bounds checking, no new line support, etc.
            val stringLen = s.length * 3
            val pos = if(!center) Position(cPos) else Position((dm.numberOfColumns - stringLen)/2 , cPos.y) //copy callers position

            for (ch in s) {
                dm.include(DotsMatrix(brailleCellFromCharacter(ch)), pos.x, pos.y)
                pos.x += 3
            }
        }
    }
}
