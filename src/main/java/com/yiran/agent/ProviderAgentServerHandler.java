package com.yiran.agent;

import com.yiran.ServiceSwitcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 对发给Provider-Agent的服务请求报文的处理
 */
public class ProviderAgentServerHandler extends SimpleChannelInboundHandler<AgentServiceRequest> {
    private static Logger logger = LoggerFactory.getLogger(ProviderAgentServerHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, AgentServiceRequest agentServiceRequest) throws Exception {
        logger.debug("<------------------------------------>");
        logger.debug("Receive agent service!");
        logger.debug("request_id:" + agentServiceRequest.getRequestId());
        logger.debug("service_id:" + agentServiceRequest.getServiceId());
        logger.debug("method_id:" + agentServiceRequest.getMethodId());
        logger.debug("parameter_num:" + agentServiceRequest.getParameters().size());

        /*协议转换*/
        ServiceSwitcher.switchToDubbo(agentServiceRequest, ctx.channel());
    }
}
