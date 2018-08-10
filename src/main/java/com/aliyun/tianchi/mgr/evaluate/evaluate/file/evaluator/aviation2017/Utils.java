package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by peicheng on 17/6/29.
 */
public class Utils {
    private static final String TIME_PATTERN = "yyyy/MM/dd HH:mm";
    private static final String DAY_PATTERN = "yyyy/MM/dd";

    /**
     * 将时间字符串转化为Date类
     * @param timeStr
     * @return
     */
    public static Date timeStringToDate(String timeStr) {
        SimpleDateFormat sf = new SimpleDateFormat(TIME_PATTERN);
        Date date = null;
        try {
            date = sf.parse(timeStr);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * 将时间戳转化为时间字符串
     * @param time
     * @return
     */
    public static String timeStampToString(long time) {
        SimpleDateFormat sf = new SimpleDateFormat(TIME_PATTERN);
        Date date = new Date(time);
        return sf.format(date);
    }

    /**
     * 将时间戳转化为对应的日期所对应的时间戳
     * @param timeStr
     * @return
     */
    public static Date timeStampToDate(String timeStr) {
        SimpleDateFormat sf = new SimpleDateFormat(DAY_PATTERN);
        Date date = null;
        try {
            date = sf.parse(timeStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }
}
