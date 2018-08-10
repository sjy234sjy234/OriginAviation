package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.util.Date;

/**
 * Created by peicheng on 17/6/28.
 */
public class Scene {
    //开始时间
    private Date startDateTime;
    //介绍时间
    private Date endDateTime;
    //场景影响类型 (0：降落，1：飞行，2：停机)
    private AffectType type;
    //机场
    private String airport;
    //航班ID
    private String flightId;
    //飞机ID
    private String airplaneId;
    //停机数
    private int stopAirplaneNum;

    public Date getStartDateTime() {
        return startDateTime;
    }

    public Date getEndDateTime() {
        return endDateTime;
    }

    public AffectType getType() {
        return type;
    }

    public String getAirport() {
        return airport;
    }

    public String getFlightId() {
        return flightId;
    }

    public String getAirplaneId() {
        return airplaneId;
    }

    public int getStopAirplaneNum() {
        return stopAirplaneNum;
    }

    public Scene(Row row){
        if(row.getPhysicalNumberOfCells() != 7){
            throw new RuntimeException("故障信息的数据列数错误，不等于7项！");
        }
        DataFormatter df = new DataFormatter();
        startDateTime = row.getCell(0).getDateCellValue();
        endDateTime = row.getCell(1).getDateCellValue();
        type = typeTransfer(df.formatCellValue(row.getCell(2)));
        airport = df.formatCellValue(row.getCell(3));
        flightId = df.formatCellValue(row.getCell(4));
        airplaneId = df.formatCellValue(row.getCell(5));
        stopAirplaneNum = Integer.parseInt(df.formatCellValue(row.getCell(6)).isEmpty() ? "0" : df.formatCellValue(row.getCell(6)));
    }

    private AffectType typeTransfer(String limitType){
        if("降落".equals(limitType)){
            return AffectType.LANDING;
        }
        else if("起飞".equals(limitType)){
            return AffectType.FLYING;
        }
        else if("停机".equals(limitType)){
            return AffectType.STOPPING;
        }
        else {
            throw new RuntimeException("出现未知的场景类型错误！");
        }
    }

    /**
     * 根据传进来的数据，判断是否落在受影响的场景内
     * @param flightId       //暂时没有航班故障做处理
     * @param airplaneId     //暂时没有飞机故障做处理
     * @param startAirport
     * @param endAirport
     * @param startTime
     * @param endTime
     * @return
     */
    public boolean isInScene(String flightId, String airplaneId, String startAirport, String endAirport, Date startTime, Date endTime){
       if(type.equals(AffectType.LANDING)){    //降落判断
            if(airport.equals(endAirport) && (endTime.after(startDateTime) && endTime.before(endDateTime)))
                return true;
        }
        else if(type.equals(AffectType.FLYING)){    //起飞判断
            if(airport.equals(startAirport) && (startTime.after(startDateTime) && startTime.before(endDateTime)))
                return true;
        }
        return false;
    }

    /**
     * 根据传进来的数据，判断起飞是否落在受影响的场景内
     * @param flightId       //暂时没有航班故障做处理
     * @param airplaneId     //暂时没有飞机故障做处理
     * @param startAirport
     * @param startTime
     * @return
     */
    public boolean isStartInScene(String flightId, String airplaneId, String startAirport, Date startTime){
        if(type.equals(AffectType.FLYING)){    //起飞故障判断
            if(airport.equals(startAirport) && (startTime.after(startDateTime) && startTime.before(endDateTime)))
                return true;
        }
        return false;
    }

    /**
     * 根据传进来的数据，判断降落是否落在受影响的场景内
     * @param flightId       //暂时没有航班故障做处理
     * @param airplaneId     //暂时没有飞机故障做处理
     * @param endAirport
     * @param endTime
     * @return
     */
    public boolean isEndInScene(String flightId, String airplaneId, String endAirport, Date endTime){
       if(type.equals(AffectType.LANDING)){    //降落故障判断
           if(airport.equals(endAirport) && (endTime.after(startDateTime) && endTime.before(endDateTime)))
               return true;
        }
        return false;
    }

    /**
     * 根据传进来的数据，判断停机时间段是否落在受影响的场景内
     * @param airport
     * @param startTime
     * @param endTime
     * @return
     */
    public boolean isStopInScene(String airport, Date startTime, Date endTime){
        if(type.equals(AffectType.STOPPING)){    //机场停机判断
            if(this.airport.equals(airport) && (!(endTime.before(endDateTime)) && (!startTime.after(startDateTime))))
                return true;
        }
        return false;
    }
}
