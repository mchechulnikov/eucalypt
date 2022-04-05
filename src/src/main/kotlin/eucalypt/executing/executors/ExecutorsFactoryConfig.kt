package eucalypt.executing.executors

data class ExecutorsFactoryConfig(
    val containerNamePrefix: String = "eucalypt-exec-",
    val factoryID: Int = 0,
)