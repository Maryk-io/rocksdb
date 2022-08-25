package maryk

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import maryk.rocksdb.RocksDBException
import maryk.rocksdb.Status
import maryk.rocksdb.getStatusCode
import maryk.rocksdb.getStatusSubCode
import platform.Foundation.NSError

fun <T: Any, R: Any> T.wrapWithErrorThrower2(runnable: T.(CValuesRef<CPointerVar<ByteVar>>) -> R): R {
    memScoped {
        val errorRef = alloc<CPointerVar<ByteVar>>()
        val result = runnable(errorRef.ptr)
        val error = errorRef.value?.toKString()

        if (error != null) {
//            val status = getStatusCode(error)
            throw RocksDBException(error, null)
        }

        return result
    }
}

fun <T: Any, R: Any> T.wrapWithNullErrorThrower2(runnable: T.(CValuesRef<CPointerVar<ByteVar>>) -> R?): R? {
    memScoped {
        val errorRef = alloc<CPointerVar<ByteVar>>()
        val result = runnable(errorRef.ptr)
        val error = errorRef.value?.toKString()

//        if (error == NotFound.value) {
//            return null
//        }

        if (error != null) {
//            val status = getStatusCode(error)

            throw RocksDBException(error, null)
        }

        return result
    }
}

private fun convertStatus(error: NSError) = Status(
    getStatusCode(error.code.toByte()),
    error.userInfo["rocksdb.subcode"]?.let { getStatusSubCode((it as Short).toByte()) },
    error.localizedFailureReason
)
