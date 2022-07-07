package entries

import android.app.Application
import android.content.res.Resources
import conf.ConfRepo
import db.testDb
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals

class EntriesModelTest {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Before
    fun beforeEachTest() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @After
    fun afterEachTest() {
        Dispatchers.resetMain() // reset the main dispatcher to the original Main dispatcher
        mainThreadSurrogate.close()
    }

    @Test
    fun getAccountTitle() {
        val title = "Title"

        val app = mockk<Application>()
        val resources = mockk<Resources>()
        every { resources.getString(any()) } returns title
        every { app.resources } returns resources

        val db = testDb()
        val confRepo = ConfRepo(db)

        val model = EntriesModel(
            app = app,
            confRepo = confRepo,
            feedsRepo = mockk(),
            entriesRepo = mockk(),
            audioEnclosuresRepo = mockk(),
            newsApiSync = mockk(),
        )

        confRepo.update { it.copy(backend = ConfRepo.BACKEND_STANDALONE) }

        assertEquals(title, runBlocking { model.accountTitle().first() })

        verify { app.resources }
    }

    @Test
    fun getAccountSubtitle() {
        val username = "test"

        val db = testDb()
        val confRepo = ConfRepo(db)

        val model = EntriesModel(
            app = mockk(),
            confRepo = confRepo,
            feedsRepo = mockk(),
            entriesRepo = mockk(),
            audioEnclosuresRepo = mockk(),
            newsApiSync = mockk(),
        )

        confRepo.update {
            it.copy(
                backend = ConfRepo.BACKEND_MINIFLUX,
                minifluxServerUsername = "test",
            )
        }

        assert(runBlocking { model.accountSubtitle().first() }.contains(username))
    }
}