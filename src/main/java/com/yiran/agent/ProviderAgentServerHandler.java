package com.yiran.agent;

import com.yiran.ServiceSwitcher;
import com.yiran.agent.web.FormDataParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;


/**
 * 对发给Provider-Agent的服务请求报文的处理
 */
public class ProviderAgentServerHandler extends SimpleChannelInboundHandler<AgentServiceRequest> {
    private static Logger logger = LoggerFactory.getLogger(ProviderAgentServerHandler.class);

//    private ByteBuf parseTempBuf = UnpooledByteBufAllocator.DEFAULT.buffer(2048);
//    private FormDataParser formDataParser = new FormDataParser(parseTempBuf, 2048);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AgentServiceRequest agentServiceRequest) throws Exception {

        /*解析表单*/
        //agentServiceRequest.setFormDataMap(formDataParser.parse(agentServiceRequest.getData()));
        //agentServiceRequest.getData().release();  // 其实这个是多了一次的拷贝

        ServiceSwitcher.switchToDubbo(agentServiceRequest, ctx.channel());

    }
}
