package eucalypt

import eucalypt.docker.DockerEventMonitorSettings
import eucalypt.docker.DockerEventsFeed
import eucalypt.docker.DockerEventsMonitor
import eucalypt.docker.DockerMonitorManager
import eucalypt.executing.ExecutorsManager
import eucalypt.executing.ExecutorsManagerImpl
import eucalypt.executing.ExecutorsManagerSettings
import eucalypt.executing.executors.ExecutorsFactory
import eucalypt.executing.executors.ExecutorsFactoryImpl
import eucalypt.executing.pool.ExecutorsPool
import eucalypt.executing.pool.ExecutorsPoolImpl
import eucalypt.executing.pool.ExecutorsPoolManager
import eucalypt.executing.pool.ExecutorsPoolSettings
import eucalypt.running.ScriptRunner
import eucalypt.running.ScriptRunnerImpl
import eucalypt.running.ScriptRunnerSettings
import org.koin.dsl.binds
import org.koin.dsl.module

val compositionRoot = module {
    factory<ScriptRunnerSettings> { Config.ScriptRunnerConfig }
    factory<DockerEventMonitorSettings> { Config.DockerEventMonitorConfig }
    factory<ExecutorsPoolSettings> { Config.ExecutorsPoolConfig }
    factory<ExecutorsManagerSettings> { Config.ExecutorsManagerConfig }

    single { DockerEventsMonitor(get()) } binds arrayOf(DockerMonitorManager::class, DockerEventsFeed::class)
    single { ExecutorsPoolImpl(get(), get()) } binds arrayOf(ExecutorsPool::class, ExecutorsPoolManager::class)

    single<ExecutorsFactory> { ExecutorsFactoryImpl(get()) }
    single<ExecutorsManager> { ExecutorsManagerImpl(get(), get()) }

    single<ScriptRunner> { ScriptRunnerImpl(get(), get()) }
}