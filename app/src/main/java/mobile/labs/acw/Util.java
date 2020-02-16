package mobile.labs.acw;

/**
 * Created by ryan on 13/03/2018.
 */

public class Util {

    public static int Mod(int x){
        return x < 0 ? 0-x : x;
    }

    public static String getMinutesSecondsString(long millis){
        int minutes, seconds;
        minutes = (int)millis / 60000;
        seconds = (int)(millis - minutes)/1000;
        String minutesString = (minutes < 10 ? "0" : "") + String.valueOf(minutes);
        String secondsString = (seconds < 10 ? "0" : "") + String.valueOf(seconds);

        return minutesString + ":" + secondsString;
    }
}
