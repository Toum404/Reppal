package com.reppal.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar

class RappelViewModel(private val dao: RappelDao) : ViewModel() {

    val tousLesRappels: Flow<List<Rappel>> = dao.getAllRappels()

    fun sauvegarderEtProgrammer(context: Context, rappel: Rappel, calendar: Calendar) {
        viewModelScope.launch {
            val idGenere = dao.insertRappel(rappel).toInt()
            AlarmUtils.programmerNotification(context, rappel.copy(id = idGenere), calendar)
        }
    }

    fun mettreAJourEtProgrammer(context: Context, rappel: Rappel, calendar: Calendar) {
        viewModelScope.launch {
            dao.insertRappel(rappel)
            AlarmUtils.programmerNotification(context, rappel, calendar)
        }
    }

    fun supprimerRappel(context: Context, rappel: Rappel) {
        viewModelScope.launch {
            AlarmUtils.annulerNotification(context, rappel.id)
            dao.deleteRappel(rappel)
        }
    }
}