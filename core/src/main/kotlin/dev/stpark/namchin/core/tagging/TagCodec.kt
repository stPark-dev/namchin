package dev.stpark.namchin.core.tagging

/** 태그 집합 ↔ "가을,전신,카페" (정렬, 쉼표 구분). DB 저장용. */
object TagCodec {
    fun encode(tags: Set<Tag>): String = tags.map { it.name }.sorted().joinToString(",")

    fun decode(text: String): Set<Tag> =
        text.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map(::Tag).toSet()
}
