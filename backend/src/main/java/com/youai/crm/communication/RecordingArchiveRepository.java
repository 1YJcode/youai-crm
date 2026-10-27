package com.youai.crm.communication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.*;
public interface RecordingArchiveRepository extends JpaRepository<RecordingArchive,String> {
    Page<RecordingArchive> findByCallIdOrderByArchivedAtDesc(Long callId,Pageable page);
}
