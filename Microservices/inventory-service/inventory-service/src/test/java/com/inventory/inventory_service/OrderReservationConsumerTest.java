package com.inventory.inventory_service;

import com.inventory.inventory_service.Entity.OutboxEvent;
import com.inventory.inventory_service.Entity.Product;
import com.inventory.inventory_service.Kafka.OrderPlacedKafkaConsumer;
import com.inventory.inventory_service.Kafka.OutboxPublisher;
import com.inventory.inventory_service.Repository.OutboxEventRepository;
import com.inventory.inventory_service.Repository.ProcessedEventRepository;
import com.inventory.inventory_service.Repository.ProductRepository;
import com.inventory.inventory_service.Service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderReservationConsumerTest {
    @Mock ProductRepository products;
    @Mock ProductService productService;
    @Mock ProcessedEventRepository processedEvents;
    @Mock OutboxEventRepository outboxEvents;
    @Mock OutboxPublisher outboxPublisher;
    @InjectMocks OrderPlacedKafkaConsumer consumer;

    private final String event = """
            {"eventId":"evt-1","eventType":"ORDER_PLACED","occurredAt":"2026-09-30T10:00:00Z",
             "orderId":7,"orderNumber":"ORD-7","items":[
               {"productSku":"A","quantity":2},{"productSku":"B","quantity":4}]}
            """;

    @Test
    void insufficientItemRejectsWholeOrderWithoutReservingAnyStock() {
        when(processedEvents.existsByEventId("evt-1")).thenReturn(false);
        when(products.findBySku("A")).thenReturn(Optional.of(Product.builder().id(1L).sku("A").quantity(5).build()));
        when(products.findBySku("B")).thenReturn(Optional.of(Product.builder().id(2L).sku("B").quantity(3).build()));

        consumer.consumeOrderPlacedEvent(event);

        verify(productService, never()).adjustStock(any(), any());
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEvents).save(captor.capture());
        assertEquals("STOCK_REJECTED", captor.getValue().getEventType());
        assertTrue(captor.getValue().getPayload().contains("Insufficient stock"));
        verify(processedEvents).save(any());
    }

    @Test
    void duplicateOrderPlacedEventIsIgnored() {
        when(processedEvents.existsByEventId("evt-1")).thenReturn(true);

        consumer.consumeOrderPlacedEvent(event);

        verify(products, never()).findBySku(any());
        verify(productService, never()).adjustStock(any(), any());
        verify(outboxEvents, never()).save(any());
    }
}
