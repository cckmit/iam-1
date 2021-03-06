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
package com.wl4g.iam.gateway.route.config;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import com.wl4g.iam.common.constant.GatewayIAMConstants;
import com.wl4g.iam.gateway.route.Https2HttpGlobalFilter;
import com.wl4g.iam.gateway.route.RefreshRouteApplicationListener;
import com.wl4g.iam.gateway.route.TimeBasedRouteRefresher;
//import com.wl4g.iam.gateway.route.ignore.IgnoreGatewayFilterFactory;
//import com.wl4g.iam.gateway.route.ignore.IgnoreGatewayFilterFactory.IgnoreGatewayFilter;
import com.wl4g.iam.gateway.route.repository.RedisRouteDefinitionRepository;
import com.wl4g.iam.gateway.security.config.IamSecurityAutoConfiguration;

/**
 * {@link IamSecurityAutoConfiguration}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2021-10-13 v1.0.0
 * @since v1.0.0
 */
public class RouteAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = GatewayIAMConstants.CONF_PREFIX_IAM_GATEWAY_ROUTE)
    public RouteProperties routeProperties() {
        return new RouteProperties();
    }

    @Bean
    public Https2HttpGlobalFilter https2HttpGlobalFilter(RouteProperties config) {
        return new Https2HttpGlobalFilter(config);
    }

    @Bean
    public RouteDefinitionRepository redisRouteDefinitionRepository() {
        return new RedisRouteDefinitionRepository();
    }

    // @Bean
    // public RouteDefinitionRepository nacosRouteDefinitionRepository() {
    // return new NacosRouteDefinitionRepository();
    // }

    @Bean
    public RefreshRouteApplicationListener refreshRouteApplicationListener() {
        return new RefreshRouteApplicationListener();
    }

    @Bean
    public TimeBasedRouteRefresher timeBasedRouteRefresher() {
        return new TimeBasedRouteRefresher();
    }

    // @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // ??????demo
                .route(p -> p.path("/get").filters(f -> f.addRequestHeader("Hello", "World")).uri("http://httpbin.org:80"))
                // ?????????
                .route(p -> p
                        // ??????????????????
                        /*
                         * .between(ZonedDateTime.of(2020, 7, 8, 9, 0, 0, 0,
                         * ZoneId.systemDefault()), ZonedDateTime.of(2020, 7, 8,
                         * 14, 0, 0, 0, ZoneId.systemDefault())).and()
                         */
                        // .before(ZonedDateTime.of(2020,7,8,9,0,0,0,
                        // ZoneId.systemDefault()))
                        .after(ZonedDateTime.of(2020, 7, 8, 14, 0, 0, 0, ZoneId.systemDefault()))
                        .and()

                        // cookie??????-- ???????????????????????????????????????
                        .cookie("token", "[a-zA-Z]+")
                        .and()

                        // head??????
                        .header("session")
                        .and()

                        // host????????
                        // .host("**.somehost.org").and()
                        // .host("**.wl4g.debug").and()

                        // method??????
                        // .method(HttpMethod.GET, HttpMethod.POST).and()

                        // query??????
                        .query("name")
                        .and()

                        // remote address ??????
                        .remoteAddr("10.0.0.113", "127.0.0.1", "localhost")
                        .and()

                        // ??????
                        .path("/**")
                        // .path("/gateway-example/test/hello")

                        // ??????
                        .filters(f -> f.addRequestHeader("token", "World").addRequestParameter("age", "18").addResponseHeader(
                                "myResponseheader", "myResponseheader"))

                        // ??????
                        .uri("http://localhost:14086"))
                .build();
    }

}
