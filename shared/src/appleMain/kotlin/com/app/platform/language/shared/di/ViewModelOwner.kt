package com.app.platform.language.shared.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlin.reflect.KClass

class ViewModelOwner<VM : ViewModel> internal constructor(
  create: () -> VM,
  type: KClass<VM>,
) {
  private val store = ViewModelStore()

  val viewModel: VM =
    ViewModelProvider.create(store, viewModelFactory { addInitializer(type) { create() } })[type]

  fun clear() = store.clear()
}

internal inline fun <reified VM : ViewModel> viewModelOwner(noinline create: () -> VM): ViewModelOwner<VM> =
  ViewModelOwner(create, VM::class)
