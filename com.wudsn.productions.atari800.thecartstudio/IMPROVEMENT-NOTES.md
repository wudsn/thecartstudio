# Improvement notes (2026-09-15)

Findings from a review of the project structure, build files, and source layout. Not committed to git — local notes for later follow-up.

## 1. Build system is mid-migration and inconsistent
- The Maven conversion (see commit `dac2283`) still gets invoked from `TheCartStudio-Makefile.ant`, which shells out to `mvn -q compile` / `mvn -q package` via `cmd`. Two build systems, one calling the other.
- `.classpath` still references Eclipse project-linked sources (`/com.wudsn.tools.base`, `/com.wudsn.tools.base.atari`, `/com.wudsn.tools.base.atari.cartridge`) and a `bin` output folder, while `pom.xml` now resolves those same dependencies as Maven artifacts from the local `.m2` repo. These two resolution mechanisms can drift out of sync (Eclipse using workspace sources vs. Maven using whatever's installed locally).
- The Ant script hardcodes absolute Windows paths (`C:\jac\system\...`, `C:\jac\bin\wbin\date.exe`), so the build can't run anywhere but this machine.

## 2. No automated tests
- `tst/` has `.atr` fixture files and expected output, but no JUnit tests wired into Maven — verification is manual (`-createSampleFiles`, `-exportToCarImage`, eyeball the result).
- `Workbook.java` (1470 lines) and `TheCartStudio.java` (1336 lines) carry the core cartridge-building logic; a handful of JUnit tests around export/import correctness would catch regressions faster than manual runs.

## 3. No CI
- No GitHub Actions or other pipeline. A basic workflow (`mvn -B package`) would catch build breaks on every push now that Maven builds the shaded jar.

## 4. Large monolithic classes
- `TheCartStudio.java` and `Workbook.java` each mix multiple concerns (app entry point + CLI arg handling; workbook domain logic). Not urgent, but worth splitting if touched again soon.

## 5. No README
- No top-level README explaining what The!Cart Studio is, how to build it (Maven vs. the Ant wrapper), or that it depends on sibling `com.wudsn.tools.*` artifacts being installed locally first.

## Possible next steps
- Add a minimal JUnit test around one of the `tst/atr` fixtures.
- Write a README covering build steps and the sibling-project dependency.
- Decide whether Ant or Maven is the "real" build going forward and simplify accordingly.
