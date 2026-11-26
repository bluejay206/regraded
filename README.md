# ReGraded

Independent Fabric mod for chunk-level terrain regrading from survey CSV data.

- Own point list (`SurveyPoint` / `PointList`) under `me.bluejay.regraded.core`
- PNEZD CSV import (compatible with LevelHeaded exports)
- Pure surface model (nearest-neighbor stub; TIN/averaged planned)
- White particle visualization toggled by **Ctrl-Z**
- Chunk-scoped 16³ bit-resolution modified layer (BitsAndChisels / C&B planned)
- No hard dependency on LevelHeaded

## Status

Scaffold + CSV import + white particle markers. Bit-surface applicator still TODO.

## Build

```bash
./gradlew :fabric:build
```
