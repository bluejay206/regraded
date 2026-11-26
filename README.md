# ReGraded

Independent Fabric mod for chunk-level terrain regrading from survey CSV.

**Active target:** mc1211 (1.21.1, Yarn)
**Paused:** mc262 / mc263 — waiting on Chisels & Bits for those versions

## Chisels & Bits
Soft dependency (`suggests: chiselsandbits`). Latest available for 1.21.1 is 21.1.40.
At launch the mod checks `FabricLoader.isModLoaded("chiselsandbits")` and installs the applicator if present.

## Build
```bat
prepareBuild.bat -1211
```
