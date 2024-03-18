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

    //�Ѿ�;� �� ó�� �� ��� - ���� �״�� �����Ǿ�� ��. front���� �Ѿ�;���
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

    //���� �� ���� �ִ� �̷� ���� ������ ������
    //����ڰ� ������ �̷����� ����Ʈ userMicroNutrients
    private void getMajorCompoundRatio(String[] userMicroNutrients){

        String query = "select * from micronutrients where micro in ("; //Ȳ�� ȭ�չ��� ������ ����

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
                String micro = resultSet.getString("micro"); //����Į��4����, ����Į��, ����ϸ� ���

                Map<String, Double> compoundRatio = new HashMap<>(); //ex. ����Į��4������ ���� ������ �̸��� ������ ���� map
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
        Map<String, Double> result = fertilization; //���߿� userFertilization�� ������ �ٲܰ� (result = userFertilization����)

        for (String compound : compoundsRatio.keySet()) { //ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}���� NH4NO3�� compound
            Map<String, Double> innerRatio = compoundsRatio.get(compound);
            double ratio = 0.0;

            //�� ������ ���� ���� ������ �ش��ϴ� ������ �� ���
            for (String micro : innerRatio.keySet()) { // ex; compound�� ���� {NH4N=1.0, NO3N=1.0}, NH4N�� NO3N�� macro
                double available = result.get(micro); //�ش� ������ ó���
                ratio = innerRatio.get(micro);
                double amountBasedOnRatio = available / ratio;
                minRatioValue = Math.min(minRatioValue, amountBasedOnRatio);
            }

            //���� ���� ���� ������� ���Ǵ� ó�� �� ��� �� ����
            for (String micro : innerRatio.keySet()) {
                ratio = innerRatio.get(micro);
                double allocatedAmount = ratio * minRatioValue;
                result.put(micro, result.get(micro) - allocatedAmount);
            }
        }

        return result; //���� ������ �� (���� ��� ���� ���, ���������� ����Ǹ� ��� ���� 0)
    }
    //���� ��� ���� ��� - ����Ʈ���� hashMap fertilization(ó���), ������ ȭ�ս� ���ڿ� �迭 �޾ƾ���
    private void calculateWithoutConsideredValue(Map<String, Double> userFertilization, String[] userMicroNutrients) { //ó�� ��
        getMajorCompoundRatio(userMicroNutrients);
        calculated100 = calculateWithRatio(userFertilization);
    }
}
