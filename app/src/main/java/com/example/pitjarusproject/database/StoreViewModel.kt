package com.example.pitjarusproject.database

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import java.lang.IllegalArgumentException

class StoreViewModel(
    private val database: TokoDao,
    application: Application
) : AndroidViewModel(application) {
    private val viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    val toko = database.getToko()

    fun insertDataToko(data: Toko) {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                database.insertToko(data)
            }
        }
    }

    fun deleteToko(id: Long) {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                database.deleteToko(id)
            }
        }
    }

    fun updateToko(data: Toko) {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                database.updateToko(data)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val dataSource: TokoDao,
        private val application: Application
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StoreViewModel::class.java)) {
                return StoreViewModel(
                    dataSource,
                    application
                ) as T
            }
            throw IllegalArgumentException("tidak diketahui class view model")
        }
    }
}