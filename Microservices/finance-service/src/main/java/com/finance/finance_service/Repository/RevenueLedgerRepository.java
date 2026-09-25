package com.finance.finance_service.Repository;
import com.finance.finance_service.Entity.RevenueLedgerEntry; import org.springframework.data.jpa.repository.JpaRepository;
public interface RevenueLedgerRepository extends JpaRepository<RevenueLedgerEntry,Long> { boolean existsByReference(String reference); }
