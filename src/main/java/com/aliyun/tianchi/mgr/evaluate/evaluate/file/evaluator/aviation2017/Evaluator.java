package com.aliyun.tianchi.mgr.evaluate.evaluate.file.evaluator.aviation2017;


import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by peicheng on 17/6/28.
 */
public class Evaluator {

    public static void main(String[] args) {
//        if(args.length != 2){
//            System.err.println("传入参数有误，使用方式为：java -jar xxx.jar  xxx.xlsx   xxx.csv");
//            return;
//        }
        String inputDataFilePath = "data/厦航大赛数据20170814.xlsx";
        String resultDataFilePath = "data/mycsv/minion rush_766505.582_0.csv";
//        String inputDataFilePath = args[0];
//        String resultDataFilePath = args[1];
        try {
            //计算所得分数
            InputStream inputDataStream = new FileInputStream(inputDataFilePath);
            InputStream resultDataStream = new FileInputStream(resultDataFilePath);
            ResultEvaluator resultEvaluator = new ResultEvaluator(inputDataStream);
            double score = resultEvaluator.runEvaluation(resultDataStream);
            System.out.println("选手所得分数为：" + score);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
