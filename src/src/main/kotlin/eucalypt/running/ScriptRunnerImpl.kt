package eucalypt.running

import eucalypt.executing.ExecutorsManager
import eucalypt.executing.executors.Executor
import eucalypt.executing.executors.ExecutorType
import eucalypt.executing.executors.ReservableExecutor
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

@Suppress("OPT_IN_USAGE")
internal class ScriptRunnerImpl(
    private val settings: ScriptRunnerSettings,
    private val executorsManager: ExecutorsManager,
) : ScriptRunner {
    override suspend fun run(script: String, type: ScriptType): Result<String> {
        type
            .let(::getExecutorType)
            .let { executorsManager.borrowExecutor(it) }
            .onSuccess { executor ->
                val executionResult = runOnExecutor(executor, script)
                executorsManager.redeemExecutor(executor as ReservableExecutor)
                return Result.success(executionResult)
            }
            .onFailure {
                // TODO handle error
            }

        return Result.failure(Error("Script execution not started"))
    }

    private suspend fun runOnExecutor(executor: Executor, script: String) = coroutineScope {
        val startTime: Long = System.currentTimeMillis()
        val (job, output) = executor.execute(script)
        val jobResult = withTimeoutOrNull(settings.runningTimeoutMs) {
            job.join()
        }
        val endTime: Long = System.currentTimeMillis()

        buildOutput(
            executor,
            output,
            durationMs = endTime - startTime,
            isTimeoutExceeded = jobResult == null
        )
    }

    private suspend fun buildOutput(
        executor: Executor,
        output: ReceiveChannel<String>,
        durationMs: Long,
        isTimeoutExceeded: Boolean
    ) = buildString {
            appendLine("> Executing script on ${executor.typeName}")
            appendLine("> Command: ${executor.executingBy}")
            if (!output.isEmpty) appendLine("> Output:\n")
            else appendLine("> No output")

            while (!output.isEmpty) {
                appendLine(output.receive())
            }
            output.cancel()

            if (isNotBlank()) appendLine()

            if (isTimeoutExceeded) {
                appendLine("----------------------------------------------------")
                appendLine("✗ Script running timeout exceeded. Execution aborted")
            } else {
                appendLine("------------------------------")
                appendLine("✓ Script executed successfully")
            }

            appendLine("> Time elapsed: ${durationMs / 1000 % 60} seconds")
        }

    private fun getExecutorType(type: ScriptType): ExecutorType {
        return when(type) {
            ScriptType.DOTNET -> ExecutorType.DOTNET6
            ScriptType.JAVA -> ExecutorType.JAVA
            ScriptType.GO -> ExecutorType.GO
        }
    }
}