package com.youtubetest.player.mp4

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class Mp4BoxesTest {

    /** Tulis satu box: ukuran u32 + jenis + muatan nol. */
    private fun box(type: String, payloadSize: Int): ByteArray {
        val out = ByteArray(8 + payloadSize)
        val size = out.size
        out[0] = (size ushr 24).toByte()
        out[1] = (size ushr 16).toByte()
        out[2] = (size ushr 8).toByte()
        out[3] = size.toByte()
        type.toByteArray(Charsets.US_ASCII).copyInto(out, 4)
        return out
    }

    private fun largesizeBox(type: String, payloadSize: Int): ByteArray {
        val total = 16 + payloadSize
        val out = ByteArray(total)
        out[3] = 1 // size32 = 1 -> largesize mengikuti
        type.toByteArray(Charsets.US_ASCII).copyInto(out, 4)
        for (i in 0 until 8) {
            out[8 + i] = (total.toLong() ushr (56 - 8 * i)).toByte()
        }
        return out
    }

    @Test
    fun `daftar box sejajar terbaca dengan posisi dan ukuran yang benar`() {
        val data = box("ftyp", 16) + box("free", 4) + box("mdat", 100)
        val boxes = Mp4Boxes.readBoxes(data)
        assertEquals(listOf("ftyp", "free", "mdat"), boxes.map { it.type })
        assertEquals(0L, boxes[0].offset)
        assertEquals(24L, boxes[0].size)
        assertEquals(24L, boxes[1].offset)
        assertEquals(32L, boxes[2].offset)
        assertEquals(108L, boxes[2].size)
        assertEquals(100L, boxes[2].payloadSize)
        assertEquals(140L, boxes[2].endExclusive)
    }

    @Test
    fun `box largesize 64-bit terbaca`() {
        val data = box("ftyp", 8) + largesizeBox("mdat", 40)
        val boxes = Mp4Boxes.readBoxes(data)
        assertEquals(2, boxes.size)
        assertEquals(16, boxes[1].headerSize)
        assertEquals(56L, boxes[1].size)
        assertEquals(40L, boxes[1].payloadSize)
    }

    @Test
    fun `box berukuran nol membentang sampai akhir rentang`() {
        val head = box("ftyp", 8)
        val tail = ByteArray(8 + 50)
        // ukuran 0 eksplisit
        tail[3] = 0
        "mdat".toByteArray(Charsets.US_ASCII).copyInto(tail, 4)
        val boxes = Mp4Boxes.readBoxes(head + tail)
        assertEquals(58L, boxes[1].size)
        assertEquals(50L, boxes[1].payloadSize)
    }

    @Test
    fun `anak box di dalam wadah terbaca`() {
        val child = box("mvhd", 20)
        val moov = ByteArray(8 + child.size)
        moov[3] = moov.size.toByte()
        "moov".toByteArray(Charsets.US_ASCII).copyInto(moov, 4)
        child.copyInto(moov, 8)
        val top = Mp4Boxes.readBoxes(moov)
        val kids = Mp4Boxes.children(moov, top.single())
        assertEquals(listOf("mvhd"), kids.map { it.type })
        assertEquals(8L, kids[0].offset)
        assertEquals(20L, kids[0].payloadSize)
    }

    @Test
    fun `data terpotong ditolak, tidak ditebak`() {
        val data = box("ftyp", 16)
        assertThrows(IllegalArgumentException::class.java) {
            Mp4Boxes.readBoxes(data.copyOf(10))
        }
    }

    @Test
    fun `ukuran mengaku melebihi sisa rentang ditolak`() {
        val data = box("mdat", 10)
        data[3] = 99 // ukuran diklaim 99+, padahal berkas cuma 18 byte
        assertThrows(IllegalArgumentException::class.java) {
            Mp4Boxes.readBoxes(data)
        }
    }
}
