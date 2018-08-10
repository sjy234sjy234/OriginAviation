package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

import java.util.Date;

/**
 * Created by peicheng on 17/6/29.
 */
public class Configuration {
    /**
     *   目标函数值 = p1*调机空飞航班数 + p2*取消航班数 + p3*机型发生变化的航班数 + p4*换飞机数量 + p5*联程拉直航班的个数 + p6*航班总延误时间（小时） + p7*航班总提前时间（小时）
     *   + p8*取消旅客人数 +p9*延迟旅客人数 +p10*签转延误旅客人数
     *   + p11 * 非可行标识（0-1变量） + p12 * 违背约束数量 。
     */

    //获取调机参数
    public static double getAdjustFlightParam(){
        return 5000;
    }
    //获取取消航班参数
    public static double getCancelFlightParam(){
        return 1200;
    }
    //获取机型变换参数
    public static double getFlightTypeChangeParam(String typeOne, String typeTwo){
        double coefficient = 0.0;
        if(typeOne.equals(typeTwo)
                || (typeOne.equals("2") && typeTwo.equals("1"))){
            coefficient = 0.0;
        }
        else if(typeOne.equals("1") && typeTwo.equals("2")){
            coefficient = 0.5;
        }
        else if((typeOne.equals("1") && typeTwo.equals("3"))
                || (typeOne.equals("1") && typeTwo.equals("4"))
                || (typeOne.equals("2") && typeTwo.equals("3"))
                || (typeOne.equals("2") && typeTwo.equals("4"))){
            coefficient = 1.5;
        }
        else if((typeOne.equals("3") && typeTwo.equals("1"))
                || (typeOne.equals("3") && typeTwo.equals("2"))
                || (typeOne.equals("3") && typeTwo.equals("4"))
                || (typeOne.equals("4") && typeTwo.equals("3"))){
            coefficient = 2.0;
        }
        else if((typeOne.equals("4") && typeTwo.equals("1"))
                || (typeOne.equals("4") && typeTwo.equals("2"))){
            coefficient = 4.0;
        }
        return 500 * coefficient;
    }
    //获取换飞机参数
    private static Date swapDateBoundary = Utils.timeStringToDate("2017/05/06 16:00");
    public static double getSwapFlightParam(long time){
        if(time <= swapDateBoundary.getTime())
            return 15.0;
        else
            return 5.0;
    }
    //获取联程航班参数
    public static double getConnectFlightStraightenParam(){
        return 750;
    }
    //获取航班延误参数
    public static double getDelayFlightParam(){
        return 100;
    }
    //获取航班提前参数
    public static double getAheadFlightParam() {
        return 150;
    }
    //获取旅客取消参数
    public static double getCancelPassengerParam() {
        return 4;
    }
    //获取正常旅客取消参数
    public static double getNormalPassengerDelayParam(double delayHour) {
        if(delayHour > 0 && delayHour <= 2) {
            return 1.0;
        }
        else if(delayHour > 2 && delayHour <= 4){
            return 1.5;
        }
        else if(delayHour > 4 && delayHour <= 8){
            return 2.0;
        }
        else if(delayHour > 8 && delayHour <= 36){
            return 3.0;
        }
        else if(delayHour > 36){
            return maxValue;
        }
        else {
            return 0.0;
        }
    }
    //获取签转旅客延误参数
    public static double getSignChangePassengerDelayParam(double delayHour) {
        if(delayHour >= 0.0 && delayHour < 6.0){
            return 1.0 / 30.0 * delayHour;
        }
        else if(delayHour >= 6.0 && delayHour < 12.0) {
            return 1.0 / 24.0 * delayHour;
        }
        else if(delayHour >= 12.0 && delayHour < 24.0){
            return 1.0 / 24.0 * delayHour;
        }
        else if(delayHour >= 24.0 && delayHour < 36.0){
            return 1.0 / 18.0 * delayHour;
        }
        else if(delayHour >= 36.0 && delayHour <= 48.0){
            return 1.0 / 16.0 * delayHour;
        }
        else {
            return maxValue;
        }
    }
    //非可行解的惩罚值参数
    public static final double infeasibilityPenaltyParam = 5 * 10e6;
    //违背约束的单项惩罚值参数
    public static final double constraintViolationPenaltyParam = 10;

    //限制条件
    public static final long maxAheadTime = 6 * 60 * 60 * 1000;
    public static final long maxIntervalTime =  50 * 60 * 1000;
    public static final long maxDomesticDelayTime =  24 * 60 * 60 * 1000;
    public static final long maxAbroadDelayTime =  36 * 60 * 60 * 1000;

    //算法参数
    public static final double maxValue = 1000000000;

}
