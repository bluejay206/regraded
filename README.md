# ReGraded

Independent Fabric mod for chunk-level terrain regrading from survey CSV.

**Clean start** — multi-version layout matching LevelHeaded versions:
- `core` — pure Java (PointList / SurveyPoint / SurfaceModel)
- `mc1211` — 1.21.1 Yarn (loom-remap)
- `mc262` — 26.2 official mappings
- `mc263` — 26.3 official mappings

## Versions
| Target | Loader | Fabric API | Loom |
|--------|--------|------------|------|
| mc1211 | 0.18.6 | 0.116.11+1.21.1 | 1.15-SNAPSHOT (loom-remap) |
| mc262 | 0.19.3 | 0.156.0+26.2 | 1.17-SNAPSHOT |
| mc263 | 0.19.5 | 0.162.0+26.3 | 1.18-SNAPSHOT |

Gradle wrapper: **9.7.0**

## Build
```bat
prepareBuild.bat -1211
prepareBuild.bat -262
prepareBuild.bat -263
prepareBuild.bat -all
```
