# FMI Workbench — Kotlin Multiplatform FMU Simulator

A full-stack app, written **entirely in Kotlin**, for loading, inspecting, and simulating [FMU](https://fmi-standard.org/) (Functional Mock-up Unit) models compliant with **FMI 2.0**.

Built with **Kotlin Multiplatform** and **Kotlin/Native `cinterop`** to bind directly to the native [FMILib](https://github.com/modelon-community/fmi-library) C library — no glue code in another language, anywhere in the stack.

---

## Features

- Upload an `.fmu` file and inspect its metadata (name, author, variables, default experiment params)
- Configure and run a Co-Simulation experiment, selecting which variables to record
- Visualize results as line charts
- Runs as a native executable on **Linux x64**, **macOS ARM64**, and **Windows x64**
- On macOS ARM64, FMUs shipped only with `x86_64` binaries are automatically recompiled from source into a universal binary

---

## Architecture

```
frontend  ──REST──▶  backend  ──▶  fmu-kt  ──▶  fmilib
(Compose UI)         (Ktor API)    (domain)     (native C lib)
```

| Module     | Role |
|------------|------|
| `fmilib`   | Builds the native FMILib C library via CMake |
| `fmu-kt`   | Domain logic — FMU lifecycle, metadata, simulation, native bindings |
| `backend`  | Ktor REST API exposing `fmu-kt` to the frontend |
| `frontend` | Compose Multiplatform UI, talks to the backend only via REST |

`fmu-kt` and `backend` are the only modules touching native code; `frontend` is pure Kotlin/UI. On macOS, an FMU recompilation step (Clang + `lipo`, isolated in its own source set) runs transparently before loading when needed.

---

## REST API

| Method | Endpoint        | Description                          |
|--------|-----------------|---------------------------------------|
| GET    | `/health`       | Liveness check                       |
| POST   | `/fmi/upload`   | Upload an `.fmu` file                |
| POST   | `/fmi/init`     | Initialize the uploaded FMU          |
| GET    | `/fmi/info`     | Get model metadata & variables       |
| POST   | `/fmi/simulate` | Run a simulation                     |
### POST /fmi/simulate
```json
{
  "startTime": 0.0,
  "stopTime": 10.0,
  "stepSize": 0.01,
  "outputVariables": ["h", "v"]
}
```

---

## Tech stack / Libraries used

Kotlin Multiplatform / Kotlin·Native · [Ktor](https://ktor.io/) · [Compose Multiplatform](https://kotlinlang.org/compose-multiplatform/) · [Voyager](https://voyager.adriel.cafe/) · [KoalaPlot](https://github.com/KoalaPlot/koalaplot-core) · [FileKit](https://github.com/vinceglb/FileKit) · [Ksoup](https://github.com/fleeksoft/ksoup) · CMake/[gradle-cmake](https://github.com/TomTzook/gradle-cmake) · [Kotest](https://kotest.io/) · GitHub Actions

---

## Getting started

```bash
git clone --recurse-submodules <repo-url>

./gradlew build

# run the backend (REST API on :8080)
./backend/build/bin/<target>/releaseExecutable/backend.kexe   # Linux/macOS
./backend/build/bin/mingwX64/releaseExecutable/backend.exe    # Windows

# run the frontend dev server
./gradlew :frontend:wasmJsBrowserDevelopmentRun
```

`<target>` is `linuxX64` or `macosArm64` depending on your host OS.

---

## Testing

Unit tests run against fakes (`FakeFmuService`, `FakeResourceManager`); integration tests exercise real `.fmu` files form the [Dymola FMI Compatibility](https://github.com/CATIA-Systems/dymola-fmi-compatibility.git) repository, via a dynamic Kotest suite. CI builds and health checks the backend on all three target platforms on every push.

---

## Limitations & roadmap

- Only **Co-Simulation FMI 2.0** is supported (FMI 1.0/3.0 planned — `FmuService` is already version-agnostic)
- No CSV/JSON export of results yet
- One FMU loaded at a time, no multimodel co-simulation yet
- The macOS recompilation pipeline synthesizes FMI headers on the fly rather than bundling the official ones

---
