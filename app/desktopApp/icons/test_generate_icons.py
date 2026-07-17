#!/usr/bin/env python3
"""Unit tests for RedisStars icon generation helpers."""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

from PIL import Image, ImageDraw

# Allow importing generate_icons from the same directory regardless of CWD.
ICONS_DIR = Path(__file__).resolve().parent
if str(ICONS_DIR) not in sys.path:
    sys.path.insert(0, str(ICONS_DIR))

import generate_icons as gi  # noqa: E402


def _solid_square(size: int = 1024, margin: int = 120) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle(
        (margin, margin, size - margin, size - margin),
        radius=size // 8,
        fill=(229, 57, 53, 255),
    )
    return img


class ValidateSourceTests(unittest.TestCase):
    def test_square_rgba_passes(self) -> None:
        gi.validate_source_image(_solid_square())

    def test_non_square_fails(self) -> None:
        img = Image.new("RGBA", (1024, 800), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        draw.rectangle((100, 100, 700, 700), fill=(229, 57, 53, 255))
        with self.assertRaises(gi.IconValidationError) as ctx:
            gi.validate_source_image(img)
        self.assertIn("square", str(ctx.exception).lower())

    def test_low_resolution_fails(self) -> None:
        img = _solid_square(size=512, margin=60)
        with self.assertRaises(gi.IconValidationError) as ctx:
            gi.validate_source_image(img)
        self.assertIn("1024", str(ctx.exception))

    def test_fully_transparent_fails(self) -> None:
        img = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
        with self.assertRaises(gi.IconValidationError) as ctx:
            gi.validate_source_image(img)
        self.assertIn("transparent", str(ctx.exception).lower())

    def test_content_touching_edge_fails(self) -> None:
        img = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)
        draw.rectangle((0, 100, 900, 900), fill=(229, 57, 53, 255))
        with self.assertRaises(gi.IconValidationError) as ctx:
            gi.validate_source_image(img)
        self.assertIn("edge", str(ctx.exception).lower())


class OutputValidationTests(unittest.TestCase):
    def test_ico_contains_required_sizes(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            source = _solid_square()
            ico_path = tmp_dir / "icon.ico"
            gi.write_ico(source, ico_path)
            sizes = gi.ico_embedded_sizes(ico_path)
            required = {(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)}
            self.assertTrue(required.issubset(sizes), msg=f"sizes={sizes}")

    def test_png_output_size(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            source = _solid_square()
            png_path = tmp_dir / "icon.png"
            gi.write_png(source, png_path, 512)
            self.assertEqual(gi.png_size(png_path), (512, 512))
            with Image.open(png_path) as img:
                self.assertEqual(img.mode, "RGBA")

    def test_failed_generate_does_not_overwrite(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            out = tmp_dir / "out"
            out.mkdir()
            # Pre-existing valid-looking placeholder that must survive failure.
            sentinel = out / "icon.png"
            sentinel.write_bytes(b"SENTINEL")

            bad_source = tmp_dir / "bad.png"
            Image.new("RGBA", (100, 100), (255, 0, 0, 255)).save(bad_source)

            paths = gi.GeneratePaths.resolve(
                source=bad_source,
                output_dir=out,
                runtime_png=tmp_dir / "runtime" / "app-icon.png",
            )
            with self.assertRaises(gi.IconValidationError):
                gi.generate_all(paths)
            self.assertEqual(sentinel.read_bytes(), b"SENTINEL")


class PathResolutionTests(unittest.TestCase):
    def test_default_paths_use_script_dir(self) -> None:
        self.assertEqual(gi.SCRIPT_DIR, ICONS_DIR)
        self.assertTrue(str(gi.DEFAULT_SOURCE).endswith(str(Path("source") / "app-icon-source.png")))
        self.assertEqual(gi.DEFAULT_SOURCE.parent, ICONS_DIR / "source")
        self.assertEqual(
            gi.RUNTIME_RESOURCE,
            ICONS_DIR.parent / "src" / "main" / "resources" / "app-icon.png",
        )

    def test_resolve_independent_of_cwd(self) -> None:
        paths_a = gi.GeneratePaths.resolve()
        # Changing CWD must not change resolved defaults.
        import os

        original = Path.cwd()
        try:
            os.chdir(tempfile.gettempdir())
            paths_b = gi.GeneratePaths.resolve()
        finally:
            os.chdir(original)
        self.assertEqual(paths_a.source, paths_b.source)
        self.assertEqual(paths_a.icon_ico, paths_b.icon_ico)
        self.assertEqual(paths_a.runtime_png, paths_b.runtime_png)


class BrandRenderTests(unittest.TestCase):
    def test_rendered_source_passes_validation(self) -> None:
        icon = gi.render_brand_icon(1024)
        gi.validate_source_image(icon)
        self.assertEqual(icon.size, (1024, 1024))
        self.assertEqual(icon.mode, "RGBA")


class AtomicGenerateTests(unittest.TestCase):
    def test_generate_all_writes_expected_files(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            source_path = tmp_dir / "source" / "app-icon-source.png"
            source_path.parent.mkdir(parents=True)
            gi.render_brand_icon(1024).save(source_path)

            out = tmp_dir / "icons"
            runtime = tmp_dir / "resources" / "app-icon.png"
            paths = gi.GeneratePaths.resolve(
                source=source_path,
                output_dir=out,
                runtime_png=runtime,
            )
            written = gi.generate_all(paths)
            self.assertTrue(paths.icon_ico.is_file())
            self.assertTrue(paths.icon_png.is_file())
            self.assertTrue(paths.icon_icns.is_file())
            self.assertTrue(paths.runtime_png.is_file())
            self.assertTrue(paths.preview_sheet.is_file())
            self.assertEqual(gi.png_size(paths.icon_png), (512, 512))
            self.assertEqual(gi.png_size(paths.runtime_png), (256, 256))
            gi.check_outputs(paths)
            self.assertGreaterEqual(len(written), 4)


if __name__ == "__main__":
    unittest.main()
