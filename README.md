# Lotus

Lotus is an Android-first private messaging project. The repository currently contains the initial visual foundation and an incremental security roadmap.

**Status:** early prototype. The current UI is a demonstration only; it does not yet send messages or implement end-to-end encryption, Tor, offline mesh, or secure deletion. Do not use it for sensitive communications.

## Development

Open the repository root in Android Studio with JDK 17, sync Gradle, then run the `app` configuration on an Android emulator or device (API 26+).

## Current branch

The initial implementation is being developed on `lotus-ui-foundation` so that `main` remains unchanged until the project has been built, reviewed, and tested.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the roadmap, security gates, and limitations.