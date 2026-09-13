package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationIsolationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences("nova_conversation_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        db = AppDatabase.getDatabase(app)
        runBlocking(Dispatchers.IO) {
            db.clearAllTables()
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDatabaseSessionAndMessageIsolation() = runTest(testDispatcher) {
        val chatDao = db.chatDao()

        // 1. Create Conversation A with messages
        val sessionAId = UUID.randomUUID().toString()
        chatDao.insertSession(ChatSessionEntity(id = sessionAId, title = "Conversation A"))
        chatDao.insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionAId,
                role = "user",
                content = "Message 1 in Conv A",
                timestamp = System.currentTimeMillis()
            )
        )
        chatDao.insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionAId,
                role = "assistant",
                content = "Response 1 in Conv A",
                timestamp = System.currentTimeMillis() + 100
            )
        )

        // 2. Create Conversation B with a message
        val sessionBId = UUID.randomUUID().toString()
        assertNotEquals(sessionAId, sessionBId)
        chatDao.insertSession(ChatSessionEntity(id = sessionBId, title = "Conversation B"))
        chatDao.insertMessage(
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionBId,
                role = "user",
                content = "Message in Conv B",
                timestamp = System.currentTimeMillis() + 200
            )
        )

        // 3. Verify complete database isolation
        val msgsA = chatDao.getMessagesList(sessionAId)
        val msgsB = chatDao.getMessagesList(sessionBId)

        assertEquals("Session A must have exactly 2 messages", 2, msgsA.size)
        assertEquals("Session B must have exactly 1 message", 1, msgsB.size)
        assertEquals("Message 1 in Conv A", msgsA[0].content)
        assertEquals("Response 1 in Conv A", msgsA[1].content)
        assertEquals("Message in Conv B", msgsB[0].content)

        // Deleting Session B should NOT affect Session A
        chatDao.deleteSession(sessionBId)
        val msgsAAfterDelete = chatDao.getMessagesList(sessionAId)
        val msgsBAfterDelete = chatDao.getMessagesList(sessionBId)
        assertEquals("Session A must still have 2 messages", 2, msgsAAfterDelete.size)
        assertTrue("Session B messages should be completely deleted", msgsBAfterDelete.isEmpty())
    }

    @Test
    fun testViewModelNewConversationStartsFreshAndIsolatesState() = runTest(testDispatcher) {
        val prefs = app.getSharedPreferences("nova_conversation_prefs", Context.MODE_PRIVATE)
        val viewModel = MainViewModel(app)

        // Wait for asynchronous Room initialization on IO dispatcher to assign session
        var waited = 0
        while (viewModel.currentSessionId.value == null && waited < 50) {
            advanceUntilIdle()
            Thread.sleep(50)
            waited++
        }

        // Verify initial session was created
        val session1Id = viewModel.currentSessionId.value
        assertNotNull(session1Id)
        assertTrue("Initial session messages must start empty", viewModel.currentMessages.value.isEmpty())

        // Create new chat
        viewModel.createNewChat()
        waited = 0
        while ((viewModel.currentSessionId.value == null || viewModel.currentSessionId.value == session1Id) && waited < 50) {
            advanceUntilIdle()
            Thread.sleep(50)
            waited++
        }

        val session2Id = viewModel.currentSessionId.value
        assertNotNull(session2Id)
        assertNotEquals("New conversation must have a distinct session ID", session1Id, session2Id)
        assertTrue("New conversation must have empty message list", viewModel.currentMessages.value.isEmpty())
        assertEquals("Active session ID must be persisted in prefs", session2Id, prefs.getString("active_session_id", null))

        // Switch back to session 1
        viewModel.selectSession(session1Id!!)
        advanceUntilIdle()
        assertEquals(session1Id, viewModel.currentSessionId.value)
        assertEquals("Switched session ID must be persisted in prefs", session1Id, prefs.getString("active_session_id", null))

        // Test app restart simulation: a new ViewModel instance restores the persisted active session
        val restartedViewModel = MainViewModel(app)
        waited = 0
        while (restartedViewModel.currentSessionId.value != session1Id && waited < 50) {
            advanceUntilIdle()
            Thread.sleep(50)
            waited++
        }
        assertEquals("Restarted ViewModel must restore active session", session1Id, restartedViewModel.currentSessionId.value)
    }

    @Test
    fun testChatDaoGranularSessionUpdatesDoNotAffectOtherSessions() = runTest(testDispatcher) {
        val chatDao = db.chatDao()

        val s1 = UUID.randomUUID().toString()
        val s2 = UUID.randomUUID().toString()
        chatDao.insertSession(ChatSessionEntity(id = s1, title = "Original 1"))
        chatDao.insertSession(ChatSessionEntity(id = s2, title = "Original 2"))

        chatDao.updateSessionTitle(s1, "Title for Session 1")
        chatDao.updateSessionTitle(s2, "Title for Session 2")

        val session1 = chatDao.getSessionByIdOnce(s1)
        val session2 = chatDao.getSessionByIdOnce(s2)

        assertEquals("Title for Session 1", session1?.title)
        assertEquals("Title for Session 2", session2?.title)

        chatDao.updateSessionPinned(s1, true)
        val session1Pinned = chatDao.getSessionByIdOnce(s1)
        val session2Pinned = chatDao.getSessionByIdOnce(s2)

        assertTrue(session1Pinned!!.isPinned)
        assertTrue(!session2Pinned!!.isPinned)
    }
}
