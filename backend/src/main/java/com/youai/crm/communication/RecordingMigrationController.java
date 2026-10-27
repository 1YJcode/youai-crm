package com.youai.crm.communication;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/recording-storage")
public class RecordingMigrationController {
    private final CallRecordingRepository recordings;
    private final RecordingStorage storage;
    public RecordingMigrationController(CallRecordingRepository recordings,RecordingStorage storage) {
        this.recordings=recordings; this.storage=storage;
    }
    public record Report(int migrated,int skipped) {}
    @PostMapping("/migrate") @PreAuthorize("hasRole('ADMIN')")
    public synchronized Report migrate(@RequestParam(defaultValue="5") int limit) throws IOException {
        int migrated=0,skipped=0;
        for(Long id:recordings.legacyIds(storage.backend(),PageRequest.of(0,Math.max(1,Math.min(limit,20))))) {
            var audio=recordings.findById(id).orElse(null);
            if(audio==null || storage.backend().equals(audio.getStorageBackend())) { skipped++; continue; }
            String key="recordings/"+id+"/"+UUID.randomUUID();
            Path temporary=Files.createTempFile("youai-migrate-",".audio");
            boolean updating=false; int outcome=-1;
            try {
                try(var input=storage.open(audio); var output=Files.newOutputStream(temporary)) { input.transferTo(output); }
                long size=Files.size(temporary); String hash=RecordingStorage.sha256(temporary);
                storage.put(key,temporary,audio.getMediaType());
                var check=new CallRecording(id,audio.getMediaType(),null);
                check.externalize(storage.backend(),key,size,hash);
                storage.verify(check); // Verify the stored bytes before removing the database copy.
                updating=true;
                outcome=recordings.externalize(id,audio.getRevision(),storage.backend(),key,size,hash);
                if(outcome>0) migrated++; else skipped++;
            } finally {
                Files.deleteIfExists(temporary);
                // If commit acknowledgement is lost, retain the object rather than risk deleting live data.
                if(!updating || outcome==0) storage.delete(key);
            }
        }
        return new Report(migrated,skipped);
    }
}
