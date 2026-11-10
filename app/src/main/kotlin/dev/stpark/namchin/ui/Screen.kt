package dev.stpark.namchin.ui

import java.util.UUID

/** 앱 화면. 액티비티가 다시 만들어져도 복원되도록 문자열 목록으로 저장한다. */
sealed interface Screen {
    /** 시작 화면. referenceId가 null이면 기본 모드, 있으면 레퍼런스 모드. */
    data class Camera(val referenceId: Long? = null) : Screen

    /** 레퍼런스 관리 (목록·추가·삭제). */
    data object Library : Screen

    /** @property token 가져오기 작업마다 고유. 같은 사진을 다시 골라도 새 작업으로 구분한다. */
    data class Import(val uri: String, val token: String) : Screen {
        companion object {
            fun new(uri: String) = Import(uri, UUID.randomUUID().toString())
        }
    }

    data class Detail(val id: Long) : Screen

    companion object {
        fun save(screen: Screen): List<String> = when (screen) {
            is Camera -> listOf("Camera", screen.referenceId?.toString().orEmpty())
            Library -> listOf("Library")
            is Import -> listOf("Import", screen.uri, screen.token)
            is Detail -> listOf("Detail", screen.id.toString())
        }

        fun restore(saved: List<String>): Screen? = when (saved.firstOrNull()) {
            "Camera" -> saved.getOrNull(1)?.let { id ->
                if (id.isEmpty()) Camera(null) else id.toLongOrNull()?.let { Camera(it) }
            }
            "Library" -> Library
            "Import" -> if (saved.size == 3) Import(saved[1], saved[2]) else null
            "Detail" -> saved.getOrNull(1)?.toLongOrNull()?.let { Detail(it) }
            else -> null
        }
    }
}
