package com.mmall.util;

import java.math.BigDecimal;

/**
 * Created by lenovo on 2018/10/10.
 * java在浮点计算过程中的丢失精度问题
 * 使用BigDecimal的Double类型的构造器
 */

public class BigDecimalUtil {
    // 不让这个类在外部调用初始化
    private BigDecimalUtil() {

    }

    // 不会丢失精度的d加法
    public static BigDecimal add(double v1, double v2){
        // 将double转换为string
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.add(b2);
    }

    public static BigDecimal sub(double v1, double v2){
        // 将double转换为string
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.subtract(b2);
    }

    public static BigDecimal mul(double v1, double v2){
        // 将double转换为string
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        return b1.multiply(b2);
    }

    public static BigDecimal div(double v1, double v2){
        // 将double转换为string
        BigDecimal b1 = new BigDecimal(Double.toString(v1));
        BigDecimal b2 = new BigDecimal(Double.toString(v2));
        // 除不尽的情况
        // （数字，保留2位小数，四舍五入模式）
        return b1.divide(b2,2,BigDecimal.ROUND_HALF_UP); // 四舍五入，保留2位小数

    }

}
