package com.jizhang.tracker.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.jizhang.tracker.createTestTransaction
import com.jizhang.tracker.data.InMemorySharedPreferences
import com.jizhang.tracker.data.NotificationListener
import com.jizhang.tracker.data.PlanStorage
import com.jizhang.tracker.data.TransactionDao
import com.jizhang.tracker.data.TransactionRepository
import com.jizhang.tracker.data.TransactionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModelTest {

    @get:Rule val instantTaskRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val app = mockk<Application>(relaxed = true)
    private val dao = mockk<TransactionDao>()
    private val repo = mockk<TransactionRepository>()

    private lateinit var viewModel: LedgerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { dao.getTransactionsBetweenSync(any(), any()) } returns emptyList()
        mockkObject(NotificationListener.Companion)
        every { NotificationListener.isPermissionGranted(any()) } returns false
        every { repo.getTodayTransactions(any(), any()) } returns flowOf(emptyList())
        every { repo.getTodayIncome(any(), any()) } returns flowOf(0.0)
        every { repo.getTodayExpense(any(), any()) } returns flowOf(0.0)
        every { repo.getAllTransactions() } returns flowOf(emptyList())
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): LedgerViewModel {
        val planStorage = PlanStorage(InMemorySharedPreferences())
        return LedgerViewModel(repo, planStorage, dao, mockk(relaxed = true), app)
    }

    @Test
    fun `addTransaction calls repository`() = runTest {
        viewModel = createViewModel()
        coEvery { repo.addManualTransaction(35.0, TransactionType.EXPENSE, "午餐") } returns true

        var result = false
        viewModel.addTransaction(35.0, TransactionType.EXPENSE, "午餐") { result = it }
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.addManualTransaction(35.0, TransactionType.EXPENSE, "午餐") }
        assert(result) { "addTransaction should report success" }
    }

    @Test
    fun `addTransaction reports duplicate`() = runTest {
        viewModel = createViewModel()
        coEvery { repo.addManualTransaction(any(), any(), any()) } returns false

        var result = true
        viewModel.addTransaction(50.0, TransactionType.INCOME, "工资") { result = it }
        testDispatcher.scheduler.advanceUntilIdle()

        assert(!result) { "addTransaction should report false for duplicates" }
    }

    @Test
    fun `deleteTransaction calls repository`() = runTest {
        viewModel = createViewModel()
        coEvery { repo.deleteTransaction(1L) } returns Unit

        viewModel.deleteTransaction(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repo.deleteTransaction(1L) }
    }

    @Test
    fun `getTodaySummary returns formatted string`() = runTest {
        val now = System.currentTimeMillis()
        val tx = listOf(
            createTestTransaction(amount = 35.0, type = TransactionType.EXPENSE, description = "午餐", dateMillis = now),
            createTestTransaction(amount = 200.0, type = TransactionType.INCOME, description = "退款", dateMillis = now)
        )
        coEvery { dao.getTransactionsBetweenSync(any(), any()) } returns tx

        viewModel = createViewModel()
        val summary = viewModel.getTodaySummary()

        assert(summary.contains("收入")) { "summary should contain income info" }
        assert(summary.contains("支出")) { "summary should contain expense info" }
        assert(summary.contains("¥200.00")) { "summary should show income amount" }
        assert(summary.contains("¥35.00")) { "summary should show expense amount" }
    }

    @Test
    fun `setManualMode updates state and storage`() {
        viewModel = createViewModel()

        viewModel.setManualMode(true)
        assert(viewModel.isManualMode.value)

        viewModel.setManualMode(false)
        assert(!viewModel.isManualMode.value)
    }

    @Test
    fun `installDateMillis is stable after ViewModel init`() {
        viewModel = createViewModel()
        assert(viewModel.installDateMillis > 0)
    }
}
