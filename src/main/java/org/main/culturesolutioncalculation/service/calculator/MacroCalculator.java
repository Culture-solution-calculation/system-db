package org.main.culturesolutioncalculation.service.calculator;

import org.main.culturesolutioncalculation.service.database.DatabaseConnector;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class MacroCalculator{


    private DatabaseConnector conn;

    private int user_id;
    private int users_macro_consideredValues_id;

    private Map<String, Map<String, Double>> compoundsRatio = new LinkedHashMap<>(); // ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}

    Map<String, Map<String, Double>> distributedValues = new LinkedHashMap<>(); //프론트에서 보여지는 자동 계산 결과

    private Map<String, FinalCal> molecularMass =  new LinkedHashMap<>();

    //1. 기준값 - 프론트에서 넘어옴
    private Map<String, Double> standardValues = new LinkedHashMap<>();
    //2. 원수 고려값 - 프론트에서 넘어옴
    private Map<String, Double> consideredValues = new LinkedHashMap<>();

    //3. 처방 값. 넘어와야 할 처방 농도 양식 예시 - 순서 그대로 유지되어야 함 (기준값 - 원수고려값)
    private Map<String, Double> fertilization = new LinkedHashMap<String, Double>(){
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

    //사용자 아이디 프론트에서 set 해줘야 함
    private void setUser_id(int id){
        user_id = id;
    }
    private int getUser_id(){
        return user_id;
    }


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


    //분자 별 갖고 있는 다량 원소 비율을 가져옴
    private void getMajorCompoundRatio(boolean is4){ //4수염인지 10수염인지를 판단하는 파라미터

        String query = "select * from macronutrients";
        query += is4? " where id != 2" : " where id != 1"; //id=1 : 질산칼슘4수염, id=2 : 질산칼슘10수염

        try (Connection connection = conn.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query);) {

            while(resultSet.next()){
                String macro = resultSet.getString("macro"); //질산칼슘4수염, 질산칼륨, 질산암모늄 등등
                String solution = resultSet.getString("solution"); //양액 타입 (A,B, C)
                double mass = resultSet.getDouble("mass");//화합물 질량

                molecularMass.put(macro, new FinalCal(solution, mass)); //100배액 계산을 위해 화합물과 그 질량 저장

                Map<String, Double> compoundRatio = new LinkedHashMap<>(); //ex. 질산칼슘4수염이 갖는 원수의 이름과 질량비를 갖는 map
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

    //자동계산 시 프론트에서 보여지는 분배된 값
    public Map<String, Map<String, Double>> calculateDistributedValues(Map<String, Double> userFertilization){
        double minRatioValue = Double.MAX_VALUE;
        //Map<String, Double> result = fertilization; //나중에 userFertilization이 들어오면 바꿀것 (result = userFertilization으로)

        for (String compound : compoundsRatio.keySet()) { //ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}에서 NH4NO3가 compound
            Map<String, Double> result = new LinkedHashMap<>();
            double ratio = 0.0, allocatedAmount = 0.0;

            //각 성분의 가장 낮은 비율에 해당하는 원수와 양 계산
            minRatioValue = getMinRatioValue(compoundsRatio.get(compound), fertilization, minRatioValue); //나중에 front에서 값 들어오면 userFertilization으로 바꿀것

            //가장 낮은 비율 기반으로 배당되는 처방 농도 계산 후 갱신
            for (String macro : compoundsRatio.get(compound).keySet()) {
                ratio = compoundsRatio.get(compound).get(macro);
                allocatedAmount = ratio * minRatioValue;
                result.put(macro, allocatedAmount);
            }
            distributedValues.put(compound, result);
            molecularMass.get(compound).setMass(minRatioValue * molecularMass.get(compound).getMass());//최종 minValue * mass 한 값
        }
        return distributedValues;
    }

    private double getMinRatioValue(Map<String, Double> innerRatio, Map<String, Double> result, double minRatioValue) {
        double ratio, available, amountBasedOnRatio;
        for (String macro : innerRatio.keySet()) { // ex; compound에 대한 {NH4N=1.0, NO3N=1.0}, NH4N과 NO3N이 macro
            available = result.get(macro); //해당 원수의 처방농도
            ratio = innerRatio.get(macro); //해당 원수의 화합물에 대한 첨가 비율
            amountBasedOnRatio = available / ratio;
            minRatioValue = Math.min(minRatioValue, amountBasedOnRatio);
        }
        return minRatioValue;
    }

    //자동 계산 - 프론트에서 hashMap fertilization, is4(4수염인지 10수염인지) 넘어오게 파라미터 넣기
    public Map<String, Map<String, Double>> calculate(Map<String, Double> userFertilization, boolean is4, boolean isConsidered){ //처방 농도
        getMajorCompoundRatio(is4);
        return calculateDistributedValues(userFertilization);
    }
    //원수 고려 여부, 처방 농도, 고려 원수, 기준값 -> db에 저장하는 함수
    public void save(boolean isConsidered, String unit, Map<String, Double> userFertilization, Map<String, Double> consideredValue, Map<String, Double>standardValue ){
        insertIntoUsersMacroConsideredValues(isConsidered, unit); //원수 고려 값 테이블에 저장
        insertIntoUsersMacroFertilization();
        insertIntoUsersMacroCalculatedMass();

    }

    private void insertIntoUsersMacroFertilization() { //계산된 처방값 DB 저장
        for (String macro : distributedValues.keySet()) {
            String query = "insert into users_macro_fertilization (users_macro_consideredValues_id, macro";
            for (String element : distributedValues.get(macro).keySet()) {
                query += ", "+element;
            }
            query += ") "; //여기까지 query = insert into user_macro_fertilization (macro, NO3N, Ca)
            query += "values (" +users_macro_consideredValues_id+", "+"'"+macro+"'";
            for (String element : distributedValues.get(macro).keySet()) {
                query += ", "+distributedValues.get(macro).get(element);
            }
            query += ")";
            try(Connection connection = conn.getConnection();
                Statement stmt = connection.createStatement();){
                int result = stmt.executeUpdate(query);

                //if(result>0) System.out.println("success insert users_macro_fertilization");
                //else System.out.println("insert failed users_macro_fertilization");
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    //100배액(kg) 계산식 저거 맞나 확인받기
    private void insertIntoUsersMacroCalculatedMass() { //계산된 질량 값 DB 저장
        String unit = "'kg'";

        for (String macro : molecularMass.keySet()) {
            double concentration_100fold = molecularMass.get(macro).getMass() / 10;

            String query = "insert into users_macro_calculatedMass (user_id, users_macro_consideredValues_id, macro, mass, unit) " +
                    "values ("+user_id+", "+users_macro_consideredValues_id+", "+"'"+macro+"'"+", "+concentration_100fold+", "+unit+")";

            System.out.println("query = " + query);
            try(Connection connection = conn.getConnection();
                    Statement stmt = connection.createStatement();){
                int result = stmt.executeUpdate(query);

                //if(result>0) System.out.println("success");
                //else System.out.println("insert failed");
            }catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    private void insertIntoUsersMacroConsideredValues(boolean isConsidered, String unit) { //고려 원수 값 DB 저장
        String query = "insert into users_macro_consideredValues ";
        String user_id = getUser_id()+"";
        String values = "(is_considered, NO3N, NH4N, " +
                "H2PO4, K, Ca, Mg, SO4S, unit, user_id) values (";

        if(!isConsidered){
            query += "(is_considered, unit, user_id) values (false, "+unit+", "+user_id+")";
        } else{
            values += "true";
            for (String value : consideredValues.keySet()) {
                values += ", "+consideredValues.get(value);
            }
            query += values;
        }
        try (Connection connection = conn.getConnection();
            Statement stmt = connection.createStatement()) {
            int result = stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
            if (result > 0) {
                System.out.println("success insert users_macro_consideredValues");
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if(generatedKeys.next()){
                    int id = generatedKeys.getInt(1);
                    users_macro_consideredValues_id = id; //fk로 사용하기 위해 배정
                }
            } else {
                System.out.println("insert failed users_macro_consideredValues");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


}
