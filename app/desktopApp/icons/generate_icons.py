#!/usr/bin/env python3
"""
Generate RedisStars application icons from a single 1024×1024 RGBA source.

Usage (from any working directory):

    python generate_icons.py
    python generate_icons.py --check
    python generate_icons.py --source path/to/source.png
    python generate_icons.py --output-dir path/to/icons

Install dependencies first:

    python -m pip install -r requirements.txt
"""

from __future__ import annotations

import argparse
import math
import shutil
import struct
import sys
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence

from PIL import Image, ImageDraw, ImageEnhance, ImageFilter

# ---------------------------------------------------------------------------
# Paths (always relative to this script — never absolute machine paths)
# ---------------------------------------------------------------------------

SCRIPT_DIR = Path(__file__).resolve().parent
DEFAULT_SOURCE = SCRIPT_DIR / "source" / "app-icon-source.png"
DEFAULT_OUTPUT_DIR = SCRIPT_DIR
RUNTIME_RESOURCE = (
    SCRIPT_DIR.parent / "src" / "main" / "resources" / "app-icon.png"
)
PREVIEW_DIR_NAME = "previews"
PREVIEW_SHEET_NAME = "icon-preview-sheet.png"

SOURCE_MIN_SIDE = 1024
SAFE_MARGIN_RATIO_MIN = 0.08
SAFE_MARGIN_RATIO_MAX = 0.18
SAFE_MARGIN_ASYMMETRY_MAX = 0.04

ICO_SIZES: tuple[int, ...] = (16, 20, 24, 32, 40, 48, 64, 128, 256)
LINUX_PNG_SIZE = 512
RUNTIME_PNG_SIZE = 256
PREVIEW_SIZES: tuple[int, ...] = (1024, 256, 128, 64, 48, 32, 24, 20, 16)

# macOS ICNS media keys → pixel size written into that slot.
# Includes Retina (@2x) representations where the type stores a larger bitmap.
ICNS_ENTRIES: tuple[tuple[str, int], ...] = (
    ("icp4", 16),  # 16×16
    ("icp5", 32),  # 32×32
    ("ic11", 32),  # 16×16@2x
    ("ic12", 64),  # 32×32@2x
    ("ic07", 128),  # 128×128
    ("ic08", 256),  # 256×256
    ("ic13", 256),  # 128×128@2x
    ("ic09", 512),  # 512×512
    ("ic14", 512),  # 256×256@2x
    ("ic10", 1024),  # 1024×1024 / 512×512@2x
)

# Brand palette (aligned with RedisColors.Primary and RedisStars star accent)
COLOR_PLATE = (229, 57, 53, 255)  # #E53935
COLOR_PLATE_DARK = (183, 28, 28, 255)  # depth edge
COLOR_LAYER = (255, 255, 255, 255)
COLOR_STAR = (255, 213, 79, 255)  # #FFD54F
COLOR_STAR_CORE = (255, 248, 225, 255)


# ---------------------------------------------------------------------------
# Validation
# ---------------------------------------------------------------------------


class IconValidationError(Exception):
    """Raised when a source or derived icon fails validation."""


@dataclass(frozen=True)
class ContentBounds:
    left: int
    top: int
    right: int
    bottom: int
    width: int
    height: int

    @property
    def margin_left(self) -> int:
        return self.left

    @property
    def margin_top(self) -> int:
        return self.top

    def margin_right(self, canvas_w: int) -> int:
        return canvas_w - self.right

    def margin_bottom(self, canvas_h: int) -> int:
        return canvas_h - self.bottom


def content_bounds(image: Image.Image) -> ContentBounds:
    """Return the non-transparent content bounding box."""
    rgba = image.convert("RGBA")
    bbox = rgba.getbbox()
    if bbox is None:
        raise IconValidationError("Source image is fully transparent.")
    left, top, right, bottom = bbox
    return ContentBounds(
        left=left,
        top=top,
        right=right,
        bottom=bottom,
        width=right - left,
        height=bottom - top,
    )


def validate_source_image(image: Image.Image, *, path: Path | None = None) -> None:
    """
    Validate a canonical application-icon source.

    Raises IconValidationError with a clear message on failure.
    """
    label = str(path) if path is not None else "source image"
    rgba = image.convert("RGBA")
    width, height = rgba.size

    if width != height:
        raise IconValidationError(
            f"{label}: must be square, got {width}×{height}.",
        )
    if width < SOURCE_MIN_SIDE or height < SOURCE_MIN_SIDE:
        raise IconValidationError(
            f"{label}: side length must be at least {SOURCE_MIN_SIDE}px, got {width}×{height}.",
        )
    if "A" not in rgba.getbands():
        raise IconValidationError(f"{label}: must have an alpha channel.")

    bounds = content_bounds(rgba)
    if bounds.margin_left <= 0 or bounds.margin_top <= 0:
        raise IconValidationError(
            f"{label}: non-transparent content touches the canvas edge "
            f"(bbox={bounds.left},{bounds.top},{bounds.right},{bounds.bottom}).",
        )
    if bounds.margin_right(width) <= 0 or bounds.margin_bottom(height) <= 0:
        raise IconValidationError(
            f"{label}: non-transparent content touches the canvas edge "
            f"(bbox={bounds.left},{bounds.top},{bounds.right},{bounds.bottom}).",
        )

    margins = (
        bounds.margin_left / width,
        bounds.margin_top / height,
        bounds.margin_right(width) / width,
        bounds.margin_bottom(height) / height,
    )
    for name, ratio in zip(("left", "top", "right", "bottom"), margins):
        if ratio < SAFE_MARGIN_RATIO_MIN:
            raise IconValidationError(
                f"{label}: {name} safety margin too small "
                f"({ratio:.1%} < {SAFE_MARGIN_RATIO_MIN:.0%}).",
            )
        if ratio > SAFE_MARGIN_RATIO_MAX:
            raise IconValidationError(
                f"{label}: {name} safety margin too large "
                f"({ratio:.1%} > {SAFE_MARGIN_RATIO_MAX:.0%}).",
            )

    if max(margins) - min(margins) > SAFE_MARGIN_ASYMMETRY_MAX:
        raise IconValidationError(
            f"{label}: safety margins are uneven "
            f"(L={margins[0]:.1%}, T={margins[1]:.1%}, "
            f"R={margins[2]:.1%}, B={margins[3]:.1%}).",
        )


def load_and_validate_source(path: Path) -> Image.Image:
    if not path.is_file():
        raise IconValidationError(f"Source file does not exist: {path}")
    try:
        image = Image.open(path)
        image.load()
    except OSError as exc:
        raise IconValidationError(f"Unable to read source image {path}: {exc}") from exc
    validate_source_image(image, path=path)
    return image.convert("RGBA")


# ---------------------------------------------------------------------------
# Brand source renderer (used to create / refresh the canonical asset)
# ---------------------------------------------------------------------------


def _star_points(
    cx: float,
    cy: float,
    outer: float,
    inner: float,
    points: int = 4,
) -> list[tuple[float, float]]:
    """4-point (8-vertex) star suitable for small-size recognition."""
    coords: list[tuple[float, float]] = []
    # Start from top so the long axis is vertical/horizontal.
    angle0 = -math.pi / 2
    for i in range(points * 2):
        radius = outer if i % 2 == 0 else inner
        angle = angle0 + i * math.pi / points
        coords.append((cx + radius * math.cos(angle), cy + radius * math.sin(angle)))
    return coords


def render_brand_icon(size: int = SOURCE_MIN_SIDE) -> Image.Image:
    """
    Render the RedisStars application icon at [size]×[size].

    Brand-retention design:
    - Warm red rounded plate (strong silhouette at 16px)
    - Simplified white stacked Redis data layers
    - Compact gold star accent (does not obscure the layers)
    - Consistent ~12% safety margin
    """
    if size < 64:
        raise ValueError("render_brand_icon requires size >= 64")

    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # ~12% margin → subject occupies ~76% of the canvas.
    margin = int(round(size * 0.12))
    plate = (margin, margin, size - margin, size - margin)
    radius = max(8, size // 6)
    draw.rounded_rectangle(plate, radius=radius, fill=COLOR_PLATE)

    # Subtle bottom edge for depth without fine gradients.
    edge_h = max(2, size // 48)
    draw.rounded_rectangle(
        (margin, size - margin - edge_h * 3, size - margin, size - margin),
        radius=radius,
        fill=COLOR_PLATE_DARK,
    )
    # Cover the upper part of the dark strip so only a thin bottom lip remains.
    draw.rounded_rectangle(
        (margin, margin, size - margin, size - margin - edge_h),
        radius=radius,
        fill=COLOR_PLATE,
    )

    # Three stacked white layers (simplified Redis data metaphor).
    plate_w = size - 2 * margin
    plate_h = size - 2 * margin
    layer_w = int(plate_w * 0.56)
    layer_h = max(4, int(plate_h * 0.10))
    gap = max(2, int(plate_h * 0.045))
    stack_h = 3 * layer_h + 2 * gap
    cx = size / 2
    # Shift stack slightly left/down so the star can sit upper-right.
    stack_top = margin + int(plate_h * 0.28)
    for i in range(3):
        y0 = stack_top + i * (layer_h + gap)
        x0 = cx - layer_w / 2 - (1 - i) * (size * 0.01)
        x1 = x0 + layer_w
        y1 = y0 + layer_h
        # Keep all layers solid white so 16px still shows three distinct bars.
        corner = max(2, layer_h // 2)
        draw.rounded_rectangle((x0, y0, x1, y1), radius=corner, fill=COLOR_LAYER)

    # Compact star accent — upper-right of the plate, clear of the layers.
    star_cx = margin + plate_w * 0.78
    star_cy = margin + plate_h * 0.28
    outer = plate_w * 0.11
    inner = outer * 0.42
    star = _star_points(star_cx, star_cy, outer, inner, points=4)
    draw.polygon(star, fill=COLOR_STAR)
    # Bright core for small-size pop.
    core_r = outer * 0.22
    draw.ellipse(
        (star_cx - core_r, star_cy - core_r, star_cx + core_r, star_cy + core_r),
        fill=COLOR_STAR_CORE,
    )

    return img


def write_canonical_source(path: Path = DEFAULT_SOURCE, size: int = SOURCE_MIN_SIDE) -> Path:
    """Render and write the canonical 1024×1024 source PNG."""
    path.parent.mkdir(parents=True, exist_ok=True)
    icon = render_brand_icon(size)
    validate_source_image(icon, path=path)
    icon.save(path, format="PNG")
    return path


# ---------------------------------------------------------------------------
# Scaling helpers
# ---------------------------------------------------------------------------


def scale_icon(source: Image.Image, size: int) -> Image.Image:
    """
    High-quality downscale with light small-size enhancement.

    For ≤32px, apply a very mild contrast + unsharp pass so the plate edge
    and star remain visible without introducing harsh aliasing.
    """
    rgba = source.convert("RGBA")
    if size >= source.width:
        return rgba.resize((size, size), Image.Resampling.LANCZOS)

    scaled = rgba.resize((size, size), Image.Resampling.LANCZOS)
    if size <= 32:
        # Mild contrast to keep the red plate distinct from dark/light desktops.
        scaled = ImageEnhance.Contrast(scaled).enhance(1.08)
        # Tiny unsharp — radius < 1 to avoid ringing.
        scaled = scaled.filter(ImageFilter.UnsharpMask(radius=0.6, percent=80, threshold=2))
        # Re-assert alpha after filter (UnsharpMask can soften edges slightly).
        alpha = scaled.getchannel("A")
        alpha = ImageEnhance.Contrast(alpha).enhance(1.15)
        scaled.putalpha(alpha)
    return scaled


# ---------------------------------------------------------------------------
# Writers
# ---------------------------------------------------------------------------


def write_ico(source: Image.Image, path: Path, sizes: Sequence[int] = ICO_SIZES) -> None:
    """
    Write a multi-resolution ICO.

    Pillow generates each requested size from the supplied image. Prefer a
    256×256 base so every embedded size is produced by high-quality downscale
    rather than a single oversized bitmap.
    """
    base = scale_icon(source, max(sizes))
    size_tuples = [(s, s) for s in sizes]
    base.save(path, format="ICO", sizes=size_tuples)


def write_png(source: Image.Image, path: Path, size: int) -> None:
    scale_icon(source, size).save(path, format="PNG")


def write_icns(source: Image.Image, path: Path, work_dir: Path) -> None:
    import icnsutil

    icns = icnsutil.IcnsFile()
    for key, pixel_size in ICNS_ENTRIES:
        media_path = work_dir / f"_icns_{key}_{pixel_size}.png"
        scale_icon(source, pixel_size).save(media_path, format="PNG")
        icns.add_media(key, file=str(media_path), force=True)
    icns.write(str(path))


def write_preview_sheet(source: Image.Image, path: Path) -> None:
    """Compose a light/dark/gray preview sheet for manual small-size review."""
    cell = 140
    label_h = 28
    pad = 24
    cols = len(PREVIEW_SIZES)
    rows = 3  # light, dark, gray
    bg_colors = (
        (245, 245, 245, 255),
        (30, 30, 30, 255),
        (128, 128, 128, 255),
    )
    sheet_w = pad * 2 + cols * cell
    sheet_h = pad * 2 + rows * (cell + label_h) + 40
    sheet = Image.new("RGBA", (sheet_w, sheet_h), (255, 255, 255, 255))
    draw = ImageDraw.Draw(sheet)
    draw.text((pad, 8), "RedisStars icon size preview", fill=(40, 40, 40, 255))

    for row, bg in enumerate(bg_colors):
        for col, size in enumerate(PREVIEW_SIZES):
            x0 = pad + col * cell
            y0 = 40 + pad + row * (cell + label_h)
            tile = Image.new("RGBA", (cell - 8, cell - 8), bg)
            icon = scale_icon(source, size)
            # Fit the icon into the preview cell; large sizes are display-scaled only.
            max_side = min(tile.width, tile.height) - 8
            if icon.width > max_side:
                icon = icon.resize((max_side, max_side), Image.Resampling.LANCZOS)
            ox = (tile.width - icon.width) // 2
            oy = (tile.height - icon.height) // 2
            tile.alpha_composite(icon, (ox, oy))
            sheet.alpha_composite(tile, (x0 + 4, y0))
            label = f"{size}"
            label_color = (20, 20, 20, 255) if row != 1 else (220, 220, 220, 255)
            draw.text((x0 + 8, y0 + cell - 4), label, fill=label_color)

    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path, format="PNG")


# ---------------------------------------------------------------------------
# ICO inspection helpers (used by --check and tests)
# ---------------------------------------------------------------------------


def ico_embedded_sizes(path: Path) -> set[tuple[int, int]]:
    """
    Read ICO directory entries and return the set of (width, height) pairs.

    ICO stores 0 for width/height when the size is 256.
    """
    data = path.read_bytes()
    if len(data) < 6:
        raise IconValidationError(f"{path}: file too small to be a valid ICO.")
    reserved, ico_type, count = struct.unpack_from("<HHH", data, 0)
    if reserved != 0 or ico_type != 1 or count == 0:
        raise IconValidationError(
            f"{path}: not a valid ICO header "
            f"(reserved={reserved}, type={ico_type}, count={count}).",
        )
    sizes: set[tuple[int, int]] = set()
    offset = 6
    for _ in range(count):
        if offset + 16 > len(data):
            raise IconValidationError(f"{path}: truncated ICO directory.")
        width, height, _colors, _reserved, _planes, _bitcount, _bytes_in_res, _image_offset = (
            struct.unpack_from("<BBBBHHII", data, offset)
        )
        w = 256 if width == 0 else width
        h = 256 if height == 0 else height
        sizes.add((w, h))
        offset += 16
    return sizes


def png_size(path: Path) -> tuple[int, int]:
    with Image.open(path) as img:
        img.load()
        return img.size


# ---------------------------------------------------------------------------
# Generate / check orchestration
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class GeneratePaths:
    source: Path
    output_dir: Path
    runtime_png: Path
    icon_ico: Path
    icon_png: Path
    icon_icns: Path
    preview_sheet: Path

    @staticmethod
    def resolve(
        source: Path | None = None,
        output_dir: Path | None = None,
        runtime_png: Path | None = None,
    ) -> "GeneratePaths":
        out = (output_dir or DEFAULT_OUTPUT_DIR).resolve()
        return GeneratePaths(
            source=(source or DEFAULT_SOURCE).resolve(),
            output_dir=out,
            runtime_png=(runtime_png or RUNTIME_RESOURCE).resolve(),
            icon_ico=out / "icon.ico",
            icon_png=out / "icon.png",
            icon_icns=out / "icon.icns",
            preview_sheet=out / PREVIEW_DIR_NAME / PREVIEW_SHEET_NAME,
        )


def generate_all(paths: GeneratePaths) -> list[Path]:
    """
    Generate all derived icons into a temporary directory, then atomically
    replace the destination files. On any failure, existing outputs are left
    untouched and temporary files are discarded.
    """
    source = load_and_validate_source(paths.source)
    written: list[Path] = []

    with tempfile.TemporaryDirectory(prefix="redisstars-icons-") as tmp:
        tmp_dir = Path(tmp)
        tmp_ico = tmp_dir / "icon.ico"
        tmp_png = tmp_dir / "icon.png"
        tmp_icns = tmp_dir / "icon.icns"
        tmp_runtime = tmp_dir / "app-icon.png"
        tmp_preview = tmp_dir / PREVIEW_SHEET_NAME
        icns_work = tmp_dir / "icns_media"
        icns_work.mkdir()

        write_ico(source, tmp_ico)
        write_png(source, tmp_png, LINUX_PNG_SIZE)
        write_icns(source, tmp_icns, icns_work)
        write_png(source, tmp_runtime, RUNTIME_PNG_SIZE)
        write_preview_sheet(source, tmp_preview)

        # Validate temp outputs before replacing anything.
        _assert_derived_ok(
            ico=tmp_ico,
            linux_png=tmp_png,
            runtime_png=tmp_runtime,
            icns=tmp_icns,
        )

        paths.output_dir.mkdir(parents=True, exist_ok=True)
        paths.preview_sheet.parent.mkdir(parents=True, exist_ok=True)
        paths.runtime_png.parent.mkdir(parents=True, exist_ok=True)

        for src, dest in (
            (tmp_ico, paths.icon_ico),
            (tmp_png, paths.icon_png),
            (tmp_icns, paths.icon_icns),
            (tmp_runtime, paths.runtime_png),
            (tmp_preview, paths.preview_sheet),
        ):
            # Atomic-ish replace: copy into place then replace.
            staging = dest.with_suffix(dest.suffix + ".tmp")
            shutil.copy2(src, staging)
            staging.replace(dest)
            written.append(dest)

    return written


def _assert_derived_ok(
    *,
    ico: Path,
    linux_png: Path,
    runtime_png: Path,
    icns: Path,
) -> None:
    if ico.stat().st_size == 0:
        raise IconValidationError(f"{ico}: empty file.")
    sizes = ico_embedded_sizes(ico)
    missing = {(s, s) for s in ICO_SIZES} - sizes
    # Some Pillow builds may omit exact 20/40; require the critical set at minimum.
    required_min = {(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)}
    if not required_min.issubset(sizes):
        raise IconValidationError(
            f"{ico}: missing required sizes. Have {sorted(sizes)}, need at least {sorted(required_min)}.",
        )
    if missing:
        # Soft warning for optional 20/40 — still fail if we asked to write them and none exist.
        optional = {(20, 20), (40, 40)}
        critical_missing = missing - optional
        if critical_missing:
            raise IconValidationError(
                f"{ico}: missing sizes {sorted(critical_missing)}.",
            )

    if png_size(linux_png) != (LINUX_PNG_SIZE, LINUX_PNG_SIZE):
        raise IconValidationError(
            f"{linux_png}: expected {LINUX_PNG_SIZE}×{LINUX_PNG_SIZE}, got {png_size(linux_png)}.",
        )
    if png_size(runtime_png) != (RUNTIME_PNG_SIZE, RUNTIME_PNG_SIZE):
        raise IconValidationError(
            f"{runtime_png}: expected {RUNTIME_PNG_SIZE}×{RUNTIME_PNG_SIZE}, "
            f"got {png_size(runtime_png)}.",
        )
    for p in (linux_png, runtime_png):
        with Image.open(p) as img:
            img.load()
            if img.mode != "RGBA":
                raise IconValidationError(f"{p}: expected RGBA, got {img.mode}.")
            # Derived PNGs keep the source's relative margins; only assert edges stay clear.
            _ensure_edge_clear(img)

    if icns.stat().st_size == 0:
        raise IconValidationError(f"{icns}: empty file.")
    _check_icns_has_1024(icns)


def _ensure_edge_clear(image: Image.Image) -> Image.Image:
    """Ensure non-transparent pixels do not touch the canvas edge."""
    rgba = image.convert("RGBA")
    bounds = content_bounds(rgba)
    w, h = rgba.size
    if (
        bounds.margin_left <= 0
        or bounds.margin_top <= 0
        or bounds.margin_right(w) <= 0
        or bounds.margin_bottom(h) <= 0
    ):
        raise IconValidationError(
            f"Derived image content touches the edge "
            f"(bbox={bounds.left},{bounds.top},{bounds.right},{bounds.bottom}).",
        )
    return rgba


def _check_icns_has_1024(path: Path) -> None:
    import icnsutil

    icns = icnsutil.IcnsFile(str(path))
    keys = set(icns.media.keys())
    required = {"icp4", "icp5", "ic07", "ic08", "ic09", "ic10"}
    missing = required - keys
    if missing:
        raise IconValidationError(f"{path}: missing ICNS keys {sorted(missing)}.")
    if "ic10" not in keys:
        raise IconValidationError(f"{path}: missing 1024×1024 (ic10) asset.")


def check_outputs(paths: GeneratePaths) -> None:
    """Validate that all required derived assets exist and look healthy."""
    required_files = (
        paths.source,
        paths.icon_ico,
        paths.icon_png,
        paths.icon_icns,
        paths.runtime_png,
    )
    missing = [p for p in required_files if not p.is_file()]
    if missing:
        raise IconValidationError(
            "Missing required icon files:\n  " + "\n  ".join(str(p) for p in missing),
        )

    for p in required_files:
        if p.stat().st_size == 0:
            raise IconValidationError(f"{p}: empty file.")

    load_and_validate_source(paths.source)

    if png_size(paths.icon_png) != (LINUX_PNG_SIZE, LINUX_PNG_SIZE):
        raise IconValidationError(
            f"{paths.icon_png}: expected {LINUX_PNG_SIZE}×{LINUX_PNG_SIZE}, "
            f"got {png_size(paths.icon_png)}.",
        )
    if png_size(paths.runtime_png) != (RUNTIME_PNG_SIZE, RUNTIME_PNG_SIZE):
        raise IconValidationError(
            f"{paths.runtime_png}: expected {RUNTIME_PNG_SIZE}×{RUNTIME_PNG_SIZE}, "
            f"got {png_size(paths.runtime_png)}.",
        )

    for p in (paths.icon_png, paths.runtime_png):
        with Image.open(p) as img:
            img.load()
            if img.mode != "RGBA":
                raise IconValidationError(f"{p}: expected RGBA, got {img.mode}.")
            _ensure_edge_clear(img)

    sizes = ico_embedded_sizes(paths.icon_ico)
    required_min = {(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)}
    if not required_min.issubset(sizes):
        raise IconValidationError(
            f"{paths.icon_ico}: missing required sizes. Have {sorted(sizes)}.",
        )

    _check_icns_has_1024(paths.icon_icns)


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def build_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Generate RedisStars platform and runtime icons from a 1024×1024 source.",
    )
    parser.add_argument(
        "--source",
        type=Path,
        default=None,
        help=f"Canonical source PNG (default: {DEFAULT_SOURCE})",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=None,
        help=f"Directory for icon.ico / icon.png / icon.icns (default: {DEFAULT_OUTPUT_DIR})",
    )
    parser.add_argument(
        "--runtime-png",
        type=Path,
        default=None,
        help=f"Runtime Window icon path (default: {RUNTIME_RESOURCE})",
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validate existing outputs without regenerating.",
    )
    parser.add_argument(
        "--write-source",
        action="store_true",
        help="Render and write the canonical brand source PNG before generating.",
    )
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    parser = build_arg_parser()
    args = parser.parse_args(list(argv) if argv is not None else None)
    paths = GeneratePaths.resolve(
        source=args.source,
        output_dir=args.output_dir,
        runtime_png=args.runtime_png,
    )

    try:
        if args.write_source:
            written = write_canonical_source(paths.source)
            print(f"wrote source {written}")

        if args.check:
            check_outputs(paths)
            print("icon check passed")
            for p in (
                paths.source,
                paths.icon_ico,
                paths.icon_png,
                paths.icon_icns,
                paths.runtime_png,
            ):
                print(f"  ok {p}")
            return 0

        written = generate_all(paths)
        for p in written:
            print(f"wrote {p}")
        return 0
    except IconValidationError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1
    except Exception as exc:  # noqa: BLE001 — CLI boundary
        print(f"error: unexpected failure: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
