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

    private Map<String, Double> calculated100 = new HashMap<>(); //����Ʈ���� �������� �ڵ� ��� ���

    private Map<String, FinalCal> calculatedMacro=  new HashMap<>();

    //���ڽ�, �ѱ۸�, ����

    //�Ѿ�;� �� ó�� �� ��� ���� - ���� �״�� �����Ǿ�� ��
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
    //��¿� �ٷ����� �� 100��� ���-  calculatedMacro�� ����
    private void calculateWith100(){
        if(calculated100.size()==0) System.err.println("�ٷ� ���� ó�� �󵵰� ������ �ʾҽ��ϴ�.");
        String query = "insert into calculatedmacro (";



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

                calculated100.put(macro, resultSet.getDouble("mass"));
                calculatedMacro.put(macro, new FinalCal(macro_kr, mass)); //100��� ����� ���� ȭ�չ��� �� ���� ����

                Map<String, Double> compoundRatio = new HashMap<>(); //ex. ����Į��4������ ���� ������ �̸��� ������ ���� map
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

        Map<String, Double> result = fertilization; //���߿� userFertilization�� ������ �ٲܰ� (result = userFertilization����)

        for (String compound : compoundsRatio.keySet()) { //ex; {NH4NO3 , {NH4N=1.0, NO3N=1.0}}���� NH4NO3�� compound
            Map<String, Double> innerRatio = compoundsRatio.get(compound);
            double ratio = 0.0;

            //�� ������ ���� ���� ������ �ش��ϴ� ������ �� ���
            minRatioValue = getMinRatioValue(innerRatio, result, minRatioValue);

            //���� ���� ���� ������� ���Ǵ� ó�� �� ��� �� ����
            for (String macro : innerRatio.keySet()) {
                ratio = innerRatio.get(macro);
                double allocatedAmount = ratio * minRatioValue;
                result.put(macro, result.get(macro) - allocatedAmount);
            }
            calculated100.put(compound, minRatioValue * calculated100.get(compound)); //�ּ� ��� ȭ�չ��� ���ڷ��� ���ؼ� ����

            //*****************************���� �� minValue * mass �� ���� ���;� ��*****************
            //�� �ڵ� �۵��ϴ��� test �غ��� (������ value������ ä, mass�� ���ŵǴ���)
            calculatedMacro.get(compound).setMass(minRatioValue * calculatedMacro.get(compound).getMass());
        }
        return result; //���� ������ �� (���� ��� ���� ���, ���������� ����Ǹ� ��� ���� 0)
    }

    private double getMinRatioValue(Map<String, Double> innerRatio, Map<String, Double> result, double minRatioValue) {
        double ratio;
        for (String macro : innerRatio.keySet()) { // ex; compound�� ���� {NH4N=1.0, NO3N=1.0}, NH4N�� NO3N�� macro
            double available = result.get(macro); //�ش� ������ ó���
            ratio = innerRatio.get(macro);
            double amountBasedOnRatio = available / ratio;
            minRatioValue = Math.min(minRatioValue, amountBasedOnRatio);
        }
        return minRatioValue;
    }

    //���� ��� ���� ��� - ����Ʈ���� hashMap fertilization, is4(4�������� 10��������) �Ѿ���� �Ķ���� �ֱ�
    private void calculateWithoutConsideredValue(Map<String, Double> userFertilization, boolean is4, boolean isConsidered){ //ó�� ��
        getMajorCompoundRatio(is4);
        calculated100 = calculateWithRatio(userFertilization);

    }



}
