package maryk.rocksdb

import rocksdb.rocksdb_backup_engine_options_create
import rocksdb.rocksdb_backup_engine_options_destroy

actual class BackupEngineOptions
    actual constructor(private val path: String)
: RocksObject() {
    val native = rocksdb_backup_engine_options_create(path)

    actual fun backupDir(): String {
        return path
    }

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_backup_engine_options_destroy(native)
            super.close()
        }
    }
}
