package com.leaseting.parkingterminal.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import com.leaseting.parkingterminal.ParkingTerminalApplication
import com.leaseting.parkingterminal.di.AppContainer

/**
 * The object graph, reached from a `ViewModelProvider.Factory` initializer.
 * Keeps every ViewModel companion factory down to one line of plumbing.
 */
fun CreationExtras.appContainer(): AppContainer =
    (this[APPLICATION_KEY] as ParkingTerminalApplication).container
