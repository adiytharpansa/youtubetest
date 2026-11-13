package com.youtubetest.player.mp4

/**
 * Satu box ISO BMFF (MP4 / fMP4) di dalam aliran byte.
 *
 * Setiap berkas MP4 — termasuk potongan DASH klasik dan segmen
 * fMP4 dari SABR — tersusun dari box-box ini: 4 byte ukuran,
 * 4 byte jenis ("ftyp", "moov", "mdat", ...), lalu isinya.
 * Membaca struktur box adalah anak tangga pertama demuxer
 * buatan sendiri: sebelum contoh video/suara bisa dipilah,
 * rangkanya harus bisa dipetakan dulu.
 */
data class Mp4Box(
    /** Jenis box, 4 karakter, mis. "moov". */
    val type: String,
    /** Posisi awal box (termasuk header-nya) di dalam data. */
    val offset: Long,
    /** Panjang total box termasuk header. */
    val size: Long,
    /** Panjang header: 8, atau 16 bila memakai largesize 64-bit. */
    val headerSize: Int,
) {
    val payloadOffset: Long get() = offset + headerSize
    val payloadSize: Long get() = size - headerSize
    val endExclusive: Long get() = offset + size
}

/**
 * Pembaca struktur box dari potongan byte yang sudah terambil.
 * Ketat: data terpotong atau ukuran tak masuk akal = galat,
 * tidak pernah ditebak.
 */
object Mp4Boxes {

    /**
     * Baca daftar box sejajar pada rentang [start, end) dari [data].
     * Dipakai untuk tingkat teratas berkas; untuk isi box wadah
     * (moov, trak, ...) panggil [children].
     */
    fun readBoxes(data: ByteArray, start: Int = 0, end: Int = data.size): List<Mp4Box> {
        require(start >= 0 && end <= data.size && start <= end) {
            "Rentang baca di luar data: [$start, $end) dari ${data.size} byte"
        }
        val boxes = mutableListOf<Mp4Box>()
        var pos = start
        while (pos < end) {
            val remaining = end - pos
            require(remaining >= 8) {
                "Sisa $remaining byte di posisi $pos tidak cukup untuk header box"
            }
            val size32 = readU32(data, pos)
            val type = readType(data, pos + 4)
            var headerSize = 8
            val size: Long = when (size32) {
                1L -> {
                    require(remaining >= 16) {
                        "Box $type di $pos memakai largesize tetapi header-nya terpotong"
                    }
                    headerSize = 16
                    readU64(data, pos + 8)
                }
                // Ukuran 0 = box membentang sampai akhir rentang
                // (hanya sah untuk box terakhir, lazimnya mdat).
                0L -> remaining.toLong()
                else -> size32
            }
            if (type == "uuid") headerSize += 16
            require(size >= headerSize) {
                "Ukuran box $type di $pos ($size) lebih kecil dari header-nya ($headerSize)"
            }
            require(size <= remaining) {
                "Box $type di $pos mengaku $size byte, sisa rentang hanya $remaining"
            }
            boxes += Mp4Box(
                type = type,
                offset = pos.toLong(),
                size = size,
                headerSize = headerSize,
            )
            pos += size.toInt()
        }
        return boxes
    }

    /** Baca box-box anak di dalam muatan box wadah [container]. */
    fun children(data: ByteArray, container: Mp4Box): List<Mp4Box> =
        readBoxes(
            data,
            start = container.payloadOffset.toInt(),
            end = container.endExclusive.toInt(),
        )

    private fun readU32(data: ByteArray, at: Int): Long =
        ((data[at].toLong() and 0xFF) shl 24) or
            ((data[at + 1].toLong() and 0xFF) shl 16) or
            ((data[at + 2].toLong() and 0xFF) shl 8) or
            (data[at + 3].toLong() and 0xFF)

    private fun readU64(data: ByteArray, at: Int): Long =
        (readU32(data, at) shl 32) or readU32(data, at + 4)

    private fun readType(data: ByteArray, at: Int): String =
        String(data, at, 4, Charsets.US_ASCII)
}
