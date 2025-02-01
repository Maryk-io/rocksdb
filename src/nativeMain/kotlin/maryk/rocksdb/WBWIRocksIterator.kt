package maryk.rocksdb

import cnames.structs.rocksdb_iterator_t
import kotlinx.cinterop.CPointer

actual class WBWIRocksIterator internal constructor(
    native: CPointer<rocksdb_iterator_t>
) : AbstractRocksIterator<WriteBatchWithIndex>(native) {
    actual fun entry(): WriteEntry {
        return TODO()
    }
}
