package ru.rsend.birthdaysync;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DBHelper extends SQLiteOpenHelper {
    private String TAG = "BIRTHDAY";
    public DBHelper(Context context) {
        super(context, "DB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS tableCommonData ("
                + "id integer primary key autoincrement,"
                + "contactId integer,"
                + "typeEventId string,"
                + "calendarId integer" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}