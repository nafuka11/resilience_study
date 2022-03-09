package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.control.Try;
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
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
public class ReservationService {

    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(Duration.ofMillis(1000))
            .callTimeout(Duration.ofMillis(1000))
            .build();

    private static final String endpoint = "http://localhost:9081/api/go/slow";

    private final CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom() //
                    .failureRateThreshold(50) // 失敗として open にする閾値(%)
                    .slowCallRateThreshold(50) // 呼び出し時間が slowCallDurationThreshold を超えて遅延とみなす閾値(%)
                    .slowCallDurationThreshold(Duration.ofMillis(500)) // 呼び出しが遅延していると判断する時間
                    .waitDurationInOpenState(Duration.ofSeconds(60)) // open から half-open に遷移するまでの待ち時間
                    .permittedNumberOfCallsInHalfOpenState(3) // half-open のときに許可される呼び出し回数
                    .minimumNumberOfCalls(10) // CircuitBreaker がエラー率を計算するようになるまでの呼び出し回数
                    .slidingWindowType(
                            CircuitBreakerConfig.SlidingWindowType.TIME_BASED) // 直近の slidingWindowSize 秒間の呼び出しを記録
                    .slidingWindowSize(5) // 直近5秒間の記録で失敗を判定する
                    .recordExceptions(IOException.class, TimeoutException.class) // 失敗として判定する例外
                    .build());

    private final RetryRegistry retryRegistry = RetryRegistry.of(RetryConfig.custom() //
            .maxAttempts(3) // 最大リトライ回数
            // .waitDuration(Duration.ofMillis(500)) // 固定の待ち時間
            .intervalFunction(IntervalFunction.ofExponentialBackoff(IntervalFunction.DEFAULT_INITIAL_INTERVAL,
                    IntervalFunction.DEFAULT_MULTIPLIER, 1000)) // リトライ回数ごとの待ち時間の決定関数をセット
            .retryOnResult(result -> result == null) // 結果が null だったときにリトライする
            //            .intervalFunction(numAttempts -> {
            //                log.info("numAttempts={}", numAttempts);
            //                return 100L;
            //            }) //
            .retryExceptions(IOException.class) // リトライする例外を定義
            .build());

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
        var circuitBreaker = getCircuitBreaker();

        var callableWithCircuitBreaker = CircuitBreaker.decorateCallable(circuitBreaker, () -> {
            Request request = new Request.Builder().url(endpoint).build();
            try (Response response = client.newCall(request).execute()) {
                Room[] rooms = objectMapper.readValue(response.body().string(), Room[].class);
                if (rooms != null && rooms.length > 0) {
                    return rooms[0];
                }
                return null;
            }
        });

        var retry = getRetry();

        var callableWithRetry = Retry.decorateCallable(retry, callableWithCircuitBreaker);

        return Try.ofCallable(callableWithRetry).recover(exception -> null).get();
    }

    private CircuitBreaker getCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("go/slow");
    }

    private Retry getRetry() {
        return retryRegistry.retry("go/slow");
    }
}
