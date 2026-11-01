# TRADA 2027 — Aplikasi Android (APK) — Model HIBRIDA

Mengikuti pola aplikasi **JERUK** Anda (native Kotlin/AGP + AndroidX +
aset luring yang dibundel). Aplikasi terdiri dari DUA layar:

1. **DashboardActivity (peluncur) — DASBOR LURING/OFFLINE.**
   Memuat `assets/dashboard.html` secara lokal (tanpa internet) dan membaca
   `assets/snapshot.json` (data nyata: 510 wilayah status 112 + ringkasan
   eksekutif, per 2026-11-01) lewat jembatan `Android.getSnapshot()`.
   Diperbarui dengan mengganti `snapshot.json` lalu build ulang (seperti JERUK).

2. **WebActivity — APLIKASI OPERASIONAL LIVE.**
   WebView ke Web App `/exec` (login password lokal, transaksi, tulis ke
   Sheets, unggah berkas `<input type=file>`, unduhan). Dibuka dari tombol
   "Buka Aplikasi Operasional" di dasbor. Perlu internet.

URL live ada di `app/src/main/res/values/strings.xml` → `start_url`.
Snapshot luring ada di `app/src/main/assets/snapshot.json`.
UI dasbor luring ada di `app/src/main/assets/dashboard.html`.

---

## Catatan penting
Lingkungan tempat project ini dibuat **tidak bisa meng-compile APK** (server
Android Google diblokir). Kompilasi dilakukan di salah satu dari dua cara di
bawah — keduanya menghasilkan file `.apk` yang sama.

---

## Cara A — Build otomatis via GitHub Actions (TANPA install Android Studio)
Paling praktis bila Anda ingin cepat dapat file `.apk`.

1. Buat repository baru di GitHub (boleh privat).
2. Unggah seluruh isi folder `TradaApp/` ini ke repo (termasuk folder
   tersembunyi `.github/`).
3. Buka tab **Actions** → workflow **"Build TRADA APK"** akan jalan otomatis
   setelah push (atau klik **Run workflow**).
4. Setelah selesai (±3–5 menit), buka run tersebut → bagian **Artifacts** →
   unduh **`trada-2027-debug-apk`**. Di dalamnya ada `app-debug.apk`.
5. Kirim `.apk` ke perangkat Android, aktifkan "Instal aplikasi tak dikenal",
   lalu pasang.

> Workflow sudah disiapkan di `.github/workflows/build-apk.yml`. Tidak perlu
> mengubah apa pun.

---

## Cara B — Build di Android Studio (lokal)
1. Install **Android Studio** (gratis).
2. **File → Open** → pilih folder `TradaApp/`. Tunggu Gradle sync
   (Android Studio otomatis mengunduh Gradle wrapper, SDK, dan dependensi).
3. Menu **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. APK ada di `app/build/outputs/apk/debug/app-debug.apk`
   (klik "locate" pada notifikasi build).

---

## Menjadikan APK siap rilis (opsional, untuk distribusi resmi)
APK debug cukup untuk instalasi internal. Untuk versi rilis bertanda tangan:
1. Di Android Studio: **Build → Generate Signed Bundle / APK** → buat keystore.
2. Atau tambahkan `signingConfigs` pada `app/build.gradle` dan ubah
   `assembleDebug` → `assembleRelease` di workflow.
Simpan keystore baik-baik; dibutuhkan untuk setiap pembaruan aplikasi.

---

## Mengganti ikon / nama aplikasi
- Nama: `app/src/main/res/values/strings.xml` → `app_name`.
- Ikon: saat ini ikon adaptif vektor (navy + aksen merah, huruf "T").
  Untuk logo resmi TRADA, ganti lewat Android Studio:
  **res → New → Image Asset**, pilih berkas logo resmi (jangan rekonstruksi
  dari deskripsi). Ikon adaptif butuh minSdk 26 (sudah diset).

## Spesifikasi teknis
- minSdk 26 (Android 8.0) • targetSdk 34 • Java 17
- Android Gradle Plugin 8.5.2 • Gradle 8.7
- Dependensi: AndroidX AppCompat, SwipeRefreshLayout
- Izin: INTERNET, ACCESS_NETWORK_STATE
- Package: `id.co.trada.app`
