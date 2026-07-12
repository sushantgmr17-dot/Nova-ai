package com.example.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.database.AppDatabase
import com.example.repository.ChatRepository
import com.example.ui.admin.AdminViewModel
import com.example.ui.chat.ChatViewModel
import com.example.ui.history.HistoryViewModel
import com.example.ui.homework.HomeworkViewModel
import com.example.ui.image.ImageGenerationViewModel
import com.example.ui.voice.VoiceViewModel
import com.example.ui.settings.SettingsViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            ChatViewModel(
                repository = novaApplication().container,
                billingManager = novaApplication().billingManager,
                personaManager = novaApplication().personaManager,
                userProfileManager = novaApplication().userProfileManager
            )
        }
        initializer {
            HistoryViewModel(novaApplication().container)
        }
        initializer {
            ImageGenerationViewModel(
                novaApplication().container,
                novaApplication().billingManager,
                novaApplication().userProfileManager
            )
        }
        initializer {
            SettingsViewModel(
                novaApplication().billingManager,
                novaApplication().appLockManager,
                novaApplication().themeManager,
                novaApplication().userProfileManager
            )
        }
        initializer {
            AdminViewModel(novaApplication().container)
        }
        initializer {
            HomeworkViewModel(novaApplication().container)
        }
        initializer {
            VoiceViewModel(
                application = novaApplication(),
                repository = novaApplication().container
            )
        }
    }
}

fun CreationExtras.novaApplication(): NovaApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NovaApplication)
