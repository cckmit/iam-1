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
package com.wl4g.iam.gateway.auth.simple.recorder;

import static java.lang.String.valueOf;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.google.common.eventbus.Subscribe;
import com.wl4g.iam.gateway.auth.config.AuthingProperties;
import com.wl4g.iam.gateway.auth.simple.event.SignAuthingFailureEvent;
import com.wl4g.iam.gateway.auth.simple.event.SignAuthingSuccessEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis-based signature authentication event accumulator, usually used in API
 * gateway billing business scenarios.
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2022-04-18 v3.0.0
 * @since v3.0.0
 */
@Slf4j
public class RedisSimpleSignEventRecoder implements InitializingBean {

    public static final String LOG_SIGN_EVENT_SUCCESS_PREFIX = "SIGN_EVENT_SUCCESS";
    public static final String LOG_SIGN_EVENT_FAILURE_PREFIX = "SIGN_EVENT_FAILURE";

    private @Autowired AuthingProperties authingConfig;
    private @Autowired StringRedisTemplate redisTemplate;
    private BoundHashOperations<String, Object, Object> successCumulator;
    private BoundHashOperations<String, Object, Object> failureCumulator;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.successCumulator = redisTemplate
                .boundHashOps(authingConfig.getSimpleSign().getRedisEventRecoderSuccessCumulatorKey());
        this.failureCumulator = redisTemplate
                .boundHashOps(authingConfig.getSimpleSign().getRedisEventRecoderFailureCumulatorKey());
    }

    @Subscribe
    public void onSuccess(SignAuthingSuccessEvent event) {
        String appId = valueOf(event.getSource());
        Long incr = null;
        try {
            incr = successCumulator.increment(appId, 1);
        } finally {
            if (log.isInfoEnabled()) {
                log.info("{} {}->{}", LOG_SIGN_EVENT_SUCCESS_PREFIX, appId, incr);
            }
        }
    }

    @Subscribe
    public void onFailure(SignAuthingFailureEvent event) {
        String appId = valueOf(event.getSource());
        Long incr = null;
        try {
            incr = failureCumulator.increment(appId, 1);
        } finally {
            if (log.isInfoEnabled()) {
                log.info("{} {}->{}", LOG_SIGN_EVENT_FAILURE_PREFIX, appId, incr);
            }
        }
    }

}
