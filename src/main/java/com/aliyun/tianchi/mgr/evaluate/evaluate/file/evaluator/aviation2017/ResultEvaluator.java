package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

import org.apache.commons.lang3.tuple.Triple;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Created by peicheng on 17/6/29.
 */
public class ResultEvaluator implements Cloneable{

    //定义输入数据集
    private InputData inputData;
    
    //定义结果数据结构
    private Map<String, List<ResultFlight>> resultAirLineMap;
    private Map<String, ResultFlight> resultFlightMap;
    
    private int[] constraintTest= new int[46];

    public InputData getInputData() {
        return inputData;
    }

    public Map<String, List<ResultFlight>> getResultAirLineMap() {
        return resultAirLineMap;
    }

    public Map<String, ResultFlight> getResultFlightMap() {
        return resultFlightMap;
    }

    /**
     *   目标函数值 = p1*调机空飞航班数 + p2*取消航班数 + p3*机型发生变化的航班数 + p4*换飞机数量 + p5*联程拉直航班的个数 + p6*航班总延误时间（小时） + p7*航班总提前时间（小时）
     *   + p8*取消旅客人数 +p9*延迟旅客人数 +p10*签转延误旅客人数
     *   + p11*非可行标识（0-1变量） + p12*违背约束数量 。
     */
    private double emptyFlightScore = 0.0;              //调机航班惩罚值
    private double cancelFlightScore = 0.0;             //取消航班惩罚值
    private double flightTypeChangeScore= 0.0;          //机型发生变化的航班惩罚值
    private double swapFlightScore = 0.0;               //换飞机的航班惩罚值
    private double connectFlightStraightenScore = 0.0;  //联程拉直航班对惩罚值
    private double totalFlightDelayScore = 0.0;         //航班总延误惩罚值
    private double totalFlightAheadScore = 0.0;         //航班总提前惩罚值
    private double passengerCancelScore = 0.0;          //旅客取消的惩罚值
    private double passengerDelayScore = 0.0;           //正常旅客延误的惩罚值
    private double signChangePassengerDelayScore = 0.0; //签转旅客延误的惩罚值
    private boolean isFeasible = true;                  //可行标识
    private int constraintViolationNum = 0;             //违背约束的次数


    public ResultEvaluator(InputStream inputStream){
        //读取输入数据
        inputData = new InputData(inputStream);
    }

    /**
     * 读取选手上传的结果文件
     * @param inputStream
     */
    private void readResultData(InputStream inputStream){
        try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while((line = reader.readLine()) != null){
                ResultFlight resultFlight = new ResultFlight(line);
                String airplaneId = resultFlight.getAirplaneId();
                if(resultAirLineMap.containsKey(airplaneId)){
                    resultAirLineMap.get(airplaneId).add(resultFlight);
                }
                else {
                    List<ResultFlight> resultFlightList = new ArrayList<>();
                    resultFlightList.add(resultFlight);
                    resultAirLineMap.put(airplaneId, resultFlightList);
                }
                //不能存在重复航班
                if(resultFlightMap.containsKey(resultFlight.getFlightId())){
                	++constraintTest[0];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                resultFlightMap.put(resultFlight.getFlightId(), resultFlight);
            }
            reader.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 重置结果数据，用于新的评分
     */
    private void resetStatisticsData(){
        resultAirLineMap = new HashMap<>();
        resultFlightMap = new HashMap<>();
        emptyFlightScore = 0.0;
        cancelFlightScore = 0.0;
        flightTypeChangeScore = 0.0;
        swapFlightScore = 0.0;
        connectFlightStraightenScore = 0.0;
        totalFlightDelayScore = 0.0;
        totalFlightAheadScore = 0.0;
        passengerCancelScore = 0.0;
        passengerDelayScore = 0.0;
        signChangePassengerDelayScore = 0.0;
        isFeasible = true;
        constraintViolationNum = 0;
    }

    /**
     * 当所有数据统计完后，按照指定的公式计算公式
     * @return
     */
    private double calculateScore(){
       return emptyFlightScore +
               cancelFlightScore +
               flightTypeChangeScore +
               swapFlightScore +
               connectFlightStraightenScore +
               totalFlightDelayScore +
               totalFlightAheadScore +
               passengerCancelScore +
               passengerDelayScore +
               signChangePassengerDelayScore +
               (isFeasible ? 0 : 1) * Configuration.infeasibilityPenaltyParam +
               constraintViolationNum * Configuration.constraintViolationPenaltyParam;
    }

    /**
     * 全局的判断结果的合法性，结果数据中是否包含全部的飞机ID, 航班ID
     * @return
     */
    private void globalJudgeLegalityOfResult(){
        Set<String> originAirplaneIdSet = inputData.getAirLineMap().keySet();
        if(!resultAirLineMap.keySet().containsAll(originAirplaneIdSet)) {
            //少一个飞机，算违背一次约束
            Iterator<String> airplaneIdIter = resultAirLineMap.keySet().iterator();
            while(airplaneIdIter.hasNext()){
                String airplaneId = airplaneIdIter.next();
                if(!originAirplaneIdSet.contains(airplaneId)){
                	++constraintTest[1];
                    constraintViolationNum += 1;
                }
            }
            isFeasible = false;
        }
        Set<String> originFlightIdSet = inputData.getFlightMap().keySet();
        if(!resultFlightMap.keySet().containsAll(originFlightIdSet)) {
            //少一个航班，算违背一次约束
            Iterator<String> flightIdIter = resultFlightMap.keySet().iterator();
            while(flightIdIter.hasNext()){
                String flightId = flightIdIter.next();
                ResultFlight resultFlight = resultFlightMap.get(flightId);
                if(resultFlight.isEmptyFly())  //过滤空飞的航班
                    continue;
                if(!originFlightIdSet.contains(flightId)){
                	++constraintTest[2];
                    constraintViolationNum += 1;
                }
            }
            isFeasible = false;
        }
    }

    /**
     * 针对已经删除了取消航班，并且按照时间排好序的航线进行合法性检测
     * @param airLine 调整后的航线
     * @return
     */
    private void judgeLegalityOfAirLine(List<ResultFlight> airLine){
        for(int index = 0; index < airLine.size(); ++ index){
            ResultFlight newFlight = airLine.get(index);
            String flightId = newFlight.getFlightId();
            String startAirport = newFlight.getStartAirport();
            String endAirport = newFlight.getEndAirport();
            String airplaneId = newFlight.getAirplaneId();
            String airplaneType = inputData.getAirLineMap().get(newFlight.getAirplaneId()).get(0).getAirplaneType();
            Flight originFlight = inputData.getFlightMap().get(newFlight.getFlightId());
            //判断第一个航班的起飞机场必须与飞机的初始起飞机场一致
            if(index == 0){
                if(!inputData.getAirplaneStartAirportMap().get(airplaneId).equals(startAirport)){
                	++constraintTest[3];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
            }

            //判断飞行时间、起始降落机场是否与原始航班一致
            if(newFlight.isStraighten()){   //判断联程拉直航班
                //首先判断联程航班拉直的方式是否有效
                if(!originFlight.isConnected()) {
                	++constraintTest[4];
                    constraintViolationNum += 1;
                    isFeasible = false;
                    continue;//联程航班拉直后，发现该航班是非联程航班，直接下一轮判断
                }
                String nextFlightId = originFlight.getConnectedFlightId();
                Flight nextFlight = inputData.getFlightMap().get(nextFlightId);
                //联程航班拉直后，第一个航班的降落机场等于联程下一个航班的降落机场
                if(!startAirport.equals(originFlight.getStartAirport())
                        || !endAirport.equals(nextFlight.getEndAirport())
                        || endAirport.equals(originFlight.getEndAirport())) {
                	++constraintTest[5];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                //判断调整后的飞行时间
                long travelTime = originFlight.getEndDateTime().getTime() - originFlight.getStartDateTime().getTime()
                        + nextFlight.getEndDateTime().getTime() - nextFlight.getStartDateTime().getTime();
                String travelTimeKey = airplaneType + "#" + startAirport + "#" + endAirport;
                if(inputData.getTravelTimeMap().containsKey(travelTimeKey)) {
                   travelTime = inputData.getTravelTimeMap().get(travelTimeKey).getTravelTime();
                }
                if(newFlight.getEndDateTime().getTime() - newFlight.getStartDateTime().getTime()
                        !=  travelTime) {
                	++constraintTest[6];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                if(!originFlight.isDomestic() || !nextFlight.isDomestic()
                        || !resultFlightMap.get(nextFlightId).isCancel()){  //联程拉直航班必须为国内航班，且后一个航班必须取消
                	++constraintTest[7];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                //当且仅当中间机场受影响时可拉直航班
                //（只判断起飞限制和降落限制，因为停机限制其实就是起飞限制和降落限制的交集，所以不用判断停机限制）
                boolean affectFlag = false;
                for(int i = 0; i < inputData.getSceneList().size(); ++i){
                    Scene scene = inputData.getSceneList().get(i);
                    if(scene.isEndInScene(originFlight.getFlightId(),
                            originFlight.getAirplaneId(),
                            originFlight.getEndAirport(),
                            originFlight.getEndDateTime())){
                        affectFlag = true;
                        break;
                    }
                    if(scene.isStartInScene(nextFlight.getFlightId(),
                            nextFlight.getAirplaneId(),
                            nextFlight.getStartAirport(),
                            nextFlight.getStartDateTime())){
                        affectFlag = true;
                        break;
                    }
                }
                if(!affectFlag){
                	++constraintTest[8];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
            }
            else if(!newFlight.isEmptyFly()){  //判断普通航班
               if(newFlight.getEndDateTime().getTime() - newFlight.getStartDateTime().getTime()
                       != originFlight.getEndDateTime().getTime() - originFlight.getStartDateTime().getTime()) {
            	   ++constraintTest[9];
                   constraintViolationNum += 1;
                   isFeasible = false;
               }
               if(!startAirport.equals(originFlight.getStartAirport())
                       || !endAirport.equals(originFlight.getEndAirport())) {
            	   ++constraintTest[10];
                   constraintViolationNum += 1;
                   isFeasible = false;
               }
            }
            else {  //判断调机（空飞）航班
                String travelTimeKey = airplaneType + "#" + startAirport + "#" + endAirport;
                if(!inputData.getTravelTimeMap().containsKey(travelTimeKey)) {
                	++constraintTest[11];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                else{
                    if(newFlight.getEndDateTime().getTime() - newFlight.getStartDateTime().getTime()
                            != inputData.getTravelTimeMap().get(travelTimeKey).getTravelTime()) {
                    	++constraintTest[12];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                }
                //只允许国内机场才能调机
                if(!(inputData.getDomesticAirportSet().contains(startAirport) && inputData.getDomesticAirportSet().contains(endAirport))){
                	++constraintTest[13];
                	constraintViolationNum += 1;
                    isFeasible = false;
                }
            }

            //判断飞机限制
            String airportPair = startAirport + "#" + endAirport;
            if(inputData.getAirplaneLimitationMap().containsKey(airportPair)){
                List<AirplaneLimitation> airplaneLimitationList = inputData.getAirplaneLimitationMap().get(airportPair);
                for(int i = 0; i < airplaneLimitationList.size(); ++ i){
                    if(airplaneLimitationList.get(i).getAirplaneId().equals(airplaneId)) {
                    	++constraintTest[14];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                }
            }
            //判断机场关闭限制
            if(inputData.getAirportCloseMap().containsKey(startAirport)){
                List<AirportClose> airportCloseList = inputData.getAirportCloseMap().get(startAirport);
                for(int i = 0; i < airportCloseList.size(); ++ i){
                    if(airportCloseList.get(i).isClosed(newFlight.getStartDateTime().getTime())) {
                    	++constraintTest[15];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                }
            }
            if(inputData.getAirportCloseMap().containsKey(endAirport)){
                List<AirportClose> airportCloseList = inputData.getAirportCloseMap().get(endAirport);
                for(int i = 0; i < airportCloseList.size(); ++ i){
                    if(airportCloseList.get(i).isClosed(newFlight.getEndDateTime().getTime())) {
                    	++constraintTest[16];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                }
            }
            //判断台风场景限制(起飞和降落限制)
            for(int i = 0; i < inputData.getSceneList().size(); ++i){
                Scene scene = inputData.getSceneList().get(i);
                if(scene.isInScene(flightId, airplaneId, startAirport, endAirport, newFlight.getStartDateTime(), newFlight.getEndDateTime())) {
                	++constraintTest[17];
                	constraintViolationNum += 1;
                    isFeasible = false;
                }
            }

            //如果联程航班两段都不取消，那么两段必须使用同一架飞机，并且继续联程
            if(!newFlight.isEmptyFly() && originFlight.isConnected()){
                String nextFlightId = originFlight.getConnectedFlightId();
                if(!resultFlightMap.get(nextFlightId).isCancel()){ //联程航班没有取消
                    if(!newFlight.getAirplaneId().equals(resultFlightMap.get(nextFlightId).getAirplaneId())) { //但是使用了不同的飞机
                    	++constraintTest[18];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                    else{
                        if(originFlight.isConnectedPrePart()){//当前航班为联程航班前段部分
                            if(index + 1 >= airLine.size() || !airLine.get(index + 1).getFlightId().equals(nextFlightId)){
                            	++constraintTest[19];
                                constraintViolationNum += 1;
                                isFeasible = false;
                            }
                        }
                        else {//当前航班为联程航班后段部分
                            if(index - 1 < 0 || !airLine.get(index - 1).getFlightId().equals(nextFlightId)){
                            	++constraintTest[20];
                                constraintViolationNum += 1;
                                isFeasible = false;
                            }
                        }
                    }
                }
            }

            //判断恢复窗口限制
            if(!newFlight.isEmptyFly() && !inputData.getAdjustTimeWindow().isInAdjustTimeWindow(originFlight.getStartDateTime().getTime())){
                if(!newFlight.getStartAirport().equals(originFlight.getStartAirport())
                        || !newFlight.getEndAirport().equals(originFlight.getEndAirport())
                        || !newFlight.getStartDateTime().equals(originFlight.getStartDateTime())
                        || !newFlight.getEndDateTime().equals(originFlight.getEndDateTime())
                        || !newFlight.getAirplaneId().equals(originFlight.getAirplaneId())
                        || newFlight.isCancel()
                        || newFlight.isEmptyFly()
                        || newFlight.isStraighten()
                        || newFlight.isSignChange()){
                	++constraintTest[21];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                if(originFlight.isConnected()){//如果是联程航班，联程航班不能取消
                    String nextFlightId = originFlight.getConnectedFlightId();
                    if(resultFlightMap.get(nextFlightId).isCancel()) {
                    	++constraintTest[22];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                }
            }

           //进行前后两个航班的约束判断
            if(index > 0){
               ResultFlight preNewResultFlight = airLine.get(index - 1);
                //判断航站衔接约束
               if(!preNewResultFlight.getEndAirport().equals(newFlight.getStartAirport())){
            	   ++constraintTest[23];
                   constraintViolationNum += 1;
                   isFeasible = false;
                   continue;
               }
                //判断间隔时间
               String flightIntervalTimeKey = preNewResultFlight.getFlightId() + "#" + newFlight.getFlightId();
               long intervalTime = Configuration.maxIntervalTime;
               if(inputData.getFlightIntervalTimeMap().containsKey(flightIntervalTimeKey)){
                  if(intervalTime > inputData.getFlightIntervalTimeMap().get(flightIntervalTimeKey))
                      intervalTime = inputData.getFlightIntervalTimeMap().get(flightIntervalTimeKey);
               }
               if(newFlight.getStartDateTime().getTime() < preNewResultFlight.getEndDateTime().getTime()
                       || newFlight.getStartDateTime().getTime() - preNewResultFlight.getEndDateTime().getTime() < intervalTime){
            	   ++constraintTest[24];
            	   constraintViolationNum += 1;
                   isFeasible = false;
               }
            }
        }
    }

    /**
     * 统计空飞、机型变换、换飞机、航班提前、航班延误的惩罚值
     * @param airLine
     */
    private void statisticFlightIndex(List<ResultFlight> airLine){
        for(int index = 0; index < airLine.size(); ++ index){
            ResultFlight newFlight = airLine.get(index);
            Flight originFlight = inputData.getFlightMap().get(newFlight.getFlightId());
            //统计空飞的惩罚值
            if(newFlight.isEmptyFly()){
                emptyFlightScore += Configuration.getAdjustFlightParam();
                continue;
            }
            //统计机型发生变化的惩罚值
            String newAirplaneType = inputData.getAirLineMap().get(newFlight.getAirplaneId()).get(0).getAirplaneType();
            if(!newAirplaneType.equals(originFlight.getAirplaneType())){
                flightTypeChangeScore += originFlight.getImportRatio() * Configuration.getFlightTypeChangeParam(originFlight.getAirplaneType(), newAirplaneType);
            }
            //统计换飞机的惩罚值
            if(!newFlight.getAirplaneId().equals(originFlight.getAirplaneId())){
                swapFlightScore += originFlight.getImportRatio() * Configuration.getSwapFlightParam(originFlight.getStartDateTime().getTime());
            }
            //统计联程航班拉直的惩罚值（重要系数选择两者之和）
            if(newFlight.isStraighten()){
                connectFlightStraightenScore += originFlight.getImportRatio() * Configuration.getConnectFlightStraightenParam();
                //同时还要加上后置航班的重要系数
                //首先判断联程航班拉直的方式是否有效
                if(!originFlight.isConnected()) {
                	++constraintTest[25];
                    constraintViolationNum += 1;
                    isFeasible = false;
                    continue;//联程航班拉直后，发现该航班是非联程航班，直接下一轮判断
                }
                String nextFlightId = originFlight.getConnectedFlightId();
                Flight nextFlight = inputData.getFlightMap().get(nextFlightId);
                connectFlightStraightenScore += nextFlight.getImportRatio() * Configuration.getConnectFlightStraightenParam();
            }
            //统计航班总延误惩罚值或者总提前惩罚值
            long timeOffset = newFlight.getStartDateTime().getTime() - originFlight.getStartDateTime().getTime();
            if(timeOffset > 0){
                boolean isDomestic = originFlight.isDomestic();
                if(isDomestic && timeOffset > Configuration.maxDomesticDelayTime){   //延迟时间不能超过赛题限制
                	++constraintTest[26];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                else if((!isDomestic) && timeOffset > Configuration.maxAbroadDelayTime){
                	++constraintTest[27];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                double delayHour = 1.0 * timeOffset / 1000 / 60 / 60;
                totalFlightDelayScore += originFlight.getImportRatio() * delayHour * Configuration.getDelayFlightParam();
            }
            else if(timeOffset < 0){
                if(-1 * timeOffset > Configuration.maxAheadTime){   //提前时间不能超过赛题限制
                	++constraintTest[28];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                if(!originFlight.isDomestic()){  //必须为国内航班才能提前
                	++constraintTest[29];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                //仅针对在受影响的起飞时间段，受影响的机场起飞的航班
                boolean affectFlag = false;
                for(int i = 0; i < inputData.getSceneList().size(); ++i){
                    Scene scene = inputData.getSceneList().get(i);
                    if(scene.isStartInScene(originFlight.getFlightId(),
                            originFlight.getAirplaneId(),
                            originFlight.getStartAirport(),
                            originFlight.getStartDateTime())){
                        affectFlag = true;
                        break;
                    }
                }
                if(!affectFlag){
                	++constraintTest[30];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
                double aheadHour = -1.0 * timeOffset / 1000 / 60 / 60;
                totalFlightAheadScore += originFlight.getImportRatio() * aheadHour * Configuration.getAheadFlightParam();
            }
        }
    }

    /**
     * 中转旅客的人员调整
     * @param resultFlight
     * @param passTransInfo
     * @return
     */
    private void adjustTransferFlight(ResultFlight resultFlight, Map<String, List<Triple<Integer, Integer, Long>>> passTransInfo){
        String flightId = resultFlight.getFlightId();
        if(inputData.getTransferInInfoMap().containsKey(flightId)){
            Map<String, TransferLimitation> transferInInfoMap = inputData.getTransferInInfoMap().get(flightId);
            Iterator<String> iter = transferInInfoMap.keySet().iterator();
            while(iter.hasNext()){
                String outFlightId = iter.next();
                TransferLimitation transferLimitation = transferInInfoMap.get(outFlightId);
                ResultFlight transferRF = resultFlightMap.get(outFlightId);
                if(transferRF.getStartDateTime().getTime() - resultFlight.getEndDateTime().getTime()
                        < transferLimitation.getMinTransferTime()) {
                    //中转失败，则从下一个航班删除正常旅客数量（数据中保证了转旅客数量不大于正常旅客数量）
                    //记录签转旅客情况，用于后面统计
                    passTransInfo.get(outFlightId).add(Triple.of(2, transferLimitation.getTransferPassNum(), 0L));
                }
            }
        }
    }

    /**
     * 中转旅客的人员取消
     * @param resultFlight
     * @param passTransInfo
     * @return
     */
    private void cancelTransferFlight(ResultFlight resultFlight, Map<String, List<Triple<Integer, Integer, Long>>> passTransInfo){
        String flightId = resultFlight.getFlightId();
        if(inputData.getTransferInInfoMap().containsKey(flightId)){
            Map<String, TransferLimitation> transferInInfoMap = inputData.getTransferInInfoMap().get(flightId);
            Iterator<String> iter = transferInInfoMap.keySet().iterator();
            while(iter.hasNext()){
                String outFlightId = iter.next();
                TransferLimitation transferLimitation = transferInInfoMap.get(outFlightId);
                //记录签转旅客情况，用于后面统计
                passTransInfo.get(outFlightId).add(Triple.of(1, transferLimitation.getTransferPassNum(), 0L));
            }
        }
    }


    /**
     * 获得指定航班指定类型操作的旅客取消数量
     * @param flightId
     * @param passTransInfo
     * @param type
     * @return
     */
    private int getPassengerNum(String flightId, Map<String, List<Triple<Integer, Integer, Long>>> passTransInfo, int type){
         int num = 0;
         if(passTransInfo.containsKey(flightId)){
             List<Triple<Integer, Integer, Long>> operationList = passTransInfo.get(flightId);
             for(int index = 0; index < operationList.size(); ++index){
                 if(operationList.get(index).getLeft().equals(type)){
                     num += operationList.get(index).getMiddle();
                 }
             }
         }
         return num;
    }

    /**
     * 获得签转旅客的延迟开销
     * @param flightId
     * @param passTransInfo
     * @return
     */
    private double getSignChangePassengerDelayCost(String flightId, Map<String, List<Triple<Integer, Integer, Long>>> passTransInfo){
        double delayCost = 0.0;
        if(passTransInfo.containsKey(flightId)){
            List<Triple<Integer, Integer, Long>> operationList = passTransInfo.get(flightId);
            for(int index = 0; index < operationList.size(); ++index){
                if(operationList.get(index).getLeft().equals(3)){
                    double delayHour = 1.0 * operationList.get(index).getRight() / 1000 / 60 / 60;
                    if(delayHour < 0){//以防违背约束后，时间差为负数，导致结果分数异常
                        delayHour = 0.0;
                    }
                    delayCost += operationList.get(index).getMiddle() * Configuration.getSignChangePassengerDelayParam(delayHour);
                }
            }
        }
        return delayCost;
    }

    /**
     * 对签转的旅客进行合法性检查，并且记录签转情况
     *
     * @param resultFlight
     * @param passTransInfo
     * @return
     */
    private void judgeLegalityOfSignChange(ResultFlight resultFlight, Map<String, List<Triple<Integer, Integer, Long>>> passTransInfo){
        String flightId = resultFlight.getFlightId();
        Map<String, Flight> originFlightMap = inputData.getFlightMap();
        Flight originFlight = originFlightMap.get(flightId);
        //判断签转的合理性
        Map<String, Integer> signChangePassInfoOfFlight = resultFlight.getSignChangePassInfo();
        int totalSignChangePassNum = 0;
        Iterator<String> iter = signChangePassInfoOfFlight.keySet().iterator();
        while (iter.hasNext()) {
            String signChangeFlightId = iter.next();
            ResultFlight signChangeResultFlight = resultFlightMap.get(signChangeFlightId);
            if (signChangeResultFlight.isCancel()) { //接受签转旅客的航班不能取消
            	++constraintTest[31];
                constraintViolationNum += 1;
                isFeasible = false;
            }
            if(signChangeResultFlight.getStartDateTime().getTime()
                    < originFlight.getStartDateTime().getTime()){ //签转旅客只能延误
            	++constraintTest[32];
                constraintViolationNum += 1;
                isFeasible = false;
            }
            if(!signChangeResultFlight.getStartAirport().equals(originFlight.getStartAirport())
                    || !signChangeResultFlight.getEndAirport().equals(originFlight.getEndAirport())){ //签转航班要与原航班的起飞降落机场一致
            	++constraintTest[33];
            	constraintViolationNum += 1;
                isFeasible = false;
            }
            if(signChangeResultFlight.getSignChangePassInfo().size() > 0){//接受签转旅客的航班不能签转旅客到其他航班
            	++constraintTest[34];
            	constraintViolationNum += 1;
                isFeasible = false;
            }
            totalSignChangePassNum += signChangePassInfoOfFlight.get(signChangeFlightId);
            //记录签转旅客情况，用于后面统计
            Triple signTranInfo = Triple.of(3,
                    signChangePassInfoOfFlight.get(signChangeFlightId),
                    signChangeResultFlight.getStartDateTime().getTime() - originFlight.getStartDateTime().getTime());
            passTransInfo.get(signChangeFlightId).add(signTranInfo);
        }
        //记录签转旅客情况，用于后面统计
        passTransInfo.get(flightId).add(Triple.of(0, totalSignChangePassNum, 0L));

        //判断签转原因的合理性以及签转数量的合理性
        if(resultFlight.isCancel()) { //航班取消签转, 包含普通航班取消和联程拉直航班取消（只能签转普通旅客）
            //获得中转到当前航班的取消旅客数量
            int transferCancelPassNum =  getPassengerNum(flightId, passTransInfo, 1);
            if(totalSignChangePassNum > originFlight.getNormalPassengerNum() - transferCancelPassNum){//签转旅客数量不能大于剩余的普通旅客数量
            	++constraintTest[35];
                constraintViolationNum += 1;
                isFeasible = false;
            }
        }
        else{//执行航班签转（中转失败、换飞机、机型变化、航班拉直、超售）
            boolean signChangeLegalityFlag = false;
            int availableSignChangePassNum = 0;
            int totalPassengerNum = originFlight.getPassengerNum() + originFlight.getConnectPassengerNum();
            //获得中转到当前航班的取消旅客数量
            int transferCancelPassNum =  getPassengerNum(flightId, passTransInfo, 1);
            totalPassengerNum -= transferCancelPassNum;
            //获得中转失败的旅客数量
            int transferFailPassNum =  getPassengerNum(flightId, passTransInfo, 2);
            availableSignChangePassNum += transferFailPassNum;
            if(transferFailPassNum > 0){ //中转失败
                signChangeLegalityFlag = true;
                totalPassengerNum -= transferFailPassNum;
            }
            if(resultFlight.isStraighten()){ //如果航班拉直
                totalPassengerNum = originFlight.getConnectPassengerNum();
                signChangeLegalityFlag = true;
                availableSignChangePassNum = originFlight.getNormalPassengerNum() - transferCancelPassNum;
            }
            else if(originFlight.isConnected()){//如果是没有拉直的联程航班
                String nextFlightId = originFlight.getConnectedFlightId();
                if(resultFlightMap.get(nextFlightId).isCancel()) { //联程航班取消
                    totalPassengerNum -= originFlight.getConnectPassengerNum();
                }
            }
            int seatNum = inputData.getAirLineMap().get(resultFlight.getAirplaneId()).get(0).getSeatNum();
            if(totalPassengerNum > seatNum){//换飞机、机型变化、超售（其实就是乘客数量大于座位数量）
                signChangeLegalityFlag = true;
                availableSignChangePassNum += (totalPassengerNum - seatNum);
            }
            if(!signChangeLegalityFlag){
            	++constraintTest[36];
                constraintViolationNum += 1;
                isFeasible = false;
            }
            if(totalSignChangePassNum > availableSignChangePassNum){//签转旅客数量不能大于可以签转的旅客数量
            	++constraintTest[37];
                constraintViolationNum += 1;
                isFeasible = false;
            }
        }
    }

    /**
     * 统计旅客的取消、普通延误、签转延误
     */
    private void statisticPassengerIndex(){
        Map<String, Flight> originFlightMap = inputData.getFlightMap();
        //记录签转旅客情况
        // (航班ID，Array(标识(0:旅客签转出去, 1:中转进来的旅客取消, 2:中转失败的旅客, 3:旅客签转进来 )，旅客数量，延误时长))
        Map<String, List<Triple<Integer, Integer, Long>>> passTransInfo = new HashMap<>();
        //获取结果航班List，并且按照时间进行排序
        List<ResultFlight> resultFlightList = new ArrayList<>();
        resultFlightList.addAll(resultFlightMap.values());
        Collections.sort(resultFlightList);

        //初始化旅客流转情况数据
        Iterator<ResultFlight> iter = resultFlightList.iterator();
        while(iter.hasNext()){
            ResultFlight resultFlight = iter.next();
            if(resultFlight.isEmptyFly())
                continue;
            passTransInfo.put(resultFlight.getFlightId(), new ArrayList<>());
        }
        //第一步中转旅客的调整
        for(int index = 0; index < resultFlightList.size(); ++index) {
            ResultFlight resultFlight = resultFlightList.get(index);
            //调机航班上没有旅客
            if (resultFlight.isEmptyFly()) {
                continue;
            }
            if(resultFlight.isCancel() || resultFlight.isStraighten()) { //航班取消 + 拉直航班
                //中转旅客的人员取消
                cancelTransferFlight(resultFlight, passTransInfo);
            }
            else {//执行航班
                //中转旅客的人员调整
                adjustTransferFlight(resultFlight, passTransInfo);
            }
        }
        //第二步判断签转的原因是否合理
        for(int index = 0; index < resultFlightList.size(); ++index){
            ResultFlight resultFlight = resultFlightList.get(index);
            String flightId = resultFlight.getFlightId();
            //调机航班不允许签转
            if(resultFlight.isEmptyFly()){
                continue;
            }
            Flight originFlight = originFlightMap.get(flightId);
            //判断是否在调整窗口范围内
            if(!inputData.getAdjustTimeWindow().isInAdjustTimeWindow(originFlight.getStartDateTime().getTime())){//处于非调整窗口
                if(resultFlight.isSignChange() || resultFlight.isCancel() || resultFlight.isStraighten()){  //处于非调整窗口内的航班不能进行转签、不能取消、不能拉直
                	++constraintTest[38];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
            }
            else{//处于调整窗口
                if(resultFlight.isSignChange()){//对存在旅客签转的航班进行合法性检查
                    judgeLegalityOfSignChange(resultFlight, passTransInfo);
                }
            }
        }
        //第三步统计每个航班的旅客组成，以及cost计算
        for(int index = 0; index < resultFlightList.size(); ++index){
            ResultFlight resultFlight = resultFlightList.get(index);
            String flightId = resultFlight.getFlightId();
            //调机航班不允许签转
            if(resultFlight.isEmptyFly()){
                continue;
            }
            Flight originFlight = originFlightMap.get(flightId);
            //判断是否在调整窗口范围内
            if(!inputData.getAdjustTimeWindow().isInAdjustTimeWindow(originFlight.getStartDateTime().getTime())){//处于非调整窗口
                //非调整窗口内航班不能调整，超售部分直接取消，且只取消普通旅客，对后面航班没有影响
                if(originFlight.getPassengerNum() + originFlight.getConnectPassengerNum() > originFlight.getSeatNum()){
                   passengerCancelScore += (originFlight.getPassengerNum() + originFlight.getConnectPassengerNum() - originFlight.getSeatNum())//以防违背约束后，人数差为负数，导致结果分数异常
                           * Configuration.getCancelPassengerParam();
                }
            }
            else{//处于调整窗口
                int totalSignOutChangePassNum =  getPassengerNum(flightId, passTransInfo, 0);
                int totalSignInChangePassNum =  getPassengerNum(flightId, passTransInfo, 3);
                if(resultFlight.isCancel()){
                    //获得取消旅客的cost
                    if(originFlight.isConnected() && originFlight.isConnectedPrePart()
                            && originFlight.getPassengerNum() + originFlight.getConnectPassengerNum() - totalSignOutChangePassNum > 0){//以防违背约束后，人数差为负数，导致结果分数异常
                        //如果取消的是联程航班的前部分，则没有拉直，需要统计联程旅客取消人数
                        passengerCancelScore += (originFlight.getPassengerNum() + originFlight.getConnectPassengerNum() - totalSignOutChangePassNum)
                                * Configuration.getCancelPassengerParam();
                    }
                    else{
                        if(originFlight.getPassengerNum() - totalSignOutChangePassNum > 0) {//以防违背约束后，人数差为负数，导致结果分数异常
                            passengerCancelScore += (originFlight.getPassengerNum() - totalSignOutChangePassNum)
                                    * Configuration.getCancelPassengerParam();
                        }
                    }
                }
                else if(resultFlight.isStraighten()){
                    passengerCancelScore += (originFlight.getPassengerNum() - totalSignOutChangePassNum)
                            * Configuration.getCancelPassengerParam();
                    int seatNum = inputData.getAirLineMap().get(resultFlight.getAirplaneId()).get(0).getSeatNum();
                    if(originFlight.getConnectPassengerNum() + totalSignInChangePassNum > seatNum){//签转旅客到某航班，必须满足该航班的座位数限制
                    	++constraintTest[39];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                    double delayHour = 1.0 * (resultFlight.getStartDateTime().getTime() - originFlight.getStartDateTime().getTime()) / 1000 / 60 / 60;
                    if(originFlight.getConnectPassengerNum() > 0 && delayHour > 0.0) {
                        passengerDelayScore += originFlight.getConnectPassengerNum() * Configuration.getNormalPassengerDelayParam(delayHour);
                    }
                    //获得签转旅客的延迟开销
                    signChangePassengerDelayScore += getSignChangePassengerDelayCost(flightId, passTransInfo);
                }
                else{
                    int totalPassengerNum = originFlight.getPassengerNum() + originFlight.getConnectPassengerNum();
                    int totalCancelPassenger = 0;
                    //获得中转到当前航班的取消旅客数量
                    int transferCancelPassNum =  getPassengerNum(flightId, passTransInfo, 1);
                    totalPassengerNum -= transferCancelPassNum;
                    totalCancelPassenger += transferCancelPassNum;
                    //获得中转失败的旅客数量
                    int transferFailPassNum =  getPassengerNum(flightId, passTransInfo, 2);
                    if(transferFailPassNum > 0){ //中转失败
                        totalPassengerNum -= transferFailPassNum;
                        totalCancelPassenger += transferFailPassNum;
                    }
                    if(originFlight.isConnected()){//如果是没有拉直的联程航班
                        String nextFlightId = originFlight.getConnectedFlightId();
                        if(resultFlightMap.get(nextFlightId).isCancel()) { //联程航班取消
                            totalPassengerNum -= originFlight.getConnectPassengerNum();
                            if(originFlight.isConnectedPrePart()){
                                totalCancelPassenger += originFlight.getConnectPassengerNum();
                            }
                        }
                    }
                    int seatNum = inputData.getAirLineMap().get(resultFlight.getAirplaneId()).get(0).getSeatNum();
                    if(totalPassengerNum > seatNum){//换飞机、机型变化、超售（其实就是乘客数量大于座位数量）
                        int overSeatNum = totalPassengerNum - seatNum;
                        totalPassengerNum -= overSeatNum;
                        totalCancelPassenger += overSeatNum;
                    }
                    if(totalSignOutChangePassNum <= totalCancelPassenger){//签转旅客数量不能大于可以取消的旅客数量
                        passengerCancelScore += (totalCancelPassenger - totalSignOutChangePassNum)
                                * Configuration.getCancelPassengerParam();
                    }
                    double delayHour = 1.0 * (resultFlight.getStartDateTime().getTime() - originFlight.getStartDateTime().getTime()) / 1000 / 60 / 60;
                    if(totalPassengerNum > 0 && delayHour > 0.0) {
                        passengerDelayScore += totalPassengerNum * Configuration.getNormalPassengerDelayParam(delayHour);
                    }
                    if(totalPassengerNum + totalSignInChangePassNum > seatNum){//签转旅客到某航班，必须满足该航班的座位数限制
                    	++constraintTest[40];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                }
                //获得签转旅客的延迟开销
                signChangePassengerDelayScore += getSignChangePassengerDelayCost(flightId, passTransInfo);
            }
        }
    }

    /**
     * 全局判断单位时间容量限制
     */
    private void globalJudgeCapacityLimitationOfResult(){
        Set<String> affectedAirportSet = inputData.getAffectedAirportSet();
        Map<String, List<Long>> affectedAirportTimeListMap = new HashMap<>();
        Iterator<String> iter = affectedAirportSet.iterator();
        while(iter.hasNext()){
            String airport = iter.next();
            affectedAirportTimeListMap.put(airport, new ArrayList<>());
        }
        Iterator<String> iterator = resultAirLineMap.keySet().iterator();
        while(iterator.hasNext()) {
            String airplaneId = iterator.next();
            List<ResultFlight> resultFlightList = resultAirLineMap.get(airplaneId);
            for(int index = 0; index < resultFlightList.size(); ++index){
                ResultFlight resultFlight = resultFlightList.get(index);
                if(affectedAirportSet.contains(resultFlight.getStartAirport())){
                    affectedAirportTimeListMap.get(resultFlight.getStartAirport()).add(resultFlight.getStartDateTime().getTime());
                }
                if(affectedAirportSet.contains(resultFlight.getEndAirport())){
                    affectedAirportTimeListMap.get(resultFlight.getEndAirport()).add(resultFlight.getEndDateTime().getTime());
                }
            }
        }
        //判断单位时间容量限制
        iter = affectedAirportSet.iterator();
        while(iter.hasNext()){
            String airport = iter.next();
            List<Long> timeList = affectedAirportTimeListMap.get(airport);
            if(!inputData.getCapacityLimitation().isSatisfyCapacityLimitation(timeList)){
            	++constraintTest[41];
                constraintViolationNum += 1;
                isFeasible = false;
            }
        }
    }

    /**
     * 全局判断基地平衡
     */
    private void globalJudgeBaseBalanceOfResult(){
        //统计飞机结束任务时，机场停的飞机次数
        Map<String, Integer> newEndAirportMap = new HashMap<>();
        Iterator<String> iterator = resultAirLineMap.keySet().iterator();
        while(iterator.hasNext()) {
            String airplaneId = iterator.next();
            List<ResultFlight> resultFlightList = resultAirLineMap.get(airplaneId);
            //记录每个飞机结束时的机场分布，用于判断基地平衡
            if(resultFlightList.size() > 0) {
                String endAirport = resultFlightList.get(resultFlightList.size() - 1).getEndAirport();
                String newAirplaneType = inputData.getAirLineMap().get(resultFlightList.get(0).getAirplaneId()).get(0).getAirplaneType();
                String key = endAirport + "#" + newAirplaneType;
                if (newEndAirportMap.containsKey(key)) {
                    newEndAirportMap.put(key, newEndAirportMap.get(key) + 1);
                } else {
                    newEndAirportMap.put(key, 1);
                }
            }
        }
        int totalLandNum = 0;
        Iterator<String> endAirportIter = inputData.getEndAirportMap().keySet().iterator();
        while(endAirportIter.hasNext()){
            String key = endAirportIter.next();
            int landNum = inputData.getEndAirportMap().get(key);
            totalLandNum += landNum;
            int newLandNum = 0;
            if(newEndAirportMap.containsKey(key)){
                newLandNum = newEndAirportMap.get(key);
            }
            if(landNum > newLandNum){
                totalLandNum -= newLandNum;
            }
            else{
                totalLandNum -= landNum;
            }
        }
        if(totalLandNum > 0){
        	constraintTest[42]+= totalLandNum;
            constraintViolationNum += totalLandNum;
            isFeasible = false;
        }
    }

    /**
     * 判断停机位数量限制
     */
    private void globalJudgeStopAirplaneNumLimitationOfResult(){
        Set<String> stopAirportSet = inputData.getStopAirportSet();
        Map<String, Integer> stopAirportNumMap = new HashMap<>();
        Iterator<String> iter = stopAirportSet.iterator();
        while(iter.hasNext()){
            String airport = iter.next();
            stopAirportNumMap.put(airport, 0);
        }
        Iterator<String> iterator = resultAirLineMap.keySet().iterator();
        while(iterator.hasNext()) {
            String airplaneId = iterator.next();
            List<ResultFlight> resultFlightList = resultAirLineMap.get(airplaneId);
            for(int index = 0; index < resultFlightList.size(); ++ index) {
                ResultFlight newFlight = resultFlightList.get(index);
                //进行前后两个航班的约束判断
                if(index > 0){
                    ResultFlight preNewResultFlight = resultFlightList.get(index - 1);
                    //判断台风场景限制(停机限制)
                    for (int i = 0; i < inputData.getSceneList().size(); ++i) {
                        Scene scene = inputData.getSceneList().get(i);
                        Date earliestStartDate = new Date(preNewResultFlight.getEndDateTime().getTime());
                        if (scene.isStopInScene(newFlight.getStartAirport(), earliestStartDate, newFlight.getStartDateTime())) {
                            stopAirportNumMap.put(newFlight.getStartAirport(), stopAirportNumMap.get(newFlight.getStartAirport()) + 1);
                        }
                    }
                }
            }
        }
        //判断停机位数量限制
        for (int i = 0; i < inputData.getSceneList().size(); ++i) {
            Scene scene = inputData.getSceneList().get(i);
            if(scene.getType().equals(AffectType.STOPPING)){
                int num = stopAirportNumMap.get(scene.getAirport());
                if(num > scene.getStopAirplaneNum()){
                	++constraintTest[43];
                    constraintViolationNum += 1;
                    isFeasible = false;
                }
            }
        }
    }

    /**
     * 运行评估器，计算选手得分
     * @param inputStream 结果文件路径
     * @return
     */
    public double runEvaluation(InputStream inputStream){
        //重置结果数据集
        resetStatisticsData();
        //读取结果数据
        readResultData(inputStream);
        
        long startTime=System.currentTimeMillis();
        
        //全局判断结果的合法性，结果数据中是否包含全部的飞机ID, 航班ID
        globalJudgeLegalityOfResult();

        //统计各项指标
        Iterator<String> iterator = resultAirLineMap.keySet().iterator();
        while(iterator.hasNext()){
            String airplaneId = iterator.next();
            List<ResultFlight> resultFlightList = resultAirLineMap.get(airplaneId);
            for(int index = resultFlightList.size() - 1; index >= 0; -- index){
                ResultFlight rf = resultFlightList.get(index);
                Flight originFlight = inputData.getFlightMap().get(rf.getFlightId());
                //统计取消航班，并且删除取消航班
                if(rf.isCancel()){
                    if(!rf.isStraighten())  //如果不是联程拉直导致取消的航班，才能计算取消权重
                        cancelFlightScore += originFlight.getImportRatio() * Configuration.getCancelFlightParam();
                    else {
                        ResultFlight connectedFlight = resultFlightMap.get(originFlight.getConnectedFlightId());
                        if(connectedFlight.isCancel() || !originFlight.isConnected()){//如果是联程拉直导致取消的航班,判断是其否是联程航班，并且判断联程航班是否取消
                        	++constraintTest[44];
                            constraintViolationNum += 1;
                            isFeasible = false;
                        }
                    }
                    if(!inputData.getAdjustTimeWindow().isInAdjustTimeWindow(originFlight.getStartDateTime().getTime())){//非调整窗口内的航班不能取消
                    	++constraintTest[45];
                        constraintViolationNum += 1;
                        isFeasible = false;
                    }
                    resultFlightList.remove(index);
                }
            }
            //对处理后的结果按时间从小到大进行排序
            Collections.sort(resultFlightList);

            //判断航线的合理性
            judgeLegalityOfAirLine(resultFlightList);

            //统计其他指标
            statisticFlightIndex(resultFlightList);
        }

        /**
         * 按照前面的步骤处理后，每一条航线中已经删除取消的航班，并且按时间排从小到大排序
         */
        //判断基地平衡
        globalJudgeBaseBalanceOfResult();
        //全局判断单位时间容量限制
        globalJudgeCapacityLimitationOfResult();
        //全局判断停机位数量限制
        globalJudgeStopAirplaneNumLimitationOfResult();

        //统计旅客的取消、延误、签转延误
        statisticPassengerIndex();
        
        System.out.println("emptyFlightScore:"+emptyFlightScore);
        System.out.println("cancelFlightScore:"+cancelFlightScore);
        System.out.println("flightTypeChangeScore:"+flightTypeChangeScore);
        System.out.println("swapFlightScore:"+swapFlightScore);
        System.out.println("connectFlightStraightenScore:"+connectFlightStraightenScore);
        System.out.println("totalFlightDelayScore:"+totalFlightDelayScore);
        System.out.println("totalFlightAheadScore:"+totalFlightAheadScore);
        System.out.println("passengerCancelScore:"+passengerCancelScore);
        System.out.println("passengerDelayScore:"+passengerDelayScore);
        System.out.println("signChangePassengerDelayScore:"+signChangePassengerDelayScore);
        System.out.println("constraintViolationNum:"+constraintViolationNum);
        
        for(int i=0;i<46;++i) {
	    		System.out.println(constraintTest[i]);
	    }
	    
        long endTime=System.currentTimeMillis(); //获取结束时间
        System.out.println("验证消耗时间："+(endTime-startTime)+"ms");

        return calculateScore();
    }

}
