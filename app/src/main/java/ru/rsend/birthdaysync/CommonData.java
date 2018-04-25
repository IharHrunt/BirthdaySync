package ru.rsend.birthdaysync;

public class CommonData {

    public int contactId;
    public String typeEventId;
    public int calendarId;

    public CommonData (int contactId, String typeEventId, int calendarId) {
        this.contactId = contactId;
        this.typeEventId = typeEventId;
        this.calendarId = calendarId;
    }

}
