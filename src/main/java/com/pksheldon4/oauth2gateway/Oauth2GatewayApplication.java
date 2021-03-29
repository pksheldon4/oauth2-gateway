package com.pksheldon4.oauth2gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.factory.TokenRelayGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class Oauth2GatewayApplication {

    @Value("${ROUTING_URL:http://localhost:8080}")
    private String ROUTING_URL;

    public static void main(String[] args) {
        SpringApplication.run(Oauth2GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, TokenRelayGatewayFilterFactory tokenRelay) {
        return builder.routes()
            .route("api", r -> r.path(true, "/**")
                .filters(f -> f.filter(tokenRelay.apply()))
                .uri(ROUTING_URL))
            .build();
    }

    @Bean
    public WebFilter csrfResponseHeaderFilter() {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            String key = CsrfToken.class.getName();
            Mono<CsrfToken> csrfToken = exchange.getAttributes().containsKey(key) ?
                exchange.getAttribute(key) : Mono.empty();
            return csrfToken
                .doOnSuccess(token -> headers.set(token.getHeaderName(), token.getToken()))
                .then(chain.filter(exchange));
        };
    }
}
