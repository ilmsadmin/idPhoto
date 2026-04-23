"""
Generate a simple, fresh-style app icon for IDPhoto.
Concept:
 - Rounded-square background with a soft fresh gradient (mint green -> sky blue).
 - A white ID card silhouette in the center.
 - A friendly circular "portrait" placeholder on the card (head + shoulders).
 - Small accent lines beside the portrait (info lines) for the ID feel.
Output: icon.png (1024x1024) at project root.
"""

from PIL import Image, ImageDraw, ImageFilter

SIZE = 1024
R = 220  # corner radius


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def make_gradient(size, top, bottom):
    img = Image.new("RGB", (size, size), top)
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        c = lerp(top, bottom, t)
        for x in range(size):
            px[x, y] = c
    return img


def rounded_mask(size, radius):
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    d.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    return mask


def main():
    # Fresh palette: mint to sky blue
    top = (122, 231, 200)     # mint
    bottom = (86, 170, 255)   # sky blue

    bg = make_gradient(SIZE, top, bottom)

    # Apply rounded corners -> RGBA
    icon = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    icon.paste(bg, (0, 0), rounded_mask(SIZE, R))

    draw = ImageDraw.Draw(icon)

    # Soft highlight: subtle diagonal glow
    glow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((-200, -300, 700, 600), fill=(255, 255, 255, 55))
    glow = glow.filter(ImageFilter.GaussianBlur(60))
    icon.alpha_composite(Image.composite(glow, Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0)), rounded_mask(SIZE, R)))

    # --- ID Card ---
    card_w, card_h = 720, 470
    card_x = (SIZE - card_w) // 2
    card_y = (SIZE - card_h) // 2 + 30
    card_r = 70

    # Card shadow
    shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sdraw.rounded_rectangle(
        (card_x + 10, card_y + 24, card_x + card_w + 10, card_y + card_h + 24),
        radius=card_r,
        fill=(20, 60, 100, 110),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(22))
    icon.alpha_composite(shadow)

    # Card body (white)
    draw.rounded_rectangle(
        (card_x, card_y, card_x + card_w, card_y + card_h),
        radius=card_r,
        fill=(255, 255, 255, 255),
    )

    # Card top strip (brand accent)
    strip_h = 90
    strip = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    stdraw = ImageDraw.Draw(strip)
    stdraw.rounded_rectangle(
        (card_x, card_y, card_x + card_w, card_y + card_h),
        radius=card_r, fill=(255, 255, 255, 255),
    )
    # Use the strip image as a mask-region trick: draw strip rectangle then intersect with card
    strip_layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sldraw = ImageDraw.Draw(strip_layer)
    sldraw.rectangle((card_x, card_y, card_x + card_w, card_y + strip_h), fill=(86, 170, 255, 255))
    # Mask to card shape
    card_mask = Image.new("L", (SIZE, SIZE), 0)
    cmdraw = ImageDraw.Draw(card_mask)
    cmdraw.rounded_rectangle(
        (card_x, card_y, card_x + card_w, card_y + card_h),
        radius=card_r, fill=255,
    )
    icon.paste(strip_layer, (0, 0), Image.eval(card_mask, lambda v: v).point(lambda v: v))
    # Re-apply strip properly using mask
    tmp = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    tdraw = ImageDraw.Draw(tmp)
    tdraw.rectangle((card_x, card_y, card_x + card_w, card_y + strip_h), fill=(86, 170, 255, 255))
    icon.paste(tmp, (0, 0), card_mask)

    draw = ImageDraw.Draw(icon)

    # --- Portrait (circle + head + shoulders) ---
    photo_cx = card_x + 180
    photo_cy = card_y + 280
    photo_r = 130

    # Photo frame circle (light mint)
    draw.ellipse(
        (photo_cx - photo_r, photo_cy - photo_r, photo_cx + photo_r, photo_cy + photo_r),
        fill=(230, 248, 240, 255),
        outline=(86, 170, 255, 255),
        width=6,
    )

    # Head
    head_r = 46
    head_cy = photo_cy - 30
    draw.ellipse(
        (photo_cx - head_r, head_cy - head_r, photo_cx + head_r, head_cy + head_r),
        fill=(86, 170, 255, 255),
    )

    # Shoulders (rounded rectangle / pill)
    sh_w = 170
    sh_h = 120
    sh_x0 = photo_cx - sh_w // 2
    sh_y0 = photo_cy + 30
    # Draw shoulders but clip to photo circle
    shoulders_layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    shdraw = ImageDraw.Draw(shoulders_layer)
    shdraw.rounded_rectangle(
        (sh_x0, sh_y0, sh_x0 + sh_w, sh_y0 + sh_h),
        radius=70,
        fill=(86, 170, 255, 255),
    )
    circle_mask = Image.new("L", (SIZE, SIZE), 0)
    cmd = ImageDraw.Draw(circle_mask)
    cmd.ellipse(
        (photo_cx - photo_r + 4, photo_cy - photo_r + 4, photo_cx + photo_r - 4, photo_cy + photo_r - 4),
        fill=255,
    )
    icon.paste(shoulders_layer, (0, 0), circle_mask)

    draw = ImageDraw.Draw(icon)

    # --- Info lines on the right of the photo ---
    line_x = photo_cx + photo_r + 60
    line_w_long = 280
    line_w_mid = 220
    line_w_short = 160
    line_h = 26
    gap = 40
    base_y = photo_cy - 80

    for i, w in enumerate([line_w_long, line_w_mid, line_w_short]):
        y = base_y + i * (line_h + gap)
        color = (86, 170, 255, 255) if i == 0 else (190, 215, 235, 255)
        draw.rounded_rectangle(
            (line_x, y, line_x + w, y + line_h),
            radius=line_h // 2,
            fill=color,
        )

    # Small check / sparkle accent (fresh touch): a little dot
    draw.ellipse(
        (card_x + card_w - 70, card_y + strip_h // 2 - 14, card_x + card_w - 42, card_y + strip_h // 2 + 14),
        fill=(255, 255, 255, 230),
    )

    # Save
    out = "icon.png"
    icon.save(out, "PNG")
    print(f"Saved {out} ({icon.size})")


if __name__ == "__main__":
    main()
