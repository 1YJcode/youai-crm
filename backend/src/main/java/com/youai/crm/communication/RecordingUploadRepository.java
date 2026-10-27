package com.youai.crm.communication;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface RecordingUploadRepository extends JpaRepository<RecordingUpload,String> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from RecordingUpload u where u.id=:id")
    Optional<RecordingUpload> lockById(String id);
    java.util.List<RecordingUpload> findByExpiresAtBefore(java.time.LocalDateTime now,org.springframework.data.domain.Pageable page);
}
