package com.microchip.lambda_core.config;

import com.microchip.lambda_core.domain.repo.FeedbackMessageRepository;
import com.microchip.lambda_core.domain.repo.ShareRepository;
import com.microchip.lambda_core.domain.repo.UserStorageRepository;
import com.microchip.lambda_core.storage.StorageBudget;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.stereotype.Component;

@Component
public class StorageMetrics implements MeterBinder {

    private final StorageBudget budget;
    private final ShareRepository shares;
    private final UserStorageRepository accounts;
    private final FeedbackMessageRepository feedback;

    public StorageMetrics(
            StorageBudget budget,
            ShareRepository shares,
            UserStorageRepository accounts,
            FeedbackMessageRepository feedback) {
        this.budget = budget;
        this.shares = shares;
        this.accounts = accounts;
        this.feedback = feedback;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("lambda.share.used", budget, StorageBudget::shareUsed).baseUnit("bytes").register(registry);
        Gauge.builder("lambda.share.limit", budget, StorageBudget::shareLimit).baseUnit("bytes").register(registry);
        Gauge.builder("lambda.disk.free", budget, StorageBudget::diskFree).baseUnit("bytes").register(registry);
        Gauge.builder("lambda.disk.floor", budget, StorageBudget::minFree).baseUnit("bytes").register(registry);
        Gauge.builder("lambda.shares", shares, ShareRepository::count).register(registry);
        Gauge.builder("lambda.personal.used", accounts, UserStorageRepository::sumTaken).baseUnit("bytes").register(registry);
        Gauge.builder("lambda.personal.quota", accounts, UserStorageRepository::sumQuota).baseUnit("bytes").register(registry);
        Gauge.builder("lambda.personal.accounts", accounts, UserStorageRepository::count).register(registry);
        Gauge.builder("lambda.feedback.unread", feedback, FeedbackMessageRepository::countByReadAtIsNull).register(registry);
    }
}
