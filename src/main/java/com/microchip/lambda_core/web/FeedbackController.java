package com.microchip.lambda_core.web;

import com.microchip.lambda_core.domain.dto.FeedbackRequest;
import com.microchip.lambda_core.domain.dto.FeedbackView;
import com.microchip.lambda_core.service.FeedbackService;
import com.microchip.lambda_core.service.exceptions.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submit(@RequestBody FeedbackRequest request, HttpServletRequest http) {
        feedbackService.submit(sender(http), request.message(), request.contact());
    }

    @GetMapping
    public List<FeedbackView> list() {
        return feedbackService.list();
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable long id) {
        feedbackService.markRead(id);
    }

    // core is only reachable through the web gateway, which sets X-Real-IP
    private static String sender(HttpServletRequest http) {
        String real = http.getHeader("X-Real-IP");
        return real != null && !real.isBlank() ? real : http.getRemoteAddr();
    }

    @ExceptionHandler(TooManyRequestsException.class)
    ProblemDetail handleTooMany(TooManyRequestsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ProblemDetail handleBadRequest(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
