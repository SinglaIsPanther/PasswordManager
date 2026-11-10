import os
import math
import numpy as np
import cv2
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ASSETS_DIR = r"D:\Android\AndroidStudioProjects6\PasswordManager\play_store_assets"
SCREENSHOTS_DIR = os.path.join(ASSETS_DIR, "screenshots")
OUTPUT_VIDEO_PATH = os.path.join(ASSETS_DIR, "password_manager_demo_video.mp4")

# Dimensions
WIDTH = 1920
HEIGHT = 1080
FPS = 30

def get_font(size, bold=False):
    font_path = r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf"
    try:
        return ImageFont.truetype(font_path, size)
    except Exception:
        return ImageFont.load_default()

# Preload screenshots
screenshot_files = [
    "screenshot_1_biometric_login.png",
    "screenshot_2_executive_dashboard.png",
    "screenshot_3_password_generator.png",
    "screenshot_4_local_wifi_sync.png",
    "screenshot_5_secure_vault_records.png",
    "screenshot_6_military_encryption.png"
]

loaded_screenshots = []
for sf in screenshot_files:
    p = os.path.join(SCREENSHOTS_DIR, sf)
    if os.path.exists(p):
        loaded_screenshots.append(Image.open(p).convert("RGBA"))
    else:
        # fallback
        loaded_screenshots.append(Image.new("RGBA", (1080, 1920), (20, 35, 55, 255)))

scenes = [
    {
        "title": "SECURE PASSWORD MANAGER",
        "subtitle": "Next-Gen 100% Offline Vault for Android",
        "badge": "AES-256 ZERO-KNOWLEDGE",
        "points": [
            "Hardware-Backed Android KeyStore",
            "Zero Cloud Leakage • 100% Private",
            "Biometric Fingerprint & Master PIN Unlock"
        ],
        "screen_idx": 0,
        "duration_sec": 5.0
    },
    {
        "title": "EXECUTIVE DASHBOARD",
        "subtitle": "Organize All Credentials with Ease",
        "badge": "ELEGANT & INTUITIVE",
        "points": [
            "Frosted Glass Slate Design",
            "Instant Search & Quick Copy to Clipboard",
            "Separate Categories: Logins, Cards & Notes"
        ],
        "screen_idx": 1,
        "duration_sec": 5.0
    },
    {
        "title": "SMART PASSWORD ENGINE",
        "subtitle": "Custom Range from 4 to 15 Characters",
        "badge": "ENTROPY GENERATOR",
        "points": [
            "Digits-Only Mode for Quick PINs / OTPs",
            "Alphanumeric & Special Symbol Rules",
            "Real-time 128-bit Password Strength Gauge"
        ],
        "screen_idx": 2,
        "duration_sec": 5.0
    },
    {
        "title": "LOCAL WI-FI CHROME SYNC",
        "subtitle": "Access Vault on PC Without Internet",
        "badge": "DYNAMIC RANDOM PORT",
        "points": [
            "Auto-generates Random Safe Port per Session",
            "Live Bidirectional Sync between Phone & PC",
            "Instant QR Code Pairing & Zero Data Leakage"
        ],
        "screen_idx": 3,
        "duration_sec": 5.0
    },
    {
        "title": "SAFE SCREEN ATTACHMENTS",
        "subtitle": "Store Sensitive Recovery Proofs",
        "badge": "PRIVACY GUARD",
        "points": [
            "Attach Recovery Code Screenshots Securely",
            "FLAG_SECURE Blocks External Screen Recording",
            "Automatic 30-Sec Clipboard Memory Cleaner"
        ],
        "screen_idx": 4,
        "duration_sec": 5.0
    },
    {
        "title": "MILITARY-GRADE PROTECTION",
        "subtitle": "Android 16 (API 36) Ready",
        "badge": "100% NATIVE JAVA",
        "points": [
            "SQLCipher 256-bit Offline Database",
            "No Ads, No Third-Party Trackers, No Telemetry",
            "Download Now & Keep Your Passwords Safe"
        ],
        "screen_idx": 5,
        "duration_sec": 5.0
    }
]

total_duration = sum(s["duration_sec"] for s in scenes)
total_frames = int(total_duration * FPS)

print(f"Creating video: {WIDTH}x{HEIGHT} at {FPS}fps, total duration: {total_duration:.1f}s ({total_frames} frames)...")

fourcc = cv2.VideoWriter_fourcc(*'mp4v')
out = cv2.VideoWriter(OUTPUT_VIDEO_PATH, fourcc, float(FPS), (WIDTH, HEIGHT))

if not out.isOpened():
    raise RuntimeError("Could not open VideoWriter with mp4v codec!")

font_badge = get_font(24, bold=True)
font_title = get_font(52, bold=True)
font_sub = get_font(28, bold=False)
font_point = get_font(26, bold=True)
font_brand = get_font(20, bold=True)

# Cache pre-resized phone screens to speed up frame generation
phone_target_h = 860
phone_target_w = int(1080 * (phone_target_h / 1920)) # ~ 483 px
resized_screens = [sc.resize((phone_target_w, phone_target_h), Image.Resampling.LANCZOS) for sc in loaded_screenshots]

current_frame = 0
for scene_idx, scene in enumerate(scenes):
    scene_frames = int(scene["duration_sec"] * FPS)
    sc_img = resized_screens[scene["screen_idx"]]

    for f in range(scene_frames):
        # Progress inside current scene (0.0 to 1.0)
        t = f / float(scene_frames)
        
        # Fade in alpha for text (0.0 to 1.0 in first 15 frames)
        fade_in = min(1.0, f / 15.0)

        # Base Frame Image
        frame_pil = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 255))
        draw = ImageDraw.Draw(frame_pil)

        # 1. Background Gradient (Royal Navy -> Deep Obsidian Slate)
        # Dynamic subtle pulse
        pulse = math.sin(current_frame * 0.05) * 4
        for y in range(0, HEIGHT, 4):
            ratio = y / float(HEIGHT)
            r = int(7 + (18 - 7) * ratio)
            g = int(18 + (34 - 18) * ratio + pulse)
            b = int(36 + (62 - 36) * ratio + pulse)
            draw.rectangle([(0, y), (WIDTH, y + 4)], fill=(r, g, b, 255))

        # Ambient glow behind phone
        glow = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
        g_draw = ImageDraw.Draw(glow)
        glow_x = int(380 + math.sin(current_frame * 0.04) * 20)
        g_draw.ellipse([glow_x - 300, 200, glow_x + 300, 880], fill=(0, 130, 255, 30))
        g_draw.ellipse([1200, 200, 1800, 800], fill=(245, 166, 35, 18))
        glow = glow.filter(ImageFilter.GaussianBlur(80))
        frame_pil = Image.alpha_composite(frame_pil, glow)
        draw = ImageDraw.Draw(frame_pil)

        # 2. Draw Phone Mockup on Left Side (with gentle floating bounce)
        phone_y_offset = int(math.sin((current_frame + scene_idx * 30) * 0.05) * 8)
        px = 180
        py = 110 + phone_y_offset

        # Phone frame
        draw.rounded_rectangle([px - 14, py - 14, px + phone_target_w + 14, py + phone_target_h + 14], radius=38, fill=(12, 22, 36), outline=(60, 95, 145), width=4)
        
        # Paste phone screenshot
        frame_pil.paste(sc_img, (px, py), sc_img)
        draw = ImageDraw.Draw(frame_pil)

        # Glass shine over phone
        draw.line([(px, py), (px + phone_target_w, py + 120)], fill=(255, 255, 255, 25), width=2)

        # 3. Right Side: Typography & Details
        rx = 760
        ty = 180

        # Security Pill Badge
        badge_alpha = int(fade_in * 255)
        draw.rounded_rectangle([rx, ty, rx + 380, ty + 50], radius=15, fill=(0, 110, 240, int(fade_in * 50)), outline=(0, 195, 255, badge_alpha), width=2)
        draw.text((rx + 25, ty + 10), f"🛡️  {scene['badge']}", font=font_badge, fill=(0, 220, 255, badge_alpha))

        # Title & Subtitle
        draw.text((rx, ty + 75), scene["title"], font=font_title, fill=(255, 255, 255, badge_alpha))
        draw.text((rx, ty + 150), scene["subtitle"], font=font_sub, fill=(175, 205, 240, badge_alpha))

        # Feature bullet cards
        card_y = ty + 230
        for p_idx, pt in enumerate(scene["points"]):
            pt_fade = min(1.0, max(0.0, (f - p_idx * 8) / 12.0))
            if pt_fade > 0:
                cur_y = card_y + p_idx * 95
                draw.rounded_rectangle([rx, cur_y, rx + 980, cur_y + 75], radius=20, fill=(18, 33, 54, int(pt_fade * 240)), outline=(35, 65, 105, int(pt_fade * 255)), width=1)
                # Glowing Checkmark Icon
                draw.ellipse([rx + 25, cur_y + 18, rx + 65, cur_y + 58], fill=(0, 140, 255, int(pt_fade * 60)), outline=(0, 200, 255, int(pt_fade * 255)), width=2)
                draw.text((rx + 36, cur_y + 22), "✔", font=font_point, fill=(80, 240, 170, int(pt_fade * 255)))
                draw.text((rx + 85, cur_y + 20), pt, font=font_point, fill=(240, 248, 255, int(pt_fade * 255)))

        # Top App Header
        draw.text((80, 45), "🔒 SECURE PASSWORD MANAGER", font=font_brand, fill=(0, 195, 255, 200))
        draw.text((WIDTH - 380, 45), "OFFICIAL APP DEMO • 1080P", font=font_brand, fill=(140, 175, 210, 180))

        # Bottom Progress Bar
        total_p = current_frame / float(total_frames)
        draw.rectangle([(0, HEIGHT - 10), (WIDTH, HEIGHT)], fill=(15, 25, 40))
        draw.rectangle([(0, HEIGHT - 10), (int(WIDTH * total_p), HEIGHT)], fill=(0, 195, 255))

        # Convert PIL RGBA -> OpenCV BGR
        rgb_frame = frame_pil.convert("RGB")
        bgr_frame = cv2.cvtColor(np.array(rgb_frame), cv2.COLOR_RGB2BGR)

        out.write(bgr_frame)
        current_frame += 1

    print(f"Scene {scene_idx + 1}/{len(scenes)} rendered.")

out.release()
print(f"\nDemo video generated successfully!")
print(f"File location: {OUTPUT_VIDEO_PATH}")
print(f"File size: {os.path.getsize(OUTPUT_VIDEO_PATH) / (1024*1024):.2f} MB")
