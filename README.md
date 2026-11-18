# DecontaPatches

Custom Morphe patches by **Deconta**.

## Patches

### Rossmann (`de.rossmann.app.android`)

* **Auto claim coupons**:
  Aktiviert automatisch alle verfügbaren Coupons beim Laden der Coupon-Liste.
  * Verwendet die native Aktivierungs-Logik der App (kein separater API-Client notwendig).
  * Filtert bereits aktive, exklusive oder eingelöste Coupons automatisch heraus.
  * Sequentielle Aktivierung mit 350 ms Verzögerung, um Rate-Limits zu verhindern.
  * Zeigt nach Abschluss eine kurze Bestätigungs-Meldung (Toast).

## Bauen

```bash
./gradlew build
```

Die fertigen Artefakte befinden sich anschließend in:
* `patches/build/libs/patches-*.mpp` (Morphe Patch Bundle)
* `extensions/extension/build/outputs/aar/extension-*.mpe` (Extension)

## Lizenz

GPLv3
