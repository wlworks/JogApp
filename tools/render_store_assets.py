"""產生 Play Console 用的商店素材，輸出到 store/：

  icon-512.png                  商店圖示（512×512，Play 會自己套圓角遮罩）
  feature-graphic-1024x500.png  主題圖片

圖形與 App 內的 adaptive icon（res/drawable/ic_launcher_foreground.xml）相同：
深色底、青色搖桿環與四向箭頭、中央白色 pin。座標沿用 108 單位畫布與 0.82 的縮放，
改設計時三處（前景、monochrome、這支腳本）一起改。
用法：python tools/render_store_assets.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

# 環與箭頭的顏色，也是 App 內的強調色
ACCENT = (0x4D, 0xD0, 0xE1)
# 主題圖片的次要文字色
DIM = (0x9A, 0x9E, 0xA3)
# Windows 系統字型目錄；找不到字型時退回 PIL 內建字型
FONT_DIR = 'C:/Windows/Fonts'
# 與 res/values/colors.xml 的 ic_launcher_background、App 背景相同
INK = (0x12, 0x14, 0x16)
# 圖形在 108 單位畫布上的縮放，與 ic_launcher_foreground.xml 的 group 相同
MARK_SCALE = 0.82
OUT_DIR = 'store'
# 超取樣倍率：先畫大再縮小，邊緣才平滑
SUPERSAMPLE = 4
# pin 本體
WHITE = (0xFF, 0xFF, 0xFF)


def bezier(p0, p1, p2, p3, steps=64):
    """三次貝茲曲線取樣點，不含起點。"""
    pts = []
    for i in range(1, steps + 1):
        t = i / steps
        u = 1 - t
        pts.append((
            u ** 3 * p0[0] + 3 * u * u * t * p1[0] + 3 * u * t * t * p2[0] + t ** 3 * p3[0],
            u ** 3 * p0[1] + 3 * u * u * t * p1[1] + 3 * u * t * t * p2[1] + t ** 3 * p3[1],
        ))
    return pts


def draw_mark(draw, cx, cy, unit, hole):
    """以 (cx, cy) 為 108 單位畫布的中心 (54, 54) 畫搖桿環 + 箭頭 + pin；unit 是一個單位的像素數。"""
    k = unit * MARK_SCALE

    def m(p):
        return (cx + (p[0] - 54) * k, cy + (p[1] - 54) * k)

    # 搖桿環：半徑 31、線寬 3.5
    r_out, r_in = (31 + 1.75) * k, (31 - 1.75) * k
    draw.ellipse([cx - r_out, cy - r_out, cx + r_out, cy + r_out], fill=ACCENT)
    draw.ellipse([cx - r_in, cy - r_in, cx + r_in, cy + r_in], fill=hole)
    # 四向箭頭
    for tri in (
        [(54, 26.5), (50, 31.5), (58, 31.5)],
        [(54, 81.5), (50, 76.5), (58, 76.5)],
        [(26.5, 54), (31.5, 50), (31.5, 58)],
        [(81.5, 54), (76.5, 50), (76.5, 58)],
    ):
        draw.polygon([m(p) for p in tri], fill=ACCENT)
    # pin 與挖空的圓孔
    draw.polygon([m(p) for p in pin_outline()], fill=WHITE)
    hx, hy = m((54, 46.5))
    r = 4.2 * k
    draw.ellipse([hx - r, hy - r, hx + r, hy + r], fill=hole)


def feature_graphic(draw, s):
    """1024×500 主題圖片：左邊圖形，右邊名稱與一句定位。"""
    draw.rectangle([0, 0, 1024 * s, 500 * s], fill=INK)
    # 環的外徑約 54 單位 × 0.82；讓它在 500 高的畫面上約佔 300 px
    draw_mark(draw, 200 * s, 250 * s, 300 / (65.5 * MARK_SCALE) * s, hole=INK)
    draw.text((390 * s, 270 * s), 'Jog',
              font=font('segoeuib.ttf', 150 * s), fill=WHITE, anchor='ls')
    draw.text((396 * s, 330 * s), 'Mock location tool for development testing',
              font=font('segoeui.ttf', 28 * s), fill=DIM, anchor='ls')


def font(name, size):
    """載入 Windows 字型，找不到就退回 PIL 內建字型。"""
    try:
        return ImageFont.truetype(os.path.join(FONT_DIR, name), size)
    except OSError:
        return ImageFont.load_default(size)


def icon(draw, s):
    """512×512 商店圖示：比照 launcher 只露出 108 畫布中間 72 單位的比例，看起來和桌面上一致。"""
    draw.rectangle([0, 0, 512 * s, 512 * s], fill=INK)
    draw_mark(draw, 256 * s, 256 * s, 512 / 72 * s, hole=INK)


def main():
    """畫出兩張素材並存檔。"""
    os.makedirs(OUT_DIR, exist_ok=True)
    render((512, 512), icon).save(os.path.join(OUT_DIR, 'icon-512.png'))
    render((1024, 500), feature_graphic).save(
        os.path.join(OUT_DIR, 'feature-graphic-1024x500.png'))
    print('wrote store/icon-512.png and store/feature-graphic-1024x500.png')


def pin_outline():
    """ic_launcher_foreground.xml 的 pin 外框（108 單位座標系），依 pathData 逐段展開。"""
    pts = [(54.0, 36.0)]
    pts += bezier((54, 36), (48, 36), (43.5, 40.5), (43.5, 46.5))
    pts += bezier((43.5, 46.5), (43.5, 54.5), (54, 66), (54, 66))
    pts += bezier((54, 66), (54, 66), (64.5, 54.5), (64.5, 46.5))
    pts += bezier((64.5, 46.5), (64.5, 40.5), (60, 36), (54, 36))
    return pts


def render(size, paint):
    """建立超取樣畫布，交給 paint 畫完後縮回目標尺寸。"""
    w, h = size
    s = SUPERSAMPLE
    img = Image.new('RGB', (w * s, h * s))
    paint(ImageDraw.Draw(img), s)
    return img.resize((w, h), Image.LANCZOS)


if __name__ == '__main__':
    main()
