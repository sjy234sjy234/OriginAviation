package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;

import java.util.Date;

/**
 * Created by peicheng on 17/8/2.
 */
public class TransferLimitation {
    //进港航班ID
    private String inFlightId;
    //出港航班ID
    private String outFlightId;
    //最短转机时限（分钟）
    private int minTransferTime;
    //中转旅客人数
    private int transferPassNum;

    public String getInFlightId() {
        return inFlightId;
    }

    public String getOutFlightId() {
        return outFlightId;
    }

    public int getMinTransferTime() {
        return minTransferTime * 60 * 1000;
    }

    public int getTransferPassNum() {
        return transferPassNum;
    }

    //构造函数
    public TransferLimitation(Row row){
        if(row.getPhysicalNumberOfCells() != 4){
            throw new RuntimeException("中转时间限制信息的数据列数错误，不等于4项！");
        }
        DataFormatter df = new DataFormatter();
        inFlightId = df.formatCellValue(row.getCell(0));
        outFlightId = df.formatCellValue(row.getCell(1));
        minTransferTime = Integer.parseInt(df.formatCellValue(row.getCell(2)));
        transferPassNum = Integer.parseInt(df.formatCellValue(row.getCell(3)));

    }
}
