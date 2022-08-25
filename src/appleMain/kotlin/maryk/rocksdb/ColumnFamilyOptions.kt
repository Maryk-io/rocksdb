package maryk.rocksdb

import cnames.structs.rocksdb_options_t
import kotlinx.cinterop.CPointer
import maryk.rocksdb.util.BytewiseComparator
import maryk.rocksdb.util.ReverseBytewiseComparator
import maryk.toBoolean
import rocksdb.rocksdb_options_create
import rocksdb.rocksdb_options_destroy
import rocksdb.rocksdb_options_get_arena_block_size
import rocksdb.rocksdb_options_get_bloom_locality
import rocksdb.rocksdb_options_get_compaction_style
import rocksdb.rocksdb_options_get_compression
import rocksdb.rocksdb_options_get_disable_auto_compactions
import rocksdb.rocksdb_options_get_level0_file_num_compaction_trigger
import rocksdb.rocksdb_options_get_level0_slowdown_writes_trigger
import rocksdb.rocksdb_options_get_level0_stop_writes_trigger
import rocksdb.rocksdb_options_get_max_bytes_for_level_base
import rocksdb.rocksdb_options_get_max_bytes_for_level_multiplier
import rocksdb.rocksdb_options_get_max_sequential_skip_in_iterations
import rocksdb.rocksdb_options_get_max_successive_merges
import rocksdb.rocksdb_options_get_max_write_buffer_number
import rocksdb.rocksdb_options_get_max_write_buffer_number_to_maintain
import rocksdb.rocksdb_options_get_memtable_huge_page_size
import rocksdb.rocksdb_options_get_memtable_prefix_bloom_size_ratio
import rocksdb.rocksdb_options_get_min_write_buffer_number_to_merge
import rocksdb.rocksdb_options_get_num_levels
import rocksdb.rocksdb_options_get_target_file_size_base
import rocksdb.rocksdb_options_get_target_file_size_multiplier
import rocksdb.rocksdb_options_get_write_buffer_size
import rocksdb.rocksdb_options_set_arena_block_size
import rocksdb.rocksdb_options_set_bloom_locality
import rocksdb.rocksdb_options_set_compaction_style
import rocksdb.rocksdb_options_set_comparator
import rocksdb.rocksdb_options_set_compression
import rocksdb.rocksdb_options_set_disable_auto_compactions
import rocksdb.rocksdb_options_set_level0_file_num_compaction_trigger
import rocksdb.rocksdb_options_set_level0_slowdown_writes_trigger
import rocksdb.rocksdb_options_set_level0_stop_writes_trigger
import rocksdb.rocksdb_options_set_max_bytes_for_level_base
import rocksdb.rocksdb_options_set_max_bytes_for_level_multiplier
import rocksdb.rocksdb_options_set_max_sequential_skip_in_iterations
import rocksdb.rocksdb_options_set_max_successive_merges
import rocksdb.rocksdb_options_set_max_write_buffer_number
import rocksdb.rocksdb_options_set_max_write_buffer_number_to_maintain
import rocksdb.rocksdb_options_set_memtable_huge_page_size
import rocksdb.rocksdb_options_set_memtable_prefix_bloom_size_ratio
import rocksdb.rocksdb_options_set_min_write_buffer_number_to_merge
import rocksdb.rocksdb_options_set_num_levels
import rocksdb.rocksdb_options_set_prefix_extractor
import rocksdb.rocksdb_options_set_target_file_size_base
import rocksdb.rocksdb_options_set_target_file_size_multiplier
import rocksdb.rocksdb_options_set_write_buffer_size
import rocksdb.rocksdb_slicetransform_create_fixed_prefix

actual class ColumnFamilyOptions private constructor(
    internal val native: CPointer<rocksdb_options_t>
) : RocksObject() {
    actual constructor() : this(rocksdb_options_create()!!)

    override fun close() {
        if (isOwningHandle()) {
            rocksdb_options_destroy(native)
            super.close()
        }
    }

    actual fun setMinWriteBufferNumberToMerge(minWriteBufferNumberToMerge: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_min_write_buffer_number_to_merge(native, minWriteBufferNumberToMerge)
        return this
    }

    actual fun minWriteBufferNumberToMerge(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_min_write_buffer_number_to_merge(native)
    }

    actual fun setMaxWriteBufferNumberToMaintain(maxWriteBufferNumberToMaintain: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_max_write_buffer_number_to_maintain(native, maxWriteBufferNumberToMaintain)
        return this
    }

    actual fun maxWriteBufferNumberToMaintain(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_max_write_buffer_number_to_maintain(native)
    }

    actual fun setBloomLocality(bloomLocality: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_bloom_locality(native, bloomLocality.toUInt())
        return this
    }

    actual fun bloomLocality(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_bloom_locality(native).toInt()
    }

    actual fun setNumLevels(numLevels: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_num_levels(native, numLevels)
        return this
    }

    actual fun numLevels(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_num_levels(native)
    }

    actual fun setCompactionStyle(compactionStyle: CompactionStyle): ColumnFamilyOptions {
        rocksdb_options_set_compaction_style(native, compactionStyle.value.toInt())
        return this
    }

    actual fun compactionStyle(): CompactionStyle {
        return getCompactionStyle(rocksdb_options_get_compaction_style(native).toByte())
    }

    actual fun setComparator(builtinComparator: BuiltinComparator): ColumnFamilyOptions {
        val comparator = when (builtinComparator) {
            BuiltinComparator.BYTEWISE_COMPARATOR -> BytewiseComparator(null)
            BuiltinComparator.REVERSE_BYTEWISE_COMPARATOR -> ReverseBytewiseComparator(null)
            else -> throw Exception("Unknown comparator")
        }
        rocksdb_options_set_comparator(native, comparator.native)
        return this
    }

    actual fun setComparator(comparator: AbstractComparator): ColumnFamilyOptions {
        rocksdb_options_set_comparator(native, comparator.native)
        return this
    }

    actual fun useFixedLengthPrefixExtractor(n: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        val prefix = rocksdb_slicetransform_create_fixed_prefix(n.toULong())
        rocksdb_options_set_prefix_extractor(native, prefix)
        // Should it be destroyed?
//        rocksdb_slicetransform_destroy(prefix)
        return this
    }

    actual fun setMaxBytesForLevelMultiplier(multiplier: Double): ColumnFamilyOptions {
        rocksdb_options_set_max_bytes_for_level_multiplier(native, multiplier)
        return this
    }

    actual fun maxBytesForLevelMultiplier(): Double =
        rocksdb_options_get_max_bytes_for_level_multiplier(native)

    actual fun setWriteBufferSize(writeBufferSize: Long): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_write_buffer_size(native, writeBufferSize.toULong())
        return this
    }

    actual fun writeBufferSize(): Long {
        assert(isOwningHandle())
        return rocksdb_options_get_write_buffer_size(native).toLong()
    }

    actual fun setDisableAutoCompactions(disableAutoCompactions: Boolean): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_disable_auto_compactions(native, if (disableAutoCompactions) 1 else 0)
        return this
    }

    actual fun disableAutoCompactions(): Boolean {
        assert(isOwningHandle())
        return rocksdb_options_get_disable_auto_compactions(native).toBoolean()
    }

    actual fun setLevel0FileNumCompactionTrigger(level0FileNumCompactionTrigger: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_level0_file_num_compaction_trigger(native, level0FileNumCompactionTrigger)
        return this
    }

    actual fun level0FileNumCompactionTrigger(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_level0_file_num_compaction_trigger(native)
    }

    actual fun setMaxBytesForLevelBase(maxBytesForLevelBase: Long): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_max_bytes_for_level_base(native, maxBytesForLevelBase.toULong())
        return this
    }

    actual fun maxBytesForLevelBase(): Long {
        assert(isOwningHandle())
        return rocksdb_options_get_max_bytes_for_level_base(native).toLong()
    }

    actual fun setCompressionType(compressionType: CompressionType): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_compression(native, compressionType.value.toInt())
        return this
    }

    actual fun compressionType(): CompressionType {
        assert(isOwningHandle())
        return getCompressionType(
            rocksdb_options_get_compression(native).toByte()
        )
    }

    actual fun setMaxWriteBufferNumber(maxWriteBufferNumber: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_max_write_buffer_number(native, maxWriteBufferNumber)
        return this
    }

    actual fun maxWriteBufferNumber(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_max_write_buffer_number(native)
    }

    actual fun setMemtablePrefixBloomSizeRatio(memtablePrefixBloomSizeRatio: Double): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_memtable_prefix_bloom_size_ratio(native, memtablePrefixBloomSizeRatio)
        return this
    }

    actual fun memtablePrefixBloomSizeRatio(): Double {
        assert(isOwningHandle())
        return rocksdb_options_get_memtable_prefix_bloom_size_ratio(native)
    }

    actual fun setMemtableHugePageSize(memtableHugePageSize: Long): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_memtable_huge_page_size(native, memtableHugePageSize.toULong())
        return this
    }

    actual fun memtableHugePageSize(): Long {
        assert(isOwningHandle())
        return rocksdb_options_get_memtable_huge_page_size(native).toLong()
    }

    actual fun setArenaBlockSize(arenaBlockSize: Long): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_arena_block_size(native, arenaBlockSize.toULong())
        return this
    }

    actual fun arenaBlockSize(): Long {
        assert(isOwningHandle())
        return rocksdb_options_get_arena_block_size(native).toLong()
    }

    actual fun setLevel0SlowdownWritesTrigger(level0SlowdownWritesTrigger: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_level0_slowdown_writes_trigger(native, level0SlowdownWritesTrigger)
        return this
    }

    actual fun level0SlowdownWritesTrigger(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_level0_slowdown_writes_trigger(native)
    }

    actual fun setLevel0StopWritesTrigger(level0StopWritesTrigger: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_level0_stop_writes_trigger(native, level0StopWritesTrigger)
        return this
    }

    actual fun level0StopWritesTrigger(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_level0_stop_writes_trigger(native)
    }

    actual fun setTargetFileSizeBase(targetFileSizeBase: Long): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_target_file_size_base(native, targetFileSizeBase.toULong())
        return this
    }

    actual fun targetFileSizeBase(): Long {
        assert(isOwningHandle())
        return rocksdb_options_get_target_file_size_base(native).toLong()
    }

    actual fun setTargetFileSizeMultiplier(multiplier: Int): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_target_file_size_multiplier(native, multiplier)
        return this
    }

    actual fun targetFileSizeMultiplier(): Int {
        assert(isOwningHandle())
        return rocksdb_options_get_target_file_size_multiplier(native)
    }

    actual fun setMaxSequentialSkipInIterations(maxSequentialSkipInIterations: Long): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_max_sequential_skip_in_iterations(native, maxSequentialSkipInIterations.toULong())
        return this
    }

    actual fun maxSequentialSkipInIterations(): Long {
        assert(isOwningHandle())
        return rocksdb_options_get_max_sequential_skip_in_iterations(native).toLong()
    }

    actual fun setMaxSuccessiveMerges(maxSuccessiveMerges: Long): ColumnFamilyOptions {
        assert(isOwningHandle())
        rocksdb_options_set_max_successive_merges(native, maxSuccessiveMerges.toULong())
        return this
    }

    actual fun maxSuccessiveMerges(): Long {
        assert(isOwningHandle())
        return rocksdb_options_get_max_successive_merges(native).toLong()
    }
}
