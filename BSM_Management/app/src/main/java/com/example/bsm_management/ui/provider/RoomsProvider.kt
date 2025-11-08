package com.example.bsm_management.ui.provider

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import database.DatabaseHelper

class RoomsProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.example.bsm_management.ui.provider"
        const val TABLE_ROOMS = "rooms"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$TABLE_ROOMS")

        private const val ROOMS = 1
        private const val ROOM_ID = 2

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, TABLE_ROOMS, ROOMS)
            addURI(AUTHORITY, "$TABLE_ROOMS/#", ROOM_ID)
        }
    }

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(context)
        return true
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        val db = dbHelper.readableDatabase
        return when (uriMatcher.match(uri)) {
            ROOMS -> db.query(TABLE_ROOMS, projection, selection, selectionArgs, null, null, sortOrder)
            ROOM_ID -> {
                val id = ContentUris.parseId(uri)
                db.query(TABLE_ROOMS, projection, "id=?", arrayOf(id.toString()), null, null, sortOrder)
            }
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val db = dbHelper.writableDatabase
        val id = db.insert(TABLE_ROOMS, null, values)
        return if (id > 0) ContentUris.withAppendedId(CONTENT_URI, id) else null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        return db.update(TABLE_ROOMS, values, selection, selectionArgs)
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbHelper.writableDatabase
        return db.delete(TABLE_ROOMS, selection, selectionArgs)
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.dir/vnd.$AUTHORITY.$TABLE_ROOMS"
}
