package maryk.rocksdb

actual class GetStatus internal constructor(
    internal val status: Status,
    internal val requiredSize: Int
)

actual fun GetStatus.getStatus(): Status = this.status

actual fun GetStatus.getRequiredSize(): Int = this.requiredSize
