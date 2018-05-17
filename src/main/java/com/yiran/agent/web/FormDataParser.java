package com.yiran.agent.web;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.AppendableCharSequence;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class FormDataParser implements ByteProcessor {
    private static final byte CHAR_AND = 38;  // &符号
    private static final byte CHAR_EQUAL = 61;  // =号

    private int size;
    private AppendableCharSequence seq;
    private final int maxLength;

    public FormDataParser(int maxLength){
        this.maxLength = maxLength;
        this.seq = new AppendableCharSequence(maxLength);
    }

    public Map<String, String> parse(ByteBuf buffer) throws UnsupportedEncodingException {
        Map<String, String> parameterMap = new HashMap<>();

        int oldReaderIndex = buffer.readerIndex();
        int k = 0;
        size = 0;
        String key = null;
        String value = null;
        while(buffer.readableBytes() > 0){
            seq.reset();
            int i = buffer.forEachByte(this);
            k++;
            if (k % 2 == 1) {
                key = URLDecoder.decode(seq.toString(), "utf-8");
            } else if (k % 2 == 0) {
                value = URLDecoder.decode(seq.toString(), "utf-8");
                parameterMap.put(key, value);
            }
            if (i == -1) {
                buffer.readerIndex(oldReaderIndex);  // 恢复buffer
                buffer.writerIndex(buffer.writerIndex() - 1);
                return parameterMap;
            }
            buffer.readerIndex(i + 1);
        }

        return parameterMap;
    }

    @Override
    public boolean process(byte value) throws Exception {
        char nextByte = (char) (value & 0xFF);
        if (nextByte == CHAR_EQUAL) {
            return false;
        }
        if (nextByte == CHAR_AND) {
            return false;
        }
        if (nextByte == HttpConstants.CR) {
            return false;
        }
        if (nextByte == HttpConstants.LF) {
            return false;
        }

        if (++ size > maxLength) {
            throw newException(maxLength);
        }

        seq.append(nextByte);
        return true;
    }

    protected TooLongFrameException newException(int maxLength) {
        return new TooLongFrameException("HTTP content is larger than " + maxLength + " bytes.");
    }
}
