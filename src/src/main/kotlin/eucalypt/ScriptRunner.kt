package eucalypt

import eucalypt.executing.ExecutorType
import eucalypt.executing.ExecutorsManager

class ScriptRunner(private val executorsManager: ExecutorsManager) {
    suspend fun run(script: String, type: ScriptType): Result<String> {
        type
            .let(::getExecutorType)
            .let { executorsManager.borrowExecutor(it) }
            .onSuccess {
                val executionResult = it.execute(script)
                executorsManager.redeemExecutor(it.id)

                return Result.success(executionResult)
            }
            .onFailure {
                // TODO handle error
            }

        return Result.failure(Error("Script execution failed"))
    }

    private fun getExecutorType(type: ScriptType): ExecutorType {
        return when(type) {
            ScriptType.DOTNET -> ExecutorType.DOTNET
            ScriptType.JAVA -> ExecutorType.JAVA
            ScriptType.GO -> ExecutorType.GO
        }
    }
}

enum class ScriptType {
    DOTNET,
    JAVA,
    GO
}
