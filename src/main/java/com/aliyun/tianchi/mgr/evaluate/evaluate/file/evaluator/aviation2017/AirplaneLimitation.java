package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

/**
 * Created by peicheng on 17/6/28.
 */
public class AirplaneLimitation {
    //起飞机场
    private String startAirport;
    //降落机场
    private String endAirport;
    //飞机ID
    private String airplaneId;

    public String getStartAirport() {
        return startAirport;
    }

    public String getEndAirport() {
        return endAirport;
    }

    public String getAirplaneId() {
        return airplaneId;
    }

    public AirplaneLimitation(Row row){
        if(row.getPhysicalNumberOfCells() != 3){
            throw new RuntimeException("航线-飞机限制信息的数据列数错误，不等于3项！");
        }
        DataFormatter df = new DataFormatter();
        startAirport = df.formatCellValue(row.getCell(0));
        endAirport = df.formatCellValue(row.getCell(1));
        airplaneId = df.formatCellValue(row.getCell(2));
    }
}
