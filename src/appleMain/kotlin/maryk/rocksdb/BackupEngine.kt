package maryk.rocksdb

import cnames.structs.rocksdb_backup_engine_t
import kotlinx.cinterop.CPointer
import maryk.toUByte
import maryk.wrapWithErrorThrower2
import rocksdb.rocksdb_backup_engine_close
import rocksdb.rocksdb_backup_engine_create_new_backup
import rocksdb.rocksdb_backup_engine_create_new_backup_flush
import rocksdb.rocksdb_backup_engine_get_backup_info
import rocksdb.rocksdb_backup_engine_info_backup_id
import rocksdb.rocksdb_backup_engine_info_count
import rocksdb.rocksdb_backup_engine_info_destroy
import rocksdb.rocksdb_backup_engine_info_number_files
import rocksdb.rocksdb_backup_engine_info_size
import rocksdb.rocksdb_backup_engine_info_timestamp
import rocksdb.rocksdb_backup_engine_open_opts
import rocksdb.rocksdb_backup_engine_purge_old_backups
import rocksdb.rocksdb_backup_engine_restore_db_from_backup
import rocksdb.rocksdb_backup_engine_restore_db_from_latest_backup

actual class BackupEngine
    internal constructor(
        internal val native: CPointer<rocksdb_backup_engine_t>
    )
: RocksObject(), AutoCloseable {
    actual fun createNewBackup(db: RocksDB) {
        wrapWithErrorThrower2 { error ->
            rocksdb_backup_engine_create_new_backup(native, db.native, error)
        }
    }

    actual fun createNewBackup(db: RocksDB, flushBeforeBackup: Boolean) {
        wrapWithErrorThrower2 { error ->
            rocksdb_backup_engine_create_new_backup_flush(native, db.native, flushBeforeBackup.toUByte(), error)
        }
    }

    actual fun createNewBackupWithMetadata(
        db: RocksDB,
        metadata: String,
        flushBeforeBackup: Boolean
    ) {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun getBackupInfo(): List<BackupInfo> {
        return buildList {
            val info = rocksdb_backup_engine_get_backup_info(native)
            val size = rocksdb_backup_engine_info_count(info)

            for (index in 0 until size) {
                this += BackupInfo(
                    backupId = rocksdb_backup_engine_info_backup_id(info, 0).toInt(),
                    timestamp = rocksdb_backup_engine_info_timestamp(info, 0),
                    size = rocksdb_backup_engine_info_size(info, 0).toLong(),
                    numberFiles = rocksdb_backup_engine_info_number_files(info, 0).toInt(),
                    appMetadata = null, // TODO META DATA
                )
            }

            rocksdb_backup_engine_info_destroy(info)
        }
    }

    actual fun getCorruptedBackups(): IntArray {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun garbageCollect() {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun purgeOldBackups(numBackupsToKeep: Int) {
        wrapWithErrorThrower2 { error ->
            rocksdb_backup_engine_purge_old_backups(native, numBackupsToKeep.toUInt(), error)
        }
    }

    actual fun deleteBackup(backupId: Int) {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun restoreDbFromBackup(
        backupId: Int,
        dbDir: String,
        walDir: String,
        restoreOptions: RestoreOptions
    ) {
        wrapWithErrorThrower2 { error ->
            rocksdb_backup_engine_restore_db_from_backup(
                native,
                dbDir,
                walDir,
                restoreOptions.native,
                backupId.toUInt(),
                error,
            )
        }
    }

    actual fun restoreDbFromLatestBackup(
        dbDir: String,
        walDir: String,
        restoreOptions: RestoreOptions
    ) {
        wrapWithErrorThrower2 { error ->
            rocksdb_backup_engine_restore_db_from_latest_backup(
                native,
                dbDir,
                walDir,
                restoreOptions.native,
                error,
            )
        }
    }

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_backup_engine_close(native)
            super.close()
        }
    }
}

actual fun openBackupEngine(
    env: Env,
    options: BackupEngineOptions
) = Unit.wrapWithErrorThrower2 { error ->
    BackupEngine(
        rocksdb_backup_engine_open_opts(options.native, env.native, error)!!
    )
}
