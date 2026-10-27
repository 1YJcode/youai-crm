package com.youai.crm.communication;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="crm_recording_archive")
public class RecordingArchive {
    @Id @Column(length=36) String revision;
    @Column(nullable=false) Long callId;
    @Column(nullable=false,length=64) String mediaType;
    @Column(nullable=false,length=16) String storageBackend;
    @Column(nullable=false,length=512) String storageKey;
    @Column(nullable=false) long audioSize;
    @Column(nullable=false,length=64) String audioSha256;
    @Column(nullable=false) LocalDateTime uploadedAt;
    @Column(nullable=false) LocalDateTime archivedAt;
    @Column(nullable=false,columnDefinition="LONGTEXT") String snapshot;
    protected RecordingArchive() {}
    CallRecording recording() {
        var audio=new CallRecording(callId,mediaType,null);
        audio.externalize(storageBackend,storageKey,audioSize,audioSha256); return audio;
    }
}
