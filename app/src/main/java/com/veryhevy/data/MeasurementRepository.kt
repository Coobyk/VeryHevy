package com.veryhevy.data

import com.veryhevy.data.db.AppDatabase
import com.veryhevy.data.db.MeasurementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class MeasurementRepository(private val db: AppDatabase) {

    private val dao get() = db.measurementDao()

    fun observeAll(): Flow<List<MeasurementEntity>> = dao.observeAll()

    suspend fun add(weightKg: Double) {
        dao.insert(MeasurementEntity(dateEpochDay = LocalDate.now().toEpochDay(), weightKg = weightKg))
    }

    suspend fun delete(m: MeasurementEntity) = dao.delete(m)
}
