package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class ReservationService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(1))
            .callTimeout(Duration.ofSeconds(10))
            .build();

    private static final String endpoint = "http://localhost:9081/api/go/slow";

    @Autowired
    private ObjectMapper objectMapper;

    public Reservation newReservation() {
        var reservation = new Reservation();

        reservation.setId(UUID.randomUUID().toString());
        reservation.setTitle("テスト");

        var now = new Date();
        var iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        reservation.setBeginDate(iso8601.format(now));

        reservation.setRoom(getAvailableRoom(now));

        return reservation;
    }

    private Room getAvailableRoom(Date beginDate) {
        Request request = new Request.Builder()
                .url(endpoint)
                .build();
        try (Response response = client.newCall(request).execute()) {
            Room[] rooms = objectMapper.readValue(response.body().string(), Room[].class);
            if (rooms != null && rooms.length > 0) {
                return rooms[0];
            }
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
