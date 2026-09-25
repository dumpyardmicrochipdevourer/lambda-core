package com.microchip.lambda_core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "lambda.auth")
public record AuthProperties(
        @DefaultValue("http://localhost:8081/.well-known/jwks.json") String jwksUri,
        @DefaultValue("http://localhost:8081") String issuer) {
}
