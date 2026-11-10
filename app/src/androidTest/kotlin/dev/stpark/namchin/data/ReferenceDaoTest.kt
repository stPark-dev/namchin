package dev.stpark.namchin.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReferenceDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ReferenceDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.referenceDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(createdAt: Long, pose: String? = "1.0|") =
        ReferenceEntity(imagePath = "/x/$createdAt.jpg", width = 800, height = 1000, pose = pose, createdAt = createdAt)

    @Test
    fun insertedReferencesAreObservedNewestFirst() = runBlocking {
        dao.insert(entity(createdAt = 1))
        dao.insert(entity(createdAt = 2))

        assertEquals(listOf(2L, 1L), dao.observeAll().first().map { it.createdAt })
    }

    @Test
    fun getReturnsTheStoredEntity() = runBlocking {
        val id = dao.insert(entity(createdAt = 5, pose = null))

        val stored = dao.get(id)!!

        assertEquals("/x/5.jpg", stored.imagePath)
        assertNull(stored.pose)
    }

    @Test
    fun deleteRemovesTheReference() = runBlocking {
        val id = dao.insert(entity(createdAt = 1))

        dao.delete(id)

        assertNull(dao.get(id))
    }
}
