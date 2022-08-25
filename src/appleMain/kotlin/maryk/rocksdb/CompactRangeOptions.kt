package maryk.rocksdb

import cnames.structs.rocksdb_compactoptions_t
import kotlinx.cinterop.CPointer
import maryk.toBoolean
import maryk.toUByte
import rocksdb.rocksdb_compactoptions_create
import rocksdb.rocksdb_compactoptions_destroy
import rocksdb.rocksdb_compactoptions_get_bottommost_level_compaction
import rocksdb.rocksdb_compactoptions_get_change_level
import rocksdb.rocksdb_compactoptions_get_exclusive_manual_compaction
import rocksdb.rocksdb_compactoptions_get_target_level
import rocksdb.rocksdb_compactoptions_set_bottommost_level_compaction
import rocksdb.rocksdb_compactoptions_set_change_level
import rocksdb.rocksdb_compactoptions_set_exclusive_manual_compaction
import rocksdb.rocksdb_compactoptions_set_target_level

actual class CompactRangeOptions internal constructor(
    internal val native: CPointer<rocksdb_compactoptions_t>
) : RocksObject() {
    actual constructor() : this(rocksdb_compactoptions_create()!!)

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_compactoptions_destroy(native)
            super.close()
        }
    }

    actual fun exclusiveManualCompaction(): Boolean =
        rocksdb_compactoptions_get_exclusive_manual_compaction(native).toBoolean()

    actual fun setExclusiveManualCompaction(exclusiveCompaction: Boolean): CompactRangeOptions {
        rocksdb_compactoptions_set_exclusive_manual_compaction(native, exclusiveCompaction.toUByte())
        return this
    }

    actual fun changeLevel(): Boolean =
        rocksdb_compactoptions_get_change_level(native).toBoolean()

    actual fun setChangeLevel(changeLevel: Boolean): CompactRangeOptions {
        rocksdb_compactoptions_set_change_level(native, changeLevel.toUByte())
        return this
    }

    actual fun targetLevel(): Int =
        rocksdb_compactoptions_get_target_level(native)

    actual fun setTargetLevel(targetLevel: Int): CompactRangeOptions {
        rocksdb_compactoptions_set_target_level(native, targetLevel)
        return this
    }

    actual fun targetPathId(): Int {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun setTargetPathId(targetPathId: Int): CompactRangeOptions {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun bottommostLevelCompaction(): BottommostLevelCompaction? =
        bottommostLevelCompactionFromByte(rocksdb_compactoptions_get_bottommost_level_compaction(native))

    actual fun setBottommostLevelCompaction(bottommostLevelCompaction: BottommostLevelCompaction): CompactRangeOptions {
        rocksdb_compactoptions_set_bottommost_level_compaction(native, bottommostLevelCompaction.value)
        return this
    }

    actual fun allowWriteStall(): Boolean {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun setAllowWriteStall(allowWriteStall: Boolean): CompactRangeOptions {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun maxSubcompactions(): Int {
        throw NotImplementedError("DO SOMETHING")
    }

    actual fun setMaxSubcompactions(maxSubcompactions: Int): CompactRangeOptions {
        throw NotImplementedError("DO SOMETHING")
    }
}
