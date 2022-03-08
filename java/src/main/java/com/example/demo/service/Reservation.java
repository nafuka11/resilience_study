package com.example.demo.service;

import lombok.Data;

@Data
public class Reservation {

    private String id;

    private String title;

    private String beginDate;

    private String endDate;

    private Room room;
}
