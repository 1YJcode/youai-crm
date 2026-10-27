package db.migration;

import org.flywaydb.core.api.migration.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import com.fasterxml.jackson.databind.*;

/** Copy existing text only; never read audio, call ASR, or overwrite a previously adopted segment. */
public class V42__BackfillCustomerTranscripts extends BaseJavaMigration {
    private final ObjectMapper json=new ObjectMapper();
    @Override public void migrate(Context context) throws Exception {backfill(context.getConnection());}
    public void backfill(Connection connection) throws Exception {
        for(String table:List.of("crm_recording_transcription","crm_recording_archive")) {
            String column=table.endsWith("archive")?"revision":"id",last="";
            while(true) {
                List<String> ids=new ArrayList<>();
                try(var p=connection.prepareStatement("SELECT "+column+" FROM "+table+" WHERE "+column+">? ORDER BY "+column+" LIMIT 100")) {
                    p.setString(1,last);try(var rows=p.executeQuery()){while(rows.next())ids.add(rows.getString(1));}
                }
                if(ids.isEmpty())break;
                for(String id:ids) {
                    if(table.endsWith("archive")) archived(connection,id);else current(connection,id);
                }
                last=ids.getLast();
            }
        }
        long last=0;
        while(true) {
            List<Long> ids=new ArrayList<>();
            try(var p=connection.prepareStatement("SELECT call_id FROM crm_call_recording WHERE call_id>? AND transcript IS NOT NULL AND analysis_status='COMPLETED' ORDER BY call_id LIMIT 100")) {
                p.setLong(1,last);try(var rows=p.executeQuery()){while(rows.next())ids.add(rows.getLong(1));}
            }
            if(ids.isEmpty())break;
            for(Long id:ids)legacy(connection,id);last=ids.getLast();
        }
    }
    private record Segment(String customerNo,long callId,String revision,String provider,String text,String customerName,
            Long salesId,String salesName,String source,LocalDateTime called,LocalDateTime uploaded,LocalDateTime sourceTime,Integer duration,LocalDateTime started) {}
    private Segment row(ResultSet r,String revision,String provider,String text,LocalDateTime uploaded,Integer duration,LocalDateTime started,String source) throws SQLException {
        LocalDateTime called="SYSTEM".equals(source)?time(r,"started_at"):null;
        return new Segment(r.getString("customer_no"),r.getLong("call_id"),revision,provider,text,r.getString("customer_name"),
                (Long)r.getObject("agent_user_id",Long.class),r.getString("agent"),source,called,uploaded,called==null?uploaded:called,duration,started);
    }
    private void current(Connection c,String id) throws Exception {
        Segment s=null;
        try(var p=c.prepareStatement("SELECT t.revision,t.transcript,t.started,c.id AS call_id,c.customer_no,c.customer_name,c.agent_user_id,c.agent,c.recording_source,c.started_at,c.duration_seconds,r.uploaded_at FROM crm_recording_transcription t JOIN crm_call_recording r ON r.revision=t.revision JOIN crm_call_record c ON c.id=r.call_id JOIN crm_customer u ON u.customer_no=c.customer_no WHERE t.id=? AND t.status='COMPLETED'")) {
            p.setString(1,id);try(var r=p.executeQuery()){if(r.next() && id.startsWith(r.getLong("call_id")+":")) {
                String provider=id.substring(id.indexOf(':')+1);int duration=r.getInt("duration_seconds");
                s=row(r,r.getString("revision"),provider,r.getString("transcript"),time(r,"uploaded_at"),duration>0?duration:null,time(r,"started"),r.getString("recording_source"));
            }}
        }
        insert(c,s);
    }
    private void archived(Connection c,String id) throws Exception {
        Segment s=null;
        try(var p=c.prepareStatement("SELECT a.snapshot,a.uploaded_at,c.id AS call_id,c.customer_no,c.customer_name,c.agent_user_id,c.agent,c.started_at FROM crm_recording_archive a JOIN crm_call_record c ON c.id=a.call_id JOIN crm_customer u ON u.customer_no=c.customer_no WHERE a.revision=?")) {
            p.setString(1,id);try(var r=p.executeQuery()){if(r.next()) {
                var snapshot=json.readTree(r.getString("snapshot"));String provider="legacy",text=snapshot.path("transcript").asText("");LocalDateTime started=null;
                for(String name:List.of("cloud","local")) {
                    var result=snapshot.path(name);
                    if("COMPLETED".equals(result.path("status").asText()) && !result.path("transcript").asText("").isBlank()) {
                        provider=name;text=result.path("transcript").asText();started=parse(result.path("started").asText(null));break;
                    }
                }
                Integer duration=snapshot.path("durationSeconds").isIntegralNumber()?snapshot.path("durationSeconds").asInt():null;
                s=row(r,id,provider,text,time(r,"uploaded_at"),duration,started,snapshot.path("recordingSource").asText("UNKNOWN"));
            }}
        }
        insert(c,s);
    }
    private void legacy(Connection c,long id) throws Exception {
        Segment s=null;
        try(var p=c.prepareStatement("SELECT r.revision,r.transcript,r.uploaded_at,r.analysis_started_at,c.id AS call_id,c.customer_no,c.customer_name,c.agent_user_id,c.agent,c.recording_source,c.started_at,c.duration_seconds FROM crm_call_recording r JOIN crm_call_record c ON c.id=r.call_id JOIN crm_customer u ON u.customer_no=c.customer_no WHERE r.call_id=? AND r.analysis_status='COMPLETED'")) {
            p.setLong(1,id);try(var r=p.executeQuery()){if(r.next()){int duration=r.getInt("duration_seconds");s=row(r,r.getString("revision"),"legacy",r.getString("transcript"),time(r,"uploaded_at"),duration>0?duration:null,time(r,"analysis_started_at"),r.getString("recording_source"));}}
        }
        insert(c,s);
    }
    private void insert(Connection c,Segment s) throws SQLException {
        if(s==null || s.text==null || s.text.isBlank())return;
        LocalDateTime now=LocalDateTime.now();
        try(var p=c.prepareStatement("INSERT IGNORE INTO crm_customer_transcript(customer_no,created_at) VALUES (?,?)")){p.setString(1,s.customerNo);p.setObject(2,now);p.executeUpdate();}
        try(var p=c.prepareStatement("INSERT IGNORE INTO crm_customer_transcript_segment(customer_no,call_id,recording_revision,provider,transcript,customer_name,sales_user_id,sales_name,recording_source,call_started_at,recording_uploaded_at,source_time,duration_seconds,transcription_started_at,transcribed_at,added_at,source_article_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,NULL,?,NULL)")) {
            Object[] values={s.customerNo,s.callId,s.revision,s.provider,s.text,s.customerName,s.salesId,s.salesName,s.source,s.called,s.uploaded,s.sourceTime,s.duration,s.started,now};
            for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);p.executeUpdate();
        }
    }
    private static LocalDateTime time(ResultSet r,String column)throws SQLException{var t=r.getTimestamp(column);return t==null?null:t.toLocalDateTime();}
    private static LocalDateTime parse(String value){return value==null?null:LocalDateTime.parse(value);}
}
