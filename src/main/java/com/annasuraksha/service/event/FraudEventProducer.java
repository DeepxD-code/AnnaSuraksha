package com.annasuraksha.service.event;

import com.annasuraksha.model.event.FraudEvent;
import com.annasuraksha.service.alert.AlertService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FraudEventProducer {

    private final ApplicationEventPublisher eventPublisher;

    public void publishFraudDetected(FraudEvent event) {
        log.info("Publishing fraud event: {} for beneficiary {} | risk={}",
            event.eventType(), event.beneficiaryId(), event.riskLevel());
        eventPublisher.publishEvent(event);
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FraudEventProducer.class);
    public FraudEventProducer(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }
}
