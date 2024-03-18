package org.main.culturesolutioncalculation.service.calculator;

import org.main.culturesolutioncalculation.service.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class MacroCalculator implements Calculator{


    private DatabaseConnector conn;
    private Map<String, Map<String, Double>> compoundsRatio = new HashMap<>(); // ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}

    private Map<String, Double> calculated100 = new HashMap<>(); //프론트에서 보여지는 자동 계산 결과

    private Map<String, FinalCal> calculatedMacro=  new HashMap<>();

    //분자식, 한글명, 질량

    //넘어와야 할 처방 농도 양식 예시 - 순서 그대로 유지되어야 함
    private Map<String, Double> fertilization = new HashMap<String, Double>(){
        {
            put("NO3N", 15.5);
            put("NH4N", 1.25);
            put("H2P04",1.25);
            put("K",6.5);
            put("Ca",4.75);
            put("Mg",1.5);
            put("SO4S",1.75);
        }
    };
    //출력용 다량원수 별 100배액 계산-  calculatedMacro에 저장
    private void calculateWith100(){
        if(calculated100.size()==0) System.err.println("다량 원수 처방 농도가 계산되지 않았습니다.");
        String query = "insert into calculatedmacro (";



    }

    //분자 별 갖고 있는 다량 원소 비율을 가져옴

    private void getMajorCompoundRatio(boolean is4){ //4수염인지 10수염인지를 판단하는 파라미터

        String query = "select * from macronutrients";
        query += is4? " where id != 2" : " where id != 1"; //id=1 : 질산칼슘4수염, id=2 : 질산칼슘10수염

        try (Connection connection = conn.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query);) {

            while(resultSet.next()){
                String macro = resultSet.getString("macro"); //질산칼슘4수염, 질산칼륨, 질산암모늄 등등
                String macro_kr = resultSet.getString("macro_kr"); //화합물 한글명
                double mass = resultSet.getDouble("mass");//화합물 질량

                calculated100.put(macro, resultSet.getDouble("mass"));
                calculatedMacro.put(macro, new FinalCal(macro_kr, mass)); //100배액 계산을 위해 화합물과 그 질량 저장

                Map<String, Double> compoundRatio = new HashMap<>(); //ex. 질산칼슘4수염이 갖는 원수의 이름과 질량비를 갖는 map
                for (String major : fertilization.keySet()) {
                    if(resultSet.getDouble(major) != 0){
                        compoundRatio.put(major,resultSet.getDouble(major));
                    }
                }
                compoundsRatio.put(macro,compoundRatio);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    public Map<String, Double> calculateWithRatio(Map<String, Double> userFertilization){
        double minRatioValue = Double.MAX_VALUE;

        Map<String, Double> result = fertilization; //나중에 userFertilization이 들어오면 바꿀것 (result = userFertilization으로)

        for (String compound : compoundsRatio.keySet()) { //ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}에서 NH4NO3가 compound
            Map<String, Double> innerRatio = compoundsRatio.get(compound);
            double ratio = 0.0;

            //각 성분의 가장 낮은 비율에 해당하는 원수와 양 계산
            minRatioValue = getMinRatioValue(innerRatio, result, minRatioValue);

            //가장 낮은 비율 기반으로 배당되는 처방 농도 계산 후 갱신
            for (String macro : innerRatio.keySet()) {
                ratio = innerRatio.get(macro);
                double allocatedAmount = ratio * minRatioValue;
                result.put(macro, result.get(macro) - allocatedAmount);
            }
            calculated100.put(compound, minRatioValue * calculated100.get(compound)); //최소 비와 화합물의 분자량을 곱해서 넣음

            //*****************************최종 딱 minValue * mass 한 값만 들어와야 함*****************
            //이 코드 작동하는지 test 해볼것 (나머지 value유지된 채, mass가 갱신되는지)
            calculatedMacro.get(compound).setMass(minRatioValue * calculatedMacro.get(compound).getMass());
        }
        return result; //남은 배양액이 들어감 (원수 고려 없는 경우, 정상적으로 수행되면 모든 값이 0)
    }

    private double getMinRatioValue(Map<String, Double> innerRatio, Map<String, Double> result, double minRatioValue) {
        double ratio;
        for (String macro : innerRatio.keySet()) { // ex; compound에 대한 {NH4N=1.0, NO3N=1.0}, NH4N과 NO3N이 macro
            double available = result.get(macro); //해당 원수의 처방농도
            ratio = innerRatio.get(macro);
            double amountBasedOnRatio = available / ratio;
            minRatioValue = Math.min(minRatioValue, amountBasedOnRatio);
        }
        return minRatioValue;
    }

    //원수 고려 없이 계산 - 프론트에서 hashMap fertilization, is4(4수염인지 10수염인지) 넘어오게 파라미터 넣기
    private void calculateWithoutConsideredValue(Map<String, Double> userFertilization, boolean is4, boolean isConsidered){ //처방 농도
        getMajorCompoundRatio(is4);
        calculated100 = calculateWithRatio(userFertilization);

    }



}
