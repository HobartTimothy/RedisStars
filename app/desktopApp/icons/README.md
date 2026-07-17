# Application icons

RedisStars platform and window icons are produced from a **single canonical source**.
Do not hand-edit the binary derived files.

## Canonical source

| File | Role |
|------|------|
| `source/app-icon-source.png` | Only normative artwork (1024×1024, RGBA, transparent) |
| `icon.ico` | Windows installer / EXE (multi-size, generated) |
| `icon.png` | Linux installer (512×512, generated) |
| `icon.icns` | macOS installer (includes 1024 / Retina slots, generated) |
| `../src/main/resources/app-icon.png` | Compose Desktop window icon (256×256, generated) |
| `previews/icon-preview-sheet.png` | Manual size review only (not packaged) |

`src/main/resources/logo.png` is legacy brand artwork (non-square pixel art). Packaging and
the runtime window no longer use it as an icon source.

## Requirements

- Python **3.10+**
- Pillow and icnsutil (see `requirements.txt`)

```bash
python -m pip install -r requirements.txt
```

## Generate

From any working directory:

```bash
python app/desktopApp/icons/generate_icons.py
```

Optional: rewrite the canonical brand source, then regenerate everything:

```bash
python app/desktopApp/icons/generate_icons.py --write-source
```

Custom paths:

```bash
python app/desktopApp/icons/generate_icons.py --source path/to/source.png --output-dir path/to/icons
```

## Verify

```bash
python app/desktopApp/icons/generate_icons.py --check
```

Or via Gradle (does **not** run on every Kotlin compile; requires Python on PATH):

```bash
./gradlew :app:desktopApp:verifyApplicationIcons
```

## Tests

```bash
python -m unittest app/desktopApp/icons/test_generate_icons.py
```

## Rules

1. Edit only `source/app-icon-source.png` (or regenerate it with `--write-source`).
2. Never hand-edit `icon.ico`, `icon.png`, `icon.icns`, or `app-icon.png`.
3. After an icon change, rebuild native installers **on each target OS** (Compose Desktop does not cross-compile).
4. Keep `previews/` for review; do not commit temporary `icon_*.png` intermediates.
