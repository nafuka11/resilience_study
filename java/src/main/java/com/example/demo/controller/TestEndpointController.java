package com.example.demo.controller;

import com.example.demo.service.Reservation;
import com.example.demo.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/java/test")
public class TestEndpointController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping("")
    public Reservation get() {
        return reservationService.newReservation();
    }
}
