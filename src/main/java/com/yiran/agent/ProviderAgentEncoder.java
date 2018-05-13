package com.yiran.agent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 自定义协议编码
 */
public class ProviderAgentEncoder extends MessageToByteEncoder {
    private static final int RETURN_VALUE_SIZE_ALIGN_BIT = 2;  // 返回值大小对齐 2^2=4

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        AgentServiceResponse agentServiceResponse = (AgentServiceResponse) msg;

        out.writeInt(agentServiceResponse.getServiceId());  // 0~3 byte
        out.writeByte(agentServiceResponse.getMethodId());  // 4 byte
        out.writeByte(0x00);  // 5 byte
        out.writeByte(0x00);  // 6 byte
        out.writeByte(agentServiceResponse.getStatus());  // 7 byte
        out.writeLong(agentServiceResponse.getRequestId());  // 8~15 byte
        int returnType = (agentServiceResponse.getReturnType() & 0x0F) << 28;
        int returnSize = agentServiceResponse.getReturnValue().length;
        out.writeInt(returnType | returnSize);  // 返回值的类型和大小  16~19 byte
        out.writeBytes(agentServiceResponse.getReturnValue(), 0, returnSize);
        int paddingSize = ((returnSize + ~(0xFFFFFFFF << RETURN_VALUE_SIZE_ALIGN_BIT)) & (0xFFFFFFFF << RETURN_VALUE_SIZE_ALIGN_BIT)) - returnSize;
        out.writerIndex(out.writerIndex() + paddingSize);

    }
}
