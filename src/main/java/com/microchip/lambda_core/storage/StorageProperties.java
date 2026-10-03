package com.microchip.lambda_core.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "lambda.storage")
public record StorageProperties(
        @DefaultValue("/srv/lambda") String root,
        @DefaultValue("2147483648") long maxFileBytes, // 2gb
        @DefaultValue("2684354560") long quotaDefaultBytes, // 2.5gb
        @DefaultValue("5368709120") long shareTotalBytes, // 5gb
        @DefaultValue("1073741824") long minFreeBytes) { // 1gb
}
