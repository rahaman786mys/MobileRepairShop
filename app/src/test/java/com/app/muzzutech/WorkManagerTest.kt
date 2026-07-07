package com.app.muzzutech

import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.app.muzzutech.work.LedgerAuditWorker
import com.app.muzzutech.work.ReorderAlertWorker
import com.app.muzzutech.work.SalaryReminderWorker
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
@RunWith(RobolectricTestRunner::class)
class WorkManagerTest {

    @Test
    fun salaryReminderWorker_constructs() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val worker = TestListenableWorkerBuilder<SalaryReminderWorker>(ctx).build()
        assertNotNull("SalaryReminderWorker must construct", worker)
    }

    @Test
    fun reorderAlertWorker_constructs() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val worker = TestListenableWorkerBuilder<ReorderAlertWorker>(ctx).build()
        assertNotNull("ReorderAlertWorker must construct", worker)
    }

    @Test
    fun ledgerAuditWorker_constructs() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val worker = TestListenableWorkerBuilder<LedgerAuditWorker>(ctx).build()
        assertNotNull("LedgerAuditWorker must construct", worker)
    }
}
