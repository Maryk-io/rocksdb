package maryk.rocksdb

import rocksdb.rocksdb_restore_options_create
import rocksdb.rocksdb_restore_options_destroy
import rocksdb.rocksdb_restore_options_set_keep_log_files

actual class RestoreOptions actual constructor(
    keepLogFiles: Boolean
) : RocksObject() {
    val native = rocksdb_restore_options_create()

    init {
        rocksdb_restore_options_set_keep_log_files(native, if (keepLogFiles) 1 else 0)
    }

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_restore_options_destroy(native)
            super.close()
        }
    }
}
