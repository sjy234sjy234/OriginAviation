package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;


import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.util.Date;

/**
 * Created by peicheng on 17/6/28.
 */
public class AirportClose {
    //机场
    private String airport;
    //关闭时间
    private long beginCloseTime;
    //开放时间
    private long endCloseTime;
    //生效日期
    private Date beginDate;
    //失效日期
    private Date endDate;

    public String getAirport() {
        return airport;
    }

    public long getBeginCloseTime() {
        return beginCloseTime;
    }

    public long getEndCloseTime() {
        return endCloseTime;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public AirportClose(Row row){
        if(row.getPhysicalNumberOfCells() != 5){
            throw new RuntimeException("机场关闭限制信息的数据列数错误，不等于5项！");
        }
        DataFormatter df = new DataFormatter();
        airport =  df.formatCellValue(row.getCell(0));
        beginCloseTime = processTimeStr( df.formatCellValue(row.getCell(1)));
        endCloseTime = processTimeStr( df.formatCellValue(row.getCell(2)));
        beginDate = row.getCell(3).getDateCellValue();
        endDate = row.getCell(4).getDateCellValue();
    }

    //时间字符串（0:00, 00:00）的处理
    private long processTimeStr(String timeStr){
        if(timeStr.length() == 4){
            int hour = Integer.parseInt(timeStr.substring(0, 1));
            int minute = Integer.parseInt(timeStr.substring(2, 4));
            return (hour * 60 + minute) * 60 * 1000;
        }
        else if(timeStr.length() == 5) {
            int hour = Integer.parseInt(timeStr.substring(0, 2));
            int minute = Integer.parseInt(timeStr.substring(3, 5));
            return (hour * 60 + minute) * 60 * 1000;
        }
        else {
            throw new RuntimeException("机场关闭限制信息中的时间字符串格式不满足格式要求(0:00, 00:00)！");
        }
    }

    //判断时间戳是否落在关闭时间窗内
    public boolean isClosed(long time){
        String timeStr = Utils.timeStampToString(time);
        Date date = Utils.timeStampToDate(timeStr);
        if(date.before(beginDate) || (!date.before(endDate))){
            return false;
        }
        long start = date.getTime() + beginCloseTime;
        long end = date.getTime() + endCloseTime;
        if(time > start && time < end){
            return true;
        }
        return false;
    }
}
