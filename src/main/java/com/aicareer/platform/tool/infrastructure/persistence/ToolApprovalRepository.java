package com.aicareer.platform.tool.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import com.aicareer.platform.tool.domain.ToolApproval;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ToolApprovalRepository extends JpaRepository<ToolApproval, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select approval
            from ToolApproval approval
            where approval.tokenHash = :tokenHash
            """)
    Optional<ToolApproval> findForUpdate(@Param("tokenHash") String tokenHash);
}
