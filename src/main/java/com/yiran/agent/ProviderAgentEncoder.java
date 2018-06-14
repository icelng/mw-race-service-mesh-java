package com.yiran.agent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 自定义协议编码
 */
public class ProviderAgentEncoder extends MessageToByteEncoder {
    private static final int HEADER_LENGTH = 12;
    private byte[] header = new byte[HEADER_LENGTH];

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        AgentServiceResponse agentServiceResponse = (AgentServiceResponse) msg;

        /*写头*/
        Bytes.long2bytes(agentServiceResponse.getRequestId(), header, 0);
        Bytes.int2bytes(4, header, 8);
        out.writeBytes(header);
        out.writeInt(agentServiceResponse.getHashCode());
    }
}
