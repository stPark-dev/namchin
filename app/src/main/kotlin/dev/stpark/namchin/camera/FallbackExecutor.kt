package dev.stpark.namchin.camera

import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/**
 * delegate에서 실행하되, 이미 종료됐으면 호출한 스레드에서 바로 실행한다.
 * 화면을 떠나 분석 스레드를 닫은 뒤 늦게 끝난 분석의 프레임 해제가 거부되어 앱이 죽는 것을 막는다.
 */
class FallbackExecutor(private val delegate: Executor) : Executor {
    override fun execute(command: Runnable) {
        try {
            delegate.execute(command)
        } catch (e: RejectedExecutionException) {
            command.run()
        }
    }
}
