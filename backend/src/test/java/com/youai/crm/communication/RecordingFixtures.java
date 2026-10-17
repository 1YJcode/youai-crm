package com.youai.crm.communication;

final class RecordingFixtures {
    static byte[] wav() {
        var b = java.nio.ByteBuffer.allocate(76).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        b.put("RIFF".getBytes()).putInt(68).put("WAVEfmt ".getBytes()).putInt(16);
        b.putShort((short)1).putShort((short)1).putInt(8000).putInt(16000);
        b.putShort((short)2).putShort((short)16).put("data".getBytes()).putInt(32);
        return b.array();
    }
}
