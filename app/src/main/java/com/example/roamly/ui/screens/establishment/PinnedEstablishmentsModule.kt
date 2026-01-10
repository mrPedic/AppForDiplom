package com.example.roamly.ui.screens.establishment


import com.example.roamly.entity.ViewModel.UserEstablishmentsViewModel
import com.example.roamly.manager.PinnedEstablishmentsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object PinnedEstablishmentsModule {

    @Provides
    @ViewModelScoped
    fun provideUserEstablishmentsViewModel(
        repository: PinnedEstablishmentsRepository
    ): UserEstablishmentsViewModel {
        return UserEstablishmentsViewModel(repository)
    }
}