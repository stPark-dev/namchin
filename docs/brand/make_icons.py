"""brand_symbol_remaster.png → 안드로이드 적응형 아이콘 리소스 생성.

사용: 저장소 루트에서 `python3 docs/brand/make_icons.py`

- 가운데 맞춤 기준은 렌즈+꺾쇠(남색 픽셀 범위). 오른쪽 위 반짝임 선은 기준에서 뺀다
  (포함하면 렌즈가 왼쪽 아래로 치우쳐 보인다).
- 꺾쇠 폭이 캔버스의 40%: 삼성 기본(둥근 사각형) 마스크에서 반짝임까지 보이고,
  원형 마스크에서는 반짝임 끝만 살짝 잘린다. (리마스터 심볼은 반짝임이 더 멀리 뻗어 46% → 40%)
- 바깥 배경은 순백(#FFFFFF)으로 칠해 ic_launcher_background와 이어지게 한다.
- 단색(테마 아이콘): 바깥 흰 배경만 투명, 하트는 구멍.
"""
from PIL import Image, ImageDraw

SRC = "docs/brand/brand_symbol_remaster.png"
RES = "app/src/main/res/"
DENSITIES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}
CONTENT_FRACTION = 0.40
WHITE_THRESHOLD = 232
DARK_THRESHOLD = 80


def flood_from(img_l: Image.Image, seed: tuple[int, int]) -> Image.Image:
    """seed에서 이어진 흰 영역만 255인 마스크."""
    white = img_l.point(lambda v: 255 if v >= WHITE_THRESHOLD else 0)
    ImageDraw.floodfill(white, seed, 128, thresh=0)
    return white.point(lambda v: 255 if v == 128 else 0)


def main() -> None:
    # 투명 배경 심볼을 흰 바탕에 올린다 (아이콘 배경 레이어가 흰색)
    symbol = Image.open(SRC).convert("RGBA")
    icon = Image.new("RGB", symbol.size, (255, 255, 255))
    icon.paste(symbol, (0, 0), symbol)
    left, top, right, bottom = icon.convert("L").point(lambda v: 255 if v < DARK_THRESHOLD else 0).getbbox()
    cx, cy = (left + right) / 2, (top + bottom) / 2
    canvas = int(max(right - left, bottom - top) / CONTENT_FRACTION)
    ox, oy = int(canvas / 2 - cx), int(canvas / 2 - cy)

    fg = Image.new("RGB", (canvas, canvas), (255, 255, 255))
    fg.paste(icon, (ox, oy))

    gray = fg.convert("L")
    background = flood_from(gray, (0, 0))
    # 원본 바탕의 옅은 회색 기 때문에 넓힌 여백과 경계가 보이므로, 바깥 배경은 순백으로 통일
    fg.paste((255, 255, 255), (0, 0, canvas, canvas), background)
    heart = flood_from(gray, (int(cx) + ox, int(cy) + oy - 20))
    opaque = background.point(lambda v: 0 if v else 255)
    mono = Image.composite(Image.new("L", fg.size, 0), opaque, heart)

    for name, px in DENSITIES.items():
        folder = f"{RES}mipmap-{name}/"
        fg.resize((px, px), Image.LANCZOS).save(folder + "ic_launcher_foreground.png")
        layer = Image.new("RGBA", (px, px), (255, 255, 255, 0))
        layer.putalpha(mono.resize((px, px), Image.LANCZOS))
        layer.save(folder + "ic_launcher_monochrome.png")


if __name__ == "__main__":
    main()
