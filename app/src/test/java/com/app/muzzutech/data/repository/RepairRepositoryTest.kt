package com.app.muzzutech.data.repository

import com.app.muzzutech.data.db.dao.RepairEntryDao
import com.app.muzzutech.data.model.RepairEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.*

class RepairRepositoryTest {

    @Test
    fun testPendingCount_ReturnsCorrectValue() = runBlocking {
        val mockDao = mock(RepairEntryDao::class.java)
        `when`(mockDao.getPendingCount()).thenReturn(kotlinx.coroutines.flow.flowOf(5))

        val repository = RepairRepository(mockDao)
        repository.getPendingCount().collect { count ->
            assertEquals(5, count)
        }
    }
}
