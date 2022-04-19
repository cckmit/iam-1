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
package com.wl4g.iam.gateway.ratelimit.recorder;

import static java.lang.String.valueOf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.google.common.eventbus.Subscribe;
import com.wl4g.iam.gateway.ratelimit.config.IamRateLimiterProperties;
import com.wl4g.iam.gateway.ratelimit.event.RateLimitHitEvent;
import com.wl4g.infra.common.lang.DateUtils2;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link RedisRateLimitEventRecorder}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2022-04-19 v3.0.0
 * @since v3.0.0
 */
@Slf4j
public class RedisRateLimitEventRecorder {

    public static final String LOG_SIGN_EVENT_HITS_PREFIX = "RATELIMIT_EVENT_HITS";

    private @Autowired IamRateLimiterProperties rateLimitConfig;
    private @Autowired StringRedisTemplate redisTemplate;

    @Subscribe
    public void onRateLimitHit(RateLimitHitEvent event) {
        String rateLimitId = valueOf(event.getSource());
        Long incr = null;
        try {
            incr = getHitsCumulator().increment(rateLimitId, 1);
        } finally {
            if (rateLimitConfig.getEvent().isRedisEventRecoderLogEnabled() && log.isInfoEnabled()) {
                log.info("{} {}->{}", LOG_SIGN_EVENT_HITS_PREFIX, rateLimitId, incr);
            }
        }
    }

    private BoundHashOperations<String, Object, Object> getHitsCumulator() {
        return redisTemplate.boundHashOps(rateLimitConfig.getEvent().getRedisEventRecoderHitsCumulatorPrefix().concat(":").concat(
                DateUtils2.getDate(rateLimitConfig.getEvent().getRedisEventRecoderCumulatorSuffixOfDatePattern())));
    }

}
