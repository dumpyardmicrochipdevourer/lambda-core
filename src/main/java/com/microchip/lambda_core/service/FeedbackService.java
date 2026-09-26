package com.microchip.lambda_core.service;

import com.microchip.lambda_core.domain.FeedbackMessage;
import com.microchip.lambda_core.domain.dto.FeedbackView;
import com.microchip.lambda_core.domain.repo.FeedbackMessageRepository;
import com.microchip.lambda_core.service.exceptions.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedbackService {

    static final int MAX_CHARS = 8192;
    private static final int PER_HOUR = 5;
    private static final int LIST_LIMIT = 200;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final FeedbackMessageRepository repository;
    private final Map<String, Deque<Instant>> recent = new ConcurrentHashMap<>();

    public FeedbackService(FeedbackMessageRepository repository) {
        this.repository = repository;
    }

    public void submit(String sender, String message, String contact) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("сообщение пустое");
        }
        String body = message.strip() + "\n\n-- \n" + (contact == null || contact.isBlank() ? "—" : contact.strip());
        if (body.length() > MAX_CHARS) {
            throw new IllegalArgumentException("сообщение длиннее " + MAX_CHARS + " символов");
        }
        throttle(sender);
        repository.save(new FeedbackMessage(body));
    }

    public List<FeedbackView> list() {
        return repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, LIST_LIMIT)).stream()
                .map(FeedbackView::of).toList();
    }

    @Transactional
    public void markRead(long id) {
        repository.findById(id).filter(m -> m.getReadAt() == null).ifPresent(FeedbackMessage::markRead);
    }

    private void throttle(String sender) {
        Instant now = Instant.now();
        recent.values().removeIf(q -> { synchronized (q) { return !q.isEmpty() && q.peekLast().isBefore(now.minus(WINDOW)); } });
        Deque<Instant> times = recent.computeIfAbsent(sender, s -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(WINDOW))) {
                times.pollFirst();
            }
            if (times.size() >= PER_HOUR) {
                throw new TooManyRequestsException();
            }
            times.addLast(now);
        }
    }
}
