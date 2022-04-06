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
import eucalypt.executing.executors.ExecutorsFactorySettings
import eucalypt.executing.pool.ExecutorsPool
import eucalypt.executing.pool.ExecutorsPoolImpl
import eucalypt.executing.pool.ExecutorsPoolSettings
import eucalypt.running.ScriptRunner
import eucalypt.running.ScriptRunnerImpl
import org.koin.dsl.binds
import org.koin.dsl.module

val compositionRoot = module {
    factory<DockerEventMonitorSettings> { Config.DockerEventMonitorConfig }
    factory<ExecutorsFactorySettings> { Config.ExecutorsFactoryConfig }
    factory<ExecutorsPoolSettings> { Config.ExecutorsPoolConfig }
    factory<ExecutorsManagerSettings> { Config.ExecutorsManagerConfig }

    single { DockerEventsMonitor(get()) } binds arrayOf(DockerMonitorManager::class, DockerEventsFeed::class)

    single<ExecutorsFactory> { ExecutorsFactoryImpl(get(), get()) }
    single<ExecutorsPool> { ExecutorsPoolImpl(get(), get()) }
    single<ExecutorsManager> { ExecutorsManagerImpl(get(), get()) }

    single<ScriptRunner> { ScriptRunnerImpl(get()) }
}