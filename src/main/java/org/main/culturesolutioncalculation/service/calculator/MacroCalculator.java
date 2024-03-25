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

    Map<String, Map<String, Double>> distributedValues = new LinkedHashMap<>(); //����Ʈ���� �������� �ڵ� ��� ���

    private Map<String, FinalCal> molecularMass =  new LinkedHashMap<>();

    //1. ���ذ� - ����Ʈ���� �Ѿ��
    private Map<String, Double> standardValues = new LinkedHashMap<>();
    //2. ���� ����� - ����Ʈ���� �Ѿ��
    private Map<String, Double> consideredValues = new LinkedHashMap<>();

    //3. ó�� ��. �Ѿ�;� �� ó�� �� ��� ���� - ���� �״�� �����Ǿ�� �� (���ذ� - ���������)
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

    //����� ���̵� ����Ʈ���� set ����� ��
    private void setUser_id(int id){
        user_id = id;
    }
    private int getUser_id(){
        return user_id;
    }


    //ó�� �� ��� �Լ�. '���ط� - ���Ҽ��� = ó���' ����. ���� ��� ���ϸ� 0���� �Ѿ�;� ��
    private void calculateFertilizationValue(Map<String, Double> standardValuesFront, Map<String, Double> consideredValuesFront ){
        //����Ʈ���� ���� �� �����ؾ� �ϴ����� �� �� ����غ���. ������ �ɵ���
        standardValues = standardValuesFront;
        consideredValues = consideredValuesFront;

        for (String valueName : standardValuesFront.keySet()) {
            if(consideredValuesFront.get(valueName) < standardValuesFront.get(valueName)){
                fertilization.put(valueName, standardValuesFront.get(valueName) - consideredValuesFront.get(valueName));
            }else{ //���� ������� ���ذ����� ũ�� �Է��� ���- ����
                System.err.println("�Է��� ���� ["+ valueName+ "]���� ���� ���� �ʰ��߽��ϴ�.");
            }
        }
    }


    //���� �� ���� �ִ� �ٷ� ���� ������ ������
    private void getMajorCompoundRatio(boolean is4){ //4�������� 10���������� �Ǵ��ϴ� �Ķ����

        String query = "select * from macronutrients";
        query += is4? " where id != 2" : " where id != 1"; //id=1 : ����Į��4����, id=2 : ����Į��10����

        try (Connection connection = conn.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query);) {

            while(resultSet.next()){
                String macro = resultSet.getString("macro"); //����Į��4����, ����Į��, ����ϸ� ���
                String solution = resultSet.getString("solution"); //��� Ÿ�� (A,B, C)
                double mass = resultSet.getDouble("mass");//ȭ�չ� ����

                molecularMass.put(macro, new FinalCal(solution, mass)); //100��� ����� ���� ȭ�չ��� �� ���� ����

                Map<String, Double> compoundRatio = new LinkedHashMap<>(); //ex. ����Į��4������ ���� ������ �̸��� ������ ���� map
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

    //�ڵ���� �� ����Ʈ���� �������� �й�� ��
    public Map<String, Map<String, Double>> calculateDistributedValues(Map<String, Double> userFertilization){
        double minRatioValue = Double.MAX_VALUE;
        //Map<String, Double> result = fertilization; //���߿� userFertilization�� ������ �ٲܰ� (result = userFertilization����)

        for (String compound : compoundsRatio.keySet()) { //ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}���� NH4NO3�� compound
            Map<String, Double> result = new LinkedHashMap<>();
            double ratio = 0.0, allocatedAmount = 0.0;

            //�� ������ ���� ���� ������ �ش��ϴ� ������ �� ���
            minRatioValue = getMinRatioValue(compoundsRatio.get(compound), fertilization, minRatioValue); //���߿� front���� �� ������ userFertilization���� �ٲܰ�

            //���� ���� ���� ������� ���Ǵ� ó�� �� ��� �� ����
            for (String macro : compoundsRatio.get(compound).keySet()) {
                ratio = compoundsRatio.get(compound).get(macro);
                allocatedAmount = ratio * minRatioValue;
                result.put(macro, allocatedAmount);
            }
            distributedValues.put(compound, result);
            molecularMass.get(compound).setMass(minRatioValue * molecularMass.get(compound).getMass());//���� minValue * mass �� ��
        }
        return distributedValues;
    }

    private double getMinRatioValue(Map<String, Double> innerRatio, Map<String, Double> result, double minRatioValue) {
        double ratio, available, amountBasedOnRatio;
        for (String macro : innerRatio.keySet()) { // ex; compound�� ���� {NH4N=1.0, NO3N=1.0}, NH4N�� NO3N�� macro
            available = result.get(macro); //�ش� ������ ó���
            ratio = innerRatio.get(macro); //�ش� ������ ȭ�չ��� ���� ÷�� ����
            amountBasedOnRatio = available / ratio;
            minRatioValue = Math.min(minRatioValue, amountBasedOnRatio);
        }
        return minRatioValue;
    }

    //�ڵ� ��� - ����Ʈ���� hashMap fertilization, is4(4�������� 10��������) �Ѿ���� �Ķ���� �ֱ�
    public Map<String, Map<String, Double>> calculate(Map<String, Double> userFertilization, boolean is4, boolean isConsidered){ //ó�� ��
        getMajorCompoundRatio(is4);
        return calculateDistributedValues(userFertilization);
    }
    //���� ��� ����, ó�� ��, ��� ����, ���ذ� -> db�� �����ϴ� �Լ�
    public void save(boolean isConsidered, String unit, Map<String, Double> userFertilization, Map<String, Double> consideredValue, Map<String, Double>standardValue ){
        insertIntoUsersMacroConsideredValues(isConsidered, unit); //���� ��� �� ���̺� ����
        insertIntoUsersMacroFertilization();
        insertIntoUsersMacroCalculatedMass();

    }

    private void insertIntoUsersMacroFertilization() { //���� ó�氪 DB ����
        for (String macro : distributedValues.keySet()) {
            String query = "insert into users_macro_fertilization (users_macro_consideredValues_id, macro";
            for (String element : distributedValues.get(macro).keySet()) {
                query += ", "+element;
            }
            query += ") "; //������� query = insert into user_macro_fertilization (macro, NO3N, Ca)
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

    //100���(kg) ���� ���� �³� Ȯ�ιޱ�
    private void insertIntoUsersMacroCalculatedMass() { //���� ���� �� DB ����
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

    private void insertIntoUsersMacroConsideredValues(boolean isConsidered, String unit) { //��� ���� �� DB ����
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
                    users_macro_consideredValues_id = id; //fk�� ����ϱ� ���� ����
                }
            } else {
                System.out.println("insert failed users_macro_consideredValues");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


}
