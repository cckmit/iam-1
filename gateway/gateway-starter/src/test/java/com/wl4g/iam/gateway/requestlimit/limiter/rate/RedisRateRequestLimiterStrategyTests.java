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
package com.wl4g.iam.gateway.requestlimit.limiter.rate;

import static com.wl4g.infra.common.serialize.JacksonUtils.parseJSON;
import static com.wl4g.infra.common.serialize.JacksonUtils.toJSONString;

import org.junit.jupiter.api.Test;

/**
 * {@link RedisQuotaRequestLimiterStrategyTests}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2022-05-17 v3.0.0
 * @since v3.0.0
 */
public class RedisRateRequestLimiterStrategyTests {

    @Test
    public void testSerializeStrategy() {
        RedisRateRequestLimiterStrategy strategy = new RedisRateRequestLimiterStrategy();
        strategy.setBurstCapacity(1000);
        strategy.setIncludeHeaders(false);
        System.out.println(toJSONString(strategy));
    }

    @Test
    public void testDeserializeStrategy() {
        String json = "{\"includeHeaders\":false,\"burstCapacity\":1000,\"replenishRate\":1,\"requestedTokens\":1}";
        System.out.println(json);
        RedisRateRequestLimiterStrategy strategy = parseJSON(json, RedisRateRequestLimiterStrategy.class);
        System.out.println(strategy.getBurstCapacity());
        System.out.println(strategy.getReplenishRate());
    }

}