package com.rakesh.dittomobile
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Clip(val id: Long, val text: String, val created: Long, val pinned: Boolean)
class ClipStore(context: Context) : SQLiteOpenHelper(context, "ditto_mobile.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE clips (id INTEGER PRIMARY KEY AUTOINCREMENT, body TEXT NOT NULL UNIQUE, created INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE INDEX clips_created ON clips(created DESC)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    @Synchronized fun add(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val values = ContentValues().apply { put("body", clean); put("created", System.currentTimeMillis()) }
        writableDatabase.insertWithOnConflict("clips", null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }
    @Synchronized fun list(query: String = ""): List<Clip> {
        val db = readableDatabase
        val cursor = if (query.isBlank()) db.rawQuery("SELECT id,body,created,pinned FROM clips ORDER BY pinned DESC, created DESC", null)
        else db.rawQuery("SELECT id,body,created,pinned FROM clips WHERE body LIKE ? ORDER BY pinned DESC, created DESC", arrayOf("%$query%"))
        return cursor.use { c -> buildList { while (c.moveToNext()) add(Clip(c.getLong(0), c.getString(1), c.getLong(2), c.getInt(3) == 1)) } }
    }
    @Synchronized fun pin(id: Long, value: Boolean) { writableDatabase.execSQL("UPDATE clips SET pinned=? WHERE id=?", arrayOf(if (value) 1 else 0, id)) }
    @Synchronized fun delete(id: Long) { writableDatabase.delete("clips", "id=?", arrayOf(id.toString())) }
    @Synchronized fun clearUnpinned() { writableDatabase.delete("clips", "pinned=0", null) }
}
