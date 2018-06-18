package com.yiran.dubbo;

import com.yiran.dubbo.model.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class DubboRpcEncoder extends MessageToByteEncoder {
    private static Logger logger = LoggerFactory.getLogger(DubboRpcEncoder.class);

    // header length.
    protected static final int HEADER_LENGTH = 4;
    // magic header.
    protected static final short MAGIC = (short) 0xdabb;
    // message flag.
    protected static final byte FLAG_REQUEST = (byte) 0x80;
    protected static final byte FLAG_TWOWAY = (byte) 0x40;
    protected static final byte FLAG_EVENT = (byte) 0x20;
    protected static final String DUBBO_VERSION = "2.6.0";
    protected static final String PATH = "com.alibaba.dubbo.performance.demo.provider.IHelloService";
    protected static final String SERVICE_VERSION = "0.0.0";
    protected static final String METHOD_NAME = "hash";
    protected static final String PARAMETER_TYPES = "Ljava/lang/String;";


    private byte[] header;
    private byte[] parambuf;
    private byte[] attachBuf;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        Map<String, String> attachment = new HashMap<>();

        header = new byte[HEADER_LENGTH];

        // set magic number.
        Bytes.short2bytes(MAGIC, header);

        // set request and serialization flag.
        header[2] = (byte) (FLAG_REQUEST | 6);

        header[2] |= FLAG_TWOWAY;

        attachment.put("path", PATH);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

        JsonUtils.writeObject(DUBBO_VERSION, writer);
        JsonUtils.writeObject(PATH, writer);
        JsonUtils.writeObject(SERVICE_VERSION, writer);
        JsonUtils.writeObject(METHOD_NAME, writer);
        JsonUtils.writeObject(PARAMETER_TYPES, writer);

        parambuf = out.toByteArray();
        out.reset();

        JsonUtils.writeObject(attachment, writer);

        attachBuf = out.toByteArray();

        writer.close();
        out.close();

        super.handlerAdded(ctx);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception {
        if (msg instanceof HardRequest) {
            hardEncode(ctx, (HardRequest) msg, buffer);
        } else if (msg instanceof Request) {
            softEncode(ctx, msg, buffer);
        }
    }

    private void hardEncode(ChannelHandlerContext ctx, HardRequest req, ByteBuf out) {
        long reqId = req.getReqId();
        int len = parambuf.length + req.getParameter().readableBytes() + 3 + attachBuf.length;

        out.writeBytes(header);
        out.writeLong(reqId);
        out.writeInt(len);
        out.writeBytes(parambuf);
        out.writeByte('\"');
        out.writeBytes(req.getParameter());
        out.writeByte('\"');
        out.writeByte('\n');
        out.writeBytes(attachBuf);

    }

    private void softEncode(ChannelHandlerContext ctx, Object msg, ByteBuf buffer) throws Exception{
        logger.info("SoftEncoder");

        Request req = (Request)msg;

        // header.
        byte[] header = new byte[HEADER_LENGTH];
        // set magic number.
        Bytes.short2bytes(MAGIC, header);

        // set request and serialization flag.
        header[2] = (byte) (FLAG_REQUEST | 6);

        if (req.isTwoWay()) header[2] |= FLAG_TWOWAY;
        if (req.isEvent()) header[2] |= FLAG_EVENT;

        // set request id.
        Bytes.long2bytes(req.getId(), header, 4);

        // encode request data.
        int savedWriteIndex = buffer.writerIndex();
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        encodeRequestData(bos, req.getData());

        int len = bos.size();
        buffer.writeBytes(bos.toByteArray());
        Bytes.int2bytes(len, header, 12);

        // write
        buffer.writerIndex(savedWriteIndex);
        buffer.writeBytes(header); // write header.
        buffer.writerIndex(savedWriteIndex + HEADER_LENGTH + len);
        req.release();

    }

    public void encodeRequestData(OutputStream out, Object data) throws Exception {
        RpcInvocation inv = (RpcInvocation)data;

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));

        JsonUtils.writeObject(inv.getAttachment("dubbo", "2.0.1"), writer);
        JsonUtils.writeObject(inv.getAttachment("path"), writer);
        JsonUtils.writeObject(inv.getAttachment("version"), writer);
        JsonUtils.writeObject(inv.getMethodName(), writer);
        JsonUtils.writeObject(inv.getParameterTypes(), writer);

        JsonUtils.writeBytes(inv.getArguments(), writer);
        JsonUtils.writeObject(inv.getAttachments(), writer);
    }

}
