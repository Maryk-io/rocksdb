package maryk.rocksdb

import cnames.structs.rocksdb_env_t
import kotlinx.cinterop.CPointer
import rocksdb.rocksdb_create_default_env
import rocksdb.rocksdb_env_get_background_threads
import rocksdb.rocksdb_env_get_bottom_priority_background_threads
import rocksdb.rocksdb_env_get_high_priority_background_threads
import rocksdb.rocksdb_env_get_low_priority_background_threads
import rocksdb.rocksdb_env_lower_high_priority_thread_pool_cpu_priority
import rocksdb.rocksdb_env_lower_high_priority_thread_pool_io_priority
import rocksdb.rocksdb_env_lower_thread_pool_cpu_priority
import rocksdb.rocksdb_env_lower_thread_pool_io_priority
import rocksdb.rocksdb_env_set_background_threads
import rocksdb.rocksdb_env_set_bottom_priority_background_threads
import rocksdb.rocksdb_env_set_high_priority_background_threads
import rocksdb.rocksdb_env_set_low_priority_background_threads

actual abstract class Env
    protected constructor(internal val native: CPointer<rocksdb_env_t>?)
: RocksObject() {
    actual fun getBackgroundThreads(priority: Priority): Int = when(priority) {
        Priority.BOTTOM -> rocksdb_env_get_bottom_priority_background_threads(native)
        Priority.HIGH -> rocksdb_env_get_high_priority_background_threads(native)
        Priority.LOW -> rocksdb_env_get_low_priority_background_threads(native)
        Priority.TOTAL -> rocksdb_env_get_background_threads(native)
        Priority.USER -> throw NotImplementedError("TODO")
    }

    actual fun setBackgroundThreads(number: Int, priority: Priority): Env {
        when(priority) {
            Priority.BOTTOM -> rocksdb_env_set_bottom_priority_background_threads(native, number)
            Priority.HIGH -> rocksdb_env_set_high_priority_background_threads(native, number)
            Priority.LOW -> rocksdb_env_set_low_priority_background_threads(native, number)
            Priority.TOTAL -> rocksdb_env_set_background_threads(native, number)
            Priority.USER -> throw NotImplementedError("TODO")
        }
        return this
    }

    actual fun getThreadPoolQueueLen(priority: Priority): Int =
        throw NotImplementedError("DO SOMETHING")

    actual fun incBackgroundThreadsIfNeeded(number: Int, priority: Priority): Env {
        throw NotImplementedError("DO SOMETHING")
//        return this
    }

    actual fun lowerThreadPoolIOPriority(priority: Priority): Env {
        when (priority) {
            Priority.HIGH -> rocksdb_env_lower_high_priority_thread_pool_io_priority(native)
            Priority.TOTAL -> rocksdb_env_lower_thread_pool_io_priority(native)
            else -> throw NotImplementedError("TODO")
        }
        return this
    }

    actual fun lowerThreadPoolCPUPriority(priority: Priority): Env {
        when (priority) {
            Priority.HIGH -> rocksdb_env_lower_high_priority_thread_pool_cpu_priority(native)
            Priority.TOTAL -> rocksdb_env_lower_thread_pool_cpu_priority(native)
            else -> throw NotImplementedError("TODO")
        }
        return this
    }
}

actual fun getDefaultEnv(): Env = RocksEnv(rocksdb_create_default_env())
