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
package com.wl4g.iam.gateway.fault.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

import com.wl4g.infra.core.web.matcher.SpelRequestMatcher.MatchHttpRequestRule;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * {@link FaultProperties}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2022-04-27 v3.0.0
 * @since v3.0.0
 */
@Getter
@Setter
@Validated
@ToString
public class FaultProperties {

    private InjectorProperties defaultInjector = new InjectorProperties();

    @Getter
    @Setter
    @Validated
    @ToString
    public static class InjectorProperties {

        /**
         * Preferred to enable fault injection match SPEL match expression.
         * Default by '#{true}', which means never match. </br>
         * </br>
         * Tip: The built-in support to get the current routeId, such as:
         * '#{routeId.get().test('my-service-route')}'
         */
        private String preferOpenMatchExpression = "#{true}";

        /**
         * Prefer to enable fault injection match rule definitions.
         */
        private List<MatchHttpRequestRule> preferMatchRuleDefinitions = new ArrayList<>();

        /**
         * Prefer percentage of requests that require fault injection enabled.
         * which is equivalent to another and condition after match the SPEL
         * expression.
         */
        private double preferMatchPercentage = 1d;

        /**
         * The enabled fault injector providers.
         */
        private InjectorProvider provider = InjectorProvider.Abort;

        /**
         * The request to abort fault injector configuration properties.
         */
        private AbortInjectorProperties abort = new AbortInjectorProperties();

        /**
         * The request to fixed delay fault injector configuration properties.
         */
        private FixedDelayInjectorProperties fixedDelay = new FixedDelayInjectorProperties();

        /**
         * The request to random range delay fault injector configuration
         * properties.
         */
        private RangeDelayInjectorProperties rangeDelay = new RangeDelayInjectorProperties();

    }

    @Getter
    @AllArgsConstructor
    public static enum InjectorProvider {
        Abort(AbortInjectorProperties.class), FixedDelay(FixedDelayInjectorProperties.class), RangeDelay(
                RangeDelayInjectorProperties.class);
        private final Class<?> injectorClass;
    }

    @Getter
    @Setter
    @Validated
    @ToString
    public static class AbortInjectorProperties {

        /**
         * The HttpStatus returned when the fault strategy is meet, the default
         * is INTERNAL_SERVER_ERROR.
         */
        private String statusCode = HttpStatus.INTERNAL_SERVER_ERROR.name();

    }

    @Getter
    @Setter
    @Validated
    @ToString
    public static class FixedDelayInjectorProperties {

        /**
         * Fixed delay in milliseconds
         */
        private long delayMs = 1000L;
    }

    @Getter
    @Setter
    @Validated
    @ToString
    public static class RangeDelayInjectorProperties {

        /**
         * Minimum delay in milliseconds
         */
        private long minDelayMs = 1000L;

        /**
         * Maximum delay in milliseconds
         */
        private long maxDelayMs = 5000L;
    }

}
