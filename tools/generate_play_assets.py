import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ASSETS_DIR = r"D:\Android\AndroidStudioProjects6\PasswordManager\play_store_assets"
SCREENSHOTS_DIR = os.path.join(ASSETS_DIR, "screenshots")
os.makedirs(SCREENSHOTS_DIR, exist_ok=True)

# Font loading helper
def get_font(size, bold=False):
    font_path = r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf"
    try:
        return ImageFont.truetype(font_path, size)
    except Exception:
        return ImageFont.load_default()

def draw_vertical_gradient(draw, width, height, top_color, bottom_color):
    for y in range(height):
        r = int(top_color[0] + (bottom_color[0] - top_color[0]) * (y / height))
        g = int(top_color[1] + (bottom_color[1] - top_color[1]) * (y / height))
        b = int(top_color[2] + (bottom_color[2] - top_color[2]) * (y / height))
        draw.line([(0, y), (width, y)], fill=(r, g, b))

# -------------------------------------------------------------
# 1. APP ICON (512 x 512)
# -------------------------------------------------------------
def generate_app_icon():
    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Gradient background: Deep rich navy blue #09172B -> #1A365D
    for y in range(512):
        ratio = y / 512.0
        r = int(9 + (26 - 9) * ratio)
        g = int(23 + (54 - 23) * ratio)
        b = int(43 + (93 - 43) * ratio)
        draw.line([(0, y), (512, y)], fill=(r, g, b, 255))

    # Ambient glow circle behind shield
    glow = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse([100, 100, 412, 412], fill=(0, 122, 255, 60))
    glow = glow.filter(ImageFilter.GaussianBlur(40))
    img = Image.alpha_composite(img, glow)
    draw = ImageDraw.Draw(img)

    # Outer Shield (Cobalt blue with electric cyan highlight)
    # Shield shape points
    shield_pts = [
        (256, 75),   # Top center peak
        (390, 115),  # Top right
        (380, 290),  # Right curved flank
        (256, 425),  # Bottom point
        (132, 290),  # Left curved flank
        (122, 115)   # Top left
    ]
    draw.polygon(shield_pts, fill=(15, 82, 186, 255)) # Cobalt blue
    
    # Inner Shield (Dark Slate #10223A)
    inner_shield = [
        (256, 95),
        (370, 130),
        (360, 280),
        (256, 400),
        (152, 280),
        (142, 130)
    ]
    draw.polygon(inner_shield, fill=(13, 27, 46, 255))

    # Golden Cyber Key & Padlock in center
    # Padlock Shackle
    draw.arc([210, 160, 302, 260], start=180, end=0, fill=(245, 166, 35, 255), width=18)
    
    # Padlock Body
    draw.rounded_rectangle([190, 220, 322, 335], radius=24, fill=(245, 166, 35, 255))
    draw.rounded_rectangle([200, 230, 312, 325], radius=18, fill=(255, 194, 51, 255))

    # Center Keyhole
    draw.ellipse([241, 252, 271, 282], fill=(20, 30, 45, 255))
    draw.polygon([(250, 275), (262, 275), (266, 305), (246, 305)], fill=(20, 30, 45, 255))

    # Shield highlight accent line
    draw.line([(256, 95), (256, 400)], fill=(0, 195, 255, 90), width=4)

    # Final RGB conversion (Google Play requires no alpha)
    final_img = img.convert("RGB")
    final_path = os.path.join(ASSETS_DIR, "icon_512x512.png")
    final_img.save(final_path, "PNG")
    print(f"Icon saved: {final_path}")

# -------------------------------------------------------------
# 2. FEATURE GRAPHIC (1024 x 500)
# -------------------------------------------------------------
def generate_feature_graphic():
    img = Image.new("RGBA", (1024, 500), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Background gradient: Slate / Navy futuristic theme #06101E to #142846
    for y in range(500):
        ratio = y / 500.0
        r = int(6 + (20 - 6) * ratio)
        g = int(16 + (40 - 16) * ratio)
        b = int(30 + (70 - 30) * ratio)
        draw.line([(0, y), (1024, y)], fill=(r, g, b, 255))

    # High-tech ambient background grid / rings
    glow = Image.new("RGBA", (1024, 500), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    g_draw.ellipse([650, 50, 1050, 450], fill=(0, 122, 255, 45))
    g_draw.ellipse([80, 200, 380, 500], fill=(245, 166, 35, 25))
    glow = glow.filter(ImageFilter.GaussianBlur(60))
    img = Image.alpha_composite(img, glow)
    draw = ImageDraw.Draw(img)

    # Left Column: Typography & Badges
    font_badge = get_font(18, bold=True)
    font_title = get_font(46, bold=True)
    font_sub = get_font(22, bold=False)
    font_pill = get_font(16, bold=True)

    # Security Tag Pill
    draw.rounded_rectangle([60, 48, 280, 84], radius=18, fill=(0, 122, 255, 45), outline=(0, 170, 255, 180), width=2)
    draw.text((78, 55), "AES-256 ZERO CLOUD", font=font_badge, fill=(0, 210, 255))

    # Headline
    draw.text((60, 102), "Secure Password", font=font_title, fill=(255, 255, 255))
    draw.text((60, 155), "Manager & Vault", font=font_title, fill=(245, 166, 35))

    # Subtitle
    draw.text((60, 222), "Offline Encrypted Storage • Wi-Fi Web Sync • Anti-Theft", font=font_sub, fill=(185, 205, 230))

    # Feature badges pills
    pills = [
        ("⚡ Fingerprint Biometric Login", (0, 100, 200, 50), (0, 180, 255)),
        ("🌐 Chrome PC Sync (Auto-Port)", (30, 80, 160, 50), (120, 210, 255)),
        ("🔐 Smart Password Generator (4-15)", (180, 120, 10, 50), (255, 195, 60)),
        ("🛡️ Hardware Keystore + No Ads", (20, 140, 90, 50), (60, 230, 150))
    ]

    py = 280
    for text, bg_col, text_col in pills:
        draw.rounded_rectangle([60, py, 460, py + 38], radius=12, fill=bg_col, outline=text_col, width=1)
        draw.text((75, py + 8), text, font=font_pill, fill=text_col)
        py += 48

    # Right Column: Phone Mockup Frame (Frosted Glass Style)
    phone_x, phone_y, phone_w, phone_h = 630, 50, 310, 420
    # Phone shadow
    p_shadow = Image.new("RGBA", (1024, 500), (0, 0, 0, 0))
    ps_draw = ImageDraw.Draw(p_shadow)
    ps_draw.rounded_rectangle([phone_x - 10, phone_y + 10, phone_x + phone_w + 10, phone_y + phone_h + 20], radius=32, fill=(0, 0, 0, 140))
    p_shadow = p_shadow.filter(ImageFilter.GaussianBlur(18))
    img = Image.alpha_composite(img, p_shadow)
    draw = ImageDraw.Draw(img)

    # Phone Body Frame
    draw.rounded_rectangle([phone_x, phone_y, phone_x + phone_w, phone_y + phone_h], radius=30, fill=(17, 30, 49), outline=(50, 85, 130), width=3)

    # Phone Screen Content inside mockup
    # Top Status Bar
    draw.text((phone_x + 25, phone_y + 15), "9:41", font=get_font(14, bold=True), fill=(200, 220, 240))
    draw.text((phone_x + phone_w - 75, phone_y + 15), "5G  89%", font=get_font(13, bold=True), fill=(200, 220, 240))

    # Mock Dashboard Header
    draw.text((phone_x + 25, phone_y + 45), "My Password Vault", font=get_font(18, bold=True), fill=(255, 255, 255))
    draw.text((phone_x + 25, phone_y + 68), "All entries encrypted locally", font=get_font(12), fill=(130, 165, 200))

    # Security Card inside phone
    draw.rounded_rectangle([phone_x + 20, phone_y + 95, phone_x + phone_w - 20, phone_y + 175], radius=16, fill=(26, 48, 77), outline=(0, 140, 255), width=1)
    draw.text((phone_x + 35, phone_y + 108), "SECURITY HEALTH", font=get_font(11, bold=True), fill=(0, 200, 255))
    draw.text((phone_x + 35, phone_y + 128), "100% Protected", font=get_font(20, bold=True), fill=(255, 255, 255))
    draw.text((phone_x + 35, phone_y + 152), "Offline • SQLCipher AES-256", font=get_font(11), fill=(140, 185, 230))

    # Mock Vault List Items inside phone
    items = [
        ("Google Workspace", "user@company.com", "••••••••••••", (220, 70, 60)),
        ("HDFC Bank NetBanking", "hdfc_corp_99", "••••••••", (40, 100, 200)),
        ("GitHub Developer", "dev_master", "••••••••••••••••", (40, 40, 40))
    ]
    iy = phone_y + 190
    for title, user, pwd, col in items:
        draw.rounded_rectangle([phone_x + 20, iy, phone_x + phone_w - 20, iy + 62], radius=14, fill=(21, 38, 62))
        # Icon circle
        draw.ellipse([phone_x + 30, iy + 14, phone_x + 64, iy + 48], fill=col)
        draw.text((phone_x + 42, iy + 19), title[0], font=get_font(16, bold=True), fill=(255, 255, 255))
        # Title and sub
        draw.text((phone_x + 75, iy + 12), title, font=get_font(13, bold=True), fill=(255, 255, 255))
        draw.text((phone_x + 75, iy + 34), f"{user}  {pwd}", font=get_font(11), fill=(130, 160, 195))
        iy += 72

    final_img = img.convert("RGB")
    final_path = os.path.join(ASSETS_DIR, "feature_graphic_1024x500.png")
    final_img.save(final_path, "PNG")
    print(f"Feature graphic saved: {final_path}")

# -------------------------------------------------------------
# 3. GOOGLE PLAY PHONE SCREENSHOTS (1080 x 1920)
# -------------------------------------------------------------
def generate_screenshot_base(headline, subheadline, badge_text="SECURE & PRIVATE"):
    img = Image.new("RGBA", (1080, 1920), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Vertical gradient: Rich deep navy to slate
    for y in range(1920):
        ratio = y / 1920.0
        r = int(8 + (22 - 8) * ratio)
        g = int(20 + (45 - 20) * ratio)
        b = int(38 + (75 - 38) * ratio)
        draw.line([(0, y), (1080, y)], fill=(r, g, b, 255))

    # Glow accents
    glow = Image.new("RGBA", (1080, 1920), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow)
    g_draw.ellipse([150, 100, 930, 880], fill=(0, 122, 255, 35))
    g_draw.ellipse([400, 1100, 1100, 1800], fill=(245, 166, 35, 22))
    glow = glow.filter(ImageFilter.GaussianBlur(100))
    img = Image.alpha_composite(img, glow)
    draw = ImageDraw.Draw(img)

    # Top Header section
    font_badge = get_font(30, bold=True)
    font_h1 = get_font(68, bold=True)
    font_h2 = get_font(38, bold=False)

    # Badge
    draw.rounded_rectangle([80, 90, 480, 150], radius=28, fill=(0, 122, 255, 45), outline=(0, 180, 255, 180), width=2)
    draw.text((115, 102), badge_text, font=font_badge, fill=(0, 215, 255))

    # Headline & Subheadline
    draw.text((80, 175), headline, font=font_h1, fill=(255, 255, 255))
    draw.text((80, 260), subheadline, font=font_h2, fill=(175, 205, 235))

    return img

def add_phone_frame(base_img, draw_content_fn):
    # Mockup phone centered at bottom
    # Dimensions: 880 w x 1450 h, centered at x=100, y=380
    px, py, pw, ph = 100, 360, 880, 1460

    # Device Shadow
    shadow = Image.new("RGBA", (1080, 1920), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_draw.rounded_rectangle([px - 20, py + 20, px + pw + 20, py + ph + 30], radius=65, fill=(0, 0, 0, 160))
    shadow = shadow.filter(ImageFilter.GaussianBlur(35))
    base_img = Image.alpha_composite(base_img, shadow)

    # Phone Outer Bezel (Titanium Slate)
    draw = ImageDraw.Draw(base_img)
    draw.rounded_rectangle([px, py, px + pw, py + ph], radius=60, fill=(14, 24, 38), outline=(55, 88, 128), width=6)

    # Phone Inner Screen Area
    sx, sy, sw, sh = px + 18, py + 18, pw - 36, ph - 36
    draw.rounded_rectangle([sx, sy, sx + sw, sy + sh], radius=46, fill=(10, 18, 30))

    # Dynamic Island / Camera punch-hole
    draw.rounded_rectangle([sx + sw // 2 - 90, sy + 18, sx + sw // 2 + 90, sy + 52], radius=17, fill=(0, 0, 0))
    draw.ellipse([sx + sw // 2 + 45, sy + 25, sx + sw // 2 + 65, sy + 45], fill=(20, 20, 30))

    # Status Bar
    draw.text((sx + 50, sy + 26), "9:41", font=get_font(24, bold=True), fill=(220, 235, 250))
    draw.text((sx + sw - 140, sy + 26), "5G  100%", font=get_font(22, bold=True), fill=(220, 235, 250))

    # Custom App Screen Rendering
    draw_content_fn(draw, sx, sy, sw, sh)

    return base_img

# --- SCREENSHOT 1: Biometric Unlock & Security ---
def render_screen_1_content(draw, sx, sy, sw, sh):
    # Centered Biometric Authentication View
    cx = sx + sw // 2
    draw.text((cx - 190, sy + 180), "Welcome Back", font=get_font(52, bold=True), fill=(255, 255, 255))
    draw.text((cx - 180, sy + 250), "Authenticate to unlock your vault", font=get_font(24), fill=(140, 175, 210))

    # Big Glowing Biometric Fingerprint Circle
    draw.ellipse([cx - 130, sy + 380, cx + 130, sy + 640], fill=(18, 38, 64), outline=(0, 140, 255), width=3)
    # Fingerprint arcs
    for radius in [35, 60, 85, 110]:
        draw.arc([cx - radius, sy + 510 - radius, cx + radius, sy + 510 + radius], start=200, end=340, fill=(0, 200, 255), width=6)
        draw.arc([cx - radius, sy + 510 - radius + 10, cx + radius, sy + 510 + radius + 10], start=30, end=150, fill=(0, 200, 255), width=6)

    # Master Password PIN Option Button
    draw.rounded_rectangle([cx - 260, sy + 730, cx + 260, sy + 810], radius=24, fill=(0, 102, 238))
    draw.text((cx - 150, sy + 748), "🔒 Unlock with Master PIN", font=get_font(28, bold=True), fill=(255, 255, 255))

    # Features Pill Row
    badges = ["Hardware KeyStore", "Anti-Brute-Force", "Zero Cloud Leakage"]
    by = sy + 900
    for b in badges:
        draw.rounded_rectangle([cx - 240, by, cx + 240, by + 60], radius=16, fill=(19, 34, 54), outline=(45, 75, 115), width=1)
        draw.text((cx - 180, by + 16), f"✔  {b}", font=get_font(22, bold=True), fill=(80, 210, 160))
        by += 80

# --- SCREENSHOT 2: Executive Dashboard ---
def render_screen_2_content(draw, sx, sy, sw, sh):
    # Top Bar: Avatar, Greeting & Menu
    draw.ellipse([sx + 40, sy + 90, sx + 110, sy + 160], fill=(245, 166, 35))
    draw.text((sx + 60, sy + 105), "JD", font=get_font(30, bold=True), fill=(20, 20, 20))
    draw.text((sx + 130, sy + 95), "Hello, Security Vault", font=get_font(34, bold=True), fill=(255, 255, 255))
    draw.text((sx + 130, sy + 138), "AES-256 Offline Database", font=get_font(20), fill=(130, 165, 200))

    # Search Bar
    draw.rounded_rectangle([sx + 40, sy + 190, sx + sw - 40, sy + 265], radius=22, fill=(20, 36, 58), outline=(45, 75, 115), width=1)
    draw.text((sx + 75, sy + 212), "🔍 Search passwords, cards, notes...", font=get_font(24), fill=(130, 160, 195))

    # Frosted Slate Health & Total Records Card
    draw.rounded_rectangle([sx + 40, sy + 290, sx + sw - 40, sy + 460], radius=28, fill=(25, 45, 72), outline=(0, 130, 255), width=2)
    draw.text((sx + 70, sy + 320), "TOTAL ENCRYPTED RECORDS", font=get_font(18, bold=True), fill=(0, 200, 255))
    draw.text((sx + 70, sy + 355), "42 Items Saved", font=get_font(42, bold=True), fill=(255, 255, 255))
    draw.text((sx + 70, sy + 412), "• 28 Logins  • 8 Bank Cards  • 6 Private Keys", font=get_font(20), fill=(170, 200, 230))

    # Filter Tabs
    tabs = ["All Records", "Recent", "Favorites", "Web Sync"]
    tx = sx + 40
    for i, t in enumerate(tabs):
        bg = (0, 110, 240) if i == 0 else (21, 38, 60)
        draw.rounded_rectangle([tx, sy + 490, tx + 180, sy + 545], radius=16, fill=bg)
        draw.text((tx + 25, sy + 505), t, font=get_font(20, bold=(i == 0)), fill=(255, 255, 255))
        tx += 195

    # Credential List Items
    items = [
        ("Google Account", "alex.smith@gmail.com", "••••••••••••", (230, 75, 60)),
        ("Amazon Web Services", "aws_root_admin", "••••••••••••••••", (255, 153, 0)),
        ("HDFC Platinum Debit Card", "Card ending in 4028", "CVV •••", (0, 102, 204)),
        ("Meta Developer Portal", "meta_dev_corp", "••••••••••••", (24, 119, 242)),
        ("Netflix Family Vault", "alex_netflix@home", "••••••••••", (229, 9, 20)),
    ]
    cy = sy + 575
    for title, user, pwd, col in items:
        draw.rounded_rectangle([sx + 40, cy, sx + sw - 40, cy + 125], radius=22, fill=(18, 33, 54), outline=(35, 60, 95), width=1)
        # Category Icon
        draw.ellipse([sx + 65, cy + 25, sx + 140, cy + 100], fill=col)
        draw.text((sx + 90, cy + 40), title[0], font=get_font(34, bold=True), fill=(255, 255, 255))
        # Details
        draw.text((sx + 165, cy + 25), title, font=get_font(26, bold=True), fill=(255, 255, 255))
        draw.text((sx + 165, cy + 62), user, font=get_font(20), fill=(140, 175, 210))
        draw.text((sx + 165, cy + 88), pwd, font=get_font(20, bold=True), fill=(0, 210, 255))
        # Copy button icon
        draw.rounded_rectangle([sx + sw - 120, cy + 35, sx + sw - 65, cy + 90], radius=14, fill=(28, 52, 82))
        draw.text((sx + sw - 105, cy + 47), "📋", font=get_font(22), fill=(255, 255, 255))
        cy += 145

# --- SCREENSHOT 3: Password Generator ---
def render_screen_3_content(draw, sx, sy, sw, sh):
    draw.text((sx + 40, sy + 100), "Smart Password Generator", font=get_font(42, bold=True), fill=(255, 255, 255))
    draw.text((sx + 40, sy + 155), "Custom length (4 to 15) & strict format rules", font=get_font(22), fill=(140, 175, 210))

    # Generated Password Box
    draw.rounded_rectangle([sx + 40, sy + 210, sx + sw - 40, sy + 360], radius=26, fill=(24, 44, 70), outline=(0, 160, 255), width=2)
    draw.text((sx + 70, sy + 240), "GENERATED SECURE PASSWORD", font=get_font(18, bold=True), fill=(0, 200, 255))
    draw.text((sx + 70, sy + 280), "9482015384", font=get_font(46, bold=True), fill=(245, 166, 35))
    draw.text((sx + sw - 180, sy + 285), "COPY", font=get_font(22, bold=True), fill=(0, 210, 255))

    # Entropy Strength Indicator
    draw.text((sx + 40, sy + 395), "Password Strength: Maximum (128-bit Entropy)", font=get_font(20, bold=True), fill=(255, 255, 255))
    draw.rounded_rectangle([sx + 40, sy + 430, sx + sw - 40, sy + 450], radius=10, fill=(22, 38, 60))
    draw.rounded_rectangle([sx + 40, sy + 430, sx + sw - 100, sy + 450], radius=10, fill=(50, 215, 130))

    # Length Slider (4 to 15 range)
    draw.rounded_rectangle([sx + 40, sy + 480, sx + sw - 40, sy + 620], radius=24, fill=(18, 33, 54), outline=(35, 60, 95), width=1)
    draw.text((sx + 70, sy + 510), "PASSWORD LENGTH (MIN 4 - MAX 15)", font=get_font(20, bold=True), fill=(200, 220, 245))
    draw.text((sx + 70, sy + 550), "Selected Length: 10 Digits", font=get_font(26, bold=True), fill=(0, 210, 255))
    # Slider bar
    draw.rounded_rectangle([sx + 70, sy + 595, sx + sw - 70, sy + 605], radius=5, fill=(35, 60, 95))
    draw.rounded_rectangle([sx + 70, sy + 595, sx + 500, sy + 605], radius=5, fill=(0, 122, 255))
    draw.ellipse([sx + 485, sy + 585, sx + 520, sy + 615], fill=(255, 255, 255))

    # Toggle Options
    options = [
        ("Only Digits (PIN / OTP Mode)", True),
        ("Include Capital Letters (A-Z)", False),
        ("Include Lowercase Letters (a-z)", False),
        ("Include Special Characters (!@#$)", False),
    ]
    oy = sy + 655
    for opt_title, is_on in options:
        draw.rounded_rectangle([sx + 40, oy, sx + sw - 40, oy + 85], radius=20, fill=(19, 35, 56), outline=(35, 60, 95), width=1)
        draw.text((sx + 70, oy + 28), opt_title, font=get_font(22, bold=True), fill=(240, 245, 255))
        # Switch toggle
        sw_bg = (0, 122, 255) if is_on else (40, 58, 85)
        draw.rounded_rectangle([sx + sw - 130, oy + 22, sx + sw - 70, oy + 62], radius=20, fill=sw_bg)
        knob_x = sx + sw - 95 if is_on else sx + sw - 125
        draw.ellipse([knob_x, oy + 25, knob_x + 32, oy + 59], fill=(255, 255, 255))
        oy += 105

    # Generate Button
    draw.rounded_rectangle([sx + 40, sy + 1150, sx + sw - 40, sy + 1245], radius=26, fill=(0, 102, 238))
    draw.text((sx + sw // 2 - 160, sy + 1180), "⚡ Generate New Password", font=get_font(28, bold=True), fill=(255, 255, 255))

# --- SCREENSHOT 4: Local Wi-Fi Browser Access ---
def render_screen_4_content(draw, sx, sy, sw, sh):
    draw.text((sx + 40, sy + 100), "Chrome PC Web Access", font=get_font(42, bold=True), fill=(255, 255, 255))
    draw.text((sx + 40, sy + 155), "Connect PC browser over local Wi-Fi without cloud", font=get_font(22), fill=(140, 175, 210))

    # Connection Active Card
    draw.rounded_rectangle([sx + 40, sy + 210, sx + sw - 40, sy + 380], radius=26, fill=(22, 45, 72), outline=(0, 200, 150), width=2)
    draw.text((sx + 70, sy + 235), "● WEB SERVER RUNNING", font=get_font(18, bold=True), fill=(60, 230, 160))
    draw.text((sx + 70, sy + 275), "http://192.168.1.5:49152", font=get_font(36, bold=True), fill=(255, 255, 255))
    draw.text((sx + 70, sy + 330), "Dynamic random port generated on every session", font=get_font(20), fill=(160, 200, 235))

    # QR Code Pairing Box
    draw.rounded_rectangle([sx + 40, sy + 410, sx + sw - 40, sy + 820], radius=26, fill=(18, 33, 54), outline=(35, 60, 95), width=1)
    draw.text((sx + sw // 2 - 180, sy + 440), "SCAN WITH CHROME ON PC", font=get_font(22, bold=True), fill=(0, 200, 255))
    # Simulated QR graphic
    draw.rounded_rectangle([sx + sw // 2 - 140, sy + 490, sx + sw // 2 + 140, sy + 770], radius=16, fill=(255, 255, 255))
    draw.rounded_rectangle([sx + sw // 2 - 120, sy + 510, sx + sw // 2 - 50, sy + 580], radius=8, fill=(0, 0, 0))
    draw.rounded_rectangle([sx + sw // 2 + 50, sy + 510, sx + sw // 2 + 120, sy + 580], radius=8, fill=(0, 0, 0))
    draw.rounded_rectangle([sx + sw // 2 - 120, sy + 680, sx + sw // 2 - 50, sy + 750], radius=8, fill=(0, 0, 0))
    draw.rectangle([sx + sw // 2 - 30, sy + 600, sx + sw // 2 + 30, sy + 660], fill=(0, 0, 0))

    # Features
    features = [
        "Bidirectional Real-Time Live Sync",
        "Save on PC -> Instant update on Phone",
        "Protected by 6-Digit Session Token",
        "Never sends data over the public internet"
    ]
    fy = sy + 860
    for f in features:
        draw.rounded_rectangle([sx + 40, fy, sx + sw - 40, fy + 70], radius=18, fill=(20, 36, 58))
        draw.text((sx + 70, fy + 20), f"⚡  {f}", font=get_font(22, bold=True), fill=(180, 215, 250))
        fy += 90

# --- SCREENSHOT 5: Secure Vault Records & Attachments ---
def render_screen_5_content(draw, sx, sy, sw, sh):
    draw.text((sx + 40, sy + 100), "Safe Screenshot Attachments", font=get_font(42, bold=True), fill=(255, 255, 255))
    draw.text((sx + 40, sy + 155), "Attach sensitive screenshots & documents securely", font=get_font(22), fill=(140, 175, 210))

    # Record Edit Card
    draw.rounded_rectangle([sx + 40, sy + 210, sx + sw - 40, sy + 700], radius=26, fill=(20, 36, 58), outline=(0, 140, 255), width=2)
    draw.text((sx + 70, sy + 240), "CREDENTIAL DETAILS", font=get_font(18, bold=True), fill=(0, 200, 255))
    
    # Input fields mock
    draw.rounded_rectangle([sx + 70, sy + 280, sx + sw - 70, sy + 350], radius=16, fill=(13, 24, 38))
    draw.text((sx + 95, sy + 300), "Account Title: Work Google Cloud Console", font=get_font(22), fill=(230, 240, 255))

    draw.rounded_rectangle([sx + 70, sy + 375, sx + sw - 70, sy + 445], radius=16, fill=(13, 24, 38))
    draw.text((sx + 95, sy + 395), "Username: devops@enterprise.internal", font=get_font(22), fill=(230, 240, 255))

    draw.rounded_rectangle([sx + 70, sy + 470, sx + sw - 70, sy + 540], radius=16, fill=(13, 24, 38))
    draw.text((sx + 95, sy + 490), "Password: ••••••••••••••••••••", font=get_font(22, bold=True), fill=(245, 166, 35))

    # Attachment box
    draw.rounded_rectangle([sx + 70, sy + 565, sx + sw - 70, sy + 665], radius=18, fill=(17, 30, 48), outline=(0, 195, 255), width=1)
    draw.text((sx + 100, sy + 588), "📷 Encrypted Screenshot Attached", font=get_font(22, bold=True), fill=(0, 215, 255))
    draw.text((sx + 100, sy + 623), "backup_recovery_codes.png (AES-256)", font=get_font(18), fill=(140, 175, 210))

    # Protection checklist
    items = [
        ("Screen Capture Prevention (FLAG_SECURE)", "Blocks external spy apps from recording"),
        ("Encrypted Storage on Device", "SQLCipher 256-bit database encryption"),
        ("Safe Clipboard Auto-Clear", "Clears copied password after 30 seconds"),
        ("100% Offline Architecture", "Zero telemetry, zero analytics tracking")
    ]
    py = sy + 735
    for title, desc in items:
        draw.rounded_rectangle([sx + 40, py, sx + sw - 40, py + 95], radius=20, fill=(18, 33, 54), outline=(35, 60, 95), width=1)
        draw.text((sx + 70, py + 20), f"🛡️  {title}", font=get_font(22, bold=True), fill=(255, 255, 255))
        draw.text((sx + 110, py + 55), desc, font=get_font(18), fill=(140, 175, 210))
        py += 115

# --- SCREENSHOT 6: Military Encryption Architecture ---
def render_screen_6_content(draw, sx, sy, sw, sh):
    draw.text((sx + 40, sy + 100), "Bank-Grade Encryption", font=get_font(42, bold=True), fill=(255, 255, 255))
    draw.text((sx + 40, sy + 155), "Your data never leaves your device unencrypted", font=get_font(22), fill=(140, 175, 210))

    # Shield graphic
    cx = sx + sw // 2
    draw.rounded_rectangle([cx - 200, sy + 210, cx + 200, sy + 520], radius=32, fill=(20, 40, 68), outline=(0, 150, 255), width=3)
    draw.text((cx - 85, sy + 250), "🛡️", font=get_font(70), fill=(245, 166, 35))
    draw.text((cx - 140, sy + 380), "AES-256 GCM", font=get_font(36, bold=True), fill=(255, 255, 255))
    draw.text((cx - 150, sy + 440), "Android Keystore Verified", font=get_font(22), fill=(0, 210, 255))

    # Pillars of security
    pillars = [
        ("Zero-Knowledge Vault", "Master password is never saved anywhere in plain text."),
        ("Dynamic Port Randomizer", "Prevents network sniffing with dynamic local ports."),
        ("No Third-Party SDKs", "Clean, lightweight, 100% Native Java architecture."),
        ("Android 16 (API 36) Ready", "Fully optimized for the latest Android OS security standard.")
    ]
    py = sy + 560
    for title, desc in pillars:
        draw.rounded_rectangle([sx + 40, py, sx + sw - 40, py + 105], radius=22, fill=(18, 33, 54), outline=(35, 60, 95), width=1)
        draw.text((sx + 70, py + 22), f"🔒  {title}", font=get_font(24, bold=True), fill=(255, 255, 255))
        draw.text((sx + 115, py + 60), desc, font=get_font(19), fill=(140, 175, 210))
        py += 125

def generate_screenshots():
    configs = [
        (
            "Biometric Authentication",
            "Instant Unlock via Fingerprint & Face",
            "MILITARY GRADE SECURITY",
            render_screen_1_content,
            "screenshot_1_biometric_login.png"
        ),
        (
            "Executive Vault Dashboard",
            "Organize Passwords, Cards & Private Notes",
            "INTUITIVE & BEAUTIFUL",
            render_screen_2_content,
            "screenshot_2_executive_dashboard.png"
        ),
        (
            "Smart Password Generator",
            "Generate Strong 4-15 Digit & Alpha Passwords",
            "CUSTOM PASSWORD ENGINE",
            render_screen_3_content,
            "screenshot_3_password_generator.png"
        ),
        (
            "Local Wi-Fi Chrome Sync",
            "Dynamic Random Port & Real-Time Syncing",
            "LOCAL NETWORK WEB ACCESS",
            render_screen_4_content,
            "screenshot_4_local_wifi_sync.png"
        ),
        (
            "Encrypted Screen Attachments",
            "Safeguard Screenshot Proofs & Private Notes",
            "COMPLETE PRIVACY",
            render_screen_5_content,
            "screenshot_5_secure_vault_records.png"
        ),
        (
            "Zero Cloud Architecture",
            "Offline SQLCipher AES-256 Encryption",
            "BANK GRADE PROTECTION",
            render_screen_6_content,
            "screenshot_6_military_encryption.png"
        )
    ]

    for idx, (h1, h2, badge, render_fn, fname) in enumerate(configs, 1):
        print(f"Generating screenshot {idx}/6: {fname}...")
        base = generate_screenshot_base(h1, h2, badge)
        framed = add_phone_frame(base, render_fn)
        final_img = framed.convert("RGB")
        out_path = os.path.join(SCREENSHOTS_DIR, fname)
        final_img.save(out_path, "PNG")
        print(f"Saved: {out_path}")

if __name__ == "__main__":
    print("Starting Play Store Assets Generation...")
    generate_app_icon()
    generate_feature_graphic()
    generate_screenshots()
    print("All Play Store Assets Generated Successfully!")
