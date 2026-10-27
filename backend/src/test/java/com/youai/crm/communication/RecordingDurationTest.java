package com.youai.crm.communication;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecordingDurationTest {
    @Test void wavDurationComesFromSamplesRatherThanFileNameOrForm() throws Exception {
        var format = new javax.sound.sampled.AudioFormat(8000, 16, 1, true, false);
        var pcm = new byte[40000]; // 2.5 seconds; rounded to 3 seconds in the existing integer field.
        var out = new java.io.ByteArrayOutputStream();
        try (var audio = new javax.sound.sampled.AudioInputStream(new java.io.ByteArrayInputStream(pcm), format, 20000)) {
            javax.sound.sampled.AudioSystem.write(audio, javax.sound.sampled.AudioFileFormat.Type.WAVE, out);
        }
        assertEquals(3, RecordingDuration.seconds(out.toByteArray(), "audio/wav"));
    }
    @Test void invalidAudioCannotSilentlyBecomeZeroDuration() {
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> RecordingDuration.seconds(new byte[]{1,2,3}, "audio/wav"));
    }
}
