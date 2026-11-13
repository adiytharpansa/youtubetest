# Modul `:mesin` — ditulis dari nol di repo ini

Keputusan pemilik repo (2026-11-14): **tanpa Lontar**. Modul ini
tidak menyalin mesin Bunny/LontarEngine; klien InnerTube dan
klien SABR-nya ditulis baru di sini, sebagai bagian dari fase
test. Modul ini sengaja masih kosong — isinya tiba per fase
sesuai RANCANGAN.md:

1. Bootstrap sesi InnerTube (ambil konfigurasi dari beranda
   youtube.com, bentuk konteks klien).
2. Endpoint player: minta data putar satu video, pilah formatnya.
3. Klien SABR minimal: buka sesi putar SABR, baca potongan media
   yang dikirim server, teruskan ke `:player`.

Aturan yang sama dengan `:player`: hanya pustaka platform
(OkHttp, coroutines, org.json), tidak ada kode YouTube pihak
ketiga, dan setiap parser diuji dari tangkapan jawaban NYATA —
bukan fixture karangan.
