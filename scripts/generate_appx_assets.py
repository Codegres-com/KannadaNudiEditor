from PIL import Image, ImageDraw
import os

def create_tile(source_img, target_width, target_height, padding_ratio=0.15, bg_color=None):
    """
    Creates a tile image with centered source_img with proper padding.
    """
    tile = Image.new("RGBA", (target_width, target_height), bg_color or (0, 0, 0, 0))
    
    # Calculate available space
    avail_w = int(target_width * (1.0 - 2 * padding_ratio))
    avail_h = int(target_height * (1.0 - 2 * padding_ratio))
    
    # Scale source while maintaining aspect ratio
    src_w, src_h = source_img.size
    scale = min(avail_w / src_w, avail_h / src_h)
    new_w = max(1, int(src_w * scale))
    new_h = max(1, int(src_h * scale))
    
    resized_src = source_img.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    # Center image
    offset_x = (target_width - new_w) // 2
    offset_y = (target_height - new_h) // 2
    
    tile.paste(resized_src, (offset_x, offset_y), resized_src)
    return tile

def main():
    root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    src_logo_path = os.path.join(root_dir, "KannadaNudiWeb", "wwwroot", "nudi_logo.png")
    out_dir = os.path.join(root_dir, "KannadaNudiWindows", "build", "appx")
    
    os.makedirs(out_dir, exist_ok=True)
    
    source = Image.open(src_logo_path).convert("RGBA")
    print(f"Loaded source logo from {src_logo_path} ({source.size[0]}x{source.size[1]})")
    
    # Dimensions required for Windows AppX / Store
    assets = {
        "StoreLogo.png": (50, 50, 0.08),
        "StoreLogo.scale-100.png": (50, 50, 0.08),
        "StoreLogo.scale-125.png": (63, 63, 0.08),
        "StoreLogo.scale-150.png": (75, 75, 0.08),
        "StoreLogo.scale-200.png": (100, 100, 0.08),
        "StoreLogo.scale-400.png": (200, 200, 0.08),
        
        "Square44x44Logo.png": (44, 44, 0.1),
        "Square44x44Logo.scale-100.png": (44, 44, 0.1),
        "Square44x44Logo.scale-125.png": (55, 55, 0.1),
        "Square44x44Logo.scale-150.png": (66, 66, 0.1),
        "Square44x44Logo.scale-200.png": (88, 88, 0.1),
        "Square44x44Logo.scale-400.png": (176, 176, 0.1),
        "Square44x44Logo.targetsize-16.png": (16, 16, 0.05),
        "Square44x44Logo.targetsize-24.png": (24, 24, 0.05),
        "Square44x44Logo.targetsize-32.png": (32, 32, 0.05),
        "Square44x44Logo.targetsize-44.png": (44, 44, 0.05),
        "Square44x44Logo.targetsize-48.png": (48, 48, 0.05),
        "Square44x44Logo.targetsize-256.png": (256, 256, 0.05),
        "Square44x44Logo.targetsize-44_altform-unplated.png": (44, 44, 0.05),
        "Square44x44Logo.targetsize-256_altform-unplated.png": (256, 256, 0.05),
        
        "Square71x71Logo.png": (71, 71, 0.12),
        "Square71x71Logo.scale-100.png": (71, 71, 0.12),
        "Square71x71Logo.scale-125.png": (89, 89, 0.12),
        "Square71x71Logo.scale-150.png": (107, 107, 0.12),
        "Square71x71Logo.scale-200.png": (142, 142, 0.12),
        "Square71x71Logo.scale-400.png": (284, 284, 0.12),
        
        "Square150x150Logo.png": (150, 150, 0.15),
        "Square150x150Logo.scale-100.png": (150, 150, 0.15),
        "Square150x150Logo.scale-125.png": (188, 188, 0.15),
        "Square150x150Logo.scale-150.png": (225, 225, 0.15),
        "Square150x150Logo.scale-200.png": (300, 300, 0.15),
        "Square150x150Logo.scale-400.png": (600, 600, 0.15),
        
        "Square310x310Logo.png": (310, 310, 0.15),
        "Square310x310Logo.scale-100.png": (310, 310, 0.15),
        "Square310x310Logo.scale-125.png": (388, 388, 0.15),
        "Square310x310Logo.scale-150.png": (465, 465, 0.15),
        "Square310x310Logo.scale-200.png": (620, 620, 0.15),
        "Square310x310Logo.scale-400.png": (1240, 1240, 0.15),
        
        "Wide310x150Logo.png": (310, 150, 0.15),
        "Wide310x150Logo.scale-100.png": (310, 150, 0.15),
        "Wide310x150Logo.scale-125.png": (388, 188, 0.15),
        "Wide310x150Logo.scale-150.png": (465, 225, 0.15),
        "Wide310x150Logo.scale-200.png": (620, 300, 0.15),
        "Wide310x150Logo.scale-400.png": (1240, 600, 0.15),
        
        "SplashScreen.png": (620, 300, 0.25),
        "SplashScreen.scale-100.png": (620, 300, 0.25),
        "SplashScreen.scale-125.png": (775, 375, 0.25),
        "SplashScreen.scale-150.png": (930, 465, 0.25),
        "SplashScreen.scale-200.png": (1240, 600, 0.25),
        "SplashScreen.scale-400.png": (2480, 1200, 0.25),
        
        "BadgeLogo.png": (24, 24, 0.05),
        "BadgeLogo.scale-100.png": (24, 24, 0.05),
        "BadgeLogo.scale-125.png": (30, 30, 0.05),
        "BadgeLogo.scale-150.png": (36, 36, 0.05),
        "BadgeLogo.scale-200.png": (48, 48, 0.05),
        "BadgeLogo.scale-400.png": (96, 96, 0.05),
    }
    
    for filename, (w, h, pad) in assets.items():
        tile = create_tile(source, w, h, padding_ratio=pad)
        dest_path = os.path.join(out_dir, filename)
        tile.save(dest_path, "PNG")
        print(f"Generated {filename} ({w}x{h})")
        
    print(f"\nAll {len(assets)} AppX tile assets generated successfully in {out_dir}")

if __name__ == "__main__":
    main()
