package maryk.rocksdb

import maryk.ByteBuffer
import maryk.allocateByteBuffer
import maryk.allocateDirectByteBuffer
import maryk.assertContainsExactly
import maryk.assertContentEquals
import maryk.createFolder
import maryk.deleteFolder
import maryk.flip
import maryk.rocksdb.util.createTestDBFolder
import kotlin.random.Random
import kotlin.test.*

val TXN_TEST_COLUMN_FAMILY: ByteArray = "txn_test_cf".encodeToByteArray()

/**
 * Base class of [TransactionTest] and [OptimisticTransactionTest]
 */
abstract class AbstractTransactionTest {
    protected lateinit var dbFolder: String

    protected abstract fun startDb(): DBContainer

    @BeforeTest
    fun setUp() {
        dbFolder = createTestDBFolder("RocksDBTest")
        createFolder(dbFolder)
    }

    @AfterTest
    fun tearDown() {
        deleteFolder(dbFolder)
    }

    @Test
    fun setSnapshot() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.setSnapshot()
            }
        }
    }

    @Test
    fun setSnapshotOnNextOperation() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.setSnapshotOnNextOperation()
                txn.put("key1".encodeToByteArray(), "value1".encodeToByteArray())
            }
        }
    }

    @Test
    fun setSnapshotOnNextOperation_transactionNotifier() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                TestTransactionNotifier().use { notifier ->
                    txn.setSnapshotOnNextOperation(notifier)
                    txn.put("key1".encodeToByteArray(), "value1".encodeToByteArray())

                    txn.setSnapshotOnNextOperation(notifier)
                    txn.put("key2".encodeToByteArray(), "value2".encodeToByteArray())

                    assertEquals(2, notifier.getCreatedSnapshots().size)
                }
            }
        }
    }

    @Test
    fun getSnapshot() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.setSnapshot()
                val snapshot = txn.getSnapshot()
                assertFalse(snapshot!!.isOwningHandle())
            }
        }
    }

    @Test
    fun getSnapshot_null() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                val snapshot = txn.getSnapshot()
                assertNull(snapshot)
            }
        }
    }

    @Test
    fun clearSnapshot() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.setSnapshot()
                txn.clearSnapshot()
            }
        }
    }

    @Test
    fun clearSnapshot_none() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.clearSnapshot()
            }
        }
    }

    @Test
    fun commit() {
        val k1 = "rollback-key1".encodeToByteArray()
        val v1 = "rollback-value1".encodeToByteArray()
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.put(k1, v1)
                txn.commit()
            }

            dbContainer.beginTransaction().use { txn2 ->
                val readOptions = ReadOptions()
                assertContentEquals(v1, txn2.get(readOptions, k1))
            }
        }
    }

    @Test
    fun rollback() {
        val k1 = "rollback-key1".encodeToByteArray()
        val v1 = "rollback-value1".encodeToByteArray()
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.put(k1, v1)
                txn.rollback()
            }

            dbContainer.beginTransaction().use { txn2 ->
                val readOptions = ReadOptions()
                assertNull(txn2.get(readOptions, k1))
            }
        }
    }

    @Test
    fun savePoint() {
        val k1 = "savePoint-key1".encodeToByteArray()
        val v1 = "savePoint-value1".encodeToByteArray()
        val k2 = "savePoint-key2".encodeToByteArray()
        val v2 = "savePoint-value2".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                txn.put(k1, v1)

                assertContentEquals(v1, txn.get(readOptions, k1))

                txn.setSavePoint()

                txn.put(k2, v2)

                assertContentEquals(v1, txn.get(readOptions, k1))
                assertContentEquals(v2, txn.get(readOptions, k2))

                txn.rollbackToSavePoint()

                assertContentEquals(v1, txn.get(readOptions, k1))
                assertNull(txn.get(readOptions, k2))

                txn.commit()
            }

            dbContainer.beginTransaction().use { txn2 ->
                assertContentEquals(v1, txn2.get(readOptions, k1))
                assertNull(txn2.get(readOptions, k2))
            }
        }
    }

    @Test
    fun getPut_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                assertNull(txn.get(readOptions, testCf, k1))

                txn.put(testCf, k1, v1)

                assertContentEquals(v1, txn.get(readOptions, testCf, k1))
            }
        }
    }

    @Test
    fun getPut() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                assertNull(txn.get(readOptions, k1))
                txn.put(k1, v1)
                assertContentEquals(v1, txn.get(readOptions, k1))
            }
        }
    }

    @Test
    fun getPutTargetBuffer_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                val target = "overwrite1".encodeToByteArray()
                val status = txn.get(readOptions, testCf, k1, target)
                assertEquals(StatusCode.NotFound, status.getStatus().getCode())
                assertEquals(0, status.getRequiredSize())

                txn.put(testCf, k1, v1)
                val statusOk = txn.get(readOptions, testCf, k1, target)
                assertEquals(StatusCode.Ok, statusOk.getStatus().getCode())
                assertEquals(v1.size, statusOk.getRequiredSize())
                assertContentEquals("value1ite1".encodeToByteArray(), target)
            }
        }
    }

    @Test
    fun getPutTargetBuffer() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val target = "overwrite1".encodeToByteArray()
                var status = txn.get(readOptions, k1, target)
                assertEquals(StatusCode.NotFound, status.getStatus().getCode())
                assertEquals(0, status.getRequiredSize())

                txn.put(k1, v1)
                status = txn.get(readOptions, k1, target)
                assertEquals(StatusCode.Ok, status.getStatus().getCode())
                assertEquals(v1.size, status.getRequiredSize())
                assertContentEquals("value1ite1".encodeToByteArray(), target)
            }
        }
    }

    @Test
    fun multiGetPutAsList_cf() {
        val keys = listOf("key1".encodeToByteArray(), "key2".encodeToByteArray())
        val values = listOf("value1".encodeToByteArray(), "value2".encodeToByteArray())

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                val cfList = listOf(testCf, testCf)

                assertContentEquals(listOf<ByteArray?>(null, null), txn.multiGetAsList(readOptions, cfList, keys))

                txn.put(testCf, keys[0], values[0])
                txn.put(testCf, keys[1], values[1])

                assertContainsExactly(values, txn.multiGetAsList(readOptions, cfList, keys))
            }
        }
    }

    @Test
    fun multiGetPutAsList() {
        val keys = listOf("key1".encodeToByteArray(), "key2".encodeToByteArray())
        val values = listOf("value1".encodeToByteArray(), "value2".encodeToByteArray())

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                assertContentEquals(listOf<ByteArray?>(null, null), txn.multiGetAsList(readOptions, keys))

                txn.put(keys[0], values[0])
                txn.put(keys[1], values[1])

                assertContainsExactly(values, txn.multiGetAsList(readOptions, keys))
            }
        }
    }

    @Test
    fun getForUpdate_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                assertNull(txn.getForUpdate(readOptions, testCf, k1, true))
                txn.put(testCf, k1, v1)
                assertContentEquals(v1, txn.getForUpdate(readOptions, testCf, k1, true))
            }
        }
    }

    @Test
    fun getForUpdate() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                assertNull(txn.getForUpdate(readOptions, k1, true))
                txn.put(k1, v1)
                assertContentEquals(v1, txn.getForUpdate(readOptions, k1, true))
            }
        }
    }

    @Test
    fun getForUpdateByteArray_cf_doValidate() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                val vNonExistent = ByteArray(1)
                val sNonExistent = txn.getForUpdate(readOptions, testCf, k1, vNonExistent, true, true)
                assertEquals(StatusCode.NotFound, sNonExistent.getStatus().getCode())

                txn.put(testCf, k1, v1)

                val vPartial = ByteArray(4)
                val sPartial = txn.getForUpdate(readOptions, testCf, k1, vPartial, true, true)
                assertEquals(StatusCode.Ok, sPartial.getStatus().getCode())
                assertEquals(v1.size, sPartial.getRequiredSize())
                assertContentEquals(v1.copyOfRange(0, vPartial.size), vPartial)

                val vTotal = ByteArray(sPartial.getRequiredSize())
                val sTotal = txn.getForUpdate(readOptions, testCf, k1, vTotal, true, true)
                assertEquals(StatusCode.Ok, sTotal.getStatus().getCode())
                assertEquals(v1.size, sTotal.getRequiredSize())
                assertContentEquals(v1, vTotal)
            }
        }
    }

    @Test
    fun getForUpdateByteArray_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                val vNonExistent = ByteArray(1)
                val sNonExistent = txn.getForUpdate(readOptions, testCf, k1, vNonExistent, true)
                assertEquals(StatusCode.NotFound, sNonExistent.getStatus().getCode())

                txn.put(testCf, k1, v1)

                val vPartial = ByteArray(4)
                val sPartial = txn.getForUpdate(readOptions, testCf, k1, vPartial, true)
                assertEquals(StatusCode.Ok, sPartial.getStatus().getCode())
                assertEquals(v1.size, sPartial.getRequiredSize())
                assertContentEquals(v1.copyOfRange(0, vPartial.size), vPartial)

                val vTotal = ByteArray(sPartial.getRequiredSize())
                val sTotal = txn.getForUpdate(readOptions, testCf, k1, vTotal, true)
                assertEquals(StatusCode.Ok, sTotal.getStatus().getCode())
                assertEquals(v1.size, sTotal.getRequiredSize())
                assertContentEquals(v1, vTotal)
            }
        }
    }

    @Test
    fun getForUpdateByteBuffer() {
        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                allocateDirectByteBuffer(20) { k1 ->
                    k1.put("key1".encodeToByteArray())
                    k1.flip()
                    allocateDirectByteBuffer(20) { v1Read1 ->
                        val getStatus1 = txn.getForUpdate(readOptions, k1, v1Read1, true)
                        assertEquals(StatusCode.NotFound, getStatus1.getStatus().getCode())

                        allocateDirectByteBuffer(20) { v1 ->
                            v1.put("value1".encodeToByteArray())
                            v1.flip()
                            txn.put(k1, v1)
                        }

                        allocateDirectByteBuffer(20) { v1Read2 ->
                            val getStatus2 = txn.getForUpdate(readOptions, k1, v1Read2, true)
                            assertEquals(StatusCode.Ok, getStatus2.getStatus().getCode())
                            assertEquals("value1".length, getStatus2.getRequiredSize())

                            allocateDirectByteBuffer(20) { expectedBuffer ->
                                expectedBuffer.put("value1".encodeToByteArray())
                                expectedBuffer.flip()
                                assertContentEquals(v1Read2.array(), expectedBuffer.array())
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun getForUpdateDirectByteBuffer_cf() {
        getForUpdateByteBuffer_cf(::allocateDirectByteBuffer)
    }

    @Test
    fun getForUpdateIndirectByteBuffer_cf() {
        getForUpdateByteBuffer_cf(::allocateByteBuffer)
    }

    private fun getForUpdateByteBuffer_cf(allocateBuffer: (Int, (ByteBuffer) -> Unit) -> Unit) {
        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()

                allocateBuffer(20) { k1 ->
                    k1.put("key1".encodeToByteArray())
                    k1.flip()

                    allocateBuffer(20) { v1Read1 ->
                        val getStatus1 = txn.getForUpdate(readOptions, testCf, k1, v1Read1, true)
                        assertEquals(StatusCode.NotFound, getStatus1.getStatus().getCode())

                        allocateBuffer(20) { v1 ->
                            v1.put("value1".encodeToByteArray())
                            v1.flip()

                            txn.put(testCf, k1, v1)

                            k1.flip()
                            v1.flip()

                            allocateBuffer(20) { k2 ->
                                k2.put("key2".encodeToByteArray())
                                k2.flip()
                                allocateBuffer(20) { v2 ->
                                    v2.put("value2".encodeToByteArray())
                                    v2.flip()

                                    txn.put(testCf, k2, v2)

                                    k2.flip()
                                    v2.flip()
                                }
                            }

                            allocateBuffer(20) { v1Read2 ->
                                val getStatus2 = txn.getForUpdate(readOptions, testCf, k1, v1Read2, true)
                                assertEquals(StatusCode.NotFound, getStatus2.getStatus().getCode())
                            }

                            txn.put(testCf, k1, v1)

                            v1.flip()
                        }

                        allocateBuffer(20) { v1Read3 ->
                            val getStatus3 = txn.getForUpdate(readOptions, testCf, k1, v1Read3, true)
                            assertEquals(StatusCode.Ok, getStatus3.getStatus().getCode())
                            assertEquals("value1".length, getStatus3.getRequiredSize())

                            allocateBuffer(20) { expectedBuffer ->
                                expectedBuffer.put("value1".encodeToByteArray())
                                expectedBuffer.flip()
                                assertContentEquals(v1Read3.array(), expectedBuffer.array())
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun multiGetForUpdateAsList_cf() {
        val keys = listOf("key1".encodeToByteArray(), "key2".encodeToByteArray())
        val values = listOf("value1".encodeToByteArray(), "value2".encodeToByteArray())

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                val cfList = listOf(testCf, testCf)

                assertContentEquals(listOf<ByteArray?>(null, null), txn.multiGetForUpdateAsList(readOptions, cfList, keys))

                txn.put(testCf, keys[0], values[0])
                txn.put(testCf, keys[1], values[1])

                val result = txn.multiGetForUpdateAsList(readOptions, cfList, keys)
                assertEquals(values.size, result.size)
                for (i in values.indices) {
                    assertContentEquals(values[i], result[i])
                }
            }
        }
    }

    @Test
    fun multiGetForUpdateAsList() {
        val keys = listOf("key1".encodeToByteArray(), "key2".encodeToByteArray())
        val values = listOf("value1".encodeToByteArray(), "value2".encodeToByteArray())

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val nulls = listOf<ByteArray?>(null, null)
                assertContentEquals(nulls, txn.multiGetForUpdateAsList(readOptions, keys))

                txn.put(keys[0], values[0])
                txn.put(keys[1], values[1])

                val result = txn.multiGetForUpdateAsList(readOptions, keys)
                assertEquals(values.size, result.size)
                for (i in values.indices) {
                    assertContentEquals(values[i], result[i])
                }
            }
        }
    }

    @Test
    fun getIterator() {
        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val k1 = "key1".encodeToByteArray()
                val v1 = "value1".encodeToByteArray()

                txn.put(k1, v1)

                txn.getIterator(readOptions).use { iterator ->
                    iterator.seek(k1)
                    assertTrue(iterator.isValid())
                    assertContentEquals(k1, iterator.key())
                    assertContentEquals(v1, iterator.value())
                }

                txn.getIterator().use { iterator ->
                    iterator.seek(k1)
                    assertTrue(iterator.isValid())
                    assertContentEquals(k1, iterator.key())
                    assertContentEquals(v1, iterator.value())
                }
            }
        }
    }

    @Test
    fun getIterator_cf() {
        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                val k1 = "key1".encodeToByteArray()
                val v1 = "value1".encodeToByteArray()

                txn.put(testCf, k1, v1)

                txn.getIterator(readOptions, testCf).use { iterator ->
                    iterator.seek(k1)
                    assertTrue(iterator.isValid())
                    assertContentEquals(k1, iterator.key())
                    assertContentEquals(v1, iterator.value())
                }

                txn.getIterator(testCf).use { iterator ->
                    iterator.seek(k1)
                    assertTrue(iterator.isValid())
                    assertContentEquals(k1, iterator.key())
                    assertContentEquals(v1, iterator.value())
                }
            }
        }
    }

    @Test
    fun merge_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()
        val v2 = "value2".encodeToByteArray()

        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                txn.put(testCf, k1, v1)
                txn.merge(testCf, k1, v2)
                assertContentEquals("value1**value2".encodeToByteArray(), txn.get(ReadOptions(), testCf, k1))
            }
        }
    }

    @Test
    fun merge() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()
        val v2 = "value2".encodeToByteArray()

        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.put(k1, v1)
                txn.merge(k1, v2)
                assertContentEquals("value1++value2".encodeToByteArray(), txn.get(ReadOptions(), k1))
            }
        }
    }

    @Test
    fun mergeDirectByteBuffer() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                allocateDirectByteBuffer(100) { k1 ->
                    k1.put("key1".encodeToByteArray())
                    k1.flip()

                    allocateDirectByteBuffer(100) { v1 ->
                        v1.put("value1".encodeToByteArray())
                        v1.flip()

                        txn.put(k1, v1)
                        k1.flip()
                        v1.flip()
                    }
                    allocateDirectByteBuffer(100) { v2 ->
                        v2.put("value2".encodeToByteArray())
                        v2.flip()
                        txn.merge(k1, v2)
                    }
                }

                assertContentEquals("value1++value2".encodeToByteArray(), txn.get(ReadOptions(), "key1".encodeToByteArray()))
            }
        }
    }

    @Test
    fun mergeIndirectByteBuffer() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                allocateByteBuffer(100) { k1 ->
                    k1.put("key1".encodeToByteArray())
                    k1.flip()

                    allocateByteBuffer(100) { v1 ->
                        v1.put("value1".encodeToByteArray())
                        v1.flip()

                        txn.put(k1, v1)
                        k1.flip()
                        v1.flip()
                    }
                    allocateByteBuffer(100) { v2 ->
                        v2.put("value2".encodeToByteArray())
                        v2.flip()

                        txn.merge(k1, v2)
                    }
                }

                assertContentEquals("value1++value2".encodeToByteArray(), txn.get(ReadOptions(), "key1".encodeToByteArray()))
            }
        }
    }

    @Test
    fun mergeDirectByteBuffer_cf() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                allocateDirectByteBuffer(100) { k1 ->
                    k1.put("key1".encodeToByteArray())
                    k1.flip()

                    allocateDirectByteBuffer(100) { v1 ->
                        v1.put("value1".encodeToByteArray())
                        v1.flip()
                        txn.put(testCf, k1, v1)
                        k1.flip()
                        v1.flip()
                    }
                    allocateDirectByteBuffer(100) { v2 ->
                        v2.put("value2".encodeToByteArray())
                        v2.flip()
                        txn.merge(testCf, k1, v2)
                    }
                }
                assertContentEquals("value1**value2".encodeToByteArray(), txn.get(ReadOptions(), testCf, "key1".encodeToByteArray()))
            }
        }
    }

    @Test
    fun mergeIndirectByteBuffer_cf() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                allocateByteBuffer(100) { k1 ->
                    k1.put("key1".encodeToByteArray())
                    k1.flip()

                    allocateByteBuffer(100) { v1 ->
                        v1.put("value1".encodeToByteArray())
                        v1.flip()
                        txn.put(testCf, k1, v1)
                        k1.flip()
                        v1.flip()
                    }
                    allocateByteBuffer(100) { v2 ->
                        v2.put("value2".encodeToByteArray())
                        v2.flip()
                        txn.merge(testCf, k1, v2)
                    }
                }

                assertContentEquals("value1**value2".encodeToByteArray(), txn.get(ReadOptions(), testCf, "key1".encodeToByteArray()))
            }
        }
    }

    @Test
    fun delete_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                txn.put(testCf, k1, v1)
                assertContentEquals(v1, txn.get(readOptions, testCf, k1))

                txn.delete(testCf, k1)
                assertNull(txn.get(readOptions, testCf, k1))
            }
        }
    }

    @Test
    fun delete() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                txn.put(k1, v1)
                assertContentEquals(v1, txn.get(readOptions, k1))

                txn.delete(k1)
                assertNull(txn.get(readOptions, k1))
            }
        }
    }

    @Test
    fun getPutUntracked_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                assertNull(txn.get(readOptions, testCf, k1))

                txn.putUntracked(testCf, k1, v1)

                assertContentEquals(v1, txn.get(readOptions, testCf, k1))
            }
        }
    }

    @Test
    fun getPutUntracked() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                assertNull(txn.get(readOptions, k1))
                txn.putUntracked(k1, v1)
                assertContentEquals(v1, txn.get(readOptions, k1))
            }
        }
    }

    @Test
    fun multiGetPutUntrackedAsList_cf() {
        val keys = listOf("key1".encodeToByteArray(), "key2".encodeToByteArray())
        val values = listOf("value1".encodeToByteArray(), "value2".encodeToByteArray())

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                val cfList = listOf(testCf, testCf)

                assertContentEquals(listOf<ByteArray?>(null, null), txn.multiGetAsList(readOptions, cfList, keys))
                txn.putUntracked(testCf, keys[0], values[0])
                txn.putUntracked(testCf, keys[1], values[1])
                assertContainsExactly(values, txn.multiGetAsList(readOptions, cfList, keys))
            }
        }
    }

    @Test
    fun multiGetPutAsListUntracked() {
        val keys = listOf("key1".encodeToByteArray(), "key2".encodeToByteArray())
        val values = listOf("value1".encodeToByteArray(), "value2".encodeToByteArray())

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                assertContentEquals(listOf<ByteArray?>(null, null), txn.multiGetAsList(readOptions, keys))
                txn.putUntracked(keys[0], values[0])
                txn.putUntracked(keys[1], values[1])
                assertContainsExactly(values, txn.multiGetAsList(readOptions, keys))
            }
        }
    }

    @Test
    fun mergeUntracked_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()
        val v2 = "value2".encodeToByteArray()

        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                txn.mergeUntracked(testCf, k1, v1)
                txn.mergeUntracked(testCf, k1, v2)
                txn.commit()
            }

            dbContainer.beginTransaction().use { txn2 ->
                val testCf = dbContainer.getTestColumnFamily()
                assertContentEquals("value1**value2".encodeToByteArray(), txn2.get(ReadOptions(), testCf, k1))
            }
        }
    }

    @Test
    fun mergeUntracked() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()
        val v2 = "value2".encodeToByteArray()

        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.mergeUntracked(k1, v1)
                txn.mergeUntracked(k1, v2)
                txn.commit()
            }

            dbContainer.beginTransaction().use { txn2 ->
                assertContentEquals("value1++value2".encodeToByteArray(), txn2.get(ReadOptions(), k1))
            }
        }
    }

    @Test
    fun mergeUntrackedByteBuffer() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                allocateDirectByteBuffer(100) { k1 ->
                    k1.put("key1".encodeToByteArray())
                    k1.flip()

                    allocateDirectByteBuffer(100) { v1 ->
                        v1.put("value1".encodeToByteArray())
                        v1.flip()

                        txn.mergeUntracked(k1, v1)
                        k1.flip()
                        v1.flip()
                    }

                    allocateDirectByteBuffer(100) { v2 ->
                        v2.put("value2".encodeToByteArray())
                        v2.flip()

                        txn.mergeUntracked(k1, v2)
                    }
                }

                txn.commit()
            }

            dbContainer.beginTransaction().use { txn2 ->
                val result = txn2.get(ReadOptions(), "key1".encodeToByteArray())
                assertContentEquals("value1++value2".encodeToByteArray(), result)
            }
        }
    }

    @Test
    fun mergeUntrackedByteBuffer_cf() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                allocateDirectByteBuffer(100) { k1 ->
                    k1.put("key1".encodeToByteArray())
                    k1.flip()
                    allocateDirectByteBuffer(100) { v1 ->
                        v1.put("value1".encodeToByteArray())
                        v1.flip()

                        txn.mergeUntracked(testCf, k1, v1)
                        k1.flip()
                        v1.flip()
                    }

                    allocateDirectByteBuffer(100) { v2 ->
                        v2.put("value2".encodeToByteArray())
                        v2.flip()

                        txn.mergeUntracked(testCf, k1, v2)
                    }
                }

                txn.commit()
            }

            dbContainer.beginTransaction().use { txn2 ->
                val testCf = dbContainer.getTestColumnFamily()
                assertContentEquals("value1**value2".encodeToByteArray(), txn2.get(ReadOptions(), testCf, "key1".encodeToByteArray()))
            }
        }
    }

    @Test
    fun deleteUntracked_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                txn.put(testCf, k1, v1)
                assertContentEquals(v1, txn.get(readOptions, testCf, k1))

                txn.deleteUntracked(testCf, k1)
                assertNull(txn.get(readOptions, testCf, k1))
            }
        }
    }

    @Test
    fun deleteUntracked() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val readOptions = ReadOptions()

            dbContainer.beginTransaction().use { txn ->
                txn.put(k1, v1)
                assertContentEquals(v1, txn.get(readOptions, k1))

                txn.deleteUntracked(k1)
                assertNull(txn.get(readOptions, k1))
            }
        }
    }

    @Test
    fun putLogData() {
        val blob = "blobby".encodeToByteArray()
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.putLogData(blob)
            }
        }
    }

    @Test
    fun enabledDisableIndexing() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.disableIndexing()
                txn.enableIndexing()
                txn.disableIndexing()
                txn.enableIndexing()
            }
        }
    }

    @Test
    fun numKeys() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()
        val k2 = "key2".encodeToByteArray()
        val v2 = "value2".encodeToByteArray()
        val k3 = "key3".encodeToByteArray()
        val v3 = "value3".encodeToByteArray()

        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                val testCf = dbContainer.getTestColumnFamily()
                txn.put(k1, v1)
                txn.put(testCf, k2, v2)
                txn.merge(k3, v3)
                txn.delete(testCf, k2)

                assertEquals(3, txn.getNumKeys())
                assertEquals(2, txn.getNumPuts())
                assertEquals(1, txn.getNumMerges())
                assertEquals(1, txn.getNumDeletes())
            }
        }
    }

//    @Test
//    fun elapsedTime() {
//        val preStartTxnTime = Clock.System.now().toEpochMilliseconds()
//        startDb().use { dbContainer ->
//            dbContainer.beginTransaction().use { txn ->
//                Thread.sleep(2)
//
//                val txnElapsedTime = txn.getElapsedTime()
//                assertTrue(txnElapsedTime < (Clock.System.now().toEpochMilliseconds() - preStartTxnTime))
//                assertTrue(txnElapsedTime > 0)
//            }
//        }
//    }

    @Test
    fun getWriteBatch() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.put(k1, v1)

                val writeBatch = txn.getWriteBatch()
                assertNotNull(writeBatch)
                assertFalse(writeBatch.isOwningHandle())
                assertEquals(1, writeBatch.count())
            }
        }
    }

    @Test
    fun setLockTimeout() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.setLockTimeout(1000)
            }
        }
    }

    @Test
    fun writeOptions() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            val writeOptions = WriteOptions().apply {
                setDisableWAL(true)
                setSync(true)
            }

            dbContainer.beginTransaction(writeOptions).use { txn ->
                txn.put(k1, v1)

                val txnWriteOptions = txn.getWriteOptions()
                assertNotNull(txnWriteOptions)
                assertFalse(txnWriteOptions.isOwningHandle())
                assertNotSame(writeOptions, txnWriteOptions)
                assertTrue(txnWriteOptions.disableWAL())
                assertTrue(txnWriteOptions.sync())

                txnWriteOptions.setDisableWAL(true)
                txnWriteOptions.setSync(false)
                txn.setWriteOptions(txnWriteOptions)

                val updatedWriteOptions = txn.getWriteOptions()
                assertNotNull(updatedWriteOptions)
                assertFalse(updatedWriteOptions.isOwningHandle())
                assertNotSame(writeOptions, updatedWriteOptions)
                assertTrue(updatedWriteOptions.disableWAL())
                assertFalse(updatedWriteOptions.sync())
            }
        }
    }

    @Test
    fun undoGetForUpdate_cf() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()
        startDb().use { dbContainer ->
            ReadOptions().use { readOptions ->
                dbContainer.beginTransaction().use { txn ->
                    val testCf = dbContainer.getTestColumnFamily()
                    assertNull(txn.getForUpdate(readOptions, testCf, k1, true))
                    txn.put(testCf, k1, v1)
                    assertContentEquals(v1, txn.getForUpdate(readOptions, testCf, k1, true))
                    txn.undoGetForUpdate(testCf, k1)
                }
            }
        }
    }

    @Test
    fun undoGetForUpdate() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()
        startDb().use { dbContainer ->
            ReadOptions().use { readOptions ->
                dbContainer.beginTransaction().use { txn ->
                    assertNull(txn.getForUpdate(readOptions, k1, true))
                    txn.put(k1, v1)
                    assertContentEquals(v1, txn.getForUpdate(readOptions, k1, true))
                    txn.undoGetForUpdate(k1)
                }
            }
        }
    }

    @Test
    fun rebuildFromWriteBatch() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()
        val k2 = "key2".encodeToByteArray()
        val v2 = "value2".encodeToByteArray()
        val k3 = "key3".encodeToByteArray()
        val v3 = "value3".encodeToByteArray()

        startDb().use { dbContainer ->
            ReadOptions().use { readOptions ->
                dbContainer.beginTransaction().use { txn ->
                    txn.put(k1, v1)

                    assertContentEquals(v1, txn.get(readOptions, k1))
                    assertEquals(1, txn.getNumKeys())

                    WriteBatch().use { writeBatch ->
                        writeBatch.put(k2, v2)
                        writeBatch.put(k3, v3)
                        txn.rebuildFromWriteBatch(writeBatch)

                        assertContentEquals(v1, txn.get(readOptions, k1))
                        assertContentEquals(v2, txn.get(readOptions, k2))
                        assertContentEquals(v3, txn.get(readOptions, k3))
                        assertEquals(3, txn.getNumKeys())
                    }
                }
            }
        }
    }

    @Test
    fun getCommitTimeWriteBatch() {
        val k1 = "key1".encodeToByteArray()
        val v1 = "value1".encodeToByteArray()

        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                txn.put(k1, v1)
                val writeBatch = txn.getCommitTimeWriteBatch()

                assertNotNull(writeBatch)
                assertFalse(writeBatch.isOwningHandle())
                assertEquals(0, writeBatch.count())
            }
        }
    }

    @Test
    fun logNumber() {
        startDb().use { dbContainer ->
            dbContainer.beginTransaction().use { txn ->
                assertEquals(0, txn.getLogNumber())
                val logNumber = Random.Default.nextLong()
                txn.setLogNumber(logNumber)
                assertEquals(logNumber, txn.getLogNumber())
            }
        }
    }
}

/**
 * Abstract container class to manage RocksDB resources.
 * Implements AutoCloseable for proper resource management.
 */
abstract class DBContainer(
    val writeOptions: WriteOptions,
    val columnFamilyHandles: List<ColumnFamilyHandle>,
    val columnFamilyOptions: ColumnFamilyOptions,
    val options: DBOptions,
) : AutoCloseable {

    /**
     * Begins a new transaction with default write options.
     *
     * @return A new Transaction instance.
     */
    abstract fun beginTransaction(): Transaction

    /**
     * Begins a new transaction with specified write options.
     *
     * @param writeOptions Write options to use for the transaction.
     * @return A new Transaction instance.
     */
    abstract fun beginTransaction(writeOptions: WriteOptions): Transaction

    /**
     * Retrieves the test column family handle.
     *
     * @return The second column family handle in the list.
     */
    fun getTestColumnFamily(): ColumnFamilyHandle {
        return columnFamilyHandles[1]
    }

    /**
     * Closes all resources managed by the DBContainer.
     * Must be implemented by subclasses to ensure all handles and options are properly closed.
     */
    override abstract fun close()
}

private class TestTransactionNotifier : AbstractTransactionNotifier() {
    private val createdSnapshots = mutableListOf<Snapshot>()

    override fun snapshotCreated(newSnapshot: Snapshot) {
        createdSnapshots.add(newSnapshot)
    }

    fun getCreatedSnapshots(): List<Snapshot> = createdSnapshots
}
