package dev.stpark.namchin.core.tagging

data class TagCategory(val name: String, val tags: List<Tag>)

/** 레퍼런스 태그 목록 (상세 화면 칩). */
object TagVocabulary {
    val CAFE = Tag("카페")
    val INDOOR = Tag("실내")
    val STREET = Tag("거리")
    val NATURE = Tag("자연")
    val WATER = Tag("물가")
    val DAY = Tag("낮")
    val NIGHT = Tag("야경")
    val FULL_BODY = Tag("전신")
    val UPPER_BODY = Tag("상반신")
    val SEATED = Tag("앉은 자세")

    val PLACE = TagCategory("장소", listOf(CAFE, INDOOR, STREET, NATURE, WATER))
    val TIME = TagCategory("시간", listOf(DAY, NIGHT))
    val FRAMING = TagCategory("구도", listOf(FULL_BODY, UPPER_BODY, SEATED))
    val SEASON = TagCategory("계절", listOf("봄", "여름", "가을", "겨울").map(::Tag))
    val CATEGORIES = listOf(PLACE, TIME, FRAMING, SEASON)

    /**
     * ML Kit 기본 이미지 라벨 → 장소 태그 (소문자 키).
     * 실제 레퍼런스에서 확인한 라벨 기준: 카페 사진엔 Desk/Room, 거리엔 Road/Building/Asphalt, 야외엔 Sky/Prairie.
     */
    val LABEL_TO_TAG: Map<String, Tag> = mapOf(
        "desk" to CAFE, "coffee" to CAFE, "cup" to CAFE, "tableware" to CAFE, "restaurant" to CAFE, "bar" to CAFE,
        "room" to INDOOR, "interior design" to INDOOR, "shelf" to INDOOR,
        "road" to STREET, "asphalt" to STREET, "building" to STREET, "skyscraper" to STREET, "street" to STREET,
        "sky" to NATURE, "prairie" to NATURE, "grass" to NATURE, "tree" to NATURE, "garden" to NATURE,
        "mountain" to NATURE, "forest" to NATURE, "field" to NATURE,
        "lake" to WATER, "river" to WATER, "sea" to WATER, "beach" to WATER, "pier" to WATER,
    )
}
