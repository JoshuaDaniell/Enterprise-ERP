package com.sales.sales_service.Service;

import com.sales.sales_service.Dto.*;
import com.sales.sales_service.Entity.*;
import com.sales.sales_service.Exception.InvalidOperationException;
import com.sales.sales_service.Exception.ResourceNotFoundException;
import com.sales.sales_service.Kafka.OrderEventProducer;
import com.sales.sales_service.Config.RedisConfig;
import com.sales.sales_service.Repository.CustomerRepository;
import com.sales.sales_service.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderEventProducer orderEventProducer;

    @Transactional
    @CacheEvict(cacheNames = RedisConfig.ORDERS, allEntries = true)
    public OrderResponseDto createOrder(CreateOrderRequestDto dto, String currentUsername) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + dto.getCustomerId()));

        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String shortId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String orderNumber = "ORD-" + datePrefix + "-" + shortId;

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<OrderPlacedEvent.OrderItemEventDto> eventItems = new ArrayList<>();

        for (OrderItemRequestDto itemDto : dto.getItems()) {
            BigDecimal subtotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .productSku(itemDto.getProductSku().trim().toUpperCase())
                    .productName(itemDto.getProductName().trim())
                    .unitPrice(itemDto.getUnitPrice())
                    .quantity(itemDto.getQuantity())
                    .subtotal(subtotal)
                    .build();
            orderItems.add(item);

            eventItems.add(OrderPlacedEvent.OrderItemEventDto.builder()
                    .productSku(item.getProductSku())
                    .productName(item.getProductName())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .subtotal(subtotal)
                    .build());
        }

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .shippingAddress(dto.getShippingAddress())
                .billingAddress(dto.getBillingAddress())
                .notes(dto.getNotes())
                .build();

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        OrderStatusHistory initialHistory = OrderStatusHistory.builder()
                .fromStatus(null)
                .toStatus(OrderStatus.PENDING)
                .remarks("Order created and placed in PENDING status")
                .changedBy(currentUsername != null ? currentUsername : "SYSTEM")
                .build();
        order.addStatusHistory(initialHistory);

        Order savedOrder = orderRepository.save(order);
        log.info("Saved new Order '{}' with total ₹{}", savedOrder.getOrderNumber(), savedOrder.getTotalAmount());

        // Publish ORDER_PLACED event to Kafka
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .eventType("ORDER_PLACED")
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .customerId(customer.getId())
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .totalAmount(savedOrder.getTotalAmount())
                .items(eventItems)
                .placedAt(savedOrder.getOrderDate())
                .build();

        orderEventProducer.publishOrderPlacedEvent(event);

        return mapToResponseDto(savedOrder);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisConfig.ORDERS)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAllWithDetails().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponseDto(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(customerId).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponseDto updateOrderStatus(Long id, UpdateOrderStatusDto dto, String changedBy) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = dto.getStatus();

        if (currentStatus == OrderStatus.CANCELLED || currentStatus == OrderStatus.CONFIRMED) {
            if (currentStatus == newStatus) {
                return mapToResponseDto(order);
            }
            if (currentStatus == OrderStatus.CANCELLED) {
                throw new InvalidOperationException("Cannot update status of an already CANCELLED order");
            }
        }

        order.setStatus(newStatus);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .fromStatus(currentStatus)
                .toStatus(newStatus)
                .remarks(dto.getRemarks() != null ? dto.getRemarks() : "Status updated to " + newStatus)
                .changedBy(changedBy != null ? changedBy : "SYSTEM")
                .build();
        order.addStatusHistory(history);

        Order saved = orderRepository.save(order);
        orderEventProducer.publishOrderStatusChanged(saved.getOrderNumber(), saved.getStatus().name());
        log.info("Order '{}' status updated: {} -> {}", saved.getOrderNumber(), currentStatus, newStatus);
        return mapToResponseDto(saved);
    }

    @Transactional
    public void updateStatusFromInventory(String orderNumber, OrderStatus newStatus, String remarks) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
        if (order.getStatus() == newStatus) {
            return;
        }
        OrderStatus previous = order.getStatus();
        order.setStatus(newStatus);
        order.addStatusHistory(OrderStatusHistory.builder()
                .fromStatus(previous)
                .toStatus(newStatus)
                .remarks(remarks)
                .changedBy("INVENTORY_SERVICE")
                .build());
        orderRepository.save(order);
        orderEventProducer.publishOrderStatusChanged(orderNumber, newStatus.name());
    }

    @Transactional
    public OrderResponseDto cancelOrder(Long id, String reason, String changedBy) {
        UpdateOrderStatusDto dto = UpdateOrderStatusDto.builder()
                .status(OrderStatus.CANCELLED)
                .remarks(reason != null ? reason : "Order cancelled by user")
                .build();
        return updateOrderStatus(id, dto, changedBy);
    }

    public OrderResponseDto mapToResponseDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems() != null
                ? order.getItems().stream().map(item -> OrderItemResponseDto.builder()
                        .id(item.getId())
                        .productSku(item.getProductSku())
                        .productName(item.getProductName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        List<OrderStatusHistoryResponseDto> historyDtos = order.getStatusHistory() != null
                ? order.getStatusHistory().stream().map(h -> OrderStatusHistoryResponseDto.builder()
                        .id(h.getId())
                        .fromStatus(h.getFromStatus())
                        .toStatus(h.getToStatus())
                        .remarks(h.getRemarks())
                        .changedBy(h.getChangedBy())
                        .changedAt(h.getChangedAt())
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomer() != null ? order.getCustomer().getFullName() : "N/A")
                .customerEmail(order.getCustomer() != null ? order.getCustomer().getEmail() : "N/A")
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .billingAddress(order.getBillingAddress())
                .notes(order.getNotes())
                .items(itemDtos)
                .statusHistory(historyDtos)
                .orderDate(order.getOrderDate())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
