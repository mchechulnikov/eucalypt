package eucalypt.running

import eucalypt.executing.executors.ExecutorType
import eucalypt.executing.ExecutorsManager
import eucalypt.executing.executors.ReservableExecutor

internal class ScriptRunnerImpl(private val executorsManager: ExecutorsManager) : ScriptRunner {
    override suspend fun run(script: String, type: ScriptType): Result<String> {
        type
            .let(::getExecutorType)
            .let { executorsManager.borrowExecutor(it) }
            .onSuccess {
                val executionResult = it.execute(script)
                executorsManager.redeemExecutor(it as ReservableExecutor)

                return Result.success(executionResult)
            }
            .onFailure {
                // TODO handle error
            }

        return Result.failure(Error("Script execution failed"))
    }

    private fun getExecutorType(type: ScriptType): ExecutorType {
        return when(type) {
            ScriptType.DOTNET -> ExecutorType.DOTNET6
            ScriptType.JAVA -> ExecutorType.JAVA
            ScriptType.GO -> ExecutorType.GO
        }
    }
}
