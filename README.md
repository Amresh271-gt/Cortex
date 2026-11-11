# Cortex

Android app for tasks, memory and (soon) system assistance.
Package: `com.amresh.cortex` | Version: **1.0** (first release of the Cortex project)

## What is in 1.0
- **Three tabs** (tap or swipe): Tasks (left), Assistant (middle), Memory (right)
- **Tasks:** typed and voice entry, time parsing, alarms, edit, check off, 15-day recycle bin
- **Second reminder:** if a task is not marked done, one more reminder comes 30 minutes later
  (gap changeable in the menu: Second reminder, or turn it off)
- **Memory:** title, content, tag, pin, copy, search. Tap a memory to read it; Edit is a separate step
- **Quick Settings tile ("Cortex voice")** with voice routing:
  - "remind me ..." or "save as task" saves a task
  - "remember ...", "note ...", "memo ..." or "save as memory" saves a memory
  - anything else goes to the Assistant: "open WhatsApp" opens an app, "go home" / "close" goes to the home screen
  - if the Assistant cannot do it, Cortex says sorry (spoken replies can be switched off in the menu)
- **Themes, name and greeting, backup and restore**
- **Restore / import** reads Cortex backups and backups made by the old Task Reminder app

## Not yet
- Opening files by voice (planned)

## Build
GitHub Actions builds the debug APK on every push (see `.github/workflows/build.yml`).
Download it from the run's Artifacts (`Cortex-<branch>-debug-apk`).
The fixed `app/debug.keystore` lets each new APK install over the previous Cortex.

## Updating
For every new version raise `versionCode` by 1 and change `versionName` in `app/build.gradle`.

## Moving data from Task Reminder
1. In the old app: menu, Backup data (already done if you have a backup file).
2. Install Cortex (it installs next to the old app).
3. In Cortex: menu, Restore / import data, choose the backup file.
4. Check tasks and memories, then uninstall the old app.
