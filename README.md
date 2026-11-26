# ReGraded

Independent Fabric mod for chunk-level terrain regrading from survey CSV data.

Multi-version layout (mirrors LevelHeaded):
- `core` — pure Java surface model + independent PointList
- `mc1211` — 1.21.1 Fabric target
- `mc262` — 26.2 Fabric target
- `mc263` — 26.3 Fabric target

## Features (current)
- Independent `SurveyPoint` / `PointList` with PNEZD CSV import
- White particle visualization toggled by Ctrl-Z
- Chunk-scoped surface layer interface (bit applicator still TODO)

## Build

```bat
prepareBuild.bat              :: default mc1211
prepareBuild.bat -1211
prepareBuild.bat -262
prepareBuild.bat -263
prepareBuild.bat -all
prepareBuild.bat -norun
prepareBuild.bat -clear
```

Or directly:

```bash
./gradlew :mc1211:build
./gradlew :mc262:build
./gradlew :mc263:build
```
