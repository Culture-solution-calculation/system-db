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
    private int getUser_id(){
        return this.user_id;
    }
    private int users_micro_consideredValues_id;
    private Map<String, Map<String, Double>> compoundsRatio = new LinkedHashMap<>(); // ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}

    Map<String, Map<String, Double>> distributedValues = new LinkedHashMap<>(); //����Ʈ���� �������� �ڵ� ��� ���

    private Map<String, FinalCal> molecularMass =  new LinkedHashMap<>();

    //1. ���ذ� - ����Ʈ���� �Ѿ��
    private Map<String, Double> standardValues = new LinkedHashMap<>();
    //2. ���� ����� - ����Ʈ���� �Ѿ��
    private Map<String, Double> consideredValues = new LinkedHashMap<>();

    //�Ѿ�;� �� ó�� �� ��� - ���� �״�� �����Ǿ�� ��. front���� �Ѿ�;���
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

    //���� �� ���� �ִ� �̷� ���� ������ ������. userMicroNutrients : ����ڰ� ������ �̷����� ����Ʈ
    private void getMajorCompoundRatio(List<String> userMicroNutrients){

        String query = "select * from micronutrients where micro in ('CuSO4��5H2O', 'ZnSO4��7H2O'"; //Ȳ�� ����, Ȳ�� �ƿ� ȭ�չ��� ������ ����

        for (String micro : userMicroNutrients) {
            query += ", '"+micro+"'";
        }
        query += ");";

        try (Connection connection = conn.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery(query);) {

            while(resultSet.next()){
                String micro = resultSet.getString("micro"); //����Į��4����, ����Į��, ����ϸ� ���
                String solution = resultSet.getString("solution");
                double mass = resultSet.getDouble("mass");

                molecularMass.put(micro, new FinalCal(solution, mass)); //100��� ����� ���� ȭ�չ��� �� ����, ��� ����

                Map<String, Double> compoundRatio = new LinkedHashMap<>(); //ex. ����Į��4������ ���� ������ �̸��� ������ ���� map
                for (String major : fertilization.keySet()) {
                    if (resultSet.getDouble(major) != 0) {
                        compoundRatio.put(major, resultSet.getDouble(major));
                        try (Statement innerStmt = connection.createStatement();
                             ResultSet set = innerStmt.executeQuery("select mass from micronutrients_mass where micro = '" + major + "'")) {
                            if (set.next()) {
                                double micro_mass = set.getDouble("mass");
                                int content_count = set.getInt("content_count");
                                compoundRatio.put("mass", micro_mass); // ���ڷ��� ���� �����ؾ� ��
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
        Map<String, Double> results = fertilization; //���߿� userFertilization�� ������ �ٲܰ� (result = userFertilization����)

        for (String compound : compoundsRatio.keySet()) { //ex; {ZnSO4��7H2O , {Zn=1.0, mass=65.37}}���� ZnSO4��7H2O�� compound
            // ���ڷ�*�ú�/���ڷ�/�Է����� = 100���
            /*
              name = ZnSO4·7H2O
              compoundsRatio = {content_count=1.0, Zn=1.0, mass=65.37}
             */
            Map<String, Double> result = new LinkedHashMap<>();
            double atomicWeight = compoundsRatio.get(compound).get("mass"); //���ڷ�
            double molecularWeight =  molecularMass.get(compound).getMass(); //���ڷ�
            double contentCount = compoundsRatio.get(compound).get("content_count"); //�Է�����
            double fertilizationAmount; //�ú�
            double microValue = 0.0; //��� ���

            for (String micro : results.keySet()) { //ó��� micro
                if(compoundsRatio.get(compound).containsKey(micro)){
                    fertilizationAmount = results.get(micro);
                    microValue = molecularWeight * fertilizationAmount / atomicWeight / contentCount;
                    result.put(micro, fertilizationAmount);
                }
            }
            distributedValues.put(compound, result); //�ش� ȭ�չ��� ���� ó�淮
            molecularMass.get(compound).setMass(microValue);//���� minValue * mass �� ��
        }

        return distributedValues;
    }

    //����Ʈ���� hashMap fertilization(ó���), ������ ȭ�ս� ���ڿ� �迭 �޾ƾ���
    private Map<String, Map<String, Double>> calculate(Map<String, Double> userFertilization, List<String> userMicroNutrients) { //ó�� ��
        getMajorCompoundRatio(userMicroNutrients);
        return calculateWithRatio(userFertilization);
    }

    //���� ��� ����, ó�� ��, ��� ����, ���ذ� -> db�� �����ϴ� �Լ�
    public void save(boolean isConsidered, String unit, Map<String, Double> userFertilization, Map<String, Double> consideredValue, Map<String, Double>standardValue ){
        insertIntoUsersMicroConsideredValues(isConsidered, unit); //���� ��� �� ���̺� ����
        insertIntoUsersMicroFertilization();
        insertIntoUsersMicroCalculatedMass();

    }

    private void insertIntoUsersMicroCalculatedMass() {
    }

    private void insertIntoUsersMicroFertilization() {
        for (String micro : distributedValues.keySet()) {
            String query = "insert into users_micro_fertilization (users_micro_consideredValues_id, micro";
            for (String element : distributedValues.get(micro).keySet()) {
                query += ", "+element;
            }
            query += ") "; //������� query = insert into user_macro_fertilization (macro, NO3N, Ca)
            query += "values (" +users_micro_consideredValues_id+", "+"'"+micro+"'";
            for (String element : distributedValues.get(micro).keySet()) {
                query += ", "+distributedValues.get(micro).get(element);
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

    private void insertIntoUsersMicroConsideredValues(boolean isConsidered, String unit) { //��� ���� �� DB ����
        String query = "insert into users_micro_consideredValues ";
        String user_id = getUser_id()+"";
        String values = "(is_considered, Fe, Cu, " +
                "B, Mn, Zn, Mo, unit, user_id) values (";

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
                System.out.println("success insert users_micro_consideredValues");
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if(generatedKeys.next()){
                    int id = generatedKeys.getInt(1);
                    users_micro_consideredValues_id = id; //fk�� ����ϱ� ���� ����
                }
            } else {
                System.out.println("insert failed users_micro_consideredValues");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
