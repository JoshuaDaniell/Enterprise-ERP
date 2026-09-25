package com.finance.finance_service.Repository;
import com.finance.finance_service.Entity.ProcessedEvent; import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent,Long> { boolean existsByEventId(String eventId); }
