"""
Bisaya Speak AI - アイコンリサイズスクリプト

ic_launcher_512.pngから各サイズのアイコンを生成します。
"""

from PIL import Image
import os

def resize_and_save_icons():
    """
    ic_launcher_512.pngから各サイズのアイコンを生成
    """
    # マスターアイコンを読み込み
    master_icon_path = "ic_launcher_512.png"
    
    if not os.path.exists(master_icon_path):
        print(f"❌ Error: {master_icon_path} not found!")
        return
    
    print(f"Loading master icon: {master_icon_path}")
    master_icon = Image.open(master_icon_path)
    
    # 出力ディレクトリ
    output_dir = "app/src/main/res"
    
    # サイズ定義
    sizes = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192
    }
    
    # Play Store用（512x512）
    print("\nCreating Play Store icon...")
    master_icon.save("ic_launcher_playstore.png")
    print("✓ Created: ic_launcher_playstore.png")
    
    # 各サイズのアイコンを生成
    print("\nGenerating Android mipmap icons...")
    for folder, size in sizes.items():
        print(f"Creating {size}x{size} icon for {folder}...")
        
        # リサイズ（高品質）
        resized_icon = master_icon.resize((size, size), Image.Resampling.LANCZOS)
        
        # ディレクトリを作成
        folder_path = os.path.join(output_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # アイコンを保存
        icon_path = os.path.join(folder_path, "ic_launcher.png")
        resized_icon.save(icon_path)
        print(f"✓ Created: {icon_path}")
        
        # 丸型アイコンも同じものを保存
        round_icon_path = os.path.join(folder_path, "ic_launcher_round.png")
        resized_icon.save(round_icon_path)
        print(f"✓ Created: {round_icon_path}")
    
    print("\n" + "=" * 60)
    print("Icon generation complete!")
    print("=" * 60)
    print("\nGenerated files:")
    print("- ic_launcher_playstore.png (For Google Play Store)")
    print(f"- {len(sizes)} sets of mipmap icons (mdpi to xxxhdpi)")
    print("\nNext steps:")
    print("1. Upload ic_launcher_playstore.png to Google Play Console")
    print("2. Build and run the app to see the new icon")
    print("3. The mipmap icons are automatically used by Android")


if __name__ == "__main__":
    print("=" * 60)
    print("Bisaya Speak AI - Icon Resizer")
    print("=" * 60)
    print("\nResizing ic_launcher_512.png to all required sizes...")
    print("\n")
    
    try:
        resize_and_save_icons()
        print("\n✅ Success! All icons have been generated.")
    except Exception as e:
        print(f"\n❌ Error: {e}")
        print("\nMake sure you have PIL (Pillow) installed:")
        print("  pip install Pillow")
