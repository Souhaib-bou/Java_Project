from PIL import Image
import os

INPUT = r"C:\Users\Firas\OneDrive\Desktop\Forum\images\logo.png"
OUTPUT = r"C:\Users\Firas\OneDrive\Desktop\Forum\images\logo-trimmed.png"
ALPHA_THRESHOLD = 10               # 0..255 (raise if you still see padding)

img = Image.open(INPUT).convert("RGBA")
r, g, b, a = img.split()

# Build a mask: keep pixels where alpha > threshold
mask = a.point(lambda p: 255 if p > ALPHA_THRESHOLD else 0)

bbox = mask.getbbox()
if not bbox:
    raise ValueError("Image seems fully transparent (no content found).")

trimmed = img.crop(bbox)

# Optional: add a tiny safe padding so it doesn't look too tight
PADDING = 6
w, h = trimmed.size
padded = Image.new("RGBA", (w + 2*PADDING, h + 2*PADDING), (0, 0, 0, 0))
padded.paste(trimmed, (PADDING, PADDING))

padded.save(OUTPUT)
print("Saved:", os.path.abspath(OUTPUT), "size:", padded.size)
