package com.app.platform.language.shared.di

import com.app.platform.language.shared.reading.ReadingSessionViewModel
import com.app.platform.language.shared.reading.ReadingTestListViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

object ViewModels : KoinComponent {
  fun readingTestList(): ViewModelOwner<ReadingTestListViewModel> = viewModelOwner { get() }

  fun readingSession(testId: String): ViewModelOwner<ReadingSessionViewModel> =
    viewModelOwner { get { parametersOf(testId) } }
}
