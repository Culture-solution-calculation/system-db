package org.main.culturesolutioncalculation.service.database;

import org.main.culturesolutioncalculation.domain.Users;

import java.sql.*;

public class UserService {

    private DatabaseConnector conn;

    //유저 정보 저장
    public void save(Users users){
        String query = "insert into users (name, address, contact, request_date, crop_name, cultivation_scale, medium_type) " +
                "values ("+users.getName()+", "+users.getAddress()+", "+users.getContact()+", "+users.getRequestDate()+", "
                +users.getCropName()+", "+users.getCultivationScale()+", "+users.getMediumType()+")";
        try(Connection connection = conn.getConnection();
            Statement stmt = connection.createStatement();){
            int result = stmt.executeUpdate(query);

            //if(result>0) System.out.println("success insert users");
            //else System.out.println("insert failed users");

        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public Users findUserById(int id){
        Users users = new Users();
        String query = "select * from users where id = 1";
        try(Connection connection = conn.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery(query);)
        {
            while(resultSet.next()){
                int userId = resultSet.getInt("id");
                String userName = resultSet.getString("name");
                String mediumType = resultSet.getString("medium_type");
                String cropName = resultSet.getString("crop_name");
                Timestamp requestDate = resultSet.getTimestamp("request_date");
                String address = resultSet.getString("address");
                String contact = resultSet.getString("contact");
                String cultivationScale = resultSet.getString("cultivation_scale");

                users.setId(userId);
                users.setName(userName);
                users.setAddress(address);
                users.setMediumType(mediumType);
                users.setContact(contact);
                users.setCropName(cropName);
                users.setRequestDate(requestDate);
                users.setCultivationScale(cultivationScale);
            }

        }catch (SQLException e){
            e.printStackTrace();
        }
        return users;
    }
}
