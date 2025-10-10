package com.lcaohoanq.gatewayserver.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter  implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        System.out.println("🌐 [Gateway] Incoming request: " + request.getMethod() + " " + request.getURI());
        log.info("🌐 [Gateway] Incoming request: {} {}", request.getMethod(), request.getURI());

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null) {
            log.info("➡ Routing to - Route ID:{} - Route Uri:{}", route.getId(), route.getUri());
        } else {
            log.warn("⚠ No route found for request: {} {}", request.getMethod(), request.getURI());
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
