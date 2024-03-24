package org.main.culturesolutioncalculation.service.calculator;

import org.main.culturesolutioncalculation.service.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MicroCalculator{
    private DatabaseConnector conn;
    private int user_id;
    private int users_micro_consideredValues_id;
    private Map<String, Map<String, Double>> compoundsRatio = new LinkedHashMap<>(); // ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}

    Map<String, Map<String, Double>> distributedValues = new LinkedHashMap<>(); //프론트에서 보여지는 자동 계산 결과

    private Map<String, FinalCal> molecularMass =  new LinkedHashMap<>();

    //1. 기준값 - 프론트에서 넘어옴
    private Map<String, Double> standardValues = new LinkedHashMap<>();
    //2. 원수 고려값 - 프론트에서 넘어옴
    private Map<String, Double> consideredValues = new LinkedHashMap<>();

    //넘어와야 할 처방 농도 양식 - 순서 그대로 유지되어야 함. front에서 넘어와야함
    private Map<String, Double> fertilization = new LinkedHashMap<String, Double>(){
        {
            put("Fe", 15.5);
            put("Cu", 0.75);
            put("B", 30.0);
            put("Mn", 10.0);
            put("Zn", 5.0);
            put("Mo",0.5);
        }
    };

    //처방 농도 계산 함수. '기준량 - 원소성분 = 처방농도' 수행. 원수 고려 안하면 0으로 넘어와야 함
    private void calculateFertilizationValue(Map<String, Double> standardValuesFront, Map<String, Double> consideredValuesFront ){
        //프론트에서 받은 값 저장해야 하는지는 좀 더 고민해보기. 지워도 될듯함
        standardValues = standardValuesFront;
        consideredValues = consideredValuesFront;

        for (String valueName : standardValuesFront.keySet()) {
            if(consideredValuesFront.get(valueName) < standardValuesFront.get(valueName)){
                fertilization.put(valueName, standardValuesFront.get(valueName) - consideredValuesFront.get(valueName));
            }else{ //원수 고려값을 기준값보다 크게 입력한 경우- 에러
                System.err.println("입력한 원수 ["+ valueName+ "]값이 기준 값을 초과했습니다.");
            }
        }
    }

    //분자 별 갖고 있는 미량 원소 비율을 가져옴. userMicroNutrients : 사용자가 선택한 미량원소 리스트
    private void getMajorCompoundRatio(List<String> userMicroNutrients){

        String query = "select * from micronutrients where micro in ('CuSO4·5H2O', 'ZnSO4·7H2O'"; //황산 구리, 황산 아연 화합물은 무조건 선택

        for (String micro : userMicroNutrients) {
            query += ", '"+micro+"'";
        }
        query += ");";

        try (Connection connection = conn.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query);) {

            while(resultSet.next()){
                String micro = resultSet.getString("micro"); //질산칼슘4수염, 질산칼륨, 질산암모늄 등등
                String solution = resultSet.getString("solution");
                double mass = resultSet.getDouble("mass");

                molecularMass.put(micro, new FinalCal(solution, mass)); //100배액 계산을 위해 화합물과 그 질량, 양액 저장

                Map<String, Double> compoundRatio = new LinkedHashMap<>(); //ex. 질산칼슘4수염이 갖는 원수의 이름과 질량비를 갖는 map
                for (String major : fertilization.keySet()) {
                    if (resultSet.getDouble(major) != 0) {
                        compoundRatio.put(major, resultSet.getDouble(major));
                        try (Statement innerStmt = connection.createStatement();
                             ResultSet set = innerStmt.executeQuery("select mass from micronutrients_mass where micro = '" + major + "'")) {
                            if (set.next()) {
                                double micro_mass = set.getDouble("mass");
                                int content_count = set.getInt("content_count");
                                compoundRatio.put("mass", micro_mass); // 원자량도 같이 저장해야 함
                                compoundRatio.put("content_count", content_count*1.0);
                            }
                        }

                    }
                }
                compoundsRatio.put(micro, compoundRatio);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public Map<String, Map<String, Double>> calculateWithRatio(Map<String, Double> userFertilization){
        double ratioValue = Double.MAX_VALUE;
        Map<String, Double> result = fertilization; //나중에 userFertilization이 들어오면 바꿀것 (result = userFertilization으로)

        for (String compound : compoundsRatio.keySet()) { //ex; {ZnSO4·7H2O , {Zn=1.0, mass=65.37}}에서 ZnSO4·7H2O이 compound
            // 분자량*시비량/원자량/함량갯수 = 100배액
            /*
              name = ZnSO4쨌7H2O
              compoundsRatio = {content_count=1.0, Zn=1.0, mass=65.37}
             */
            double atomicWeight = compoundsRatio.get(compound).get("mass");
            double molecularWeight =  molecularMass.get(compound).getMass();
            double contentCount = compoundsRatio.get(compound).get("content_count");
            double fertilizationAmount = result.get(compound);
            double microValue;

            for (String micro : result.keySet()) {
                if(compoundsRatio.get(compound).containsKey(micro)){
                    
                    microValue = compoundsRatio.get(compound).get(micro);
                }
            }

            distributedValues.put(compound, result);
            molecularMass.get(compound).setMass(ratioValue * molecularMass.get(compound).getMass());//최종 minValue * mass 한 값
        }

        return distributedValues;
    }
    double getRatioValue(Map<String, Double> innerRatio, Map<String, Double> result, double ratioValue) {
        double ratio;
        for (String micro : innerRatio.keySet()) { // ex; compound에 대한 {NH4N=1.0, mass=65.0}, NH4N과 NO3N이 macro
            if(micro.equals("mass")) continue;
            double available = result.get(micro); //해당 원수의 처방농도
            ratio = innerRatio.get(micro);
            double amountBasedOnRatio = available / ratio;
            ratioValue = Math.min(ratioValue, amountBasedOnRatio);
        }
        return ratioValue;
    }
    //원수 고려 없이 계산 - 프론트에서 hashMap fertilization(처방농도), 선택한 화합식 문자열 배열 받아야함
    private Map<String, Map<String, Double>> calculate(Map<String, Double> userFertilization, List<String> userMicroNutrients) { //처방 농도
        getMajorCompoundRatio(userMicroNutrients);
        return calculateWithRatio(userFertilization);
    }
}
