package eucalypt.utils

import org.slf4j.Logger


interface LoggerFactory {
    fun getLogger(name: String): Logger
    fun getLogger(clazz: Class<*>): Logger
}

