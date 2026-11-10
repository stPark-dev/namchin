package dev.stpark.namchin.ui.camera

/** 카드 한 장의 놓인 모양. offset은 오른쪽 아래 모서리 기준(양수 = 오른쪽/아래로 밀려남). */
data class CardPose(val rotationDeg: Float, val offsetXDp: Float, val offsetYDp: Float)

/** 오른쪽 아래 모서리에 카드 게임 손패처럼 부채꼴로 겹쳐 놓는다. 마지막 카드가 맨 앞. */
object CardFan {
    const val MAX_CARDS = 3
    private const val ROTATION_STEP = 9f
    private const val X_STEP = 28f
    private const val Y_STEP = 6f

    fun layout(count: Int): List<CardPose> {
        val n = count.coerceAtMost(MAX_CARDS)
        return List(n) { i ->
            val behind = n - 1 - i // 0 = 맨 앞
            CardPose(
                // 부호는 정수 쪽에서 바꿔 맨 앞 카드가 -0.0이 아닌 0이 되게 한다
                rotationDeg = ROTATION_STEP * -behind,
                offsetXDp = X_STEP * -behind,
                offsetYDp = Y_STEP * behind,
            )
        }
    }
}
