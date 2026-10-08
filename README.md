# Bin Night

A small installable web app that counts down to the next bin night, built from Midlothian Council's collection schedule. It runs entirely in the browser and works offline once installed.

Anyone in Midlothian can use it: on first open it asks for the next collection date of each bin and repeats them on the council's pattern (the `BINS` list in `index.html`). Dates are saved on the phone only. In the Android app, "Find my dates" opens the council's lookup page and reads the dates it shows.

## Android app

Every push to `main` builds `BinNight.apk` with GitHub Actions and attaches it to the **latest** release. The Android app (in `android/`) shows the same page from inside the APK, so it works offline.
