package com.yiran.agent;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConsumerAgentEncoder extends MessageToByteEncoder<AgentServiceRequest> {
    private static Logger logger = LoggerFactory.getLogger(ConsumerAgentEncoder.class);

    protected void encode(ChannelHandlerContext ctx, AgentServiceRequest msg, ByteBuf out) throws Exception {

        logger.info("Request reqId:{},  data:{}", msg.getRequestId(), msg.getData().toString(CharsetUtil.UTF_8));
        /*写requestId*/
        out.writeLong(msg.getRequestId());
        /*写入数据长度*/
        out.writeInt(msg.getData().readableBytes());
        /*写入数据*/
        out.writeBytes(msg.getData());
        msg.getData().release();

    }

}
