package com.yiran.dubbo;

import com.yiran.agent.Bytes;
import com.yiran.dubbo.model.RpcResponse;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class DubboRpcDecoder extends ByteToMessageDecoder {
    // header length.
    protected static final int HEADER_LENGTH = 16;
    private Boolean isHeader = true;
    private long requestId;
    private int dataLength;

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) {
        if (isHeader) {
            if (byteBuf.readableBytes() < HEADER_LENGTH) {
                return;
            }
            /*解析报头*/
            byte[] header = new byte[HEADER_LENGTH];
            requestId = Bytes.bytes2long(header, 4);
            dataLength = Bytes.bytes2int(header, 12);

            isHeader = false;
        }

        if (byteBuf.readableBytes() < dataLength) {
            return;
        }
        RpcResponse response = new RpcResponse();
        response.setRequestId(String.valueOf(requestId));
        byte[] data = new byte[dataLength];
        byteBuf.readBytes(data, 0, dataLength);
        response.setBytes(data);

        list.add(response);

        isHeader = true;
    }

}
