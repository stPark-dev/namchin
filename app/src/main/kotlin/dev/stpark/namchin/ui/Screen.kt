package dev.stpark.namchin.ui

import java.util.UUID

/** 앱 화면. 액티비티가 다시 만들어져도 복원되도록 문자열 목록으로 저장한다. */
sealed interface Screen {
    data object Home : Screen

    /** @property token 가져오기 작업마다 고유. 같은 사진을 다시 골라도 새 작업으로 구분한다. */
    data class Import(val uri: String, val token: String) : Screen {
        companion object {
            fun new(uri: String) = Import(uri, UUID.randomUUID().toString())
        }
    }

    data class Detail(val id: Long) : Screen

    data class Camera(val referenceId: Long) : Screen

    companion object {
        fun save(screen: Screen): List<String> = when (screen) {
            Home -> listOf("Home")
            is Import -> listOf("Import", screen.uri, screen.token)
            is Detail -> listOf("Detail", screen.id.toString())
            is Camera -> listOf("Camera", screen.referenceId.toString())
        }

        fun restore(saved: List<String>): Screen? = when (saved.firstOrNull()) {
            "Home" -> Home
            "Import" -> if (saved.size == 3) Import(saved[1], saved[2]) else null
            "Detail" -> saved.getOrNull(1)?.toLongOrNull()?.let { Detail(it) }
            "Camera" -> saved.getOrNull(1)?.toLongOrNull()?.let { Camera(it) }
            else -> null
        }
    }
}
