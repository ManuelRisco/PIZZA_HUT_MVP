package com.example.events;

import com.example.services.StockAlertNotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderAndStockNotificationListener {

    private final StockAlertNotificationService stockAlertNotificationService;

    public OrderAndStockNotificationListener(StockAlertNotificationService stockAlertNotificationService) {
        this.stockAlertNotificationService = stockAlertNotificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onStockChanged(StockChangedNotificationEvent event) {
        stockAlertNotificationService.synchronizeAlertsAsync();
    }
}
