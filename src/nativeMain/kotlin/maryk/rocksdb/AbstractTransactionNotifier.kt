package maryk.rocksdb

actual abstract class AbstractTransactionNotifier: RocksCallbackObject() {
    actual abstract fun snapshotCreated(newSnapshot: Snapshot)
}
