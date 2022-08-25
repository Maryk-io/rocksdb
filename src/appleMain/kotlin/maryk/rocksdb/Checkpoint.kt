package maryk.rocksdb

import cnames.structs.rocksdb_checkpoint_t
import kotlinx.cinterop.CPointer
import maryk.wrapWithErrorThrower2
import rocksdb.rocksdb_checkpoint_create
import rocksdb.rocksdb_checkpoint_object_create

actual class Checkpoint
    internal constructor(private val native: CPointer<rocksdb_checkpoint_t>?)
: RocksObject() {
    actual fun createCheckpoint(checkpointPath: String) {
        wrapWithErrorThrower2 { error ->
            rocksdb_checkpoint_create(native, checkpointPath, 1024, error)
        }
    }
}

actual fun createCheckpoint(db: RocksDB): Checkpoint {
    check(db.isOwningHandle()) { "RocksDB instance must be initialized." }
    return Unit.wrapWithErrorThrower2 { error ->
        Checkpoint(
            rocksdb_checkpoint_object_create(db.native, error)
        )
    }
}
