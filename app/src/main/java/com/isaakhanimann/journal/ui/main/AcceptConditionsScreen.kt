/*
 * Copyright (c) 2022.
 * This file is part of PsychonautWiki Journal.
 *
 * PsychonautWiki Journal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.isaakhanimann.journal.ui.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.localization.I18n
import com.isaakhanimann.journal.localization.i18n
import java.util.Locale

@Preview
@Composable
fun AcceptConditionsPreview() {
    AcceptConditionsScreen(onTapAccept = {}, onTapReject = {})
}

@Composable
fun AcceptConditionsScreen(
    onTapAccept: () -> Unit,
    onTapReject: () -> Unit
) {
    var isShowingAgreement by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isShowingAgreement = false }
    if (isShowingAgreement) {
        UserAgreementScreen(onBack = { isShowingAgreement = false })
        return
    }

    Scaffold { padding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = i18n("user_agreement_title"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = i18n("user_agreement_accept_summary"),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = i18n("user_agreement_accept_warning"),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { isShowingAgreement = true }) {
                Text(i18n("user_agreement_open_full"))
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onTapReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(i18n("user_agreement_disagree"))
                }
                Button(
                    onClick = onTapAccept,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(i18n("user_agreement_agree"))
                }
            }
        }
    }
}

@Composable
private fun UserAgreementScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val agreementText = remember(context) { loadUserAgreementText(context) }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            TextButton(onClick = onBack) {
                Text(i18n("user_agreement_back"))
            }
            Text(
                text = i18n("user_agreement_title"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = agreementText,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun loadUserAgreementText(context: Context): String {
    val languageKey = (
        I18n.getPreferredLanguageKey()
            ?: I18n.getCurrentLanguageKey()
        ).lowercase(Locale.US)
    val fileKey = when {
        languageKey.startsWith("zh_cn") || languageKey == "zh" -> "zh_cn"
        languageKey.startsWith("zh_tw") ||
            languageKey.startsWith("zh_hk") -> "zh_tw"
        else -> "en_us"
    }
    return context.assets.open("legal/user_agreement_$fileKey.txt")
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
}
