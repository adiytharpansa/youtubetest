package com.youtubetest.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.SurfaceView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.youtubetest.player.mp4.Mp4Boxes

/**
 * Aplikasi uji fase test pemutar buatan sendiri.
 *
 * Layar ini sengaja polos: satu permukaan hitam (tempat pemutar
 * buatan sendiri akan menggambar nanti), satu tombol uji komponen
 * `:player` yang sudah ada (pembaca struktur box MP4), dan catatan
 * hasilnya. Mesin InnerTube/SABR ditulis dari nol di `:mesin`
 * (tanpa Lontar) dan belum punya apa pun untuk diuji di sini.
 */
class MainActivity : Activity() {

    private lateinit var logView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        root.addView(TextView(this).apply {
            text = "Youtubetest — fase test InnerTube + SABR + pemutar buatan sendiri"
            textSize = 18f
        })

        // Permukaan gambar untuk pemutar buatan sendiri (:player).
        // Fase ini masih kosong; demuxer-nya sedang dibangun.
        root.addView(
            SurfaceView(this).apply { setBackgroundColor(Color.BLACK) },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                600,
            ).apply { topMargin = 24; bottomMargin = 8 },
        )
        root.addView(TextView(this).apply {
            text = "Permukaan pemutar buatan sendiri (belum memutar apa pun)"
            textSize = 12f
        })

        val boxButton = Button(this).apply {
            text = "Uji :player — baca struktur box MP4"
        }
        root.addView(boxButton)

        logView = TextView(this).apply {
            textSize = 13f
            movementMethod = ScrollingMovementMethod()
        }
        root.addView(
            logView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        setContentView(root)

        log("Siap. :mesin masih kosong (ditulis dari nol, tanpa Lontar).")
        boxButton.setOnClickListener { runBoxCheck() }
    }

    /**
     * Rangka MP4 contoh yang sah secara struktur (ftyp, moov berisi
     * mvhd, mdat) disusun byte demi byte, lalu dibaca oleh
     * [Mp4Boxes] — komponen pertama `:player` — dan hasilnya
     * dicetak apa adanya.
     */
    private fun runBoxCheck() {
        val data = box("ftyp", 16) +
            box("moov", box("mvhd", 20).size) +
            box("mdat", 64)
        // box() di atas menulis muatan nol; moov butuh anak aslinya:
        val mvhd = box("mvhd", 20)
        mvhd.copyInto(data, 24 + 8)
        try {
            val boxes = Mp4Boxes.readBoxes(data)
            log("Struktur terbaca: ${boxes.size} box")
            boxes.forEach { b ->
                log("• ${b.type} @${b.offset} ukuran ${b.size}")
                if (b.type == "moov") {
                    Mp4Boxes.children(data, b).forEach { child ->
                        log("   └ ${child.type} @${child.offset} ukuran ${child.size}")
                    }
                }
            }
        } catch (e: Exception) {
            log("Galat: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Tulis satu box: ukuran u32 + jenis 4 huruf + muatan nol. */
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

    private fun log(line: String) {
        logView.append(line + "\n")
    }
}
