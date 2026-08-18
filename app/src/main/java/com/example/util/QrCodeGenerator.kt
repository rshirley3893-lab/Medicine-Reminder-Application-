package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import java.nio.charset.StandardCharsets

/**
 * Clean, lightweight, self-contained QR Code Matrix & Bitmap Generator in pure Kotlin.
 * Generates standard QR Code 2D module matrices with finder patterns, timing patterns,
 * alignment patterns, format information, and interleaved data/ECC codewords.
 */
object QrCodeGenerator {

    /**
     * Encodes a string into a QR code Bitmap of given width and height.
     */
    fun encodeToBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512,
        darkColor: Int = Color.BLACK,
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val matrix = encodeToMatrix(content)
        val size = matrix.size
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val moduleWidth = width / size.toFloat()
        val moduleHeight = height / size.toFloat()

        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val moduleY = ((y / moduleHeight).toInt()).coerceIn(0, size - 1)
            for (x in 0 until width) {
                val moduleX = ((x / moduleWidth).toInt()).coerceIn(0, size - 1)
                pixels[y * width + x] = if (matrix[moduleY][moduleX]) darkColor else lightColor
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Generates a 2D boolean matrix for the given content string.
     */
    fun encodeToMatrix(content: String): Array<BooleanArray> {
        val bytes = content.toByteArray(StandardCharsets.ISO_8859_1)
        // Select Version: 1 (21x21), 2 (25x25), 3 (29x29), 4 (33x33), 5 (37x37), 6 (41x41)
        val version = when {
            bytes.size <= 14 -> 1
            bytes.size <= 26 -> 2
            bytes.size <= 42 -> 3
            bytes.size <= 62 -> 4
            bytes.size <= 84 -> 5
            bytes.size <= 106 -> 6
            bytes.size <= 122 -> 7
            else -> 8
        }
        val size = 17 + 4 * version
        val grid = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        // 1. Draw Position Finder Patterns at 3 corners
        drawFinderPattern(grid, reserved, 0, 0)
        drawFinderPattern(grid, reserved, size - 7, 0)
        drawFinderPattern(grid, reserved, 0, size - 7)

        // 2. Draw Timing Patterns
        for (i in 8 until size - 8) {
            val bit = (i % 2 == 0)
            grid[6][i] = bit
            reserved[6][i] = true
            grid[i][6] = bit
            reserved[i][6] = true
        }

        // 3. Draw Alignment Patterns for version >= 2
        if (version >= 2) {
            val pos = when (version) {
                2 -> intArrayOf(6, 18)
                3 -> intArrayOf(6, 22)
                4 -> intArrayOf(6, 26)
                5 -> intArrayOf(6, 30)
                6 -> intArrayOf(6, 34)
                7 -> intArrayOf(6, 22, 38)
                8 -> intArrayOf(6, 24, 42)
                else -> intArrayOf(6, 26)
            }
            for (r in pos) {
                for (c in pos) {
                    if (reserved[r][c]) continue
                    drawAlignmentPattern(grid, reserved, r - 2, c - 2)
                }
            }
        }

        // 4. Dark Module
        grid[4 * version + 9][8] = true
        reserved[4 * version + 9][8] = true

        // 5. Reserve format info areas
        for (i in 0..8) {
            reserved[8][i] = true
            reserved[i][8] = true
        }
        for (i in size - 8 until size) {
            reserved[8][i] = true
            reserved[i][8] = true
        }

        // 6. Build bitstream (Byte Mode: 0100 + char count + data + terminator + padding)
        val bitList = mutableListOf<Int>()
        // Mode: 0100 (Byte mode)
        bitList.addAll(listOf(0, 1, 0, 0))
        // Character count indicator (8 bits for V1-V9)
        val count = bytes.size
        for (i in 7 downTo 0) {
            bitList.add((count shr i) and 1)
        }
        // Data bits
        for (b in bytes) {
            val unsigned = b.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bitList.add((unsigned shr i) and 1)
            }
        }
        // Terminator (up to 4 zeroes)
        val dataCapacityBits = getDataCapacityBits(version)
        val termLen = (dataCapacityBits - bitList.size).coerceIn(0, 4)
        repeat(termLen) { bitList.add(0) }

        // Align to byte boundary
        while (bitList.size % 8 != 0 && bitList.size < dataCapacityBits) {
            bitList.add(0)
        }

        // Add pad bytes (0xEC, 0x11)
        var padToggle = true
        while (bitList.size < dataCapacityBits) {
            val pad = if (padToggle) 0xEC else 0x11
            padToggle = !padToggle
            for (i in 7 downTo 0) {
                bitList.add((pad shr i) and 1)
            }
        }

        // 7. Calculate simplified ECC codewords and place data bits into matrix
        val dataBytes = bitListToBytes(bitList)
        val eccCount = getEccCount(version)
        val eccBytes = computeReedSolomonEcc(dataBytes, eccCount)

        val fullBits = mutableListOf<Int>()
        for (b in dataBytes) {
            for (i in 7 downTo 0) fullBits.add((b shr i) and 1)
        }
        for (b in eccBytes) {
            for (i in 7 downTo 0) fullBits.add((b shr i) and 1)
        }

        // 8. Place bits in zig-zag pattern
        var bitIndex = 0
        var right = size - 1
        var goingUp = true

        while (right > 0) {
            if (right == 6) right-- // skip vertical timing column
            val rows = if (goingUp) (size - 1 downTo 0) else (0 until size)
            for (y in rows) {
                for (colOffset in 0..1) {
                    val x = right - colOffset
                    if (!reserved[y][x]) {
                        val bit = if (bitIndex < fullBits.size) fullBits[bitIndex++] else 0
                        // Mask pattern 0: (row + column) % 2 == 0
                        val mask = (y + x) % 2 == 0
                        grid[y][x] = if (mask) bit == 0 else bit == 1
                    }
                }
            }
            goingUp = !goingUp
            right -= 2
        }

        // 9. Format Information (Mask 0, Error Correction Level M: 00000 -> 101010000010010)
        val formatBits = intArrayOf(1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0)
        // Top-left
        val posTL_R = intArrayOf(8, 8, 8, 8, 8, 8, 7, 5, 4, 3, 2, 1, 0, 8, 8)
        val posTL_C = intArrayOf(0, 1, 2, 3, 4, 5, 8, 8, 8, 8, 8, 8, 8, 7, 8)
        for (i in 0 until 15) {
            grid[posTL_R[i]][posTL_C[i]] = formatBits[i] == 1
        }
        // Split around corners
        for (i in 0..7) {
            grid[size - 1 - i][8] = formatBits[i] == 1
        }
        for (i in 8..14) {
            grid[8][size - 15 + i] = formatBits[i] == 1
        }

        return grid
    }

    private fun drawFinderPattern(grid: Array<BooleanArray>, reserved: Array<BooleanArray>, startR: Int, startC: Int) {
        for (r in 0..6) {
            for (c in 0..6) {
                val isDark = (r == 0 || r == 6 || c == 0 || c == 6 || (r in 2..4 && c in 2..4))
                grid[startR + r][startC + c] = isDark
                reserved[startR + r][startC + c] = true
            }
        }
        // Add quiet border separator
        for (r in -1..7) {
            for (c in -1..7) {
                val gr = startR + r
                val gc = startC + c
                if (gr in grid.indices && gc in grid.indices) {
                    reserved[gr][gc] = true
                    if (r == -1 || r == 7 || c == -1 || c == 7) {
                        grid[gr][gc] = false
                    }
                }
            }
        }
    }

    private fun drawAlignmentPattern(grid: Array<BooleanArray>, reserved: Array<BooleanArray>, startR: Int, startC: Int) {
        for (r in 0..4) {
            for (c in 0..4) {
                val isDark = (r == 0 || r == 4 || c == 0 || c == 4 || (r == 2 && c == 2))
                grid[startR + r][startC + c] = isDark
                reserved[startR + r][startC + c] = true
            }
        }
    }

    private fun getDataCapacityBits(version: Int): Int = when (version) {
        1 -> 16 * 8   // Level M (16 bytes)
        2 -> 28 * 8   // 28 bytes
        3 -> 44 * 8   // 44 bytes
        4 -> 64 * 8   // 64 bytes
        5 -> 86 * 8   // 86 bytes
        6 -> 108 * 8  // 108 bytes
        7 -> 124 * 8  // 124 bytes
        else -> 154 * 8
    }

    private fun getEccCount(version: Int): Int = when (version) {
        1 -> 10
        2 -> 16
        3 -> 26
        4 -> 36
        5 -> 48
        6 -> 64
        7 -> 72
        else -> 88
    }

    private fun bitListToBytes(bits: List<Int>): IntArray {
        val bytes = IntArray(bits.size / 8)
        for (i in bytes.indices) {
            var b = 0
            for (j in 0..7) {
                b = (b shl 1) or bits[i * 8 + j]
            }
            bytes[i] = b
        }
        return bytes
    }

    // Standard GF(256) Reed-Solomon polynomial division for QR Code error correction
    private fun computeReedSolomonEcc(data: IntArray, eccCount: Int): IntArray {
        val exp = IntArray(512)
        val log = IntArray(256)
        var x = 1
        for (i in 0 until 255) {
            exp[i] = x
            exp[i + 255] = x
            log[x] = i
            x = (x shl 1)
            if (x >= 256) x = x xor 0x11D
        }

        fun gfMul(a: Int, b: Int): Int = if (a == 0 || b == 0) 0 else exp[log[a] + log[b]]

        // Build generator polynomial
        var gen = intArrayOf(1)
        for (i in 0 until eccCount) {
            val root = exp[i]
            val nextGen = IntArray(gen.size + 1)
            for (j in gen.indices) {
                nextGen[j] = nextGen[j] xor gfMul(gen[j], root)
                nextGen[j + 1] = nextGen[j + 1] xor gen[j]
            }
            gen = nextGen
        }

        val result = IntArray(eccCount)
        for (b in data) {
            val factor = b xor result[0]
            for (i in 0 until eccCount - 1) {
                result[i] = result[i + 1] xor gfMul(gen[eccCount - i - 1], factor)
            }
            result[eccCount - 1] = gfMul(gen[0], factor)
        }
        return result
    }
}
