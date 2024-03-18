package org.main.culturesolutioncalculation.service.calculator;

import org.main.culturesolutioncalculation.service.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class MicroCalculator{
    private DatabaseConnector conn;
    private Map<String, Map<String, Double>> compoundsRatio = new HashMap<>(); // ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}

    private Map<String, Double> calculated100 = new HashMap<>();

    private Map<String, FinalCal> calculatedMacro=  new HashMap<>();

    //넘어와야 할 처방 농도 양식 - 순서 그대로 유지되어야 함. front에서 넘어와야함
    private Map<String, Double> fertilization = new HashMap<String, Double>(){
        {
            put("Fe", 15.5);
            put("Cu", 1.25);
            put("B",1.25);
            put("Mn",6.5);
            put("Zn",4.75);
            put("Mo",1.5);
        }
    };

    //분자 별 갖고 있는 미량 원소 비율을 가져옴
    //사용자가 선택한 미량원소 리스트 userMicroNutrients
    private void getMajorCompoundRatio(String[] userMicroNutrients){

        String query = "select * from micronutrients where micro in ("; //황산 화합물은 무조건 선택

        for (int i = 0; i < userMicroNutrients.length; i++) {
            query += "'"+userMicroNutrients[i]+"'";
            if (i < userMicroNutrients.length - 1) {
                query += ", ";
            }
        }

        try (Connection connection = conn.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query);) {

            while(resultSet.next()){
                String micro = resultSet.getString("micro"); //질산칼슘4수염, 질산칼륨, 질산암모늄 등등

                Map<String, Double> compoundRatio = new HashMap<>(); //ex. 질산칼슘4수염이 갖는 원수의 이름과 질량비를 갖는 map
                for (String major : fertilization.keySet()) {
                    if(resultSet.getDouble(major) != 0){
                        compoundRatio.put(major,resultSet.getDouble(major));
                    }
                }
                compoundsRatio.put(micro,compoundRatio);
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
            for (String micro : innerRatio.keySet()) { // ex; compound에 대한 {NH4N=1.0, NO3N=1.0}, NH4N과 NO3N이 macro
                double available = result.get(micro); //해당 원수의 처방농도
                ratio = innerRatio.get(micro);
                double amountBasedOnRatio = available / ratio;
                minRatioValue = Math.min(minRatioValue, amountBasedOnRatio);
            }

            //가장 낮은 비율 기반으로 배당되는 처방 농도 계산 후 갱신
            for (String micro : innerRatio.keySet()) {
                ratio = innerRatio.get(micro);
                double allocatedAmount = ratio * minRatioValue;
                result.put(micro, result.get(micro) - allocatedAmount);
            }
        }

        return result; //남은 배양액이 들어감 (원수 고려 없는 경우, 정상적으로 수행되면 모든 값이 0)
    }
    //원수 고려 없이 계산 - 프론트에서 hashMap fertilization(처방농도), 선택한 화합식 문자열 배열 받아야함
    private void calculateWithoutConsideredValue(Map<String, Double> userFertilization, String[] userMicroNutrients) { //처방 농도
        getMajorCompoundRatio(userMicroNutrients);
        calculated100 = calculateWithRatio(userFertilization);
    }
}
