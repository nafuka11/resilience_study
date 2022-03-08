package com.example.demo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Configuration
public class Resilience4JConfiguration {

    @Bean
    public CircuitBreakerRegistry getCircuitBreakerRegistry() {
        return CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                    .failureRateThreshold(50) // 失敗として open にする閾値(%)
                    .slowCallRateThreshold(50) // 呼び出し時間が slowCallDurationThreshold を超えて遅延とみなす閾値(%)
                    .slowCallDurationThreshold(Duration.ofMillis(500)) // 呼び出しが遅延していると判断する時間
                    .waitDurationInOpenState(Duration.ofSeconds(60)) // open から half-open に遷移するまでの待ち時間
                    .permittedNumberOfCallsInHalfOpenState(3) // half-open のときに許可される呼び出し回数
                    .minimumNumberOfCalls(10) // CircuitBreaker がエラー率を計算するようになるまでの呼び出し回数
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED) // 直近の slidingWindowSize 秒間の呼び出しを記録
                    .slidingWindowSize(5) // 直近5秒間の記録で失敗を判定する
                    .recordExceptions(IOException.class, TimeoutException.class) // 失敗として判定する例外
                    .build()
        );
    }

}
