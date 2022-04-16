/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.iam.gateway.circuitbreaker.config;

import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link CustomCircuitBreakerProperties}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2022-04-16 v3.0.0
 * @since v3.0.0
 */
@Getter
@Setter
@ToString
public class CustomCircuitBreakerProperties {

    private TimeLimiterProperties timeLimiter = new TimeLimiterProperties();

    private float failureRateThreshold = CircuitBreakerConfig.DEFAULT_FAILURE_RATE_THRESHOLD;
    private int permittedNumberOfCallsInHalfOpenState = CircuitBreakerConfig.DEFAULT_PERMITTED_CALLS_IN_HALF_OPEN_STATE;
    private int slidingWindowSize = CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_SIZE;
    private SlidingWindowType slidingWindowType = CircuitBreakerConfig.DEFAULT_SLIDING_WINDOW_TYPE;
    private int minimumNumberOfCalls = CircuitBreakerConfig.DEFAULT_MINIMUM_NUMBER_OF_CALLS;
    private boolean writableStackTraceEnabled = CircuitBreakerConfig.DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
    private boolean automaticTransitionFromOpenToHalfOpenEnabled = false;
    private Duration waitIntervalFunctionInOpenState = Duration
            .ofSeconds(CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE);
    private float slowCallRateThreshold = CircuitBreakerConfig.DEFAULT_SLOW_CALL_RATE_THRESHOLD;
    private Duration slowCallDurationThreshold = Duration.ofSeconds(CircuitBreakerConfig.DEFAULT_SLOW_CALL_DURATION_THRESHOLD);

    public TimeLimiterConfig toTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(getTimeLimiter().getTimeoutDuration())
                .cancelRunningFuture(getTimeLimiter().isCancelRunningFuture())
                .build();
    }

    public CircuitBreakerConfig toCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(getFailureRateThreshold())
                .permittedNumberOfCallsInHalfOpenState(getPermittedNumberOfCallsInHalfOpenState())
                .slidingWindowSize(getSlidingWindowSize())
                .slidingWindowType(getSlidingWindowType())
                .minimumNumberOfCalls(getMinimumNumberOfCalls())
                .writableStackTraceEnabled(isWritableStackTraceEnabled())
                .automaticTransitionFromOpenToHalfOpenEnabled(isAutomaticTransitionFromOpenToHalfOpenEnabled())
                .waitIntervalFunctionInOpenState(IntervalFunction.of(getWaitIntervalFunctionInOpenState()))
                .slowCallRateThreshold(getSlowCallRateThreshold())
                .slowCallDurationThreshold(getSlowCallDurationThreshold())
                .build();
    }

    @Getter
    @Setter
    @ToString
    public static class TimeLimiterProperties {
        private Duration timeoutDuration = Duration.ofSeconds(1);
        private boolean cancelRunningFuture = true;
    }

}