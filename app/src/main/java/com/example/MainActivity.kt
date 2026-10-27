package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.DenaPaonaDatabase
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.LedgerRepository
import com.example.ui.DenaPaonaApp
import com.example.ui.viewmodel.LedgerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = DenaPaonaDatabase.getInstance(applicationContext)
        val repository = LedgerRepository(database.ledgerDao())
        val preferencesRepository = AppPreferencesRepository(applicationContext)
        val viewModel = ViewModelProvider(
            this,
            LedgerViewModel.provideFactory(repository, preferencesRepository)
        )[LedgerViewModel::class.java]

        setContent {
            DenaPaonaApp(viewModel = viewModel)
        }
    }
}
