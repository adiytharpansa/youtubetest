# Rancangan Fase Test: InnerTube + SABR + Pemutar Buatan Sendiri

## Tujuan

Membuktikan satu tumpukan putar yang seluruh lapis pengaturnya
buatan sendiri, ditulis dari nol di repo ini:

- **Mesin data** (`:mesin`): klien InnerTube + klien SABR yang
  ditulis baru di sini. Keputusan pemilik (2026-11-14): **tanpa
  Lontar** — tidak menyalin mesin Bunny/LontarEngine.
- **Pemutar inti** (`:player`): pengganti peran ExoPlayer, ditulis
  dari nol.

Repo ini TERPISAH dari Bunny dan tidak menyentuh Bunny sama sekali.
Bunny tetap memakai mesin dan pemutar lamanya sampai fase test
ini lulus.

## Yang buatan sendiri dan yang bukan

| Lapis | Siapa |
| --- | --- |
| Pengambilan data (InnerTube, SABR) | Kita (`:mesin`, dari nol) |
| Pemilah berkas (demuxer MP4/WebM) | Kita (`:player`) |
| Penjadwal contoh & sinkronisasi suara–gambar | Kita (`:player`) |
| Penggambar ke layar & pengeras suara | Kita, lewat Surface/AudioTrack |
| Penerjemah codec (H.264/VP9/AV1 → gambar) | Chip HP lewat MediaCodec |

Baris terakhir memang bukan buatan siapa pun: ExoPlayer, VLC, dan
semua pemutar lain juga memakai codec chip. Menulis codec sendiri
dari nol adalah proyek bertahun-tahun dan hasilnya di HP lebih
buruk; itu di luar rancangan ini, secara sadar.

## Aturan modul

- `:player` DILARANG bergantung pada Media3/ExoPlayer dalam bentuk
  apa pun. Hanya Android SDK (MediaCodec, MediaFormat, AudioTrack,
  Surface) dan Kotlin murni.
- `:mesin` hanya boleh memakai pustaka platform (OkHttp,
  coroutines, org.json). Tidak ada kode YouTube pihak ketiga.
- Setiap parser diuji dari tangkapan jawaban NYATA atau data
  sintetis yang bentuknya meniru data nyata persis — bukan fixture
  karangan yang disesuaikan dengan kodenya (pelajaran dari proyek
  Bunny: fixture karangan pernah menutupi bug parser Shorts).

## Fase dan gerbang

### Fase 0 — Rangka (sedang berjalan)
Repo, modul `:mesin` + `:player` + `:app`, CI hijau. Komponen
pertama `:player`: pembaca struktur box MP4 (`Mp4Boxes`) + tes
unitnya. `:mesin` masih kosong.

### Fase 1 — Demuxer MP4 (`:player`)
Baca `moov` → daftar trek (video/suara) + tabel contoh (ukuran,
posisi, cap waktu dari `stbl`). Gerbang: tes unit atas berkas
fMP4 NYATA (potongan aliran video yang ditangkap) — jumlah
contoh, durasi, dan cap waktu cocok dengan kenyataan berkasnya.

### Fase 2 — Putar berkas lokal (`:player`)
Satu berkas MP4 lokal diputar tuntas di SurfaceView: video lewat
MediaCodec, suara lewat AudioTrack, sinkronisasi oleh jam kita
sendiri. Gerbang: video uji ≥ 30 detik selesai; selisih
suara–gambar terukur dari catatan cap waktu < 100 ms; jeda/lanjut
bekerja.

### Fase 3 — Mesin InnerTube minimal (`:mesin`)
Bootstrap sesi dari beranda youtube.com (konfigurasi + konteks
klien), lalu endpoint player untuk satu video: pilah format
klasiknya (URL langsung). Gerbang: tes dari tangkapan nyata +
aplikasi uji berhasil menampilkan daftar format satu video
nyata.

### Fase 4 — Putar dari jaringan klasik (gabungan)
Aliran klasik dari Fase 3 (URL langsung, unduh per Range) mengalir
ke pemutar Fase 2. Gerbang: satu video YouTube nyata diputar
tuntas dari awal sampai akhir di aplikasi uji.

### Fase 5 — Klien SABR (`:mesin`) + putar dari SABR
Buka sesi putar SABR, baca potongan media dari server, teruskan
ke demuxer `:player`. Format SABR menentukan demuxer yang
dibutuhkan (fMP4 atau WebM — demuxer WebM adalah pekerjaan
tersendiri bila ternyata perlu). Gerbang: satu video YouTube
nyata diputar tuntas lewat SABR dengan pemutar buatan sendiri.

### Sesudah Fase 5
Baru diputuskan — oleh pemilik repo, bukan otomatis — apakah
tumpukan ini masuk ke Bunny menggantikan Lontar + ExoPlayer.

## Cara verifikasi

- Lokal tidak bisa build (daemon Gradle butuh TCP mentah yang
  terblokir di mesin ini) → CI GitHub Actions adalah wasitnya:
  lint + tes unit + rakit APK debug.
- Bukti perangkat lewat Firebase Test Lab dan/atau HP uji,
  sama seperti pola Bunny.
