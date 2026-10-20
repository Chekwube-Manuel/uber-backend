package com.chekwube.uber.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationJob.class);
    private final PaymentService payments;

    public PaymentReconciliationJob(PaymentService payments) { this.payments = payments; }

    @Scheduled(fixedDelayString = "${PAYMENT_RECONCILIATION_DELAY_MS:60000}",
               initialDelayString = "${PAYMENT_RECONCILIATION_INITIAL_DELAY_MS:30000}")
    public void reconcile() {
        try {
            int checked = payments.reconcileStalePayments();
            if (checked > 0) log.info("Payment reconciliation checked {} transactions", checked);
        } catch (RuntimeException exception) {
            log.error("Scheduled payment reconciliation failed", exception);
        }
    }
}
