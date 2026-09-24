package com.microchip.lambda_core.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "lambda.storage")
public record StorageProperties(
        @DefaultValue("/srv/lambda") String root,
        @DefaultValue("2147483648") long maxFileBytes) { // 2gb
}
