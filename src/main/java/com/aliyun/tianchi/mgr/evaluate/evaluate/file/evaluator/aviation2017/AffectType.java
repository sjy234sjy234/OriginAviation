package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;

/**
 * Created by peicheng on 17/7/17.
 */
//定义场景影响类型，(0：降落，1：飞行，2：停机)
public enum AffectType {
    LANDING(0),
    FLYING(1),
    STOPPING(2);

    AffectType(int val){
        this.value = val;
    }
    private int value;
}
