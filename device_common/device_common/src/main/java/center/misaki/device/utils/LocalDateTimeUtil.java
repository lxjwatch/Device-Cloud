package center.misaki.device.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author Misaki
 */
public class LocalDateTimeUtil {
    
    //1.匹配日期格式：yyyy-MM-dd HH:mm:ss
   public static final  String timeRegex1 = "^((([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29))\\s+([0-1]?[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$";
    //2.匹配日期格式：yyyy-MM-dd
    public static final String timeRegex2 = "(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29)$";
    //3.匹配日期格式：HH:mm:ss
    public static final String timeRegex3 = "^([0-1]?[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$";
    
    public static LocalDateTime parse(String time) {
        if (time.matches(timeRegex1)) {
            return LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else if (time.matches(timeRegex2)) {
            return LocalDateTime.parse(time + " 00:00:00",DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else if (time.matches(timeRegex3)) {
            return LocalDateTime.now().withHour(Integer.parseInt(time.split(":")[0])).withMinute(Integer.parseInt(time.split(":")[1])).withSecond(Integer.parseInt(time.split(":")[2]));
        } else {
            throw new IllegalArgumentException("时间格式不正确");
        }
    }
    
}
