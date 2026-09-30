package com.sales.sales_service;

import com.sales.sales_service.Entity.OrderStatus;
import com.sales.sales_service.Kafka.InventoryReservationConsumer;
import com.sales.sales_service.Repository.ProcessedEventRepository;
import com.sales.sales_service.Service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReservationConsumerTest {
    @Mock OrderService orderService;
    @Mock ProcessedEventRepository processedEvents;
    @Spy ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    @InjectMocks InventoryReservationConsumer consumer;

    @Test
    void stockReservedAutomaticallyConfirmsOrder() throws Exception {
        when(processedEvents.existsByEventId("reserved-1")).thenReturn(false);
        consumer.consumeReserved(mapper.writeValueAsString(new Event("reserved-1", "STOCK_RESERVED", "2026-09-30T10:00:00Z", "ORD-1")));
        verify(orderService).updateStatusFromInventory("ORD-1", OrderStatus.CONFIRMED,
                "Stock reserved by Inventory. Order automatically CONFIRMED.", "reserved-1");
    }

    @Test
    void stockRejectedSetsRejectedStatus() throws Exception {
        when(processedEvents.existsByEventId("rejected-1")).thenReturn(false);
        consumer.consumeRejected(mapper.writeValueAsString(new Event("rejected-1", "STOCK_REJECTED", "2026-09-30T10:00:00Z", "ORD-2")));
        verify(orderService).updateStatusFromInventory("ORD-2", OrderStatus.REJECTED,
                "Stock unavailable in Inventory. Order REJECTED.", "rejected-1");
    }

    @Test
    void duplicateResultDoesNotUpdateOrderAgain() throws Exception {
        when(processedEvents.existsByEventId("reserved-1")).thenReturn(true);
        consumer.consumeReserved(mapper.writeValueAsString(new Event("reserved-1", "STOCK_RESERVED", "2026-09-30T10:00:00Z", "ORD-1")));
        verify(orderService, never()).updateStatusFromInventory(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private record Event(String eventId, String eventType, String occurredAt, String orderNumber) { }
}
