package com.yiran.agent;

import com.google.common.util.concurrent.RateLimiter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConsumerAgentClientHandler extends SimpleChannelInboundHandler<AgentServiceBaseMsg> {
    private static Logger logger = LoggerFactory.getLogger(ConsumerAgentClientHandler.class);

    private RateLimiter rateLimiter = RateLimiter.create(6000);
    private static Executor executor = Executors.newFixedThreadPool(512);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                ctx.flush();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    protected void channelRead0(ChannelHandlerContext ctx, AgentServiceBaseMsg msg) throws Exception {
        AgentServiceRequestFuture future = AgentServiceRequestHolder.get(String.valueOf(msg.getRequestId()));
        if (future != null) {
            AgentServiceRequestHolder.remove(String.valueOf(msg.getRequestId()));
            if (!rateLimiter.tryAcquire(0, TimeUnit.MILLISECONDS)) {
                executor.execute(() -> {
                    rateLimiter.acquire();
                    try {
                        future.done(msg);
                    } catch (UnsupportedEncodingException e) {
                        logger.error("", e);
                    }

                });
            } else {
                future.done(msg);
            }
        }
    }
}
