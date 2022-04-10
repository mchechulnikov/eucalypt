package eucalypt

import eucalypt.business.executing.ExecutorsManager
import eucalypt.business.executing.ExecutorsManagerImpl
import eucalypt.business.executing.ExecutorsManagerSettings
import eucalypt.business.executing.executors.ExecutorsFactory
import eucalypt.business.executing.executors.ExecutorsFactoryImpl
import eucalypt.business.executing.pool.*
import eucalypt.business.ScriptRunner
import eucalypt.business.ScriptRunnerImpl
import eucalypt.business.ScriptRunnerSettings
import eucalypt.http.HTTPServer
import eucalypt.http.HTTPServerImpl
import eucalypt.infra.docker.*
import eucalypt.infra.docker.DockerEventsMonitor
import eucalypt.infra.utils.LoggerFactory
import eucalypt.infra.utils.LoggerFactoryImpl
import org.koin.dsl.binds
import org.koin.dsl.module

val compositionRoot = module {
    factory<LoggerFactory> { LoggerFactoryImpl() }

    factory<ScriptRunnerSettings> { Config.ScriptRunnerConfig }
    factory<DockerEventMonitorSettings> { Config.DockerEventMonitorConfig }
    factory<ExecutorsPoolSettings> { Config.ExecutorsPoolConfig }
    factory<ExecutorsManagerSettings> { Config.ExecutorsManagerConfig }

    single<DockerOperator> { DockerOperatorImpl() }

    single {
        val logger = get<LoggerFactory>().getLogger(DockerEventsMonitor::class.java)
        DockerEventsMonitor(get(), get(), logger)
    } binds arrayOf(DockerMonitorManager::class, DockerEventsFeed::class)

    single<ExecutorsPoolGarbageCollector> {
        val logger = get<LoggerFactory>().getLogger(ExecutorsPoolGarbageCollectorImpl::class.java)
        ExecutorsPoolGarbageCollectorImpl(get(), get(), logger)
    }

    single {
        val logger = get<LoggerFactory>().getLogger(ExecutorsPoolImpl::class.java)
        ExecutorsPoolImpl(get(), get(), get(), logger)
    } binds arrayOf(ExecutorsPool::class, ExecutorsPoolManager::class)

    single<ExecutorsFactory> { ExecutorsFactoryImpl(get(), get(), get()) }

    single<ExecutorsManager> {
        val logger = get<LoggerFactory>().getLogger(ExecutorsManagerImpl::class.java)
        ExecutorsManagerImpl(get(), get(), logger)
    }

    single<ScriptRunner> {
        val logger = get<LoggerFactory>().getLogger(ScriptRunnerImpl::class.java)
        ScriptRunnerImpl(get(), get(), logger)
    }

    single<HTTPServer> {
        val logger = get<LoggerFactory>().getLogger(HTTPServerImpl::class.java)
        HTTPServerImpl(logger)
    }
}