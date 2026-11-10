import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_gradient_radial(width, height, center_color, edge_color):
    img = Image.new("RGBA", (width, height))
    draw = ImageDraw.Draw(img)
    cx, cy = width / 2.0, height / 2.0
    max_radius = math.sqrt(cx * cx + cy * cy)
    
    # Render with stepped circles
    for r in range(int(max_radius), 0, -2):
        factor = r / max_radius
        red = int(center_color[0] + (edge_color[0] - center_color[0]) * factor)
        green = int(center_color[1] + (edge_color[1] - center_color[1]) * factor)
        blue = int(center_color[2] + (edge_color[2] - center_color[2]) * factor)
        draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(red, green, blue, 255))
    return img

def create_gradient_linear(width, height, top_color, bottom_color):
    img = Image.new("RGBA", (width, height))
    draw = ImageDraw.Draw(img)
    for y in range(height):
        factor = y / float(height)
        r = int(top_color[0] + (bottom_color[0] - top_color[0]) * factor)
        g = int(top_color[1] + (bottom_color[1] - top_color[1]) * factor)
        b = int(top_color[2] + (bottom_color[2] - top_color[2]) * factor)
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))
    return img

def draw_shield(draw, cx, cy, width, height, fill_color, outline_color=None, outline_width=0):
    # Crest shield polygon
    top = cy - height * 0.5
    bottom = cy + height * 0.5
    left = cx - width * 0.5
    right = cx + width * 0.5
    
    p1 = (cx, top)
    p2 = (right, top + height * 0.15)
    p3 = (right, cy + height * 0.1)
    p4 = (cx, bottom)
    p5 = (left, cy + height * 0.1)
    p6 = (left, top + height * 0.15)
    
    points = [p1, p2, p3, p4, p5, p6]
    draw.polygon(points, fill=fill_color)
    if outline_color and outline_width > 0:
        # Draw thick outline lines
        for i in range(len(points)):
            draw.line([points[i], points[(i + 1) % len(points)]], fill=outline_color, width=outline_width)

def draw_key_icon(draw, cx, cy, size, color):
    # Modern Key emblem
    r = size * 0.28
    draw.ellipse([cx - r, cy - size * 0.45, cx + r, cy - size * 0.45 + 2 * r], outline=color, width=max(2, int(size * 0.08)))
    draw.ellipse([cx - r * 0.45, cy - size * 0.45 + r * 0.55, cx + r * 0.45, cy - size * 0.45 + r * 1.45], fill=color)
    
    # Stem
    stem_top = cy - size * 0.45 + 2 * r - 2
    stem_bottom = cy + size * 0.45
    draw.line([(cx, stem_top), (cx, stem_bottom)], fill=color, width=max(3, int(size * 0.09)))
    
    # Teeth
    draw.line([(cx, stem_bottom - size * 0.18), (cx + size * 0.22, stem_bottom - size * 0.18)], fill=color, width=max(2, int(size * 0.08)))
    draw.line([(cx, stem_bottom - size * 0.04), (cx + size * 0.16, stem_bottom - size * 0.04)], fill=color, width=max(2, int(size * 0.08)))

def draw_lock_icon(draw, cx, cy, size, color):
    # Padlock shackle
    shackle_w = size * 0.32
    shackle_h = size * 0.35
    shackle_t = max(2, int(size * 0.08))
    draw.arc([cx - shackle_w, cy - size * 0.45, cx + shackle_w, cy - size * 0.45 + 2 * shackle_h], 180, 0, fill=color, width=shackle_t)
    
    # Padlock body
    body_w = size * 0.44
    body_h = size * 0.45
    draw.rounded_rectangle([cx - body_w, cy - size * 0.1, cx + body_w, cy - size * 0.1 + body_h], radius=int(size * 0.08), fill=color)
    
    # Keyhole
    draw.ellipse([cx - size * 0.06, cy + size * 0.05, cx + size * 0.06, cy + size * 0.17], fill=(13, 27, 42, 255))
    draw.polygon([(cx - size * 0.04, cy + size * 0.13), (cx + size * 0.04, cy + size * 0.13), (cx + size * 0.06, cy + size * 0.24), (cx - size * 0.06, cy + size * 0.24)], fill=(13, 27, 42, 255))

def generate_app_icon(size=512):
    # 512x512 High Quality App Icon
    img = create_gradient_radial(size, size, (27, 42, 74), (10, 17, 40))
    draw = ImageDraw.Draw(img)
    
    # Soft background outer circular glow
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse([size * 0.1, size * 0.1, size * 0.9, size * 0.9], fill=(37, 99, 235, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(size * 0.06))
    img.paste(glow, (0, 0), glow)
    
    draw = ImageDraw.Draw(img)
    
    # Draw Shield
    cx, cy = size / 2.0, size / 2.0
    shield_w = size * 0.65
    shield_h = size * 0.72
    
    # Shield Outer Golden Border
    draw_shield(draw, cx, cy, shield_w, shield_h, (30, 58, 138, 240), (245, 158, 11, 255), int(size * 0.025))
    # Inner Shield Layer
    draw_shield(draw, cx, cy + size * 0.005, shield_w * 0.88, shield_h * 0.88, (15, 23, 42, 255), (59, 130, 246, 200), int(size * 0.015))
    
    # Center Key / Lock Emblem
    draw_key_icon(draw, cx, cy - size * 0.02, size * 0.38, (251, 191, 36, 255))
    
    # Security star accent at top
    draw.polygon([(cx, cy - shield_h * 0.44), (cx + size * 0.03, cy - shield_h * 0.38), (cx, cy - shield_h * 0.32), (cx - size * 0.03, cy - shield_h * 0.38)], fill=(255, 255, 255, 255))
    
    return img

def generate_feature_graphic(width=1024, height=500):
    # Deep Cyber-Security Background
    img = create_gradient_radial(width, height, (20, 36, 75), (7, 12, 28))
    draw = ImageDraw.Draw(img)
    
    # Draw subtle background grid
    grid_color = (255, 255, 255, 12)
    for x in range(0, width, 40):
        draw.line([(x, 0), (x, height)], fill=grid_color)
    for y in range(0, height, 40):
        draw.line([(0, y), (width, y)], fill=grid_color)
    
    # Cyan/Blue glowing ambient circles
    ambient = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    amb_draw = ImageDraw.Draw(ambient)
    amb_draw.ellipse([width * 0.05, height * 0.1, width * 0.38, height * 0.9], fill=(30, 64, 175, 75))
    amb_draw.ellipse([width * 0.65, -height * 0.2, width * 1.1, height * 0.8], fill=(14, 165, 233, 45))
    ambient = ambient.filter(ImageFilter.GaussianBlur(40))
    img.paste(ambient, (0, 0), ambient)
    draw = ImageDraw.Draw(img)
    
    # ------------------ LEFT SIDE: 3D SHIELD & EMBLEM ------------------
    cx = width * 0.22
    cy = height * 0.48
    s_w = 260
    s_h = 300
    
    # Shield glow
    shield_glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    sg_draw = ImageDraw.Draw(shield_glow)
    draw_shield(sg_draw, cx, cy, s_w * 1.15, s_h * 1.15, (245, 158, 11, 40))
    shield_glow = shield_glow.filter(ImageFilter.GaussianBlur(25))
    img.paste(shield_glow, (0, 0), shield_glow)
    draw = ImageDraw.Draw(img)
    
    # Outer Shield
    draw_shield(draw, cx, cy, s_w, s_h, (17, 24, 39, 250), (245, 158, 11, 255), 7)
    # Inner Shield
    draw_shield(draw, cx, cy, s_w * 0.88, s_h * 0.88, (15, 23, 42, 255), (59, 130, 246, 220), 4)
    # Key Emblem
    draw_key_icon(draw, cx, cy - 8, 160, (251, 191, 36, 255))
    
    # Shield Bottom Badge
    draw.rounded_rectangle([cx - 85, cy + 120, cx + 85, cy + 152], radius=16, fill=(245, 158, 11, 255))
    # Badge text
    try:
        font_badge = ImageFont.truetype("arialbd.ttf", 15)
    except:
        font_badge = ImageFont.load_default()
    draw.text((cx, cy + 136), "100% OFFLINE VAULT", font=font_badge, fill=(13, 27, 42, 255), anchor="mm")
    
    # ------------------ RIGHT SIDE: TYPOGRAPHY & FULL FEATURES ------------------
    rx = width * 0.44
    
    try:
        font_tag = ImageFont.truetype("arialbd.ttf", 14)
        font_title = ImageFont.truetype("arialbd.ttf", 36)
        font_sub = ImageFont.truetype("arial.ttf", 17)
        font_feat_title = ImageFont.truetype("arialbd.ttf", 15)
        font_feat_desc = ImageFont.truetype("arial.ttf", 12)
    except:
        font_tag = font_title = font_sub = font_feat_title = font_feat_desc = ImageFont.load_default()
    
    # Category / Security Tag Pill
    tag_x = rx
    tag_y = 48
    draw.rounded_rectangle([tag_x, tag_y, tag_x + 310, tag_y + 28], radius=14, fill=(30, 58, 138, 200), outline=(59, 130, 246, 255), width=1)
    draw.text((tag_x + 155, tag_y + 14), "MILITARY-GRADE AES-256-GCM SECURITY", font=font_tag, fill=(147, 197, 253, 255), anchor="mm")
    
    # Main Title
    draw.text((rx, 90), "Secure Password Manager", font=font_title, fill=(255, 255, 255, 255))
    
    # Subtitle
    draw.text((rx, 132), "Android Native Vault + Chrome Web Access + Remote VPN", font=font_sub, fill=(209, 213, 219, 255))
    
    # Divider
    draw.line([(rx, 162), (width - 40, 162)], fill=(59, 130, 246, 120), width=2)
    
    # 6 Core Detailed Features in 2 Columns x 3 Rows
    features = [
        ("Hardware Keystore AES-256", "Android Keystore hardware-backed keys, unique IV per item"),
        ("Chrome Web Interface", "Direct access from computer browser on same Wi-Fi"),
        ("Realtime Live Sync", "Instant 2-way WebSocket updates between PC & Mobile"),
        ("Biometric & Auto-Lock", "Fingerprint unlock, rate-limit lockout, clipboard wiper"),
        ("Remote VPN Access", "Connect securely anywhere via Tailscale / WireGuard VPN"),
        ("Zero Cloud / Zero Tracking", "100% Local SQLite storage with anti-reverse engineering")
    ]
    
    col_w = 265
    row_h = 88
    start_y = 178
    
    col_coords = [rx, rx + col_w + 20]
    
    for i, (f_title, f_desc) in enumerate(features):
        col_idx = i % 2
        row_idx = i // 2
        fx = col_coords[col_idx]
        fy = start_y + row_idx * row_h
        
        # Feature card backdrop
        card_w = col_w
        card_h = 72
        draw.rounded_rectangle([fx, fy, fx + card_w, fy + card_h], radius=10, fill=(15, 23, 42, 190), outline=(30, 58, 138, 160), width=1)
        
        # Bullet / Icon dot
        dot_color = (245, 158, 11, 255) if (i % 2 == 0) else (56, 189, 248, 255)
        draw.ellipse([fx + 10, fy + 14, fx + 20, fy + 24], fill=dot_color)
        
        # Feature Title
        draw.text((fx + 28, fy + 19), f_title, font=font_feat_title, fill=(243, 244, 246, 255), anchor="lm")
        
        # Feature Description (wrapped if needed)
        # 2 lines text
        draw.text((fx + 10, fy + 38), f_desc[:40], font=font_feat_desc, fill=(156, 163, 175, 255))
        if len(f_desc) > 40:
            draw.text((fx + 10, fy + 53), f_desc[40:], font=font_feat_desc, fill=(156, 163, 175, 255))
            
    # Bottom security footer watermark
    draw.text((width - 45, height - 20), "Package: jmd.shiv.mypassword • Zero Knowledge Architecture", font=font_feat_desc, fill=(100, 116, 139, 255), anchor="rm")
    
    return img

def main():
    os.makedirs("assets/playstore", exist_ok=True)
    
    # 1. Feature Graphic (1024x500)
    feat_img = generate_feature_graphic(1024, 500)
    feat_path = "assets/playstore/feature_graphic_1024x500.png"
    feat_img.save(feat_path, "PNG", quality=100)
    print(f"Generated Feature Graphic: {feat_path} (1024x500)")
    
    # 2. Play Store 512x512 Icon
    icon_512 = generate_app_icon(512)
    icon_512_path = "assets/playstore/play_store_512.png"
    icon_512.save(icon_512_path, "PNG", quality=100)
    print(f"Generated Play Store Icon: {icon_512_path} (512x512)")
    
    # 3. Mipmap densities
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192
    }
    
    for density, size in densities.items():
        res_dir = f"app/src/main/res/mipmap-{density}"
        os.makedirs(res_dir, exist_ok=True)
        
        # Square launcher
        scaled_icon = icon_512.resize((size, size), Image.Resampling.LANCZOS)
        scaled_icon.save(f"{res_dir}/ic_launcher.png", "PNG")
        
        # Round launcher
        round_mask = Image.new("L", (size, size), 0)
        mask_draw = ImageDraw.Draw(round_mask)
        mask_draw.ellipse([0, 0, size, size], fill=255)
        
        round_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        round_icon.paste(scaled_icon, (0, 0), round_mask)
        round_icon.save(f"{res_dir}/ic_launcher_round.png", "PNG")
        
        # Foreground for adaptive icon
        # Scale shield emblem in center of size
        fg_icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        fg_scaled = icon_512.resize((int(size * 0.72), int(size * 0.72)), Image.Resampling.LANCZOS)
        offset = int(size * 0.14)
        fg_icon.paste(fg_scaled, (offset, offset), fg_scaled)
        fg_icon.save(f"{res_dir}/ic_launcher_foreground.png", "PNG")
        
        print(f"Saved {density} icons ({size}x{size}) in {res_dir}")

if __name__ == "__main__":
    main()
