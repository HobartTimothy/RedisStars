from PIL import Image
from pathlib import Path
import icnsutil

src = Path(r"E:\Code\Personal\RedisStars\app\desktopApp\src\main\resources\logo.png")
icons = Path(r"E:\Code\Personal\RedisStars\app\desktopApp\icons")
icons.mkdir(parents=True, exist_ok=True)

img = Image.open(src).convert("RGBA")
side = max(img.size)
canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
canvas.paste(img, ((side - img.width) // 2, (side - img.height) // 2), img)

sizes = [(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]
base = canvas.resize((256, 256), Image.Resampling.LANCZOS)
base.save(icons / "icon.ico", format="ICO", sizes=sizes)

png512 = canvas.resize((512, 512), Image.Resampling.LANCZOS)
png512.save(icons / "icon.png", format="PNG")

# macOS type keys (PNG media in .icns)
icns_sizes = {
    16: "icp4",
    32: "icp5",
    64: "icp6",
    128: "ic07",
    256: "ic08",
    512: "ic09",
}
icns = icnsutil.IcnsFile()
for size, key in icns_sizes.items():
    path = icons / f"icon_{size}.png"
    canvas.resize((size, size), Image.Resampling.LANCZOS).save(path, format="PNG")
    icns.add_media(key, file=str(path), force=True)
icns.write(str(icons / "icon.icns"))

for path in icons.glob("icon_*.png"):
    path.unlink()

print("wrote", icons / "icon.ico")
print("wrote", icons / "icon.png")
print("wrote", icons / "icon.icns")
