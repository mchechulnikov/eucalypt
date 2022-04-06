package eucalypt.running

interface ScriptRunner {
    suspend fun run(script: String, type: ScriptType): Result<String>
}