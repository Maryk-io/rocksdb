package maryk.rocksdb

import cnames.structs.rocksdb_comparator_t
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import maryk.ByteBuffer
import rocksdb.rocksdb_comparator_create
import rocksdb.rocksdb_comparator_destroy

actual abstract class AbstractComparator
    protected actual constructor(val copt: ComparatorOptions?)
: RocksCallbackObject() {
    protected actual constructor() : this(null)

    private val namePointer = nativeHeap.alloc<ByteVar>()
    private val state = nativeHeap.alloc<ByteVar>()

//    private val refToComparator = StableRef.create(this)

    val native: CPointer<rocksdb_comparator_t>? get() {
        throw NotImplementedError("DO SOMETHING")
//        return rocksdb_comparator_create(
//            state = state.ptr,
//            compare = staticCFunction { refToComparator, a, aLen, b, bLen ->
//        //            println(refToComparator)
//                    0
//        //            (refToComparator?. as AbstractComparator).compare()
//        //            refToCompare.get().invoke(DirectByteBuffer(a!!, aLen.toInt()), DirectByteBuffer(b!!, bLen.toInt()))
//            },
//            destructor = staticCFunction { refToComparator ->
//            },
//            name = staticCFunction<COpaquePointer?, CPointer<ByteVar>?> { null },
//        )!!
    }


    actual override fun close() {
        if (isOwningHandle()) {
//            refToComparator.dispose()
            nativeHeap.free(namePointer.rawPtr)
            nativeHeap.free(state.rawPtr)
            rocksdb_comparator_destroy(native)
            super.close()
        }
    }

    actual abstract fun name(): String

    actual abstract fun compare(a: ByteBuffer, b: ByteBuffer): Int

    actual open fun findShortestSeparator(start: ByteBuffer, limit: ByteBuffer) {
        // no opp
    }

    actual open fun findShortSuccessor(key: ByteBuffer) {
        // no opp
    }
}
