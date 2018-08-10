package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.*;

/**
 * Created by peicheng on 17/7/11.
 */
public class InputData {

    //定义输入数据结构
    private Map<String, Flight> flightMap = new HashMap<>();
    private Map<String, List<Flight>> airLineMap = new HashMap<>();
    private Map<String, List<AirplaneLimitation>> airplaneLimitationMap = new HashMap<>();
    private Map<String, List<AirportClose>> airportCloseMap = new HashMap<>();
    private List<Scene> sceneList = new ArrayList<>();
    private Map<String, TravelTime> travelTimeMap = new HashMap<>();
    private Map<String, Long> flightIntervalTimeMap = new HashMap<>();
    private Map<String, Map<String, TransferLimitation>> transferInInfoMap = new HashMap<>();
    private Map<String, Map<String, TransferLimitation>> transferOutInfoMap = new HashMap<>();
    private Set<String> domesticAirportSet = new HashSet<>();               //国内机场集合，用于控制调机
    private Map<String, String> airplaneStartAirportMap = new HashMap<>();  //飞机起始机场映射表，用于限定飞机起始机场，判断飞机是否空飞
    private Map<String, String> airplaneEndAirportMap = new HashMap<>();  //飞机起始机场映射表，用于限定飞机结束机场，判断飞机是否空飞
    private Map<String, Integer> endAirportMap = new HashMap<>();           //结束机场统计，用于实现基地平衡
    private AdjustTimeWindow adjustTimeWindow;     //控制恢复调整窗口
    private CapacityLimitation capacityLimitation; //用于控制单位时间容量限制
    private Set<String> affectedAirportSet = new HashSet<>();  //记录受台风影响降落和起飞的机场
    private Set<String> stopAirportSet = new HashSet<>();      //记录受台风影响停机的机场

    public Map<String, Flight> getFlightMap() {
        return flightMap;
    }

    public Map<String, List<Flight>> getAirLineMap() {
        return airLineMap;
    }

    public Map<String, List<AirplaneLimitation>> getAirplaneLimitationMap() {
        return airplaneLimitationMap;
    }

    public Map<String, List<AirportClose>> getAirportCloseMap() {
        return airportCloseMap;
    }

    public List<Scene> getSceneList() {
        return sceneList;
    }

    public Map<String, TravelTime> getTravelTimeMap() {
        return travelTimeMap;
    }

    public Map<String, Long> getFlightIntervalTimeMap() {
        return flightIntervalTimeMap;
    }

    public Set<String> getDomesticAirportSet() {
        return domesticAirportSet;
    }

    public Map<String, String> getAirplaneStartAirportMap() {
        return airplaneStartAirportMap;
    }

    public Map<String, String> getAirplaneEndAirportMap() {
        return airplaneEndAirportMap;
    }

    public Map<String, Integer> getEndAirportMap() {
        return endAirportMap;
    }

    public Map<String, Map<String, TransferLimitation>> getTransferInInfoMap() {
        return transferInInfoMap;
    }

    public Map<String, Map<String, TransferLimitation>> getTransferOutInfoMap() {
        return transferOutInfoMap;
    }

    public AdjustTimeWindow getAdjustTimeWindow() {
        return adjustTimeWindow;
    }

    public CapacityLimitation getCapacityLimitation() {
        return capacityLimitation;
    }

    public Set<String> getAffectedAirportSet() {
        return affectedAirportSet;
    }

    public Set<String> getStopAirportSet() {
        return stopAirportSet;
    }

    public InputData(InputStream inputStream){
        //读取输入数据
        readInputData(inputStream);
        //设置联程航班标识
        Iterator<String> iterator = airLineMap.keySet().iterator();
        while(iterator.hasNext()){
            String airplaneId = iterator.next();
            List<Flight> flightList = airLineMap.get(airplaneId);
            Collections.sort(flightList);
            airplaneStartAirportMap.put(airplaneId, flightList.get(0).getStartAirport());
            airplaneEndAirportMap.put(airplaneId , flightList.get(flightList.size() - 1).getEndAirport());
            for(int index = 1; index < flightList.size(); ++ index){
                Flight preFlight = flightList.get(index - 1);
                Flight flight = flightList.get(index);
                if(flight.getDate().equals(preFlight.getDate()) && preFlight.getFlightNo().equals(flight.getFlightNo())){
                    preFlight.setConnected(flight.getFlightId(), true);
                    flight.setConnected(preFlight.getFlightId(), false);
                    if (preFlight.getConnectPassengerNum() != flight.getConnectPassengerNum()){
                        throw  new RuntimeException("联程航班数据有问题");
                    }
                }
                flightIntervalTimeMap.put(preFlight.getFlightId() + "#" + flight.getFlightId(),
                        flight.getStartDateTime().getTime() - preFlight.getEndDateTime().getTime());
                if(flight.getSeatNum() != flightList.get(0).getSeatNum()){
                    throw  new RuntimeException("同一飞机的座位数不同");
                }
            }
            for(int index = 0; index < flightList.size(); ++ index){
                Flight flight = flightList.get(index);
                if(!flight.isConnected() && flight.getConnectPassengerNum() > 0){
                    throw  new RuntimeException("非联程航班，联程旅客数量大于0");
                }
            }
            String endAirport = flightList.get(flightList.size() - 1).getEndAirport();
            String key = endAirport + "#" + flightList.get(0).getAirplaneType();
            if(endAirportMap.containsKey(key)){
                endAirportMap.put(key, endAirportMap.get(key) + 1);
            }
            else{
                endAirportMap.put(key, 1);
            }
        }
        iterator = flightMap.keySet().iterator();
        while(iterator.hasNext()) {
            String flightId = iterator.next();
            Flight flight = flightMap.get(flightId);
            int totalTransferInPassNum = 0;
            if(transferInInfoMap.containsKey(flightId)){
                Iterator<String> iter = transferInInfoMap.get(flightId).keySet().iterator();
                while(iter.hasNext()){
                    String inFlightId = iter.next();
                    totalTransferInPassNum += transferInInfoMap.get(flightId).get(inFlightId).getTransferPassNum();
                }
                //数据上保证中转出去的旅客数不大于航班中旅客数量
                if (totalTransferInPassNum > flight.getSeatNum() - flight.getConnectPassengerNum()
                        || totalTransferInPassNum > flight.getPassengerNum()) {
                    throw new RuntimeException("中转出去的旅客数大于旅客数量");
                }
            }
            int totalTransferOutPassNum = 0;
            if(transferOutInfoMap.containsKey(flightId)){
                Iterator<String> iter = transferOutInfoMap.get(flightId).keySet().iterator();
                while(iter.hasNext()){
                    String outFlightId = iter.next();
                    totalTransferOutPassNum += transferOutInfoMap.get(flightId).get(outFlightId).getTransferPassNum();
                }
                //数据上保证中转进来的旅客数不大于航班中旅客数量
                if (totalTransferOutPassNum > flight.getSeatNum() - flight.getConnectPassengerNum()
                        || totalTransferOutPassNum > flight.getPassengerNum()) {
                    throw new RuntimeException("中转进来的旅客数大于旅客数量");
                }
            }
            //数据上保证中转进来和出去的旅客数量不大于航班中旅客数量
            if (totalTransferInPassNum + totalTransferOutPassNum > flight.getSeatNum() - flight.getConnectPassengerNum()
                    || totalTransferInPassNum + totalTransferOutPassNum > flight.getPassengerNum()) {
                throw new RuntimeException("中转进来和出去的旅客数大于旅客数量");
            }
            flight.setNormalPassengerNum(flight.getPassengerNum() - totalTransferInPassNum);

            //数据上保证联程旅客数量不大于航班中座位数
            if (flight.getConnectPassengerNum() > flight.getSeatNum()) {
                throw new RuntimeException("联程旅客数量大于航班座位数");
            }
        }

        //设置恢复窗口限制
        long startAdjustTime = Utils.timeStringToDate("2017/05/06 06:00").getTime();
        long endAdjustTime = Utils.timeStringToDate("2017/05/09 00:00").getTime();
        adjustTimeWindow = new AdjustTimeWindow(startAdjustTime, endAdjustTime);
        //设置单位时间容量限制
        long startBeforeTime = Utils.timeStringToDate("2017/05/06 15:00").getTime();
        long endBeforeTime = Utils.timeStringToDate("2017/05/06 16:00").getTime();
        long startAfterTime = Utils.timeStringToDate("2017/05/07 17:00").getTime();
        long endAfterTime = Utils.timeStringToDate("2017/05/07 19:00").getTime();
        capacityLimitation = new CapacityLimitation(startBeforeTime, endBeforeTime, startAfterTime, endAfterTime);
    }

    /**
     * 读取输入数据集，将数据进行整理，方便约束检测，结果评分
     * @param inputStream
     */
    private void readInputData(InputStream inputStream){
        try {
            XSSFWorkbook workBook = new XSSFWorkbook(inputStream);

            //读取航班信息
            XSSFSheet flightSheet = workBook.getSheet("航班");
            Iterator<Row> rowIterator = flightSheet.iterator();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                if(row.getRowNum() == 0)
                    continue;
                Flight flight = new Flight(row);
                flightMap.put(flight.getFlightId(), flight);
                if(airLineMap.containsKey(flight.getAirplaneId())){
                    airLineMap.get(flight.getAirplaneId()).add(flight);
                }
                else {
                    List<Flight> flightList = new ArrayList<>();
                    flightList.add(flight);
                    airLineMap.put(flight.getAirplaneId(), flightList);
                }
            }

            //读取航线-飞机限制信息
            XSSFSheet airplaneLimitSheet = workBook.getSheet("航线-飞机限制");
            rowIterator = airplaneLimitSheet.iterator();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                if(row.getRowNum() == 0)
                    continue;
                AirplaneLimitation airplaneLimitation = new AirplaneLimitation(row);
                String key = airplaneLimitation.getStartAirport() + "#" + airplaneLimitation.getEndAirport();
                if(airplaneLimitationMap.containsKey(key))
                    airplaneLimitationMap.get(key).add(airplaneLimitation);
                else {
                    List<AirplaneLimitation> airplaneLimitationList = new ArrayList<>();
                    airplaneLimitationList.add(airplaneLimitation);
                    airplaneLimitationMap.put(key, airplaneLimitationList);
                }
            }

            //读取机场关闭限制信息
            XSSFSheet airportCloseSheet = workBook.getSheet("机场关闭限制");
            rowIterator = airportCloseSheet.iterator();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                if(row.getRowNum() == 0)
                    continue;
                AirportClose airportClose = new AirportClose(row);
                String key = airportClose.getAirport();
                if(airportCloseMap.containsKey(key))
                    airportCloseMap.get(key).add(airportClose);
                else {
                    List<AirportClose> airportCloseList = new ArrayList<>();
                    airportCloseList.add(airportClose);
                    airportCloseMap.put(key, airportCloseList);
                }
            }

            //读取故障信息
            XSSFSheet typhoonSceneSheet = workBook.getSheet("台风场景");
            rowIterator = typhoonSceneSheet.iterator();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                if(row.getRowNum() == 0)
                    continue;
                Scene scene = new Scene(row);
                if(scene.getType().equals(AffectType.FLYING) || scene.getType().equals(AffectType.LANDING)) {
                    affectedAirportSet.add(scene.getAirport());
                }
                if(scene.getType().equals(AffectType.STOPPING)){
                    stopAirportSet.add(scene.getAirport());
                }
                sceneList.add(scene);
            }

            //读取飞行时间信息
            XSSFSheet travelTimeSheet = workBook.getSheet("飞行时间");
            rowIterator = travelTimeSheet.iterator();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                if(row.getRowNum() == 0)
                    continue;
                TravelTime travelTime = new TravelTime(row);
                String key = travelTime.getAirplaneType() + "#"
                        + travelTime.getStartAirport() + "#"
                        + travelTime.getEndAirport();
                travelTimeMap.put(key, travelTime);
            }

            //读取机场信息
            XSSFSheet airportSheet = workBook.getSheet("机场");
            rowIterator = airportSheet.iterator();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                if(row.getRowNum() == 0)
                    continue;
                DataFormatter df = new DataFormatter();
                String airport = df.formatCellValue(row.getCell(0));
                int domesticFlag = Integer.parseInt(df.formatCellValue(row.getCell(1)));
                if(domesticFlag == 1){
                    domesticAirportSet.add(airport);
                }
            }

            //读取中转时间限制信息
            XSSFSheet transferTimeSheet = workBook.getSheet("中转时间限制");
            rowIterator = transferTimeSheet.iterator();
            while(rowIterator.hasNext()){
                Row row = rowIterator.next();
                if(row.getRowNum() == 0)
                    continue;
                TransferLimitation transferTime = new TransferLimitation(row);
                if(transferInInfoMap.containsKey(transferTime.getInFlightId())){
                    transferInInfoMap.get(transferTime.getInFlightId()).put(transferTime.getOutFlightId(), transferTime);
                }
                else {
                    Map<String, TransferLimitation> transferLimitationMap = new HashMap<>();
                    transferLimitationMap.put(transferTime.getOutFlightId(), transferTime);
                    transferInInfoMap.put(transferTime.getInFlightId(), transferLimitationMap);
                }
                if(transferOutInfoMap.containsKey(transferTime.getOutFlightId())){
                    transferOutInfoMap.get(transferTime.getOutFlightId()).put(transferTime.getInFlightId(), transferTime);
                }
                else {
                    Map<String, TransferLimitation> transferLimitationMap = new HashMap<>();
                    transferLimitationMap.put(transferTime.getInFlightId(), transferTime);
                    transferOutInfoMap.put(transferTime.getOutFlightId(), transferLimitationMap);
                }
            }
            //关闭excel
            workBook.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
