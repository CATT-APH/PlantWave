/*
 * Copyright (c) 2026 American Printing House for the Blind
 * Use of this source code is governed by an MIT-style license that can be found in the LICENSE file.
 * Ported from org.aph.monarchblocks.monarch_utils.Position (monarch-dotsandboxes/MonarchBlocks).
 */

package org.aph.plantwave.monarch_utils

class Position(var x: Int = 0, var y: Int = 0) {
    constructor(p: Position): this(p.x, p.y) {}

    override fun toString(): String {
        return x.toString() + ", " + y.toString()
    }
}
