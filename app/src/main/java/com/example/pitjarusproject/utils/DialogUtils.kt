package com.example.pitjarusproject.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

@Suppress("SpellCheckingInspection")
object DialogUtils {

    fun showDialog(context: Context, title: String, message: String, callback: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Ya") { dialog, _ ->
                callback()
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}