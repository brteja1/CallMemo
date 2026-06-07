package com.example.androidcallnotes

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactUtils {
    fun getContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber == null || phoneNumber.isBlank()) return null
        
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
