package ru.rsend.birthdaysync;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import java.text.SimpleDateFormat;

import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import android.view.Menu;
import android.view.MenuItem;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

    public String TAG = "BIRTHDAY";
    public Boolean prefStartDefault = false;
    public String prefCalendarNumber;
    public long prefTimePicker;
    public String prefYearRepeat;
    public String prefReminder1;
    public String prefReminder2;
    public String prefReminder3;
    public Set<String> perfType;

    public static Boolean locked = false;
    public static String status = "";
    public static int logInsertEventTotal = 0;
    public static int logUpdateEventTotal = 0;
    public static int logDeleteEventTotal = 0;
    public static TextView textViewMain;
    public static ProgressBar progressBar;

    public Context context;
    public ArrayList<ContactData> ContactDataList;
    public ArrayList<CalendarData> CalendarDataList;
    public ArrayList<CommonData> CommonDataList;
    public AsyncTaskSyncNow asyncTaskSyncNow;
    public AsyncTaskRemoveAll asyncTaskRemoveAll;
    public static final String STATE_STATUS = "status";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            status = savedInstanceState.getString(STATE_STATUS);
        }
        setContentView(R.layout.activity_main);
        context = MainActivity.this;
        textViewMain = (TextView) findViewById(R.id.text_view_main);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        setScreenOrientation();
    }

    @Override
    public  void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_STATUS, status);
    }

    public void  setScreenOrientation() {
        try {
            switch (status) {
                case "start_sync":
                    menuSyncNow();
                    break;
                case "start_remove_all":
                    menuRemoveAll();
                    break;
                case "start_async_task":
                    textViewMain.setText(getResources().getString(R.string.progress_bar));
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case "end_sync":
                    progressBar.setVisibility(View.INVISIBLE);
                    textViewMain.setText(getResources().getString(R.string.text_view_main));
                    showAlertDialog(getResources().getString(R.string.alert_total_insert) + String.valueOf(logInsertEventTotal) +
                            "\n" + getResources().getString(R.string.alert_total_update) + String.valueOf(logUpdateEventTotal) +
                            "\n" + getResources().getString(R.string.alert_total_delete)  + String.valueOf(logDeleteEventTotal));
                    break;
                case "end_remove_all":
                    progressBar.setVisibility(View.INVISIBLE);
                    textViewMain.setText(getResources().getString(R.string.text_view_main));
                    showAlertDialog(getResources().getString(R.string.alert_total_delete) +  String.valueOf(logDeleteEventTotal));
                    break;
                default:
                    break;
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/setScreenOrientation();");
        }
    }

    public void  taskSyncNow() {
        try {
            getContactDataList();
            getCalendarDataList();
            getCommonDataList();

            logInsertEventTotal = 0;
            logUpdateEventTotal = 0;
            logDeleteEventTotal = 0;
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(prefTimePicker);
            int hour = cal.get(cal.HOUR_OF_DAY);
            int minute = cal.get(cal.MINUTE);

            for (CommonData common: CommonDataList) {
                //skip empty record
                if((common.contactId == 0)&&(common.calendarId == 0)) {
                    continue;
                }
                //delete event
                if((common.contactId == 0)&&(common.calendarId > 0)) {
                    deleteCalendarEvent(common.calendarId);
                }
                for (ContactData contact:ContactDataList){

                    Calendar calendar_dtstart = Calendar.getInstance(TimeZone.getDefault());
                    calendar_dtstart.set(calendar_dtstart.get(calendar_dtstart.YEAR), contact.month, contact.day, hour, minute, 0);
                    Long dtstart = calendar_dtstart.getTimeInMillis();
                    String subst = String.valueOf(dtstart);
                    subst = subst.substring(0, subst.length()-3) + "000";
                    dtstart = Long.valueOf(subst);

                    Long dtend = null;
                    String location = contact.typeEventEntry;
                    String description =  getResources().getString(R.string.event_date) + ": " + contact.date;
                    Boolean alarm = true;
                    String rrule = "FREQ=YEARLY;COUNT=" + String.valueOf(Integer.valueOf(prefYearRepeat) + 1) + ";WKST=MO";
                    String duration = "P0S"; //"+P1H" P3600S

                    //insert event
                    if ((contact.id == common.contactId) && (common.calendarId == 0) && (contact.typeEventId.equals(common.typeEventId))) {
                        //rdelete existed contactId and typeEventId
                        modifyTableCommonData("DELETE_CONTACT_ID_TYPE_EVENT_ID", common.contactId, common.typeEventId, 0);
                        insertCalendarEvent(common.contactId, common.typeEventId, Integer.valueOf(prefCalendarNumber),
                                            contact.name, location, description, dtstart, dtend, alarm, rrule, duration);
                        break;
                    }
                    //update event
                    if((contact.id == common.contactId) && (common.calendarId > 0) && (contact.typeEventId.equals(common.typeEventId))) {
                        updateCalendarEvent(common.contactId, common.typeEventId, common.calendarId, Integer.valueOf(prefCalendarNumber),
                                            contact.name + " ", location, description, dtstart, dtend, alarm, rrule, duration);
                        break;
                    }
                }
            }
       }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/taskInsertDataCalendar();");
        }
    }

    public void taskRemoveAll() {
        try {
            getContactDataList();
            getCalendarDataList();
            getCommonDataList();

            logDeleteEventTotal = 0;
            if (CommonDataList != null) {
                for (CommonData common : CommonDataList) {
                    if (common.calendarId > 0) {
                        deleteCalendarEvent(common.calendarId);
                    }
                }
            }
            modifyTableCommonData("DELETE_ALL", 0, "0", 0);
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/taskRemoveAll();");
        }
    }
    ////////////////////////////////////show Dialog Alert///////////////////////////////////////////
    public void showAlertDialog (String text) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(text);
            builder.setPositiveButton(R.string.btn_ok, new OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                   if (!locked) {
                       status = "";
                   }
                   return;
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/showAlertDialog();");
        }
    }

    //////////////////////////////////////menu item start///////////////////////////////////////////
    public void menuSyncNow() {
        try {
            if (locked) {
                showAlertDialog(getResources().getString(R.string.alert_message_wait));
                return;
            }
            status = "start_sync";
            getPreferenceData();
            if (!prefStartDefault) {
                showAlertDialog(getResources().getString(R.string.alert_message_warning));
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(R.string.alert_message_add);
            builder.setPositiveButton(R.string.btn_ok, new OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    status = "start_async_task";
                    asyncTaskSyncNow = new AsyncTaskSyncNow();
                    asyncTaskSyncNow.execute();
                }
            });
            builder.setNegativeButton(R.string.btn_cancel, new OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    status = "";
                    return;
                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/menuInsertDataCalendar();");
        }
    }

    public void menuRemoveAll() {
        try {
            if (locked) {
                showAlertDialog(getResources().getString(R.string.alert_message_wait));
                return;
            }
            status = "start_remove_all";
            getPreferenceData();
            if (!prefStartDefault) {
                showAlertDialog(getResources().getString(R.string.alert_message_warning));
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage(R.string.alert_message_remove);
            builder.setPositiveButton(R.string.btn_ok, new OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    status = "start_async_task";
                    asyncTaskRemoveAll = new AsyncTaskRemoveAll();
                    asyncTaskRemoveAll.execute();
               }
            });
            builder.setNegativeButton(R.string.btn_cancel, new OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {
                    status = "";
                    return;
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/menuRemoveAll()");
        }
    }

    /////////////////////////////////get calendar events////////////////////////////////////////////
    public void getCalendarDataList() {
        try {
            CalendarDataList = new ArrayList<>();
            Cursor cursor = getContentResolver().query(Events.CONTENT_URI, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    if (cursor.getInt(cursor.getColumnIndex(Events.DELETED)) != 1) {
                        CalendarDataList.add(new CalendarData(cursor.getInt(cursor.getColumnIndex(Events._ID))));
                        ////////////////////////////////////////////////////////////////////////////
                        //Calendar cl = Calendar.getInstance();
                        //cl.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(Events.DTSTART)));  //here your time in miliseconds
                        //String date = "" + cl.get(Calendar.DAY_OF_MONTH) + ":" + cl.get(Calendar.MONTH) + ":" + cl.get(Calendar.YEAR);
                        //String time = "" + cl.get(Calendar.HOUR_OF_DAY) + ":" + cl.get(Calendar.MINUTE) + ":" + cl.get(Calendar.SECOND)+
                        //              ":" + cl.get(Calendar.MILLISECOND);
                        //if (cursor.getInt(cursor.getColumnIndex(Events.CALENDAR_ID)) <= 2 ) {
                        //    Log.e(TAG, "DEBUG CalendarDataList: " +
                        //            "Calendar_id=" + cursor.getInt(cursor.getColumnIndex(Events.CALENDAR_ID)) +
                        //            " id=" + cursor.getInt(cursor.getColumnIndex(Events._ID)) +
                        //            " Title=" + cursor.getString(cursor.getColumnIndex(Events.TITLE)) +
                        //            " Description=" + cursor.getString(cursor.getColumnIndex(Events.DESCRIPTION)) +
                        //            " DTStart=" + cursor.getLong(cursor.getColumnIndex(Events.DTSTART)) +
                        //            " DTEnd=" + cursor.getLong(cursor.getColumnIndex(Events.DTEND)) +
                        //            " rrule=" + cursor.getString(cursor.getColumnIndex(Events.RRULE)) +
                        //            " duration: " + cursor.getString(cursor.getColumnIndex(Events.DURATION)) +
                        //            " has_alarm: " + cursor.getString(cursor.getColumnIndex(Events.HAS_ALARM)) +
                        //            " dirty: " + cursor.getString(cursor.getColumnIndex(Events.DIRTY)) +
                        //            " deleted=" + cursor.getString(cursor.getColumnIndex(Events.DELETED)) +
                        //            " date=" + date + " time=" + time
                        //    );
                        //}
                        ////////////////////////////////////////////////////////////////////////////
                    }
                    Cursor cursorDetail = getContentResolver().query(Reminders.CONTENT_URI, null, null, null, null);
                    if (cursorDetail.moveToFirst()) {
                        do {
                            if (cursor.getInt(cursor.getColumnIndex(Events._ID)) == cursorDetail.getInt(cursorDetail.getColumnIndex(Reminders.EVENT_ID))) {
                                ////////////////////////////////////////////////////////////////////////
                                //Log.e(TAG, "DEBUG Reminders: Id=" +
                                //           cursorDetail.getInt(cursorDetail.getColumnIndex(Reminders._ID)) +
                                //           ", Minutes=" + cursorDetail.getInt(cursorDetail.getColumnIndex(Reminders.MINUTES)) +
                                //           ", Method=" + cursorDetail.getInt(cursorDetail.getColumnIndex(Reminders.METHOD)) +
                                //           ", EventId=" + cursorDetail.getInt(cursorDetail.getColumnIndex(Reminders.EVENT_ID))
                                //);
                                ////////////////////////////////////////////////////////////////////////
                            }
                        } while (cursorDetail.moveToNext());
                    }
                    cursorDetail.close();
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        catch(Exception e )
        {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/getCalendarDataList();");
        }
    }

    ////////////////////////////////add calendar event and reminder/////////////////////////////////
    public int insertCalendarEvent(int contactId, String typeEventId, int calendarNumber, String title, String location,
                                   String description, Long dtstart, Long dtend, Boolean alarm, String rrule, String duration) {
        try {
            ContentValues values = new ContentValues();
            values.put(Events.CALENDAR_ID, calendarNumber);
            values.put(Events.TITLE, title);
            values.put(Events.EVENT_LOCATION, location);
            values.put(Events.DESCRIPTION, description);
            values.put(Events.DTSTART, dtstart);
            values.put(Events.DTEND, dtend);
            values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            values.put(Events.HAS_ALARM, alarm);
            values.put(Events.RRULE, rrule);
            values.put(Events.DURATION, duration);
            Uri addUri = getContentResolver().insert(Events.CONTENT_URI, values);
            int result = Integer.parseInt(addUri.getLastPathSegment());
            int calendarId = result;
            if (!(prefReminder1.equalsIgnoreCase("Off"))) {
                insertCalendarEventReminder(calendarId, prefReminder1);
            }
            if (!(prefReminder2.equalsIgnoreCase("Off"))) {
                insertCalendarEventReminder(calendarId, prefReminder2);
            }
            if (!(prefReminder3.equalsIgnoreCase("Off"))) {
                insertCalendarEventReminder(calendarId, prefReminder3);
            }
            if (result != 0 ) {
                modifyTableCommonData("INSERT", contactId, typeEventId, calendarId);
                logInsertEventTotal++;
            }
            ////////////////////////////////////////////////////////////////////////////////////////
            //Log.e(TAG,"DEBUG insert contactId=" + contactId + "; typeEventId=" + typeEventId + "; calendarId=" +
            //           calendarId + "; calendarNumber=" +  calendarNumber + "; " + title + "; dtstart=" + dtstart +
            //           "; rrule = " + rrule + "; duration" + duration);
            ////////////////////////////////////////////////////////////////////////////////////////
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/insertCalendarEvent();");
            return 0;
        }
    }

    public int insertCalendarEventReminder(int calendarId, String prefReminder)  {
        try {
            ContentValues values = new ContentValues();
            values.put(Reminders.EVENT_ID, calendarId);
            values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
            values.put(Reminders.MINUTES, Integer.valueOf(prefReminder));
            Uri addUri = getContentResolver().insert(Reminders.CONTENT_URI, values);
            int result = Integer.parseInt(addUri.getLastPathSegment());
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/insertCalendarEventReminder();");
            return 0;
        }
    }

    ////////////////////////////////update calendar event and reminder//////////////////////////////
    public int updateCalendarEvent(int contactId, String typeEventId, int calendarId, int calendarNumber, String title, String location,
                                   String description, Long dtstart, Long dtend, Boolean alarm, String rrule, String duration) {
        try {
            Cursor cursor = getContentResolver().query(Reminders.CONTENT_URI, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    if (calendarId == cursor.getInt(cursor.getColumnIndex(Reminders.EVENT_ID))) {
                        deleteCalendarEventReminder(cursor.getInt(cursor.getColumnIndex(Reminders._ID)));
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();

            if (!(prefReminder1.equalsIgnoreCase("Off"))) {
                insertCalendarEventReminder(calendarId, prefReminder1);
            }
            if (!(prefReminder2.equalsIgnoreCase("Off"))) {
                insertCalendarEventReminder(calendarId, prefReminder2);
            }
            if (!(prefReminder3.equalsIgnoreCase("Off"))) {
                insertCalendarEventReminder(calendarId, prefReminder3);
            }
            ContentValues values = new ContentValues();
            values.put(Events.CALENDAR_ID, calendarNumber);
            values.put(Events.TITLE, title);
            values.put(Events.EVENT_LOCATION, location);
            values.put(Events.DESCRIPTION, description);
            values.put(Events.DTSTART, dtstart);
            values.put(Events.DTEND, dtend);
            values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
            values.put(Events.HAS_ALARM, alarm);
            values.put(Events.RRULE, rrule);
            values.put(Events.DURATION, duration);
            Uri updateUri2 = ContentUris.withAppendedId(Events.CONTENT_URI, calendarId);
            int result = getContentResolver().update(updateUri2, values, null, null);
            if (result != 0 ) {
                modifyTableCommonData("UPDATE", contactId, typeEventId, calendarId);
                logUpdateEventTotal++;
            }
            ////////////////////////////////////////////////////////////////////////////
            //Log.e(TAG,"DEBUG update contactId=" + contactId + "; typeEventId=" + typeEventId +
            //          "; calendarId=" + calendarId + ";" + "; calendarNumber=" + calendarNumber +
            //          "; " + title + "; dtstart" + dtstart  + "; rrule = " + rrule + "; duration" + duration);
            ////////////////////////////////////////////////////////////////////////////
            return result;
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  MainActivity.java/updateCalendarEvent();");
            return 0;
        }
    }

    ///////////////////////////////////delete calendar event and reminder///////////////////////////
    public int deleteCalendarEvent(int calendarId) {
        try {
            Cursor cursor = getContentResolver().query(Reminders.CONTENT_URI, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    if (calendarId == cursor.getInt(cursor.getColumnIndex(Reminders.EVENT_ID))) {
                        deleteCalendarEventReminder(cursor.getInt(cursor.getColumnIndex(Reminders._ID)));
                    }
                }   while (cursor.moveToNext());
            }
            cursor.close();
            Uri deleteUri = ContentUris.withAppendedId(Events.CONTENT_URI, calendarId);
            int result = getContentResolver().delete(deleteUri, null, null);
            if (result != 0 ) {
                modifyTableCommonData("DELETE_CALENDAR_ID", 0, "0",calendarId);
                logDeleteEventTotal++;
            }
            ////////////////////////////////////////////////////////////////////////////////////////
            //Log.e(TAG,"DEBUG delete contactId=0; calendarId=" + calendarId );
            ////////////////////////////////////////////////////////////////////////////////////////
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/deleteCalendarEvent();");
            return 0;
        }
    }

    public int deleteCalendarEventReminder(int calendarId) {
        try {
            Uri deleteUri = ContentUris.withAppendedId(Reminders.CONTENT_URI, calendarId);
            int result = getContentResolver().delete(deleteUri, null, null);
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/deleteCalendarEventReminder();");
            return 0;
        }
    }

    /////////////////////////////// read and modify database////////////////////////////////////////
    public void getTableCommonData() {
        try {
            CommonDataList = new ArrayList<>();//Important
            DBHelper dbHelper = new DBHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor cursor = db.query("tableCommonData", null, null, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    CommonDataList.add(new CommonData(
                        cursor.getInt(cursor.getColumnIndex("contactId")),
                        cursor.getString(cursor.getColumnIndex("typeEventId")),
                        cursor.getInt(cursor.getColumnIndex("calendarId"))
                    ));
                    ////////////////////////////////////////////////////////////////////////////////
                    //Log.e(TAG, "DEBUG TableCommonData Id = " + cursor.getInt(cursor.getColumnIndex("id")) +
                    //           ", contactId = " + cursor.getInt(cursor.getColumnIndex("contactId")) +
                    //           ", typeEventId = " + cursor.getString(cursor.getColumnIndex("typeEventId")) +
                    //           ", calendarId = " + cursor.getInt(cursor.getColumnIndex("calendarId")));
                    ////////////////////////////////////////////////////////////////////////////////
                } while (cursor.moveToNext());
            }
            cursor.close();
            dbHelper.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/readTableCommonData();");
        }
    }

    public long modifyTableCommonData(String flag, int contactId, String typeEventId, int calendarId) {
        try {
            long result = 0;
            DBHelper dbHelper = new DBHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            switch (flag) {
                case "INSERT":
                    values.put("contactId", contactId);
                    values.put("typeEventId", typeEventId);
                    values.put("calendarId", calendarId);
                    result = db.insert("tableCommonData", null, values);
                    break;
                case "UPDATE":
                    values.put("contactId", contactId);
                    values.put("typeEventId", typeEventId);
                    values.put("calendarId", calendarId);
                    result = db.update("tableCommonData", values, "contactId = ? AND calendarId = ?",
                                        new String[] { String.valueOf(contactId),String.valueOf(calendarId)});
                    break;
                case "DELETE_CALENDAR_ID":
                    result = db.delete("tableCommonData", "calendarId = ?",
                                       new String[] {String.valueOf(calendarId)});
                    break;
                case "DELETE_CONTACT_ID_TYPE_EVENT_ID":
                    result = db.delete("tableCommonData", "contactId = ? AND typeEventId = ?",
                                       new String[] {String.valueOf(contactId), typeEventId});
                    break;
                case "DELETE_ALL":
                    result = db.delete("tableCommonData", null, null);
                    break;
            }
            dbHelper.close();
            return result;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/modifyTableCommonData();");
            return  0;
        }
    }

    /////////////////////////////// get preferences ////////////////////////////////////////////////
    public void getPreferenceData() {
        try  {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            prefStartDefault = sp.getBoolean("pref_start_default", false);
            if (!prefStartDefault) {
                return;
            }
            String defaultCalendarId = "1";
            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
            int year = calendar.get(calendar.YEAR);
            int month = calendar.get(calendar.MONTH) + 1;
            int day = calendar.get(calendar.DAY_OF_MONTH);
            calendar.set(year, month, day, 9, 00, 000);
            long defaultTimePicker = calendar.getTimeInMillis();
            String defaultYear = "1";
            String defaultReminder1 = "1";
            String defaultReminder2 = "Off";
            String defaultReminder3 = "Off";
            Set<String> defaultType = new HashSet<>();
            defaultType.add("3");

            prefCalendarNumber = sp.getString("pref_list_calendar_number", defaultCalendarId);
            prefTimePicker = sp.getLong("pref_time_picker", defaultTimePicker);
            prefYearRepeat = sp.getString("pref_list_year_repeat", defaultYear);
            prefReminder1 = sp.getString("pref_list_reminder1", defaultReminder1);
            prefReminder2 = sp.getString("pref_list_reminder2", defaultReminder2);
            prefReminder3 = sp.getString("pref_list_reminder3", defaultReminder3);
            perfType = sp.getStringSet("pref_list_type",defaultType);
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/getConfigData();");
        }
    }

    public void getCommonDataList() {
        try {
            //get data from sql table
            getTableCommonData();
            //add contact to update and insert into calendar
            ArrayList<CommonData> CommonDataListTmp = new ArrayList<>();
            for (ContactData contact:ContactDataList) {
                Boolean found = false;
                int i = 0;
                for (CommonData common: CommonDataList){
                    if ((contact.id == common.contactId)&&(contact.typeEventId.equals(common.typeEventId))) {
                        found = true;
                        CommonDataListTmp.add(new CommonData(common.contactId, common.typeEventId, common.calendarId));
                        break;
                    }
                    i++;
                }
                if (!found) {
                    CommonDataListTmp.add(new CommonData(contact.id, contact.typeEventId, 0));
                }
                else {
                    CommonDataList.remove(i);
                }
            }
            //add calendar event without contact
            for (CommonData database: CommonDataList){
                CommonDataListTmp.add(new CommonData(0, "0", database.calendarId));
            }
            //calendar event that have contact
            CommonDataList = new ArrayList<>();
            for (CommonData common:CommonDataListTmp ) {
                Boolean found = false;
                for (CalendarData calendar: CalendarDataList){
                    if (calendar.id == common.calendarId) {
                        found = true;
                        CommonDataList.add(new CommonData(common.contactId, common.typeEventId, common.calendarId));
                        break;
                    }
                }
                if (!found) {
                    CommonDataList.add(new CommonData(common.contactId, common.typeEventId, 0));
                }
            }

            ////////////////////////////////////////////////////////////////////////////////////////
            //if (CommonDataList != null) {
            //for (int i = 0; i < CommonDataList.size(); i++) {
            //        CommonData common = CommonDataList.get(i);
            //        Log.e(TAG,"DEBUG CommonDataList:  i=" + i + "; contactId=" +
            //                  common.contactId + "; typeEventId =" + common.typeEventId + "; calendarId=" + common.calendarId);
            //    }
            //}
            ////////////////////////////////////////////////////////////////////////////////////////
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/loadCommonData();");
        }
    }

    ///////////////////////////////// Menu selection ///////////////////////////////////////////////
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sync_now:
                menuSyncNow();
                break;
            case R.id.menu_remove_all:
                menuRemoveAll();
                break;
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("EXIT", true);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /////////////////////////////get Contacts///////////////////////////////////////////////////////
    public void getContactDataList() {
        try {
            ContactDataList = new ArrayList<>();//Important
            ContentResolver cr = getContentResolver();
            Cursor cursor = cr.query(Contacts.CONTENT_URI, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(Contacts._ID));
                    String displayName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
                    Cursor cursorDetail = cr.query(
                        (Data.CONTENT_URI),
                        (new String[]{Data.CONTACT_ID, CommonDataKinds.Event.START_DATE, Data.MIMETYPE, CommonDataKinds.Event.TYPE, CommonDataKinds.Event._ID}),
                        (Data.CONTACT_ID + "=? AND " + Data.MIMETYPE + "=?"),
                        (new String[]{String.valueOf(id), CommonDataKinds.Event.CONTENT_ITEM_TYPE}),
                        (null)
                    );
                    if (cursorDetail.moveToFirst()) {
                        do {
                            String startDate = cursorDetail.getString(cursorDetail.getColumnIndex(CommonDataKinds.Event.START_DATE));
                            String typeEventId = cursorDetail.getString(cursorDetail.getColumnIndex(CommonDataKinds.Event._ID));
                            String typeEventValue = cursorDetail.getString(cursorDetail.getColumnIndex(CommonDataKinds.Event.TYPE));
                            Boolean found = false;
                            if (!(perfType.isEmpty())) {
                                for (String str : perfType) {
                                    if (typeEventValue.equalsIgnoreCase(str)) {
                                        found = true;
                                    }
                                }
                            }
                            String typeEventEntry = "";
                            if (!(startDate.equalsIgnoreCase("")) && (found)) {
                                switch (typeEventValue) {
                                    case "0":
                                        typeEventEntry = getResources().getString(R.string.type_event_custom);
                                        break;
                                    case "1":
                                        typeEventEntry = getResources().getString(R.string.type_event_anniversary);
                                        break;
                                    case "2":
                                        typeEventEntry = getResources().getString(R.string.type_event_other);
                                        break;
                                    case "3":
                                        typeEventEntry = getResources().getString(R.string.type_event_birthday);
                                        break;
                                    default:
                                        typeEventEntry="";
                                        break;
                                }
                                Date date = convertStringToDate(startDate);
                                if (date != null) {
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTime(date);
                                    ContactDataList.add(new ContactData(id, displayName, startDate, typeEventId,
                                            typeEventValue, typeEventEntry, calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
                                    ));
                                }
                            }
                        } while (cursorDetail.moveToNext());
                    }
                    cursorDetail.close();
                } while (cursor.moveToNext());
            }
            cursor.close();
            ////////////////////////////////////////////////////////////////////////////////////////
            //if (ContactDataList != null) {
            //    for (int i = 0; i < ContactDataList.size(); i++) {
            //        ContactData cm = ContactDataList.get(i);
            //        Log.e(TAG,"DEBUG ContactDataList: i=" + i + "; " + cm.id + "; " + cm.name + "; " + cm.typeEventId + "; " + cm.typeEventValue );
            //    }
            //}
            ////////////////////////////////////////////////////////////////////////////////////////
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/getContactDataList();");
        }
    }

    public Date convertStringToDate(String strIn) {
        Date date = null;
        try {
            final SimpleDateFormat[] dateFormats = {
                    new SimpleDateFormat("yyyy-MM-dd"), new SimpleDateFormat("yyyyMMdd"), new SimpleDateFormat("yyyy.MM.dd"),
                    new SimpleDateFormat("yy-MM-dd"),   new SimpleDateFormat("yyMMdd"),   new SimpleDateFormat("yy.MM.dd"),
                    new SimpleDateFormat("yy/MM/dd"),   new SimpleDateFormat("MM-dd"),    new SimpleDateFormat("MMdd"),
                    new SimpleDateFormat("MM/dd"),      new SimpleDateFormat("MM.dd")
            };
            for (SimpleDateFormat f : dateFormats) {
                try {
                    date = f.parse(strIn);
                    if (date!=null) {
                        break;
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "Function error: MainActivity.java/convertStringToDate();");
                    return date;
                }
            }
            return date;
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: MainActivity.java/convertStringToDate();");
            return date;
        }
    }

    //////////////// AsyncTask class SyncNow////////////////////////////////////////////////////////
    class AsyncTaskSyncNow extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            locked = true;
            textViewMain.setText(getResources().getString(R.string.progress_bar));
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                taskSyncNow();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            status = "end_sync";
            locked = false;
            progressBar.setVisibility(View.INVISIBLE);
            textViewMain.setText(getResources().getString(R.string.text_view_main));
            showAlertDialog(getResources().getString(R.string.alert_total_insert) + String.valueOf(logInsertEventTotal) +
                    "\n" + getResources().getString(R.string.alert_total_update) + String.valueOf(logUpdateEventTotal) +
                    "\n" + getResources().getString(R.string.alert_total_delete)  + String.valueOf(logDeleteEventTotal));
        }
    }

    //////////////// AsyncTask class RemoveAll/////////////////////////////////////////////////////
    class AsyncTaskRemoveAll extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            locked = true;
            textViewMain.setText(getResources().getString(R.string.progress_bar));
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                taskRemoveAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            locked = false;
            status = "end_remove_all";
            progressBar.setVisibility(View.INVISIBLE);
            textViewMain.setText(getResources().getString(R.string.text_view_main));
            showAlertDialog(getResources().getString(R.string.alert_total_delete) +  String.valueOf(logDeleteEventTotal));
        }
    }

}
