package com.mmall.util;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

/**
 * Created by lenovo on 2018/10/9.
 */

/**
 * 时间转换工具类
 */
public class DateTimeUtil {
    //joda-time
    //str->Date 字符串转换为Date类型
    //Date->str Date类型转换为字符串类型
    // 转换的格式
    public static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static Date strToDate(String dateTimeStr, String formatStr) {
        // 传入字符串的格式
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(formatStr);
        // 传入需要转换的时间字符串
        DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);
        return dateTime.toDate();

    }

    // 传入的是一个Date类型的数据，传出string类型
    public static String dateToStr(Date date, String formatSt) {
        if(date == null)
            return StringUtils.EMPTY;
        DateTime dateTime = new DateTime(date);
        return dateTime.toString(formatSt);
    }



    public static Date strToDate(String dateTimeStr) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(STANDARD_FORMAT);
        DateTime dateTime = dateTimeFormatter.parseDateTime(dateTimeStr);
        return dateTime.toDate();

    }

    public static String dateToStr(Date date) {
        if(date == null)
            return StringUtils.EMPTY;
        DateTime dateTime = new DateTime(date);
        return dateTime.toString(STANDARD_FORMAT);
    }
}
