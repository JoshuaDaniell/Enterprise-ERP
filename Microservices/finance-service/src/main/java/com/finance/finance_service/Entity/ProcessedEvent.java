package com.finance.finance_service.Entity;
import jakarta.persistence.*; import lombok.*; import java.time.Instant;
@Entity @Table(name="processed_events", indexes=@Index(name="uk_processed_event_id", columnList="eventId", unique=true))
@Getter @Setter @NoArgsConstructor @Builder @AllArgsConstructor
public class ProcessedEvent { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(nullable=false, unique=true) private String eventId; @Column(nullable=false) @Builder.Default private Instant processedAt=Instant.now(); }
