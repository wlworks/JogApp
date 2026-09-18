"""產生 Play Console 用的商店素材，輸出到 store/：

  icon-512.png                  商店圖示（512×512，Play 會自己套圓角遮罩）
  feature-graphic-1024x500.png  主題圖片

圖形與 App 內的 adaptive icon 共用同一個 pin 形狀與配色，改設計時兩邊一起改。
用法：python tools/render_store_assets.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

# 與 res/values/colors.xml 的 ic_launcher_background 相同
ACCENT = (0x4D, 0xD0, 0xE1)
# 主題圖片的次要文字色
DIM = (0x9A, 0x9E, 0xA3)
# Windows 系統字型目錄；找不到字型時退回 PIL 內建字型
FONT_DIR = 'C:/Windows/Fonts'
# 與 App 背景 / adaptive icon 前景相同
INK = (0x12, 0x14, 0x16)
OUT_DIR = 'store'
# 超取樣倍率：先畫大再縮小，邊緣才平滑
SUPERSAMPLE = 4


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


def draw_pin(draw, cx, cy, height, fill, hole):
    """以 (cx, cy) 對應 24 單位座標系的 (12, 12) 畫 pin；height 是 pin 總高度（像素）。"""
    scale = height / 20.0

    def m(p):
        return (cx + (p[0] - 12) * scale, cy + (p[1] - 12) * scale)

    draw.polygon([m(p) for p in pin_outline()], fill=fill)
    r = 2.5 * scale
    hx, hy = m((12, 9))
    draw.ellipse([hx - r, hy - r, hx + r, hy + r], fill=hole)


def feature_graphic(draw, s):
    """1024×500 主題圖片：左邊 pin，右邊名稱與一句定位。"""
    draw.rectangle([0, 0, 1024 * s, 500 * s], fill=INK)
    draw_pin(draw, 190 * s, 250 * s, 300 * s, fill=ACCENT, hole=INK)
    draw.text((370 * s, 270 * s), 'Jog',
              font=font('segoeuib.ttf', 150 * s), fill=(255, 255, 255), anchor='ls')
    draw.text((376 * s, 330 * s), 'Mock location tool for development testing',
              font=font('segoeui.ttf', 28 * s), fill=DIM, anchor='ls')


def font(name, size):
    """載入 Windows 字型，找不到就退回 PIL 內建字型。"""
    try:
        return ImageFont.truetype(os.path.join(FONT_DIR, name), size)
    except OSError:
        return ImageFont.load_default(size)


def icon(draw, s):
    """512×512 商店圖示：與 adaptive icon 相同比例（pin 高度佔畫布 48/108）。"""
    draw.rectangle([0, 0, 512 * s, 512 * s], fill=ACCENT)
    draw_pin(draw, 256 * s, 256 * s, 512 * 48 / 108 * s, fill=INK, hole=ACCENT)


def main():
    """畫出兩張素材並存檔。"""
    os.makedirs(OUT_DIR, exist_ok=True)
    render((512, 512), icon).save(os.path.join(OUT_DIR, 'icon-512.png'))
    render((1024, 500), feature_graphic).save(
        os.path.join(OUT_DIR, 'feature-graphic-1024x500.png'))
    print('wrote store/icon-512.png and store/feature-graphic-1024x500.png')


def pin_outline():
    """ic_pin.xml 的外框（24 單位座標系），依 pathData 逐段展開。"""
    pts = [(12.0, 2.0)]
    pts += bezier((12, 2), (8.13, 2), (5, 5.13), (5, 9))
    pts += bezier((5, 9), (5, 14.25), (12, 22), (12, 22))
    pts += bezier((12, 22), (12, 22), (19, 14.25), (19, 9))
    pts += bezier((19, 9), (19, 5.13), (15.87, 2), (12, 2))
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
