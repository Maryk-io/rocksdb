package maryk.rocksdb

import cnames.structs.rocksdb_writebatch_t
import kotlinx.cinterop.CPointer
import rocksdb.rocksdb_writebatch_create

actual class WriteBatch(
    internal val native: CPointer<rocksdb_writebatch_t>
) : AbstractWriteBatch(native) {
    actual constructor() : this(rocksdb_writebatch_create()!!)

    actual fun getDataSize(): Long = throw NotImplementedError("DO SOMETHING")

    actual fun getWalTerminationPoint(): WriteBatchSavePoint {
        throw NotImplementedError("DO SOMETHING")
//        val terminationPoint = native.getWalTerminationPoint()
//        return WriteBatchSavePoint(
//            terminationPoint.size.toLong(),
//            terminationPoint.count.toLong(),
//            terminationPoint.contentFlags.toLong()
//        )
    }

    actual fun data(): ByteArray = throw NotImplementedError("DO SOMETHING")

    actual fun hasPut(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun hasDelete(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun hasSingleDelete(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun hasDeleteRange(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun hasMerge(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun hasBeginPrepare(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun hasEndPrepare(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun hasCommit(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun hasRollback(): Boolean = throw NotImplementedError("DO SOMETHING")

    actual fun markWalTerminationPoint() {
        throw NotImplementedError("DO SOMETHING")
    }

    override fun getWriteBatch(): WriteBatch {
        return this
    }
}
