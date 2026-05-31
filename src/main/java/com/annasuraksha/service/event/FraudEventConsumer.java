package com.annasuraksha.service.event;

import com.annasuraksha.model.event.FraudEvent;
import com.annasuraksha.service.alert.AlertService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class FraudEventConsumer {

    private final AlertService alertService;

    @Async
    @EventListener
    public void onFraudDetected(FraudEvent event) {
        log.info("[FRAUD-CONSUMER] event={} | beneficiary={} | level={}",
            event.eventId(), event.beneficiaryId(), event.riskLevel());
        if ("HIGH".equals(event.riskLevel())) {
            alertService.triggerFraudAlert(event);
        }
    }
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FraudEventConsumer.class);
    public FraudEventConsumer(AlertService alertService) {
        this.alertService = alertService;
    }
}
