package com.reppal.app

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(
    rappelInitial: Rappel? = null,
    viewModel: RappelViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // 1. Initialisation propre du calendrier local
    val instantInitial = remember {
        Calendar.getInstance().apply {
            if (rappelInitial != null) {
                timeInMillis = rappelInitial.timestamp
            } else {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    // 2. CORRECTION DATE : Calcul du timestamp "fictif" UTC pour que le DatePicker affiche la bonne date locale
    val utcInitialMillis = remember(rappelInitial) {
        if (rappelInitial != null) {
            rappelInitial.timestamp
        } else {
            val cal = Calendar.getInstance()
            // On prend le minuit local et on compense le décalage zone pour le DatePicker (qui pense en UTC)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis + TimeZone.getDefault().getOffset(cal.timeInMillis)
        }
    }

    var titre by remember { mutableStateOf(rappelInitial?.titre ?: "") }
    var description by remember { mutableStateOf(rappelInitial?.description ?: "") }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcInitialMillis)
    val timePickerState = rememberTimePickerState(
        initialHour = instantInitial.get(Calendar.HOUR_OF_DAY),
        initialMinute = instantInitial.get(Calendar.MINUTE),
        is24Hour = DateFormat.is24HourFormat(context)
    )

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // 3. CORRECTION RÉCUPÉRATION : Fusion de la date UTC et de l'heure locale
    val currentSelectedTimestamp by remember {
        derivedStateOf {
            val calendar = Calendar.getInstance()
            datePickerState.selectedDateMillis?.let { selectedUtc ->
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    timeInMillis = selectedUtc
                }
                calendar.set(
                    utcCal.get(Calendar.YEAR),
                    utcCal.get(Calendar.MONTH),
                    utcCal.get(Calendar.DAY_OF_MONTH)
                )
            }
            calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
            calendar.set(Calendar.MINUTE, timePickerState.minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
    }

    // Validation : Le rappel doit être au moins 1 minute dans le futur
    val isDateTimeValid = currentSelectedTimestamp > System.currentTimeMillis()
    val isFormValid = titre.isNotBlank() && isDateTimeValid

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (rappelInitial == null) stringResource(R.string.new_reminder) else stringResource(R.string.edit_reminder),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = titre,
            onValueChange = { titre = it },
            label = { Text(stringResource(R.string.title_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                capitalization = KeyboardCapitalization.Sentences
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.description_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoChip(
                iconRes = R.drawable.ic_calendrier,
                label = DateFormat.getMediumDateFormat(context).format(Date(currentSelectedTimestamp)),
                modifier = Modifier.weight(1f),
                isError = !isDateTimeValid,
                onClick = { showDatePicker = true }
            )
            InfoChip(
                iconRes = R.drawable.ic_montre,
                label = DateFormat.getTimeFormat(context).format(Date(currentSelectedTimestamp)),
                modifier = Modifier.weight(1f),
                isError = !isDateTimeValid,
                onClick = { showTimePicker = true }
            )
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } }
            ) { DatePicker(state = datePickerState) }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = { TextButton(onClick = { showTimePicker = false }) { Text("OK") } },
                text = { TimePicker(state = timePickerState) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (isFormValid) {
                        val rappelFinal = Rappel(
                            id = rappelInitial?.id ?: 0,
                            titre = titre.formatReminderText(),
                            description = description.formatReminderText(),
                            timestamp = currentSelectedTimestamp
                        )

                        val calendarAlarme = Calendar.getInstance().apply {
                            timeInMillis = currentSelectedTimestamp
                        }

                        if (rappelInitial == null) {
                            viewModel.sauvegarderEtProgrammer(context, rappelFinal, calendarAlarme)
                        } else {
                            viewModel.mettreAJourEtProgrammer(context, rappelFinal, calendarAlarme)
                        }
                        onDismiss()
                    }
                },
                enabled = isFormValid,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (rappelInitial == null) stringResource(R.string.add_action) else stringResource(R.string.save_action))
            }
        }
    }
}

@Composable
fun InfoChip(
    iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

fun String.formatReminderText(): String {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}