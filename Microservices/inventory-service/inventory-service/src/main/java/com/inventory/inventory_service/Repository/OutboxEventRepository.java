package com.inventory.inventory_service.Repository;

import com.inventory.inventory_service.Entity.OutboxEvent;
import com.inventory.inventory_service.Entity.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);
    Optional<OutboxEvent> findByEventId(String eventId);
}
