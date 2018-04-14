package twilio;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class TimeProvider {

    String format;
    DateFormat dateFormat;
    Calendar cal;

    static TimeProvider tp = new TimeProvider();

    public static TimeProvider getInstance() {
        return tp;
    }

    private TimeProvider() {
        format = "yyyy/MM/dd HH:mm:ss";
        dateFormat = new SimpleDateFormat(format);
        cal = Calendar.getInstance();
    }

    public String getDateAndTime() {
        return dateFormat.format(cal.getTime());
    }
}
