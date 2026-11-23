package com.costemp.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.costemp.app.studio.StudioState
import com.costemp.app.studio.StudioViewModel
import com.costemp.app.ui.theme.CostempPalette
import com.costemp.core.measurement.BodyProfile
import com.costemp.core.measurement.LengthUnit
import com.costemp.core.measurement.MannequinProportions
import com.costemp.core.measurement.MeasurementKind

/**
 * Right-hand properties panel: the editable measurement set plus proportions derived
 * from it.
 *
 * Edits commit when focus leaves a field or the keyboard "done" action fires, so a
 * half-typed number never rebuilds the mannequin or floods the undo history.
 */
@Composable
fun PropertiesPanel(
    state: StudioState,
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier,
) {
    val profile = state.profile
    val unit = state.unit

    Column(modifier.background(CostempPalette.Panel)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("MEASUREMENTS", style = SectionLabel, color = CostempPalette.TextSecondary)
                Text(
                    text = state.projectName,
                    color = CostempPalette.TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }
            UnitToggle(current = unit, onSelect = viewModel::setUnit)
        }
        HorizontalDivider(color = CostempPalette.Outline)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(MeasurementKind.entries, key = { it.name }) { kind ->
                MeasurementRow(
                    kind = kind,
                    valueCm = kind.read(profile),
                    unit = unit,
                    onCommit = { viewModel.updateMeasurement(kind, it) },
                )
            }
            item { Spacer(Modifier.height(2.dp)) }
            item {
                HorizontalDivider(color = CostempPalette.Outline)
                Spacer(Modifier.height(10.dp))
                Text("DERIVED PROPORTIONS", style = SectionLabel, color = CostempPalette.TextSecondary)
                Spacer(Modifier.height(8.dp))
                DerivedProportions(profile = profile, unit = unit)
            }
            item {
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = viewModel::resetMeasurements) {
                    Text("Reset to reference body", color = CostempPalette.TextSecondary, fontSize = 11.sp)
                }
            }
        }

        HorizontalDivider(color = CostempPalette.Outline)
        Text(
            text = state.statusMessage ?: "Autosaves after each edit",
            color = CostempPalette.TextSecondary,
            fontSize = 10.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            maxLines = 2,
        )
    }
}

@Composable
private fun UnitToggle(current: LengthUnit, onSelect: (LengthUnit) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(CostempPalette.PanelRaised)
            .border(1.dp, CostempPalette.Outline, RoundedCornerShape(7.dp)),
    ) {
        LengthUnit.entries.forEach { unit ->
            val selected = unit == current
            Text(
                text = unit.symbol,
                color = if (selected) CostempPalette.Accent else CostempPalette.TextSecondary,
                fontSize = 10.sp,
                modifier = Modifier
                    .background(if (selected) CostempPalette.AccentSoft else CostempPalette.PanelRaised)
                    .clickable { onSelect(unit) }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun MeasurementRow(
    kind: MeasurementKind,
    valueCm: Double,
    unit: LengthUnit,
    onCommit: (Double) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(kind.label, color = CostempPalette.TextPrimary, fontSize = 12.sp)
            Text(kind.hint, color = CostempPalette.TextSecondary, fontSize = 9.sp, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        MeasurementInput(
            valueCm = valueCm,
            unit = unit,
            onCommit = onCommit,
        )
    }
}

@Composable
private fun MeasurementInput(valueCm: Double, unit: LengthUnit, onCommit: (Double) -> Unit) {
    val focusManager = LocalFocusManager.current
    val formatted = LengthUnit.formatNumber(unit.fromCentimetres(valueCm))
    var text by remember(valueCm, unit) { mutableStateOf(formatted) }
    var hadFocus by remember { mutableStateOf(false) }

    fun commit() {
        val parsed = text.trim().replace(',', '.').toDoubleOrNull()
        if (parsed == null) {
            text = formatted
        } else {
            onCommit(unit.toCentimetres(parsed))
        }
    }

    BasicTextField(
        value = text,
        onValueChange = { raw -> text = raw.filter { it.isDigit() || it == '.' || it == ',' } },
        singleLine = true,
        textStyle = TechnicalText.copy(
            fontSize = 12.sp,
            color = CostempPalette.TextPrimary,
            textAlign = TextAlign.End,
        ),
        cursorBrush = SolidColor(CostempPalette.Accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            commit()
            focusManager.clearFocus()
        }),
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CostempPalette.PanelRaised)
            .border(1.dp, CostempPalette.Outline, RoundedCornerShape(6.dp))
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hadFocus = true
                } else if (hadFocus) {
                    hadFocus = false
                    commit()
                }
            }
            .padding(horizontal = 8.dp, vertical = 7.dp),
    )
}

@Composable
private fun DerivedProportions(profile: BodyProfile, unit: LengthUnit) {
    val proportions = remember(profile) { MannequinProportions.from(profile) }
    val headUnits = if (proportions.headLength > 0f) profile.heightCm / proportions.headLength else 0.0
    val shoulderRatio = profile.shoulderWidthCm / profile.heightCm * 100.0
    val inseamRatio = profile.inseamCm / profile.heightCm * 100.0
    val torsoLength = (proportions.shoulderY - proportions.hipY).toDouble()
    val chestDepth = proportions.chestHalfDepth * 2.0
    val sleeveSpan = profile.shoulderWidthCm + 2.0 * profile.armLengthCm

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DerivedRow("Head units", String.format(java.util.Locale.US, "%.1f", headUnits))
        DerivedRow("Shoulder / height", String.format(java.util.Locale.US, "%.1f%%", shoulderRatio))
        DerivedRow("Inseam / height", String.format(java.util.Locale.US, "%.1f%%", inseamRatio))
        DerivedRow("Torso length", "${unit.format(torsoLength)} ${unit.symbol}")
        DerivedRow("Chest depth", "${unit.format(chestDepth)} ${unit.symbol}")
        DerivedRow("Sleeve span", "${unit.format(sleeveSpan)} ${unit.symbol}")
    }
}

@Composable
private fun DerivedRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = CostempPalette.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = CostempPalette.Teal, style = TechnicalText)
    }
}
