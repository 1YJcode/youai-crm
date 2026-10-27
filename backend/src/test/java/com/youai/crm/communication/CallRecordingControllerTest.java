package com.youai.crm.communication;

import com.youai.crm.customer.CustomerAccessPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CallRecordingControllerTest {
    private final CallRecordRepository calls = mock(CallRecordRepository.class);
    private final CallRecordingRepository recordings = mock(CallRecordingRepository.class);
    private final CustomerAccessPolicy access = mock(CustomerAccessPolicy.class);
    private final TestingAuthenticationToken user = new TestingAuthenticationToken("sales", "unused");
    private final RecordingStorage storage = mock(RecordingStorage.class);
    private final CallRecordingController controller = new CallRecordingController(calls, recordings, access, storage,
            mock(RecordingHistory.class),mock(RecordingArchiveRepository.class),mock(RecordingAutomation.class));

    private void call() {
        var call = new CallRecord(); call.setCustomerNo("1001");
        when(calls.findById(1L)).thenReturn(Optional.of(call));
        when(calls.lockById(1L)).thenReturn(Optional.of(call));
    }

    @Test
    void storesAudioAndReadsWithCurrentCustomerPermission() throws Exception {
        call();
        byte[] audio = RecordingFixtures.wav();
        when(storage.backend()).thenReturn("file");
        when(storage.read(any())).thenReturn(audio);
        when(storage.resource(any())).thenReturn(new org.springframework.core.io.ByteArrayResource(audio));
        when(recordings.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var info = controller.upload(1L, new MockMultipartFile("file", "../../fake.html", "text/html", audio), user);
        assertEquals("audio/wav", info.mediaType());
        assertEquals(audio.length, info.size());
        when(recordings.findById(1L)).thenReturn(Optional.of(new CallRecording(1L, info.mediaType(), audio)));
        var result = controller.download(1L, user, null);
        assertArrayEquals(audio, result.getBody().getInputStream().readAllBytes());
        assertEquals("no-store", result.getHeaders().getCacheControl());
        verify(access, times(3)).requireAccess("1001", user);
        // A transfer revokes access even when a recording still exists.
        doThrow(new AccessDeniedException("transferred")).when(access).requireAccess("1001", user);
        assertThrows(AccessDeniedException.class, () -> controller.download(1L, user, null));
        verify(recordings, times(2)).findById(1L);
    }

    @Test
    void rejectsEmptyAndDisguisedNonAudio() {
        call();
        assertThrows(ResponseStatusException.class, () -> controller.upload(1L,
                new MockMultipartFile("file", new byte[0]), user));
        var error = assertThrows(ResponseStatusException.class, () -> controller.upload(1L,
                new MockMultipartFile("file", "call.mp3", "audio/mpeg", "<script>alert(1)</script>".getBytes()), user));
        assertEquals(415, error.getStatusCode().value());
        verifyNoInteractions(recordings);
    }

    @Test
    void rejectsTruncatedWavEvenWithCorrectFileSignature() {
        byte[] truncated = java.util.Arrays.copyOf(RecordingFixtures.wav(), 48);
        assertThrows(ResponseStatusException.class, () -> CallRecordingController.detectMediaType(truncated));
        assertThrows(ResponseStatusException.class, () -> CallRecordingController.detectMediaType("RIFF0000WAVEgarbage".getBytes()));
    }

    @Test
    void refusesUnauthorizedUploadsBeforeReadingFile() {
        call();
        doThrow(new AccessDeniedException("forbidden")).when(access).requireAccess("1001", user);
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        assertThrows(AccessDeniedException.class, () -> controller.upload(1L, file, user));
        verifyNoInteractions(file, recordings);
    }

    @Test
    void rejectsOversizeBeforeReadingContent() {
        call();
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.getSize()).thenReturn((long) CallRecordingController.MAX_BYTES + 1);
        var error = assertThrows(ResponseStatusException.class, () -> controller.upload(1L, file, user));
        assertEquals(413, error.getStatusCode().value());
        verifyNoInteractions(recordings);
    }
}
