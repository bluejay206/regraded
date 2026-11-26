# ReGraded

Companion mod to LevelHeaded. Reads survey CSV (PNEZD) and regrades terrain surfaces as a 16³ bit-resolution modified layer on affected chunks (via BitsAndChisels).

## Architecture

- `survey-io` (from levelheaded-fabric) — pure-Java point store + PNEZD CSV.
- `core` — SurfaceModel (TIN or averaged height grid).
- Fabric layer — ChunkSurfaceLayer + BitApplicator that writes into BitsAndChisels blocks.

## Status

Scaffold only. No full TIN yet. Designed to stay independent of LevelHeaded (soft/optional dependency for live PointStore).

## Build

Requires BitsAndChisels. See build.gradle for the target version.
