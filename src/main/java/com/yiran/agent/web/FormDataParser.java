package com.yiran.agent.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.util.ByteProcessor;
import io.netty.util.internal.AppendableCharSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public class FormDataParser implements ByteProcessor {
    private static Logger logger = LoggerFactory.getLogger(FormDataParser.class);
    private static final byte CHAR_AND = 38;  // &符号
    private static final byte CHAR_EQUAL = 61;  // =号

    private boolean nextIsValue = false;
    private int size;
    private ByteBuf seq;
    private final int maxLength;

    public FormDataParser(int maxLength){
        this.maxLength = maxLength;
        this.seq = PooledByteBufAllocator.DEFAULT.buffer(64);
    }

    public String parseInterface(ByteBuf buffer) throws UnsupportedEncodingException {

        int oldReaderIndex = buffer.readerIndex();
        int k = 0;
        this.nextIsValue = false;
        size = 0;
        String key = null;
        String value;
        while(true){
            seq.clear();
            int i = buffer.forEachByte(this);
            k++;
            if (k % 2 == 1) {
                /*key*/
                key = URLDecoder.decode(seq.toString(Charset.forName("utf-8")), "utf-8");
                if (key == null) {
                    logger.error("Failed to parse key!");
                }
            } else if (k % 2 == 0) {
                /*value*/
                value = URLDecoder.decode(seq.toString(Charset.forName("utf-8")), "utf-8");
                if ("interface".equals(key)) {
                    buffer.readerIndex(oldReaderIndex);  // 恢复buffer
                    return value;
                }
            }
            if (i == -1) {
                buffer.readerIndex(oldReaderIndex);  // 恢复buffer
                return null;
            }
            buffer.readerIndex(i + 1);
        }

    }

    public Map<String, String> parse(ByteBuf buffer) throws UnsupportedEncodingException {
        Map<String, String> parameterMap = new HashMap<>();

        int oldReaderIndex = buffer.readerIndex();
        int k = 0;
        this.nextIsValue = false;
        size = 0;
        String key = null;
        String value = null;
        while(true){
            seq.clear();
            int i = buffer.forEachByte(this);
            k++;
            if (k % 2 == 1) {
                /*key*/
                key = URLDecoder.decode(seq.toString(Charset.forName("utf-8")), "utf-8");
                if (key == null) {
                    logger.error("Failed to parse key!");
                }
            } else if (k % 2 == 0) {
                /*value*/
                value = URLDecoder.decode(seq.toString(Charset.forName("utf-8")), "utf-8");
                if (value == null && this.nextIsValue) {
                    parameterMap.put(key, "");
                } else {
                    parameterMap.put(key, value);
                }
            }
            if (i == -1) {
                buffer.readerIndex(oldReaderIndex);  // 恢复buffer
                return parameterMap;
            }
            buffer.readerIndex(i + 1);
        }
    }

    @Override
    public boolean process(byte value) throws Exception {
        byte nextByte = value;
        if (nextByte == CHAR_EQUAL) {
            this.nextIsValue = true;
            return false;
        }
        if (nextByte == CHAR_AND) {
            this.nextIsValue = false;
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

        seq.writeByte(nextByte);
        return true;
    }

    protected TooLongFrameException newException(int maxLength) {
        return new TooLongFrameException("HTTP content is larger than " + maxLength + " bytes.");
    }
}
