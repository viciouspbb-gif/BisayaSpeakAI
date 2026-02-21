package com.bisayaspeak.ai.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.res.stringResource
import com.bisayaspeak.ai.R
import java.util.Locale

class FeedbackManager(private val context: Context) {
    
    companion object {
        private const val DEFAULT_FORM_URL = "https://docs.google.com/forms/d/e/YOUR_FORM_ID/viewform"
        private const val ENTRY_USER_ID = "entry.1234567890"
        private const val ENTRY_LANGUAGE = "entry.0987654321"
        private const val ENTRY_TIMESTAMP = "entry.1122334455"
        private const val PREFS_NAME = "feedback_prefs"
        private const val KEY_LAST_FEEDBACK_TIME = "last_feedback_time"
        private const val KEY_FEEDBACK_COUNT = "feedback_count"
    }
    
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    fun openFeedbackForm(userId: String = "anonymous") {
        try {
            val formUrl = context.getString(R.string.feedback_form_url).ifBlank { DEFAULT_FORM_URL }
            if (formUrl.isBlank() || formUrl.contains("YOUR_FORM_ID")) {
                Toast.makeText(context, context.getString(R.string.feedback_error), Toast.LENGTH_SHORT).show()
                return
            }
            val timestamp = System.currentTimeMillis().toString()
            val language = Locale.getDefault().language
            val url = buildFeedbackUrl(formUrl, userId, language, timestamp)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            saveFeedbackLog(timestamp)
            Toast.makeText(context, context.getString(R.string.feedback_thank_you), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.feedback_error), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun buildFeedbackUrl(baseUrl: String, userId: String, language: String, timestamp: String): String {
        val baseUri = Uri.parse(baseUrl)
        val supportsPrefill = baseUri.host?.contains("docs.google.com", ignoreCase = true) == true &&
            baseUri.path?.contains("/forms/", ignoreCase = true) == true

        if (!supportsPrefill) {
            return baseUri.toString()
        }

        return baseUri
            .buildUpon()
            .appendQueryParameter(ENTRY_USER_ID, userId)
            .appendQueryParameter(ENTRY_LANGUAGE, language)
            .appendQueryParameter(ENTRY_TIMESTAMP, timestamp)
            .build()
            .toString()
    }
    
    private fun saveFeedbackLog(timestamp: String) {
        sharedPreferences.edit().apply {
            putString(KEY_LAST_FEEDBACK_TIME, timestamp)
            putInt(KEY_FEEDBACK_COUNT, getFeedbackCount() + 1)
            apply()
        }
    }
    
    fun getFeedbackCount(): Int = sharedPreferences.getInt(KEY_FEEDBACK_COUNT, 0)
    
    fun getLastFeedbackTime(): Long {
        val timestampString = sharedPreferences.getString(KEY_LAST_FEEDBACK_TIME, "0") ?: "0"
        return timestampString.toLongOrNull() ?: 0L
    }
}

@Composable
fun FeedbackDialog(showDialog: MutableState<Boolean>, onConfirm: () -> Unit) {
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = { showDialog.value = false },
            title = { Text(stringResource(R.string.feedback_dialog_title)) },
            text = { Text(stringResource(R.string.feedback_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDialog.value = false
                    onConfirm()
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
