package eucalypt.executing.executors

enum class ExecutorState {
    NEW,
    READY,
    RESERVED,
    EXECUTING,
    RELEASED,
    RESET,
    ELIMINATED;

    val isReady get() = this == READY
    val isTimeoutable get(): Boolean
    = this == RESERVED
                || this == EXECUTING
                || this == RELEASED
}