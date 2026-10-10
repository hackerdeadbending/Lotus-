# Lotus Android foundation

Lotus is being built as a native Android app. This branch is the first visual foundation, intentionally separate from `main`.

## Current status

- Native Kotlin + Jetpack Compose app scaffold.
- Lotus dark visual system with botanical green accent.
- Initial Chats, Explore, Privacy and Settings navigation.
- Demonstration conversation previews only. They are not real messages.
- No network access, messaging, encryption, Tor routing or secure deletion is implemented yet. The UI must not be represented as a secure messenger until those systems exist and are independently tested.

## Open in Android Studio

1. Open the repository root in a current stable Android Studio.
2. Use JDK 17 for Gradle.
3. Allow Gradle sync to finish.
4. Run the `app` configuration on an Android device or emulator (API 26+).

## Planned architecture

- `:app`: app shell, navigation and feature presentation.
- `core/designsystem`: shared typography, colors and UI components.
- `core/identity`: identity and key lifecycle contracts.
- `core/crypto`: integration layer around audited protocol libraries; no custom cryptographic primitives.
- `core/storage`: encrypted persistence and attachment-key lifecycle.
- `core/transport`: transport interfaces and bounded retry queue.
- `feature/chats`, `feature/contacts`, `feature/privacy`, `feature/browser`, `feature/settings`: isolated features.

Modules should be introduced when implementation requires them; avoid empty modules and speculative dependencies.

## Security implementation gates

1. Threat model and dependency review before selecting cryptographic libraries.
2. Identity and key verification, then local encrypted storage.
3. 1:1 end-to-end messaging using a maintained protocol implementation.
4. Relay transport with minimal metadata, then Tor integration and leak tests.
5. Offline peer-to-peer transport with explicit device-discovery warnings.
6. Browser isolation and forced proxy enforcement.
7. Deletion tests against app databases, WAL files, caches, thumbnails, backups and temporary files.
8. Independent security review before any claim of production-grade protection.

Android flash storage cannot reliably promise physical overwrite of a particular file. Design for cryptographic erasure and minimize plaintext lifetime instead. A Tor connection alone does not eliminate traffic correlation or endpoint risks.

## Branch policy

Work on `lotus-ui-foundation` first. Run Gradle build and tests in Android Studio or CI, review the diff, then open a pull request into `main`. Do not merge unbuilt code or claim tests passed without a recorded successful run.
