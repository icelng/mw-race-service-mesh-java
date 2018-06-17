package com.yiran.agent;

import com.yiran.LoadBalance;
import com.yiran.dubbo.model.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConsumerAgentClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    private static Logger logger = LoggerFactory.getLogger(ConsumerAgentClientHandler.class);
//    private static Executor executor = Executors.newFixedThreadPool(512);
    private LoadBalance loadBalance;

    public ConsumerAgentClientHandler (LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }


    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        logger.info("Recv response data!");
        AgentServiceRequestFuture future = AgentServiceRequestHolder.get(String.valueOf(response.getRequestId()));
        if (future != null) {
            AgentServiceRequestHolder.remove(String.valueOf(response.getRequestId()));

            /*done*/
            future.done2(response);

        }
    }
}
