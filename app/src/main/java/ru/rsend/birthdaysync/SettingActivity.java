package ru.rsend.birthdaysync;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.content.Intent;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.MenuItem;
import android.support.v7.app.ActionBar;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TimeZone;


public class SettingActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public String TAG = "BIRTHDAY";
    public CharSequence [] csCalendarNumberEntry;
    public CharSequence [] csCalendarNumberEntryValue;
    public CharSequence [] csYearRepeatEntry;
    public CharSequence [] csYearRepeatEntryValue;
    public CharSequence [] csReminderEntry;
    public CharSequence [] csReminderEntryValue;
    public CharSequence [] csTypeEntry;
    public CharSequence [] csTypeEntryValue;
    public Context context;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        context = SettingActivity.this;
        setupActionBar();
        getCalendarArray();
        getYearArray();
        getReminderArray();
        getTypeArray();
        getPreferenceDefault();
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(root);
        populatePreferenceHierarchy(root);
    }

    private void populatePreferenceHierarchy(PreferenceScreen root) {
        try {
            // Category first
            PreferenceCategory prefCatFirst = new PreferenceCategory(this);
            prefCatFirst.setTitle(R.string.pref_category_first);
            root.addPreference(prefCatFirst);

            // List calendar list preference
            ListPreference listPrefCal = new ListPreference(this);
            listPrefCal.setDialogTitle(R.string.pref_title_list_calendar_number);
            listPrefCal.setKey("pref_list_calendar_number");
            listPrefCal.setTitle(R.string.pref_title_list_calendar_number);
            listPrefCal.setEntries(csCalendarNumberEntry);
            listPrefCal.setEntryValues(csCalendarNumberEntryValue);
            prefCatFirst.addPreference(listPrefCal);

            // Time picker to notify preference
            TimePreference timePref = new TimePreference(this);
            timePref.setKey("pref_time_picker");
            timePref.setTitle(R.string.pref_title_time_picker);
            prefCatFirst.addPreference(timePref);

            // List years to notify preference
            ListPreference listPrefYear = new ListPreference(this);
            listPrefYear.setDialogTitle(R.string.pref_title_list_year_repeat);
            listPrefYear.setKey("pref_list_year_repeat");
            listPrefYear.setTitle(R.string.pref_title_list_year_repeat);
            listPrefYear.setEntries(csYearRepeatEntry);
            listPrefYear.setEntryValues(csYearRepeatEntryValue);
            prefCatFirst.addPreference(listPrefYear);

            // Category second
            PreferenceCategory prefCatSecond = new PreferenceCategory(this);
            prefCatSecond.setTitle(R.string.pref_category_second);
            root.addPreference(prefCatSecond);

            // Type multilist preference
            MultiSelectListPreference listPrefType = new MultiSelectListPreference(this);
            listPrefType.setDialogTitle(R.string.pref_title_list_type);
            listPrefType.setKey("pref_list_type");
            listPrefType.setTitle(R.string.pref_title_list_type);
            listPrefType.setEntries(csTypeEntry);
            listPrefType.setEntryValues(csTypeEntryValue);
            prefCatFirst.addPreference(listPrefType);

            // List reminder1 list preference
            ListPreference listPrefReminder1 = new ListPreference(this);
            listPrefReminder1.setDialogTitle(R.string.pref_title_list_reminder1);
            listPrefReminder1.setKey("pref_list_reminder1");
            listPrefReminder1.setTitle(R.string.pref_title_list_reminder1);
            listPrefReminder1.setEntries(csReminderEntry);
            listPrefReminder1.setEntryValues(csReminderEntryValue);
            prefCatSecond.addPreference(listPrefReminder1);

            // List reminder2 list preference
            ListPreference listPrefReminder2 = new ListPreference(this);
            listPrefReminder2.setDialogTitle(R.string.pref_title_list_reminder2);
            listPrefReminder2.setKey("pref_list_reminder2");
            listPrefReminder2.setTitle(R.string.pref_title_list_reminder2);
            listPrefReminder2.setEntries(csReminderEntry);
            listPrefReminder2.setEntryValues(csReminderEntryValue);
            prefCatSecond.addPreference(listPrefReminder2);

            // List reminder3 list preference
            ListPreference listPrefReminder3 = new ListPreference(this);
            listPrefReminder3.setDialogTitle(R.string.pref_title_list_reminder3);
            listPrefReminder3.setKey("pref_list_reminder3");
            listPrefReminder3.setTitle(R.string.pref_title_list_reminder3);
            listPrefReminder3.setEntries(csReminderEntry);
            listPrefReminder3.setEntryValues(csReminderEntryValue);
            prefCatSecond.addPreference(listPrefReminder3);

        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: SettingActivity.java/populatePreferenceHierarchy();");
        }
    }

    public void getPreferenceDefault() {
        try  {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            Boolean prefStartDefault = sp.getBoolean("pref_start_default", false);
            if (prefStartDefault == false) {
                setPreferenceData();
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: SettingActivity.java/getPreferenceDat();");
        }
    }

    public void setPreferenceData() {
        try {

            String defaultCalendarId = "1";
            if (csCalendarNumberEntryValue != null) {
                defaultCalendarId = csCalendarNumberEntryValue[0].toString();
            }
            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
            int year = calendar.get(calendar.YEAR);
            int month = calendar.get(calendar.MONTH) + 1;
            int day = calendar.get(calendar.DAY_OF_MONTH);
            calendar.set(year, month, day, 9, 00);
            long defaultTimePicker = calendar.getTimeInMillis();
            String defaultYear = "1";
            //minute 0, 1, 5, 10, 15, 20, 25, 30, 45, hour  1/60, 2/120, 3/180, 12/720, 24/1440, day 2/2880, week 1/10080
            String defaultReminder1 = "1";
            String defaultReminder2 = "Off";
            String defaultReminder3 = "Off";
            Set<String> defaultType = new HashSet<>();
            defaultType.add("3");

            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("pref_list_calendar_number",defaultCalendarId);
            editor.putLong("pref_time_picker", defaultTimePicker);
            editor.putString("pref_list_year_repeat", defaultYear);
            editor.putString("pref_list_reminder1", defaultReminder1);
            editor.putString("pref_list_reminder2", defaultReminder2);
            editor.putString("pref_list_reminder3", defaultReminder3);
            editor.putStringSet("pref_list_type", defaultType);
            editor.putBoolean("pref_start_default", true);
            editor.commit();
        }
        catch(Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error: SettingActivity.java/setPreferenceDat();");
        }
    }

    public void getYearArray() {
        try {
            csYearRepeatEntryValue = getResources().getStringArray(R.array.array_year_repeat_entry_value);
            csYearRepeatEntry = getResources().getStringArray(R.array.array_year_repeat_entry);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  SettingActivity.java/getYearArray();");
        }
    }

    public void getReminderArray() {
        try {
            csReminderEntryValue = getResources().getStringArray(R.array.array_reminder_entry_value);
            csReminderEntry = getResources().getStringArray(R.array.array_reminder_entry);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  SettingActivity.java/getReminderArray();");
        }
    }

    public void getTypeArray() {
        try {
            csTypeEntryValue = getResources().getStringArray(R.array.array_type_entry_value);
            csTypeEntry = getResources().getStringArray(R.array.array_type_entry);
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  SettingActivity.java/getReminderArray();");
        }
    }
    public void getCalendarArray() {
        try {
            ContentResolver cr = getContentResolver();
            Cursor cursor =  cr.query(
                    (CalendarContract.Calendars.CONTENT_URI),
                    (new String[]{CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME, CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE}),
                    (CalendarContract.Calendars.VISIBLE + " = 1"),
                    (null),
                    (CalendarContract.Calendars._ID + " ASC")
            );

            int i = 0;
            int count = cursor.getCount();
            csCalendarNumberEntry = new String[count];
            csCalendarNumberEntryValue = new String[count];
            if (count > 0) {
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) != null) {
                        csCalendarNumberEntry[i] = cursor.getString(1);
                    }
                    else {
                        csCalendarNumberEntry[i] = getResources().getString(R.string.pref_calendar_noname);
                    }
                    csCalendarNumberEntryValue[i] = String.valueOf(cursor.getInt(0));
                    i++;
                }
            }
            cursor.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  SettingActivity.java/getCalendarArray();");
        }
    }

    public void setListPreferenceSummary() {
        try {
            Preference prefListCalendar = findPreference("pref_list_calendar_number");
            if (((ListPreference) prefListCalendar).getEntry() != null) {
                prefListCalendar.setSummary(((ListPreference) prefListCalendar).getEntry());
            }
            Preference prefListYear = findPreference("pref_list_year_repeat");
            if (((ListPreference) prefListYear).getEntry() != null) {
                prefListYear.setSummary(((ListPreference) prefListYear).getEntry());
            }
            Preference prefListReminder1 = findPreference("pref_list_reminder1");
            if (((ListPreference) prefListReminder1).getEntry() != null) {
                prefListReminder1.setSummary(((ListPreference) prefListReminder1).getEntry());
            }
            Preference prefListReminder2 = findPreference("pref_list_reminder2");
            if (((ListPreference) prefListReminder2).getEntry() != null) {
                prefListReminder2.setSummary(((ListPreference) prefListReminder2).getEntry());
            }
            Preference prefListReminder3 = findPreference("pref_list_reminder3");
            if (((ListPreference) prefListReminder3).getEntry() != null) {
                prefListReminder3.setSummary(((ListPreference) prefListReminder3).getEntry());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  SettingActivity.java/setSharedPrferencesSummary(;");
        }
    }

    public void setMultiListPreferenceSummary() {
        try {
            Preference prefListType = findPreference("pref_list_type");
            if (((MultiSelectListPreference) prefListType).getEntries() != null) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
                Set<String> defaultType = new LinkedHashSet<String>();
                defaultType.add("0");
                String summary = "";
                Set<String> perfType = sp.getStringSet("pref_list_type",defaultType);
                ArrayList<String> listTmp = new ArrayList<>();

                if (!(perfType.isEmpty())) {
                    for (String str1 : perfType) {
                        listTmp.add(str1);
                    }
                    Collections.sort(listTmp);
                    for (String str2: listTmp) {
                        switch (str2) {
                            case "0":
                                str2 = getResources().getString(R.string.type_event_custom);
                                break;
                            case "1":
                                str2 = getResources().getString(R.string.type_event_anniversary);
                                break;
                            case "2":
                                str2 = getResources().getString(R.string.type_event_other);
                                break;
                            case "3":
                                str2 = getResources().getString(R.string.type_event_birthday);
                                break;
                            default:
                                str2="";
                                break;
                        }
                        if (summary.equals("")) {
                            summary = summary  + str2;
                        }
                        else {
                            summary = summary  + ", " + str2;
                        }
                    }
                }
                if (summary.equals("")) {
                    summary = getResources().getString(R.string.pref_type_off);
                }
                prefListType.setSummary(summary);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  SettingActivity.java/setMultiPreferenceSummary(;");
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        try {
            setListPreferenceSummary();
            setMultiListPreferenceSummary();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  SettingActivity.java/onSharedPreferenceChanged();");
        }
    }

    @Override
    public void onResume() {
        try {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            setListPreferenceSummary();
            setMultiListPreferenceSummary();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Function error:  SettingActivity.java/onResume();");
        }
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
}