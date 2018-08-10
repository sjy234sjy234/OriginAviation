package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;


import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.util.Date;

/**
 * Created by peicheng on 17/6/28.
 */
public class Flight implements Comparable{
    //航班ID
    private String flightId;
    //日期
    private Date date;
    //国际/国内
    private boolean isDomestic;
    //航班号
    private String flightNo;
    //起飞机场
    private String startAirport;
    //降落机场
    private String endAirport;
    //起飞时间
    private Date startDateTime;
    //降落时间
    private Date endDateTime;
    //飞机ID
    private String airplaneId;
    //机型
    private String airplaneType;
    //旅客数
    private int passengerNum;
    //联程旅客数
    private int connectPassengerNum;
    //座位数
    private int seatNum;
    //重要系数
    private float importRatio;

    //飞行时间
    private long travelTime;
    //是否是联程航班
    private boolean isConnected = false;
    //联程航班位置
    private boolean isConnectedPrePart;
    //联程航班后置航班
    private String connectedFlightId;

    //普通旅客数量
    private int normalPassengerNum;

    public void setConnected(String connectedFlightId, boolean loc) {
        isConnected = true;
        this.connectedFlightId = connectedFlightId;
        this.isConnectedPrePart = loc;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public String getConnectedFlightId() {
        return connectedFlightId;
    }

    public String getFlightId() {
        return flightId;
    }

    public Date getDate() {
        return date;
    }

    public boolean isDomestic() {
        return isDomestic;
    }

    public String getFlightNo() {
        return flightNo;
    }

    public String getStartAirport() {
        return startAirport;
    }

    public String getEndAirport() {
        return endAirport;
    }

    public Date getStartDateTime() {
        return startDateTime;
    }

    public Date getEndDateTime() {
        return endDateTime;
    }

    public String getAirplaneId() {
        return airplaneId;
    }

    public String getAirplaneType() {
        return airplaneType;
    }

    public float getImportRatio() {
        return importRatio;
    }

    public long getTravelTime() {
        return travelTime;
    }

    public int getPassengerNum() {
        return passengerNum;
    }

    public int getConnectPassengerNum() {
        return connectPassengerNum;
    }

    public int getSeatNum() {
        return seatNum;
    }

    public boolean isConnectedPrePart() {
        return isConnectedPrePart;
    }

    public int getNormalPassengerNum() {
        return normalPassengerNum;
    }

    public void setNormalPassengerNum(int normalPassengerNum) {
        this.normalPassengerNum = normalPassengerNum;
    }

    //构造函数
    public Flight(Row row){
        if(row.getPhysicalNumberOfCells() != 14){
            throw new RuntimeException("航班信息的数据列数错误，不等于14项！");
        }
        DataFormatter df = new DataFormatter();
        flightId = df.formatCellValue(row.getCell(0));
        date = row.getCell(1).getDateCellValue();
        isDomestic = df.formatCellValue(row.getCell(2)).equals("国内") ? true : false;
        flightNo = df.formatCellValue(row.getCell(3));
        startAirport = df.formatCellValue(row.getCell(4));
        endAirport = df.formatCellValue(row.getCell(5));
        startDateTime = row.getCell(6).getDateCellValue();
        endDateTime = row.getCell(7).getDateCellValue();
        airplaneId = df.formatCellValue(row.getCell(8));
        airplaneType = df.formatCellValue(row.getCell(9));
        passengerNum = Integer.parseInt(df.formatCellValue(row.getCell(10)));
        connectPassengerNum = Integer.parseInt(df.formatCellValue(row.getCell(11)));
        seatNum = Integer.parseInt(df.formatCellValue(row.getCell(12)));
        importRatio = Float.parseFloat(df.formatCellValue(row.getCell(13)));
        travelTime = endDateTime.getTime() - startDateTime.getTime();
    }

    @Override
    public int compareTo(Object o) {
        Flight other = (Flight) o;
        if (this.airplaneId.equals(other.airplaneId)) {
            if (this.startDateTime.after(other.startDateTime)) {
                return 1;
            } else if (this.startDateTime.before(other.startDateTime)) {
                return -1;
            } else {
                return 0;
            }
        } else {
            return this.airplaneId.compareTo(other.airplaneId);
        }
    }
}
