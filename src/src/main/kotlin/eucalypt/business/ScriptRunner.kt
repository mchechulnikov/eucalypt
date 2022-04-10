package eucalypt.business

interface ScriptRunner {
    suspend fun run(script: String, type: ScriptType): Result<String>
}