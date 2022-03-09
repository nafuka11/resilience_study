package com.example.demo.controller;

import com.example.demo.service.Reservation;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/java/slow")
public class SlowEndpointController {

    public static record Temperature(double celsius) {}

    @GetMapping("")
    public Temperature get() {
        var sleep = (long)(erlangKL(0.5, 2) * 200.0);
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException ignore) {}

        log.info("slow endpoint is requested: sleep={}", sleep);

        return new Temperature(17.5);
    }

    private final ExponentialDistribution exponentialDistribution = new ExponentialDistribution(1.0);

    private double erlangKL(double lambda, int k) {
        double g = 0.0;
        for (int i = 0; i < k; i++) {
            g += (exponentialDistribution.sample() / lambda);
        }
        return g;
    }
}
