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
    private Map<String, Map<String, Double>> compoundsRatio = new LinkedHashMap<>(); // ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}

    private Map<String, Double> distributedValues = new LinkedHashMap<>(); //����Ʈ���� �������� �ڵ� ��� ���

    private Map<String, FinalCal> calculatedMacro=  new LinkedHashMap<>();

    //���ڽ�, �ѱ۸�, ����

    //�Ѿ�;� �� ó�� �� ��� ���� - ���� �״�� �����Ǿ�� ��
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
    //��¿� �ٷ����� �� 100��� ���-  calculatedMacro�� ����
    private void calculateWith100(){
        if(distributedValues.size()==0) System.err.println("�ٷ� ���� ó�� �󵵰� ������ �ʾҽ��ϴ�.");
        String query = "insert into calculatedMacro (";


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
                String macro_kr = resultSet.getString("macro_kr"); //ȭ�չ� �ѱ۸�
                double mass = resultSet.getDouble("mass");//ȭ�չ� ����

                distributedValues.put(macro, resultSet.getDouble("mass"));
                calculatedMacro.put(macro, new FinalCal(macro_kr, mass)); //100��� ����� ���� ȭ�չ��� �� ���� ����

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
    public Map<String, Map<String, Double>> calculateWithRatio(Map<String, Double> userFertilization){
        double minRatioValue = Double.MAX_VALUE;
        //Map<String, Double> result = fertilization; //���߿� userFertilization�� ������ �ٲܰ� (result = userFertilization����)
        Map<String, Map<String, Double>> results = new LinkedHashMap<>();

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
            distributedValues.put(compound, minRatioValue * distributedValues.get(compound)); //�ּ� ��� ȭ�չ��� ���ڷ��� ���ؼ� ����
            results.put(compound, result);

            //*****************************���� �� minValue * mass �� ���� ���;� ��*****************
            //�� �ڵ� �۵��ϴ��� test �غ��� (������ value������ ä, mass�� ���ŵǴ���)
            calculatedMacro.get(compound).setMass(minRatioValue * calculatedMacro.get(compound).getMass());
        }
        return results; //���� ������ �� (���� ��� ���� ���, ���������� ����Ǹ� ��� ���� 0)
    }

    private double getMinRatioValue(Map<String, Double> innerRatio, Map<String, Double> result, double minRatioValue) {
        double ratio, available, amountBasedOnRatio;
        for (String macro : innerRatio.keySet()) { // ex; compound�� ���� {NH4N=1.0, NO3N=1.0}, NH4N�� NO3N�� macro
            available = result.get(macro); //�ش� ������ ó���
            ratio = innerRatio.get(macro);
            amountBasedOnRatio = available / ratio;
            minRatioValue = Math.min(minRatioValue, amountBasedOnRatio);
        }
        return minRatioValue;
    }

    //���� ��� ���� ��� - ����Ʈ���� hashMap fertilization, is4(4�������� 10��������) �Ѿ���� �Ķ���� �ֱ�
    public Map<String, Map<String, Double>> calculateWithoutConsideredValue(Map<String, Double> userFertilization, boolean is4, boolean isConsidered){ //ó�� ��
        getMajorCompoundRatio(is4);
        return calculateWithRatio(userFertilization);
    }
    //���� ��� ����, ó�� ��, ��� ����, ���ذ� -> db�� �����ϴ� �Լ�
    public void save(boolean isConsidered, Map<String, Double> userFertilization, Map<String, Double> consideratedValue, Map<String, Double>standardValue ){
        String query = "";
        if(isConsidered){}
    }


}
