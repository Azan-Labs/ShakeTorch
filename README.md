# Shake Torch (Android)

Phone ko jaldi se **2 baar shake** karein → flashlight ON/OFF (Motorola "chop chop" ki tarah).
- Screen band ho tab bhi kaam karti hai
- Phone restart / power-off ke baad **khud start** hoti hai – app dobara kholne ki zaroorat nahi
- Sensitivity adjust kar sakte hain (1–10)
- Android 8.0 (API 26) se Android 14+ tak

## APK kaise banayein

### Option A – GitHub (PC par kuch install nahi karna)
1. github.com par naya repository banayein aur is folder ki saari files upload karein (`.github` folder samait).
2. **Actions** tab → "Build APK" → khud chal jayega (ya "Run workflow" dabayein).
3. ~3-5 minute baad run kholein → neeche **Artifacts** → `ShakeTorch-apk` download karein → unzip → `app-debug.apk`.

### Option B – Android Studio
1. Android Studio → **Open** → `ShakeTorch` folder.
2. Gradle sync hone dein → **Build > Build APK(s)**.
3. APK: `app/build/outputs/apk/debug/app-debug.apk`

## Phone par install ke baad (zaroori)
1. APK install karein ("Unknown sources" allow karna padega).
2. App ek dafa kholein → notification permission **Allow**.
3. **"Battery optimization band karein"** dabayein → Allow.
4. Xiaomi / Oppo / Vivo / Realme / Infinix / Tecno: App settings mein **Autostart ON** aur Battery = **No restrictions** karein. Warna yeh companies restart ke baad app ko rok deti hain.
5. Ek chhoti si notification "Shake Torch active" hamesha rahegi – yeh Android ka rule hai background mein chalne ke liye.
