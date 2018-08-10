package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by peicheng on 17/8/10.
 */
public class CapacityLimitation {
    //台风前容量控制的起始时间
    private long startBeforeTime;
    //台风前容量控制的结束时间
    private long endBeforeTime;
    //台风后容量控制的起始时间
    private long startAfterTime;
    //台风后容量控制的结束时间
    private long endAfterTime;

    //按每5分钟，划分时间片段
    private Map<Integer, Integer> sliceMap = new HashMap<>();

    public CapacityLimitation(long startBeforeTime, long endBeforeTime, long startAfterTime, long endAfterTime){
        this.startBeforeTime = startBeforeTime;
        this.endBeforeTime = endBeforeTime;
        this.startAfterTime = startAfterTime;
        this.endAfterTime = endAfterTime;
        if(!(startBeforeTime < endBeforeTime && endBeforeTime < startAfterTime && startAfterTime < endAfterTime)){
            throw new RuntimeException("单位时间容量限制时间窗数据有误！");
        }
        generateSliceMap();
    }

    //按每5分钟，划分时间片段
    private void generateSliceMap(){
        sliceMap.clear();
        int loc = 0;
        while(startBeforeTime + loc * 5 * 60 * 1000 <= endBeforeTime){
            sliceMap.put(loc, 0);
            loc += 1;
        }
        loc = (int)((startAfterTime - startBeforeTime) / 1000 / 60 / 5);
        while(startBeforeTime + loc * 5 * 60 * 1000 <= endAfterTime){
            sliceMap.put(loc, 0);
            loc += 1;
        }
    }

    //获取当前时间对应的location
    private int getLocIndex(long time){
        if((time >= startBeforeTime && time <= endBeforeTime) || (time >= startAfterTime && time <= endAfterTime)){
            return  (int)((time - startBeforeTime) / 1000 / 60 / 5);
        }
        else {
            return -1;
        }

    }

    //判断传进来的时间序列是否满足每5分钟两个起降的限制
    public boolean isSatisfyCapacityLimitation(List<Long> timeList){
        //清空之前的记录信息
        Iterator<Integer> iter = sliceMap.keySet().iterator();
        while(iter.hasNext()){
            Integer key = iter.next();
            sliceMap.put(key, 0);
        }
        //统计出现的次数
        for(int index = 0; index < timeList.size(); ++index){
            long currentTime = timeList.get(index);
            int loc = getLocIndex(currentTime);
            if(loc != -1){
                sliceMap.put(loc, sliceMap.get(loc) + 1);
            }
        }
        //判断每5分钟内是否只有2个航班起飞和降落
        iter = sliceMap.keySet().iterator();
        while(iter.hasNext()){
            Integer key = iter.next();
            if(sliceMap.get(key) > 2)
                return false;
        }
        return true;
    }
}
