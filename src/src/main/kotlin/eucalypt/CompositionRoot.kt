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
import eucalypt.business.executing.executors.Executor
import eucalypt.http.HTTPServer
import eucalypt.http.HTTPServerImpl
import eucalypt.http.HTTPServerSettings
import eucalypt.infra.docker.*
import eucalypt.infra.docker.DockerEventsMonitor
import org.koin.dsl.binds
import org.koin.dsl.module
import org.slf4j.LoggerFactory

val compositionRoot = module {
    factory<HTTPServerSettings> { Config.HTTPServerConfig }
    factory<ScriptRunnerSettings> { Config.ScriptRunnerConfig }
    factory<DockerEventMonitorSettings> { Config.DockerEventMonitorConfig }
    factory<ExecutorsPoolSettings> { Config.ExecutorsPoolConfig }
    factory<ExecutorsManagerSettings> { Config.ExecutorsManagerConfig }

    single<DockerOperator> { DockerOperatorImpl() }

    single {
        val logger = LoggerFactory.getLogger(DockerEventsMonitor::class.java)
        DockerEventsMonitor(get(), get(), logger)
    } binds arrayOf(DockerMonitorManager::class, DockerEventsFeed::class)

    single<ExecutorsPoolGarbageCollector> {
        val logger = LoggerFactory.getLogger(ExecutorsPoolGarbageCollectorImpl::class.java)
        ExecutorsPoolGarbageCollectorImpl(get(), get(), logger)
    }

    single {
        val logger = LoggerFactory.getLogger(ExecutorsPoolImpl::class.java)
        ExecutorsPoolImpl(get(), get(), get(), logger)
    } binds arrayOf(ExecutorsPool::class, ExecutorsPoolManager::class)

    single<ExecutorsFactory> {
        val logger = LoggerFactory.getLogger(Executor::class.java)
        ExecutorsFactoryImpl(get(), get(), logger)
    }

    single<ExecutorsManager> {
        val logger = LoggerFactory.getLogger(ExecutorsManagerImpl::class.java)
        ExecutorsManagerImpl(get(), get(), logger)
    }

    single<ScriptRunner> {
        val logger = LoggerFactory.getLogger(ScriptRunnerImpl::class.java)
        ScriptRunnerImpl(get(), get(), logger)
    }

    single<HTTPServer> {
        val logger = LoggerFactory.getLogger(HTTPServerImpl::class.java)
        HTTPServerImpl(get(), logger)
    }
}