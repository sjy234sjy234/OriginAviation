package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;


import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

/**
 * Created by peicheng on 17/7/2.
 */
public class TravelTime {
    //机型
    private String airplaneType;
    //起飞机场
    private String startAirport;
    //降落机场
    private String endAirport;
    //起飞时间
    private int travelTime;

    public String getAirplaneType() {
        return airplaneType;
    }

    public String getStartAirport() {
        return startAirport;
    }

    public String getEndAirport() {
        return endAirport;
    }

    public long getTravelTime() {
        return travelTime * 60 * 1000l;
    }

    //构造函数
    public TravelTime(Row row){
        if(row.getPhysicalNumberOfCells() != 4){
            throw new RuntimeException("机场之间的飞行时间数据列数错误，不等于4项！");
        }
        DataFormatter df = new DataFormatter();
        airplaneType = df.formatCellValue(row.getCell(0));
        startAirport = df.formatCellValue(row.getCell(1));
        endAirport = df.formatCellValue(row.getCell(2));
        travelTime = Integer.parseInt(df.formatCellValue(row.getCell(3)));
    }
}
