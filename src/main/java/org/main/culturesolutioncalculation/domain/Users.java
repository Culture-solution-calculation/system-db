package org.main.culturesolutioncalculation.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Users {

    private int id;
    private String name;
    private String mediumType;
    private String requestDate;

    private String address;
    private String contact;
    private String cropName;

    public Users(int id, String name, String mediumType, String requestDate, String address, String contact, String cropName) {
        this.id = id;
        this.name = name;
        this.mediumType = mediumType;
        this.requestDate = requestDate;
        this.address = address;
        this.contact = contact;
        this.cropName = cropName;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getMediumType() {
        return mediumType;
    }

    public String getRequestDate() {
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
}
