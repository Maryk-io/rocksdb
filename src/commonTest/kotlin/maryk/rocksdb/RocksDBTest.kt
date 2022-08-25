package maryk.rocksdb

import maryk.assertContains
import maryk.assertContainsExactly
import maryk.assertContentEquals
import maryk.doesFolderExist
import maryk.rocksdb.StatusCode.NotSupported
import maryk.rocksdb.util.createTestDBFolder
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class RocksDBTest {
    init {
        loadRocksDBLibrary()
    }

    private fun createTestFolder() = createTestDBFolder("RocksDBTest")

    @Test
    fun open() {
        openRocksDB(createTestFolder()).use { db ->
            assertNotNull(db)
        }
    }

    @Test
    fun open_opt() {
        Options().setCreateIfMissing(true).use { opt ->
            openRocksDB(
                opt,
                createTestFolder()
            ).use { db ->
                assertNotNull(db)
            }
        }
    }

    @Test
    fun openWhenOpen() {
        val dbPath = createTestFolder()

        openRocksDB(dbPath).use {
            try {
                openRocksDB(dbPath).use {
                    fail("Should have thrown an exception when opening the same db twice")
                }
            } catch (e: RocksDBException) {
                assertEquals(StatusCode.IOError, e.getStatus()?.getCode())
                assertEquals(StatusSubCode.None, e.getStatus()?.getSubCode())
                assertTrue(e.getStatus()?.getState()!!.contains("lock "))
            }
        }
    }

    @Test
    fun createColumnFamily() {
        val col1Name = "col1".encodeToByteArray()

        val testFolder = createTestFolder()

        openRocksDB(testFolder).use { db ->
            ColumnFamilyOptions().use { cfOpts ->
                db.createColumnFamily(
                    ColumnFamilyDescriptor(col1Name, cfOpts)
                ).use { col1 ->
                    assertNotNull(col1)
                    assertContentEquals(col1Name, col1.getName())
                }
            }
        }

        val cfHandles = mutableListOf<ColumnFamilyHandle>()
        openRocksDB(
            testFolder,
            listOf(
                ColumnFamilyDescriptor(defaultColumnFamily),
                ColumnFamilyDescriptor(col1Name)
            ),
            cfHandles
        ).use {
            try {
                assertEquals(2, cfHandles.size)
                assertNotNull(cfHandles[1])
                assertContentEquals(col1Name, cfHandles[1].getName())
            } finally {
                for (cfHandle in cfHandles) {
                    cfHandle.close()
                }
            }
        }
    }

    @Test
    fun createColumnFamilies() {
        val col1Name = "col1".encodeToByteArray()
        val col2Name = "col2".encodeToByteArray()

        val testFolder = createTestFolder()

        openRocksDB(testFolder).use { db ->
            ColumnFamilyOptions().use { cfOpts ->
                val cfHandles = db.createColumnFamilies(cfOpts, listOf(col1Name, col2Name))
                try {
                    assertNotNull(cfHandles)
                    assertEquals(2, cfHandles.size)
                    assertContentEquals(col1Name, cfHandles[0].getName())
                    assertContentEquals(col2Name, cfHandles[1].getName())
                } finally {
                    for (cfHandle in cfHandles) {
                        cfHandle.close()
                    }
                }
            }
        }

        val cfHandles = mutableListOf<ColumnFamilyHandle>()
        openRocksDB(
            testFolder,
            listOf(
                ColumnFamilyDescriptor(defaultColumnFamily),
                ColumnFamilyDescriptor(col1Name),
                ColumnFamilyDescriptor(col2Name)
            ),
            cfHandles
        ).use {
            try {
                assertEquals(3, cfHandles.size)
                assertNotNull(cfHandles[1])
                assertContentEquals(col1Name, cfHandles[1].getName())
                assertNotNull(cfHandles[2])
                assertContentEquals(col2Name, cfHandles[2].getName())
            } finally {
                for (cfHandle in cfHandles) {
                    cfHandle.close()
                }
            }
        }
    }

    @Test
    fun createColumnFamiliesFromDescriptors() {
        val col1Name = "col1".encodeToByteArray()
        val col2Name = "col2".encodeToByteArray()

        val testFolder = createTestFolder()
        openRocksDB(testFolder).use { db ->
            ColumnFamilyOptions().use { cfOpts ->
                val cfHandles = db.createColumnFamilies(
                    listOf(
                        ColumnFamilyDescriptor(col1Name, cfOpts),
                        ColumnFamilyDescriptor(col2Name, cfOpts)
                    )
                )
                try {
                    assertNotNull(cfHandles)
                    assertEquals(2, cfHandles.size)
                    assertContentEquals(col1Name, cfHandles[0].getName())
                    assertContentEquals(col2Name, cfHandles[1].getName())
                } finally {
                    for (cfHandle in cfHandles) {
                        cfHandle.close()
                    }
                }
            }
        }

        val cfHandles = mutableListOf<ColumnFamilyHandle>()
        openRocksDB(
            testFolder,
            listOf(
                ColumnFamilyDescriptor(defaultColumnFamily),
                ColumnFamilyDescriptor(col1Name),
                ColumnFamilyDescriptor(col2Name)
            ),
            cfHandles
        ).use {
            try {
                assertEquals(3, cfHandles.size)
                assertNotNull(cfHandles[1])
                assertContentEquals(col1Name, cfHandles[1].getName())
                assertNotNull(cfHandles[2])
                assertContentEquals(col2Name, cfHandles[2].getName())
            } finally {
                for (cfHandle in cfHandles) {
                    cfHandle.close()
                }
            }
        }
    }

    @Test
    fun put() {
        openRocksDB(createTestFolder()).use { db ->
            WriteOptions().use { opt ->
                db.put("key1".encodeToByteArray(), "value".encodeToByteArray())
                db.put(opt, "key2".encodeToByteArray(), "12345678".encodeToByteArray())
                assertContentEquals("value".encodeToByteArray(), db["key1".encodeToByteArray()]!!)
                assertContentEquals("12345678".encodeToByteArray(), db["key2".encodeToByteArray()]!!)

                // put
                val key3 = sliceSegment("key3")
                val key4 = sliceSegment("key4")
                val value0 = sliceSegment("value 0")
                val value1 = sliceSegment("value 1")
                db.put(key3.data, key3.offset, key3.len, value0.data, value0.offset, value0.len)
                db.put(opt, key4.data, key4.offset, key4.len, value1.data, value1.offset, value1.len)

                // compare
                assertTrue(value0.isSamePayload(db.get(key3.data, key3.offset, key3.len)))
                assertTrue(value1.isSamePayload(db.get(key4.data, key4.offset, key4.len)))
            }
        }
    }

    private class Segment(val data: ByteArray, val offset: Int, val len: Int) {
        fun isSamePayload(value: ByteArray?): Boolean {
            if (value == null) {
                return false
            }
            if (value.size != len) {
                return false
            }

            for (i in value.indices) {
                if (data[i + offset] != value[i]) {
                    return false
                }
            }

            return true
        }
    }

    @Test
    fun getWithOutValue() {
        openRocksDB(createTestFolder()).use { db ->
            db.put("key1".encodeToByteArray(), "value".encodeToByteArray())
            db.put("key2".encodeToByteArray(), "12345678".encodeToByteArray())
            val outValue = ByteArray(5)
            // not found value
            var getResult = db.get("keyNotFound".encodeToByteArray(), outValue)
            assertEquals(rocksDBNotFound, getResult)
            // found value which fits in outValue
            getResult = db.get("key1".encodeToByteArray(), outValue)
            assertNotEquals(rocksDBNotFound, getResult)
            assertContentEquals(outValue, "value".encodeToByteArray())
            // found value which fits partially
            getResult = db.get("key2".encodeToByteArray(), outValue)
            assertNotEquals(rocksDBNotFound, getResult)
            assertContentEquals(outValue, "12345".encodeToByteArray())
        }
    }

    @Test
    fun getWithOutValueReadOptions() {
        openRocksDB(createTestFolder()).use { db ->
            ReadOptions().use { rOpt ->
                db.put("key1".encodeToByteArray(), "value".encodeToByteArray())
                db.put("key2".encodeToByteArray(), "12345678".encodeToByteArray())
                val outValue = ByteArray(5)
                // not found value
                var getResult = db.get(
                    rOpt, "keyNotFound".encodeToByteArray(),
                    outValue
                )
                assertEquals(rocksDBNotFound, getResult)
                // found value which fits in outValue
                getResult = db.get(rOpt, "key1".encodeToByteArray(), outValue)
                assertNotEquals(rocksDBNotFound, getResult)
                assertContentEquals(outValue, "value".encodeToByteArray())
                // found value which fits partially
                getResult = db.get(rOpt, "key2".encodeToByteArray(), outValue)
                assertNotEquals(rocksDBNotFound, getResult)
                assertContentEquals(outValue, "12345".encodeToByteArray())
            }
        }
    }

    @Test
    fun multiGetAsList() {
        openRocksDB(createTestFolder()).use { db ->
            ReadOptions().use { rOpt ->
                db.put("key1".encodeToByteArray(), "value".encodeToByteArray())
                db.put("key2".encodeToByteArray(), "12345678".encodeToByteArray())
                val lookupKeys = mutableListOf(
                    "key1".encodeToByteArray(),
                    "key2".encodeToByteArray()
                )
                var results = db.multiGetAsList(lookupKeys)
                assertNotNull(results)
                assertEquals(lookupKeys.size, results.size)
                assertContainsExactly(results, "value".encodeToByteArray(), "12345678".encodeToByteArray())
                // test same method with ReadOptions
                results = db.multiGetAsList(rOpt, lookupKeys)
                assertNotNull(results)
                assertContains(results, "value".encodeToByteArray(), "12345678".encodeToByteArray())

                // remove existing key
                lookupKeys.removeAt(1)
                // add non existing key
                lookupKeys.add("key3".encodeToByteArray())
                results = db.multiGetAsList(lookupKeys)
                assertNotNull(results)
                assertContainsExactly(results, "value".encodeToByteArray(), null)
                // test same call with readOptions
                results = db.multiGetAsList(rOpt, lookupKeys)
                assertNotNull(results)
                assertContains(results, "value".encodeToByteArray())
            }
        }
    }

    @Test
    fun delete() {
        openRocksDB(createTestFolder()).use { db ->
            WriteOptions().use { wOpt ->
                db.put("key1".encodeToByteArray(), "value".encodeToByteArray())
                db.put("key2".encodeToByteArray(), "12345678".encodeToByteArray())
                assertContentEquals("value".encodeToByteArray(), db["key1".encodeToByteArray()])
                assertContentEquals("12345678".encodeToByteArray(), db["key2".encodeToByteArray()])
                db.delete("key1".encodeToByteArray())
                db.delete(wOpt, "key2".encodeToByteArray())
                assertNull(db["key1".encodeToByteArray()])
                assertNull(db["key2".encodeToByteArray()])

                val key3 = sliceSegment("key3")
                val key4 = sliceSegment("key4")
                db.put("key3".encodeToByteArray(), "key3 value".encodeToByteArray())
                db.put("key4".encodeToByteArray(), "key4 value".encodeToByteArray())

                db.delete(key3.data, key3.offset, key3.len)
                db.delete(wOpt, key4.data, key4.offset, key4.len)

                assertNull(db["key3".encodeToByteArray()])
                assertNull(db["key4".encodeToByteArray()])
            }
        }
    }

    @Test
    fun deleteRange() {
        openRocksDB(createTestFolder()).use { db ->
            db.put("key1".encodeToByteArray(), "value".encodeToByteArray())
            db.put("key2".encodeToByteArray(), "12345678".encodeToByteArray())
            db.put("key3".encodeToByteArray(), "abcdefg".encodeToByteArray())
            db.put("key4".encodeToByteArray(), "xyz".encodeToByteArray())
            assertContentEquals("value".encodeToByteArray(), db["key1".encodeToByteArray()])
            assertContentEquals("12345678".encodeToByteArray(), db["key2".encodeToByteArray()])
            assertContentEquals("abcdefg".encodeToByteArray(), db["key3".encodeToByteArray()])
            assertContentEquals("xyz".encodeToByteArray(), db["key4".encodeToByteArray()])
//            db.deleteRange("key2".encodeToByteArray(), "key4".encodeToByteArray())
//            assertContentEquals("value".encodeToByteArray(), db["key1".encodeToByteArray()])
//            assertNull(db["key2".encodeToByteArray()])
//            assertNull(db["key3".encodeToByteArray()])
//            assertContentEquals("xyz".encodeToByteArray(), db["key4".encodeToByteArray()])
        }
    }

    @Test
    fun getIntProperty() {
        Options().apply {
            setCreateIfMissing(true)
            setMaxWriteBufferNumber(10)
            setMinWriteBufferNumberToMerge(10)
        }.use { options ->
            openRocksDB(
                options,
                createTestFolder()
            ).use { db ->
                WriteOptions().setDisableWAL(true).use { wOpt ->
                    db.put(wOpt, "key1".encodeToByteArray(), "value1".encodeToByteArray())
                    db.put(wOpt, "key2".encodeToByteArray(), "value2".encodeToByteArray())
                    db.put(wOpt, "key3".encodeToByteArray(), "value3".encodeToByteArray())
                    db.put(wOpt, "key4".encodeToByteArray(), "value4".encodeToByteArray())
                    assertTrue(db.getLongProperty("rocksdb.num-entries-active-mem-table") > 0)
                    assertTrue(db.getLongProperty("rocksdb.cur-size-active-mem-table") > 0)
                }
            }
        }
    }

    @Test
    fun fullCompactRange() {
        Options().apply {
            setCreateIfMissing(true)
            setDisableAutoCompactions(true)
            setCompactionStyle(CompactionStyle.LEVEL)
            setNumLevels(4)
            setWriteBufferSize((100 shl 10).toLong())
            setLevel0FileNumCompactionTrigger(3)
            setTargetFileSizeBase((200 shl 10).toLong())
            setTargetFileSizeMultiplier(1)
            setMaxBytesForLevelBase((500 shl 10).toLong())
            setMaxBytesForLevelMultiplier(1.0)
            setDisableAutoCompactions(false)
        }.use { opt ->
            openRocksDB(
                opt,
                createTestFolder()
            ).use { db ->
                // fill database with key/value pairs
                val b = ByteArray(10000)
                for (i in 0..199) {
                    Random.nextBytes(b)
                    db.put("$i".encodeToByteArray(), b)
                }
                db.compactRange()
            }
        }
    }

    @Test
    fun fullCompactRangeColumnFamily() {
        DBOptions().apply {
            setCreateIfMissing(true)
            setCreateMissingColumnFamilies(true)
        }.use { opt ->
            ColumnFamilyOptions().apply {
                setDisableAutoCompactions(true)
                setCompactionStyle(CompactionStyle.LEVEL)
                setNumLevels(4).setWriteBufferSize((100 shl 10).toLong())
                setLevel0FileNumCompactionTrigger(3)
                setTargetFileSizeBase((200 shl 10).toLong())
                setTargetFileSizeMultiplier(1)
                setMaxBytesForLevelBase((500 shl 10).toLong())
                setMaxBytesForLevelMultiplier(1.0)
                setDisableAutoCompactions(false)
            }.use { new_cf_opts ->
                val columnFamilyDescriptors = listOf(
                    ColumnFamilyDescriptor(defaultColumnFamily),
                    ColumnFamilyDescriptor("new_cf".encodeToByteArray(), new_cf_opts)
                )

                // open database
                val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()
                openRocksDB(
                    opt,
                    createTestFolder(),
                    columnFamilyDescriptors,
                    columnFamilyHandles
                ).use { db ->
                    try {
                        // fill database with key/value pairs
                        val b = ByteArray(10000)
                        for (i in 0..199) {
                            Random.nextBytes(b)
                            db.put(
                                columnFamilyHandles[1],
                                i.toString().encodeToByteArray(), b
                            )
                        }
                        db.compactRange(columnFamilyHandles[1])
                    } finally {
                        for (handle in columnFamilyHandles) {
                            handle.close()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun compactRangeWithKeys() {
        Options().apply {
            setCreateIfMissing(true)
            setDisableAutoCompactions(true)
            setCompactionStyle(CompactionStyle.LEVEL)
            setNumLevels(4)
            setWriteBufferSize((100 shl 10).toLong())
            setLevel0FileNumCompactionTrigger(3)
            setTargetFileSizeBase((200 shl 10).toLong())
            setTargetFileSizeMultiplier(1)
            setMaxBytesForLevelBase((500 shl 10).toLong())
            setMaxBytesForLevelMultiplier(1.0)
            setDisableAutoCompactions(false)
        }.use { opt ->
            openRocksDB(
                opt,
                createTestFolder()
            ).use { db ->
                // fill database with key/value pairs
                val b = ByteArray(10000)
                for (i in 0..199) {
                    Random.nextBytes(b)
                    db.put(i.toString().encodeToByteArray(), b)
                }
                db.compactRange("0".encodeToByteArray(), "201".encodeToByteArray())
            }
        }
    }

    @Test
    fun compactRangeWithKeysColumnFamily() {
        DBOptions().apply {
            setCreateIfMissing(true)
            setCreateMissingColumnFamilies(true)
        }.use { opt ->
            ColumnFamilyOptions().apply {
                setDisableAutoCompactions(true)
                setCompactionStyle(CompactionStyle.LEVEL)
                setNumLevels(4)
                setWriteBufferSize((100 shl 10).toLong())
                setLevel0FileNumCompactionTrigger(3)
                setTargetFileSizeBase((200 shl 10).toLong())
                setTargetFileSizeMultiplier(1)
                setMaxBytesForLevelBase((500 shl 10).toLong())
                setMaxBytesForLevelMultiplier(1.0)
                setDisableAutoCompactions(false)
            }.use { new_cf_opts ->
                val columnFamilyDescriptors = listOf(
                    ColumnFamilyDescriptor(defaultColumnFamily),
                    ColumnFamilyDescriptor("new_cf".encodeToByteArray(), new_cf_opts)
                )

                // open database
                val columnFamilyHandles = mutableListOf<ColumnFamilyHandle>()
                openRocksDB(
                    opt,
                    createTestFolder(),
                    columnFamilyDescriptors,
                    columnFamilyHandles
                ).use { db ->
                    try {
                        // fill database with key/value pairs
                        val b = ByteArray(10000)
                        for (i in 0..199) {
                            Random.nextBytes(b)
                            db.put(
                                columnFamilyHandles[1],
                                i.toString().encodeToByteArray(), b
                            )
                        }
                        db.compactRange(
                            columnFamilyHandles[1],
                            "0".encodeToByteArray(), "201".encodeToByteArray()
                        )
                    } finally {
                        for (handle in columnFamilyHandles) {
                            handle.close()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun pauseContinueBackgroundWork() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(
                options,
                createTestFolder()
            ).use { db ->
                db.pauseBackgroundWork()
                db.continueBackgroundWork()
                db.pauseBackgroundWork()
                db.continueBackgroundWork()
            }
        }
    }

    @Test
    fun enableDisableFileDeletions() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(
                options,
                createTestFolder()
            ).use { db ->
                db.disableFileDeletions()
                db.enableFileDeletions(false)
                db.disableFileDeletions()
                db.enableFileDeletions(true)
            }
        }
    }

    @Test
    fun destroyDB() {
        Options().setCreateIfMissing(true).use { options ->
            val dbPath = createTestFolder()
            openRocksDB(options, dbPath).use { db ->
                db.put("key1".encodeToByteArray(), "value".encodeToByteArray())
            }
            assertTrue(doesFolderExist(dbPath))
            destroyRocksDB(dbPath, options)
            assertFalse(doesFolderExist(dbPath))
        }
    }

    @Test
    fun destroyDBFailIfOpen() {
        Options().setCreateIfMissing(true).use { options ->
            val dbPath = createTestFolder()
            openRocksDB(options, dbPath).use {
                // Fails as the db is open and locked.
                assertFailsWith<RocksDBException> {
                    destroyRocksDB(dbPath, options)
                }
            }
        }
    }

    @Test
    fun enableAutoCompaction() {
        DBOptions().setCreateIfMissing(true).use { options ->
            val cfDescs = listOf(
                ColumnFamilyDescriptor(defaultColumnFamily)
            )
            val cfHandles = mutableListOf<ColumnFamilyHandle>()
            openRocksDB(options, createTestFolder(), cfDescs, cfHandles).use { db ->
                try {
                    db.enableAutoCompaction(cfHandles)
                } finally {
                    for (cfHandle in cfHandles) {
                        cfHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun numberLevels() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db ->
                assertEquals(7, db.numberLevels())
            }
        }
    }

    @Test
    fun maxMemCompactionLevel() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db ->
                assertEquals(0, db.maxMemCompactionLevel())
            }
        }
    }

    @Test
    fun level0StopWriteTrigger() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db ->
                assertEquals(36, db.level0StopWriteTrigger())
            }
        }
    }

    @Test
    fun getEnv() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db ->
                assertEquals(db.getEnv(), getDefaultEnv())
            }
        }
    }

    @Test
    fun flushWal() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db -> db.flushWal(true) }
        }
    }

    @Test
    fun syncWal() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db -> db.syncWal() }
        }
    }

    @Test
    fun deleteFile() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db ->
                try {
                    db.deleteFile("/000003.log")
                } catch (e: RocksDBException) {
                    // Will throw an exception on Native because log is not archived
                    assertEquals(NotSupported, e.getStatus()?.getCode())
                }
            }
        }
    }

    @Test
    fun getColumnFamilyMetaData() {
        DBOptions().apply {
            setCreateIfMissing(true)
        }.use { options ->
            val cfDescs = listOf(
                ColumnFamilyDescriptor(defaultColumnFamily)
            )
            val cfHandles = mutableListOf<ColumnFamilyHandle>()
            openRocksDB(options, "${createTestFolder()}columnFamilies4", cfDescs, cfHandles).use { db ->
                db.put(cfHandles[0], "key1".encodeToByteArray(), "value1".encodeToByteArray())
                try {
                    val cfMetadata = db.getColumnFamilyMetaData(cfHandles[0])
                    assertNotNull(cfMetadata)
                    assertContentEquals(cfMetadata.name(), defaultColumnFamily)
                    assertEquals(7, cfMetadata.levels().size)
                } finally {
                    for (cfHandle in cfHandles) {
                        cfHandle.close()
                    }
                }
            }
        }
    }

    @Test
    fun verifyChecksum() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db ->
                db.verifyChecksum()
            }
        }
    }

    @Test
    fun promoteL0() {
        Options().setCreateIfMissing(true).use { options ->
            openRocksDB(options, createTestFolder()).use { db ->
                db.promoteL0(2)
            }
        }
    }

    private fun sliceSegment(key: String): Segment {
        val rawKey = ByteArray(key.length + 4)
        val keyBytes = key.encodeToByteArray()
        keyBytes.copyInto(rawKey, 2, 0, keyBytes.size)
        return Segment(rawKey, 2, key.length)
    }
}
