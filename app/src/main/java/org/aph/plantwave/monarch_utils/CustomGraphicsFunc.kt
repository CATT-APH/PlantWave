/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 * Ported from org.aph.monarchblocks.monarch_utils.CustomGraphicsFunc (monarch-dotsandboxes/MonarchBlocks).
 */

package org.aph.plantwave.monarch_utils

import com.humanware.keysoftsdk.selfbrailling.aidl.DotsMatrix


class CustomGraphicsFunc {
    companion object{

        /**
         * Takes a sentence and converts it into a graphic made using the Fancy Font graphics.
         */
        fun sentenceToFancyFontGraphic(sentence: String): DotsMatrix?{
            if(sentence.length <= 9) {//The sentence displayed needs to be no more than 9 characters. That is all that will fit across the device using Fancy Font.
                val rows = 8
                val cols = 10
                var graphic = Array(rows) { ByteArray(cols) }

                for (i in sentence.indices) {
                    val letterGraphic = letterToFancyFontGraphic(sentence[i])

                    if (i == 0) {
                        graphic = letterGraphic
                    } else {
                        for (j in graphic.indices) {
                            graphic[j] += letterGraphic[j]
                        }
                    }
                }

                return DotsMatrix(graphic)
            }
            else{
                return null
            }
        }



        /**
         * Method takes a letter A-Z and creates a byte array for the pin positions to create a graphical representation of the letter. This
         * only cover A-Z and exclamation points (!). This is mainly used for Title Screens.
         *
         * @param letter: A char A-Z or !.
         *
         * @returns An Array<ByteArray> of 0s and 1s to create a graphical representation of the letter passed. It must be converted to a DotsMatrix object to be displayed on the screen.
         */
        fun letterToFancyFontGraphic(letter: Char): Array<ByteArray>{
            val letterGraphic = when(letter.uppercase()){
                "A" -> {
                    arrayOf(
                        byteArrayOf(0,0,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0))
                }
                "B" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0))
                }
                "C" -> {
                    arrayOf(
                        byteArrayOf(0,0,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,1,1,1,1,1,1,1,0))
                }
                "D" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0))
                }
                "E" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0))
                }
                "F" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0))
                }
                "G" -> {
                    arrayOf(
                        byteArrayOf(0,0,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,1,1,1,1,1,1,0,0))
                }
                "H" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0))
                }
                "I" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0))
                }
                "J" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,1,1,0,1,1,0,0,0,0),
                        byteArrayOf(0,1,1,0,1,1,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,0,0,0,0,0))
                }
                "K" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,1,1,1,0,0,0),
                        byteArrayOf(0,1,1,1,1,0,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,1,1,1,0,0,0),
                        byteArrayOf(0,1,1,0,0,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,1,1,1,0))
                }
                "L" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0))
                }
                "M" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,0,0,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,1,1,0,1,1,0),
                        byteArrayOf(0,1,1,0,1,1,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0))
                }
                "N" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,1,1,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0))
                }
                "O" -> {
                    arrayOf(
                        byteArrayOf(0,0,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,1,1,1,1,1,1,0,0))
                }
                "P" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0))
                }
                "Q" -> {
                    arrayOf(
                        byteArrayOf(0,0,1,1,1,1,1,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,0,0,0,1,1,0,0),
                        byteArrayOf(0,1,1,0,0,0,1,1,0,0),
                        byteArrayOf(0,1,1,0,0,0,1,1,0,0),
                        byteArrayOf(0,1,1,0,0,0,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,1,1,1,1,1,0,1,0))
                }
                "R" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0))
                }
                "S" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,0,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0))
                }
                "T" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0))
                }
                "U" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,1,1,1,1,1,1,0,0))
                }
                "V" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,0,1,1,0,0,0,1,1,0),
                        byteArrayOf(0,0,1,1,0,0,1,1,0,0),
                        byteArrayOf(0,0,1,1,0,0,1,1,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0))
                }
                "W" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,1,1,0,1,1,0),
                        byteArrayOf(0,1,1,0,1,1,0,1,1,0),
                        byteArrayOf(0,1,1,1,0,0,1,1,1,0),
                        byteArrayOf(0,1,1,1,0,0,1,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0))
                }
                "X" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,0,1,1,0,0,1,1,1,0),
                        byteArrayOf(0,0,0,1,1,1,1,0,0,0),
                        byteArrayOf(0,0,0,1,1,1,1,0,0,0),
                        byteArrayOf(0,0,1,1,0,0,1,1,0,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0))
                }
                "Y" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,1,1,0,0,0,0,1,1,0),
                        byteArrayOf(0,0,1,1,0,0,1,1,0,0),
                        byteArrayOf(0,0,0,1,1,1,1,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0))
                }
                "Z" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,0,0,0,0,0,1,1,1,0),
                        byteArrayOf(0,0,0,0,0,1,1,0,0,0),
                        byteArrayOf(0,0,0,0,1,1,0,0,0,0),
                        byteArrayOf(0,0,1,1,1,0,0,0,0,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0),
                        byteArrayOf(0,1,1,1,1,1,1,1,1,0))
                }
                " " -> {
                    arrayOf(
                        byteArrayOf(0,0),
                        byteArrayOf(0,0),
                        byteArrayOf(0,0),
                        byteArrayOf(0,0),
                        byteArrayOf(0,0),
                        byteArrayOf(0,0),
                        byteArrayOf(0,0),
                        byteArrayOf(0,0)
                    )
                }
                "!" -> {
                    arrayOf(
                        byteArrayOf(0,1,1,0),
                        byteArrayOf(0,1,1,0),
                        byteArrayOf(0,1,1,0),
                        byteArrayOf(0,1,1,0),
                        byteArrayOf(0,1,1,0),
                        byteArrayOf(0,0,0,0),
                        byteArrayOf(0,1,1,0),
                        byteArrayOf(0,1,1,0)
                    )
                }
                else -> {
                    arrayOf(
                        byteArrayOf(0,0,0,0,0,0,0,0,0,0),
                        byteArrayOf(0,0,0,0,0,0,0,0,0,0),
                        byteArrayOf(0,0,0,0,0,0,0,0,0,0),
                        byteArrayOf(0,0,0,0,0,0,0,0,0,0),
                        byteArrayOf(0,0,0,0,0,0,0,0,0,0),
                        byteArrayOf(0,0,0,0,0,0,0,0,0,0),
                        byteArrayOf(0,0,0,0,0,0,0,0,0,0),
                        byteArrayOf(0,0,0,0,0,0,0,0,0,0))
                }
            }

            return letterGraphic
        }


        /**
         *
         */
        fun createBrailleSentenceGraphic(sentence: String): DotsMatrix{
            var brailleGraphic = arrayOf(
                byteArrayOf(0),
                byteArrayOf(0),
                byteArrayOf(0)
            )
            val brailleSpace = arrayOf(
                byteArrayOf(0),
                byteArrayOf(0),
                byteArrayOf(0),
                byteArrayOf(0)
            )

            for (i in sentence.indices) {
                val brailleCell = Drawing.brailleCellFromCharacter(sentence[i])

                if (i == 0) {
                    brailleGraphic = brailleCell
                }
                else {
                    for (j in brailleGraphic.indices) {
                        brailleGraphic[j] += brailleSpace[j]
                        brailleGraphic[j] += brailleCell[j]
                    }
                }
            }


            return DotsMatrix(brailleGraphic)
        }


        /**
         * Creates a down arrow image used for indicating more text can be scrolled to.
         *
         * @return A DotsMatrix object of a down arrow graphic.
         */
        fun downArrowGraphic(): DotsMatrix{
            val arrowGraphic = arrayOf(
                byteArrayOf(1,1,1,1,1),
                byteArrayOf(0,1,1,1,0),
                byteArrayOf(0,0,1,0,0)
            )

            return DotsMatrix(arrowGraphic)
        }


        /**
         * This just creates a simple square graphic created just for this tutorial.
         *
         * @return A DotsMatrix object of a square.
         */
        fun createSquareGraphic(): DotsMatrix{
            val squareGraphic = arrayOf(
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1),
                byteArrayOf(1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1)
            )

            return DotsMatrix(squareGraphic)
        }


        fun calculateImageCenterOffset(imageMeasurement: Int, screenMeasurement: Int): Int{
            return if(imageMeasurement != screenMeasurement) (screenMeasurement - imageMeasurement) / 2 else 0
        }
    }
}
