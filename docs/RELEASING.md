# Release handover

This repository is configured so that release signing is performed only with a
maintainer's local key. No private key, password or signing configuration is
kept in Git.

## Direct release

1. Copy `signing.properties.example` to `keystore.properties` without adding it
   to Git, and replace every example value with the local signing information.
2. Run `./gradlew :app:clean :app:assembleRelease`.
3. Verify the resulting signed APK before distribution:

   ```sh
   apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
   ```

4. Calculate and publish a SHA-256 checksum next to the APK, and attach both to
   the matching public GitHub and GitLab release tag.

If `keystore.properties` is absent or incomplete, Gradle intentionally produces
an unsigned release APK for local/F-Droid-style build checks. That APK must not
be distributed to end users.

## F-Droid

F-Droid does not accept the direct APK as its release artifact. It checks out
the public source at the exact commit listed in its own `fdroiddata` metadata,
builds it, and signs the resulting APK with F-Droid's key. The draft at
`docs/fdroiddata-org.jpi59.ethictuner.yml` is prepared for copying to
`fdroiddata/metadata/org.jpi59.ethictuner.yml`; validate it with the F-Droid
tooling and its GitLab CI before opening a merge request.

The upstream GitHub and GitLab source releases must remain public and tagged.
Do not replace a published tag: publish a newer version code for every future
release.
