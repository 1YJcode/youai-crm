package com.youai.crm.communication;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

/** Private immutable objects. Database rows contain keys, never public URLs. */
@Service
public class RecordingStorage {
    private final String backend, bucket;
    private final Path root;
    private final S3Client s3;
    private final S3Presigner signer;

    public RecordingStorage(@Value("${recording.storage.backend:file}") String backend,
            @Value("${recording.storage.directory:./data/recordings}") String directory,
            @Value("${recording.storage.s3.bucket:}") String bucket,
            @Value("${recording.storage.s3.endpoint:}") String endpoint,
            @Value("${recording.storage.s3.region:us-east-1}") String region,
            @Value("${recording.storage.s3.access-key:}") String accessKey,
            @Value("${recording.storage.s3.secret-key:}") String secretKey,
            @Value("${recording.storage.s3.path-style:false}") boolean pathStyle) {
        if (!backend.equals("file") && !backend.equals("s3")) throw new IllegalArgumentException("Unknown recording storage backend");
        this.backend=backend; this.bucket=bucket; root=Path.of(directory).toAbsolutePath().normalize();
        if (backend.equals("s3")) {
            if(bucket.isBlank()) throw new IllegalArgumentException("Recording S3 bucket is required");
            var credentials=accessKey.isBlank()?DefaultCredentialsProvider.create():
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey,secretKey));
            var config=S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build();
            var client=S3Client.builder().region(Region.of(region)).credentialsProvider(credentials).serviceConfiguration(config)
                    .overrideConfiguration(c -> c.apiCallTimeout(Duration.ofMinutes(5)).apiCallAttemptTimeout(Duration.ofMinutes(2)));
            var presigner=S3Presigner.builder().region(Region.of(region)).credentialsProvider(credentials).serviceConfiguration(config);
            if(!endpoint.isBlank()) { client.endpointOverride(URI.create(endpoint)); presigner.endpointOverride(URI.create(endpoint)); }
            s3=client.build(); signer=presigner.build();
        } else { s3=null; signer=null; }
    }
    public String backend() { return backend; }
    public String presignUpload(String key,long size) {
        if(signer==null) return null;
        return signer.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).contentLength(size)
                        .contentType("application/octet-stream").build()).build()).url().toString();
    }
    public void receive(String key,long size,Path target) throws IOException {
        if(s3==null) throw new IOException("Direct upload requires S3");
        var head=s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
        if(head.contentLength()!=size || size<=0 || size>CallRecordingController.MAX_BYTES) throw new IOException("Upload size mismatch");
        try(var input=s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build()); var output=Files.newOutputStream(target)) {
            byte[] buffer=new byte[65536]; long actual=0; int count;
            while((count=input.read(buffer))!=-1) {
                actual+=count; if(actual>size) throw new IOException("Upload size mismatch");
                output.write(buffer,0,count);
            }
            if(actual!=size) throw new IOException("Upload size mismatch");
        }
    }
    private Path file(String key) {
        if(key==null || !key.matches("recordings/[0-9]+/[a-f0-9-]{36}")) throw new IllegalArgumentException("Invalid recording key");
        Path path=root.resolve(key).normalize();
        if(!path.startsWith(root)) throw new IllegalArgumentException("Invalid recording path");
        return path;
    }
    public void put(String key, Path source, String mediaType) throws IOException {
        if(backend.equals("s3")) {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(mediaType).build(),RequestBody.fromFile(source));
        } else {
            Path target=file(key); Files.createDirectories(target.getParent());
            Path temporary=Files.createTempFile(target.getParent(),"upload-",".part");
            try {
                Files.copy(source,temporary,StandardCopyOption.REPLACE_EXISTING);
                Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE);
            } finally { Files.deleteIfExists(temporary); }
        }
    }
    public InputStream open(CallRecording recording) throws IOException {
        if(recording.getStorageKey()==null) return new ByteArrayInputStream(recording.getAudio());
        if("file".equals(recording.getStorageBackend())) return Files.newInputStream(file(recording.getStorageKey()));
        if(s3==null) throw new IOException("S3 recording storage is not configured");
        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(recording.getStorageKey()).build());
    }
    public byte[] read(CallRecording recording) throws IOException {
        try(var stream=open(recording)) {
            byte[] bytes=stream.readNBytes(CallRecordingController.MAX_BYTES+1);
            if(bytes.length>CallRecordingController.MAX_BYTES || bytes.length!=recording.getAudioSize()) throw new IOException("Recording size mismatch");
            if(recording.getAudioSha256()!=null && !sha256(bytes).equals(recording.getAudioSha256())) throw new IOException("Recording checksum mismatch");
            return bytes;
        }
    }
    public void verify(CallRecording recording) throws IOException {
        try(var stream=open(recording)) {
            var digest=MessageDigest.getInstance("SHA-256");
            byte[] buffer=new byte[65536]; long size=0; int count;
            while((count=stream.read(buffer))!=-1) {
                size+=count;
                if(size>CallRecordingController.MAX_BYTES) throw new IOException("Recording too large");
                digest.update(buffer,0,count);
            }
            if(size!=recording.getAudioSize() || !HexFormat.of().formatHex(digest.digest()).equals(recording.getAudioSha256()))
                throw new IOException("Recording verification failed");
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public org.springframework.core.io.Resource resource(CallRecording recording) throws IOException {
        if(recording.getStorageKey()==null) return new org.springframework.core.io.ByteArrayResource(recording.getAudio());
        if("file".equals(recording.getStorageBackend())) return new org.springframework.core.io.FileSystemResource(file(recording.getStorageKey()));
        return new org.springframework.core.io.InputStreamResource(open(recording)) {
            @Override public long contentLength() { return recording.getAudioSize(); }
        };
    }
    public String playbackUrl(CallRecording recording) {
        // Playback always uses the authenticated CRM endpoint. Do not hand out bearer URLs
        // that could bypass current customer ownership checks when copied to another user.
        return null;
    }
    public void delete(String key) throws IOException {
        if(backend.equals("s3")) s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        else Files.deleteIfExists(file(key));
    }
    public static String sha256(byte[] audio) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(audio)); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public static String sha256(Path file) throws IOException {
        try {
            var hash=MessageDigest.getInstance("SHA-256");
            try(var stream=Files.newInputStream(file)) {
                byte[] buffer=new byte[65536]; int count;
                while((count=stream.read(buffer))!=-1) hash.update(buffer,0,count);
            }
            return HexFormat.of().formatHex(hash.digest());
        } catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @jakarta.annotation.PreDestroy void close() { if(s3!=null) s3.close(); if(signer!=null) signer.close(); }
}
