package eucalypt.utils

import org.slf4j.Logger

class LoggerFactoryImpl : LoggerFactory {
    override fun getLogger(name: String): Logger = org.slf4j.LoggerFactory.getLogger(name)

    override fun getLogger(clazz: Class<*>): Logger =  org.slf4j.LoggerFactory.getLogger(clazz)
}