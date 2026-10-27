package com.youai.crm.communication;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class RecordingAutomation {
    private final RecordingQueue queue;private final CallRecordingRepository recordings;
    private final QwenTranscriptionClient cloud;private final LocalTranscriptionClient local;
    public RecordingAutomation(RecordingQueue queue,CallRecordingRepository recordings,QwenTranscriptionClient cloud,LocalTranscriptionClient local){this.queue=queue;this.recordings=recordings;this.cloud=cloud;this.local=local;}
    public void validate(boolean automatic,boolean consent){
        if(!automatic)return;
        if(!consent)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请确认上传后自动转写和DeepSeek分析授权");
        if(!cloud.configured()&&!local.configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"管理员尚未配置自动转写服务");
    }
    public void enqueue(Long id,boolean automatic,boolean consent,Authentication auth){
        if(!automatic)return;validate(true,consent);
        queue.enqueue(id,recordings.findRevision(id).orElseThrow(),cloud.configured()?"cloud":"local",auth,true);
    }
}
