"""
Bisaya Speak AI - アイコン生成スクリプト

ICON_DESIGN.mdの仕様に基づき、512x512pxのPNGアイコンと
Android用の複数サイズを生成します。
"""

from PIL import Image, ImageDraw
import os

def create_icon(size=512):
    """
    シンプル版アイコンを作成
    
    Parameters:
    - size: アイコンのサイズ（デフォルト: 512）
    """
    # 画像を作成（白背景）
    img = Image.new('RGB', (size, size), color='#FFFFFF')
    draw = ImageDraw.Draw(img)
    
    # スケール係数
    scale = size / 512
    
    # 色定義
    turquoise_blue = '#00BCD4'
    dark_blue = '#0097A7'
    bright_green = '#4CAF50'
    white = '#FFFFFF'
    
    # マージン（セーフゾーン）
    margin = int(51 * scale)
    
    # 吹き出しのサイズと位置
    bubble_width = int(350 * scale)
    bubble_height = int(280 * scale)
    bubble_x = (size - bubble_width) // 2
    bubble_y = margin
    bubble_radius = int(40 * scale)
    
    # 吹き出し本体を描画（角丸長方形）
    draw.rounded_rectangle(
        [bubble_x, bubble_y, bubble_x + bubble_width, bubble_y + bubble_height],
        radius=bubble_radius,
        fill=turquoise_blue,
        outline=dark_blue,
        width=int(8 * scale)
    )
    
    # 吹き出しの尾を描画（三角形）
    tail_width = int(60 * scale)
    tail_height = int(40 * scale)
    tail_x = size // 2
    tail_y = bubble_y + bubble_height
    
    tail_points = [
        (tail_x - tail_width // 2, tail_y),
        (tail_x, tail_y + tail_height),
        (tail_x + tail_width // 2, tail_y)
    ]
    draw.polygon(tail_points, fill=turquoise_blue, outline=dark_blue)
    
    # 内部の白背景（角丸長方形）
    inner_margin = int(8 * scale)
    inner_radius = int(32 * scale)
    draw.rounded_rectangle(
        [bubble_x + inner_margin, bubble_y + inner_margin,
         bubble_x + bubble_width - inner_margin, bubble_y + bubble_height - inner_margin],
        radius=inner_radius,
        fill=white
    )
    
    # 回路ノードのサイズと位置
    node_radius = int(15 * scale)
    node_margin_x = int(60 * scale)
    node_margin_y = int(60 * scale)
    
    # 回路ノード（4つの円）
    nodes = [
        (bubble_x + node_margin_x, bubble_y + node_margin_y),  # 左上
        (bubble_x + bubble_width - node_margin_x, bubble_y + node_margin_y),  # 右上
        (bubble_x + node_margin_x, bubble_y + bubble_height - node_margin_y),  # 左下
        (bubble_x + bubble_width - node_margin_x, bubble_y + bubble_height - node_margin_y)  # 右下
    ]
    
    for node_x, node_y in nodes:
        draw.ellipse(
            [node_x - node_radius, node_y - node_radius,
             node_x + node_radius, node_y + node_radius],
            fill=bright_green
        )
    
    # 接続線
    line_width = int(6 * scale)
    
    # 上の線
    draw.line([nodes[0][0] + node_radius, nodes[0][1],
               nodes[1][0] - node_radius, nodes[1][1]],
              fill=bright_green, width=line_width)
    
    # 下の線
    draw.line([nodes[2][0] + node_radius, nodes[2][1],
               nodes[3][0] - node_radius, nodes[3][1]],
              fill=bright_green, width=line_width)
    
    # 左の線
    draw.line([nodes[0][0], nodes[0][1] + node_radius,
               nodes[2][0], nodes[2][1] - node_radius],
              fill=bright_green, width=line_width)
    
    # 右の線
    draw.line([nodes[1][0], nodes[1][1] + node_radius,
               nodes[3][0], nodes[3][1] - node_radius],
              fill=bright_green, width=line_width)
    
    # 音声波形（中央）
    wave_y_start = bubble_y + bubble_height // 2 - int(20 * scale)
    wave_y_spacing = int(10 * scale)
    wave_width = int(4 * scale)
    
    for i in range(3):
        wave_y = wave_y_start + i * wave_y_spacing
        # 簡易的な波形（ジグザグ）
        wave_points = []
        wave_x_start = bubble_x + bubble_width // 2 - int(60 * scale)
        wave_x_end = bubble_x + bubble_width // 2 + int(60 * scale)
        wave_segments = 6
        
        for j in range(wave_segments + 1):
            x = wave_x_start + (wave_x_end - wave_x_start) * j / wave_segments
            y_offset = int(8 * scale) if j % 2 == 0 else -int(8 * scale)
            wave_points.append((x, wave_y + y_offset))
        
        if len(wave_points) > 1:
            draw.line(wave_points, fill=bright_green, width=wave_width, joint='curve')
    
    return img


def generate_all_sizes():
    """
    Android用の全サイズを生成
    """
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
    
    # 512x512のマスターアイコンを作成
    print("Creating 512x512 master icon...")
    master_icon = create_icon(512)
    master_icon.save("ic_launcher_512.png")
    print("✓ Created: ic_launcher_512.png")
    
    # Google Play Store用（同じファイル）
    master_icon.save("ic_launcher_playstore.png")
    print("✓ Created: ic_launcher_playstore.png (for Google Play Store)")
    
    # 各サイズのアイコンを生成
    print("\nGenerating Android mipmap icons...")
    for folder, size in sizes.items():
        print(f"Creating {size}x{size} icon for {folder}...")
        icon = create_icon(size)
        
        # ディレクトリを作成
        folder_path = os.path.join(output_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # アイコンを保存
        icon_path = os.path.join(folder_path, "ic_launcher.png")
        icon.save(icon_path)
        print(f"✓ Created: {icon_path}")
        
        # 丸型アイコンも同じものを保存
        round_icon_path = os.path.join(folder_path, "ic_launcher_round.png")
        icon.save(round_icon_path)
        print(f"✓ Created: {round_icon_path}")
    
    print("\n" + "=" * 60)
    print("Icon generation complete!")
    print("=" * 60)
    print("\nGenerated files:")
    print("- ic_launcher_512.png (Master icon)")
    print("- ic_launcher_playstore.png (For Google Play Store)")
    print(f"- {len(sizes)} sets of mipmap icons (mdpi to xxxhdpi)")
    print("\nNext steps:")
    print("1. Upload ic_launcher_playstore.png to Google Play Console")
    print("2. Build and run the app to see the new icon")
    print("3. The mipmap icons are automatically used by Android")


if __name__ == "__main__":
    print("=" * 60)
    print("Bisaya Speak AI - Icon Generator")
    print("=" * 60)
    print("\nGenerating icons based on ICON_DESIGN.md specifications...")
    print("Design: Simple version (Variation 1)")
    print("Colors: Turquoise Blue (#00BCD4) + Bright Green (#4CAF50)")
    print("Background: White (#FFFFFF)")
    print("\n")
    
    try:
        generate_all_sizes()
        print("\n✅ Success! All icons have been generated.")
    except Exception as e:
        print(f"\n❌ Error: {e}")
        print("\nMake sure you have PIL (Pillow) installed:")
        print("  pip install Pillow")
