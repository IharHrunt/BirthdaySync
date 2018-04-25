package ru.rsend.birthdaysync;

public class ContactData {

    public int id;
    public String name;
    public String date;
    public String typeEventId;
    public String typeEventValue;
    public String typeEventEntry;
    public int year;
    public int month;
    public int day;

    public ContactData (int id, String name, String date, String typeEventId, String typeEventValue,
                        String typeEventEntry, int year, int month, int day) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.typeEventId = typeEventId;
        this.typeEventValue = typeEventValue;
        this.typeEventEntry = typeEventEntry;
        this.year = year;
        this.month= month;
        this.day= day;
    }
}
