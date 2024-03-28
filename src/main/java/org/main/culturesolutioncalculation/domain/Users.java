package org.main.culturesolutioncalculation.domain;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Users {


    private int id;
    private String name;
    private String mediumType;
    private Timestamp requestDate;

    private String cultivationScale;

    private String address;
    private String contact;
    private String cropName;
    public Users(){

    }

    public Users(int id, String name, String mediumType, Timestamp requestDate, String address, String contact, String cropName, String cultivationScale) {
        this.id = id;
        this.name = name;
        this.mediumType = mediumType;
        this.requestDate = requestDate;
        this.address = address;
        this.contact = contact;
        this.cropName = cropName;
        this.cultivationScale = cultivationScale;
    }

    public int getId() {
        return id;
    }

    public String getCultivationScale() {
        return cultivationScale;
    }

    public void setCultivationScale(String cultivationScale) {
        this.cultivationScale = cultivationScale;
    }

    public String getName() {
        return name;
    }

    public String getMediumType() {
        return mediumType;
    }

    public void setMediumType(String type){
        this.mediumType = type;
    }
    public void setCropName(String type){
        this.cropName = type;
    }


    public Timestamp getRequestDate() {
        return requestDate;
    }

    public String getAddress() {
        return address;
    }

    public String getContact() {
        return contact;
    }

    public String getCropName() {
        return cropName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRequestDate(Timestamp requestDate) {
        this.requestDate = requestDate;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }
}
