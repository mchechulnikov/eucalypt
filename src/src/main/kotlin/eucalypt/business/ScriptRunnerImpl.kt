package eucalypt.business

import eucalypt.business.executing.ExecutorsManager
import eucalypt.business.executing.executors.Executor
import eucalypt.business.executing.executors.ExecutorParameters
import eucalypt.business.executing.executors.ExecutorType
import eucalypt.business.executing.executors.ReservableExecutor
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger

@Suppress("OPT_IN_USAGE")
internal class ScriptRunnerImpl(
    private val settings: ScriptRunnerSettings,
    private val executorsManager: ExecutorsManager,
    private val logger: Logger,
) : ScriptRunner {
    override suspend fun run(script: String, type: ScriptType): Result<String> {
        if (script.isBlank()) {
            return Result.failure(Error("Script is empty"))
        }

        type
            .let(::getExecutorType)
            .let { executorsManager.borrowExecutor(it) }
            .onSuccess { executor ->
                val executionResult = runOnExecutor(executor, script)
                executorsManager.redeemExecutor(executor as ReservableExecutor)
                return Result.success(executionResult)
            }
            .onFailure { logger.error("Failed to borrow executor", it) }

        logger.error("Failed to run script of type $type")
        return Result.failure(Error("Script execution isn't started"))
    }

    private suspend fun runOnExecutor(executor: Executor, script: String) = coroutineScope {
        val startTime: Long = System.currentTimeMillis()
        val (job, outputChannel) = executor.execute(script)
        val jobResult = withTimeoutOrNull(settings.runningTimeoutMs) {
            job.join()
        }
        val endTime: Long = System.currentTimeMillis()

        buildOutput(
            executorParameters = executor.parameters,
            output = outputChannel,
            durationMs = endTime - startTime,
            isTimeoutExceeded = jobResult == null
        )
    }

    private suspend fun buildOutput(
        executorParameters: ExecutorParameters,
        output: ReceiveChannel<String>,
        durationMs: Long,
        isTimeoutExceeded: Boolean
    ) = buildString {
            appendLine("> Executing on ${executorParameters.executorTypeName}")
            appendLine("> Resources: " +
                    "CPU ${executorParameters.cpuCores}, " +
                    "RAM ${executorParameters.memoryMB} MB, " +
                    "space ${executorParameters.spaceSizeMB} MB, " +
                    "network - ${executorParameters.isNetworkDisabled.not()}"
            )
            appendLine()

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
