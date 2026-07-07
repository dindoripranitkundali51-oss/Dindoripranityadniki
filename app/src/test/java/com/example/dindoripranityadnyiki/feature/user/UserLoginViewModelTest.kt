package com.example.dindoripranityadnyiki.feature.user

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.dindoripranityadnyiki.core.data.UserRepository
import com.example.dindoripranityadnyiki.core.util.SessionManager
import com.google.firebase.auth.FirebaseAuth
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for UserLoginViewModel.
 * Uses MockK for mocking dependencies and CoroutinesTest for testing coroutines.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserLoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: UserLoginViewModel
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockContext: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockAuth = mockk()
        mockUserRepository = mockk()
        mockContext = mockk()

        viewModel = UserLoginViewModel(mockUserRepository, mockAuth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with valid credentials succeeds`() = runTest {
        // Arrange
        val mobile = "9876543210"
        val password = "password123"
        val mockFirebaseUser = mockk<com.google.firebase.auth.FirebaseUser>()
        mockFirebaseUser.uid = "test_uid"
        
        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns mockk {
            every { await() } returns mockk {
                every { user } returns mockFirebaseUser
            }
        }
        coEvery { mockUserRepository.getUserProfile() } returns mockk()
        coEvery { mockUserRepository.getUserProfileFlow() } returns mockk()

        // Act
        viewModel.login(mockContext, mobile, password)

        // Assert
        verify { mockAuth.signInWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun `login with invalid mobile shows error`() = runTest {
        // Arrange
        val mobile = "123"
        val password = "password123"

        // Act
        viewModel.login(mockContext, mobile, password)

        // Assert - Should not attempt auth with invalid mobile
        verify(exactly = 0) { mockAuth.signInWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun `login with empty password shows error`() = runTest {
        // Arrange
        val mobile = "9876543210"
        val password = ""

        // Act
        viewModel.login(mockContext, mobile, password)

        // Assert - Should not attempt auth with empty password
        verify(exactly = 0) { mockAuth.signInWithEmailAndPassword(any(), any()) }
    }

    @Test
    fun `resetPassword with valid email succeeds`() = runTest {
        // Arrange
        val email = "test@example.com"
        every { mockAuth.sendPasswordResetEmail(any()) } returns mockk {
            every { await() } returns mockk()
        }

        // Act
        var callbackResult: String? = null
        viewModel.resetPassword(mockContext, email) { result ->
            callbackResult = result
        }

        // Assert
        verify { mockAuth.sendPasswordResetEmail(email) }
        assertNotNull(callbackResult)
    }

    @Test
    fun `resetPassword with invalid mobile resolves to email format`() = runTest {
        // Arrange
        val mobile = "9876543210"
        every { mockAuth.sendPasswordResetEmail(any()) } returns mockk {
            every { await() } returns mockk()
        }

        // Act
        var callbackResult: String? = null
        viewModel.resetPassword(mockContext, mobile) { result ->
            callbackResult = result
        }

        // Assert - Should resolve mobile to email format
        verify { mockAuth.sendPasswordResetEmail(any()) }
    }

    @Test
    fun `logout clears session and signs out`() = runTest {
        // Arrange
        every { mockAuth.signOut() } just Runs
        coEvery { SessionManager.signOut(any()) } just Runs

        // Act
        var callbackCalled = false
        viewModel.logout(mockContext) {
            callbackCalled = true
        }

        // Assert
        assertTrue(callbackCalled)
        verify { mockAuth.signOut() }
    }
}
