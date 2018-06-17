package com.yiran.agent.web;

import com.yiran.LoadBalance;
import com.yiran.agent.*;
import com.yiran.dubbo.model.RpcResponse;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HttpChannelHandler extends ChannelInboundHandlerAdapter {
    private static Logger logger = LoggerFactory.getLogger(HttpChannelHandler.class);

    private static Executor executor = Executors.newFixedThreadPool(512);

    private LoadBalance loadBalance;
//    private ByteBuf contentBuf = PooledByteBufAllocator.DEFAULT.buffer(2048);
    private ByteBuf parseTempBuf = PooledByteBufAllocator.DEFAULT.buffer(2048);


    public HttpChannelHandler (LoadBalance loadBalance) {
        this.loadBalance = loadBalance;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

//    @Override
//    public void channelRead0(ChannelHandlerContext ctx, Object msg)
//            throws Exception {
//        if (msg instanceof HttpContent) {
//            HttpContent content = (HttpContent) msg;
//            ByteBuf buf = content.content();
//            if (buf.isReadable()) {
//                contentBuf.writeBytes(buf);
//            }
//            if (msg instanceof LastHttpContent) {
//                try{
//                    FormDataParser formDataParser = new FormDataParser(parseTempBuf, 2048);
//                    String serviceName = formDataParser.parseInterface(contentBuf);
//                    if(serviceName == null) {
//                        logger.error("Failed to parse form data!{}.", contentBuf.toString(Charset.forName("utf-8")));
//                        ctx.close();
//                        return;
//                    }
//                    /*截出parameter*/
//                    parseParameter(contentBuf);
//
//                    ///*开始调用服务*/
//
//                    ////logger.info("serviceName:{}", serviceName);
//
//                    ///*选出最优客户端*/
//                    AgentClient agentClient = loadBalance.findOptimalAgentClient(serviceName);
//                    if (agentClient == null) {
//                        ctx.close();
//                        return;
//                    }
//                    ///*调用服务*
//                    agentClient.request(ctx.channel(), contentBuf);
//
//                } catch (Exception e) {
//                    logger.error("", e);
//                }
//            }
//        }
//    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf contentBuf = (ByteBuf) msg;
            try{
                AgentServiceRequestFuture future = new AgentServiceRequestFuture();

                /*补充令牌*/
                loadBalance.supplementToken();

                /*计算qps*/
                loadBalance.calRequestRate();

                FormDataParser formDataParser = new FormDataParser(parseTempBuf, 2048);
                String serviceName = formDataParser.parseInterface(contentBuf);
                if(serviceName == null) {
                    logger.error("Failed to parse form data!{}.", contentBuf.toString(Charset.forName("utf-8")));
                    responseFailure(ctx);
                    return;
                }
                /*截出parameter*/
                parseParameter(contentBuf);

                ///*选出最优客户端*/
                AgentClient agentClient = loadBalance.findOptimalAgentClient(serviceName);
                if (agentClient == null) {
                    //ctx.close();
                    responseFailure(ctx);
                    return;
                }

                ///*调用服务*
//                AgentServiceRequestFuture future = agentClient.request(ctx.channel(), contentBuf);
                future.addListener(() -> {
                    try {
                        RpcResponse response = future.get();
                        loadBalance.calLatencyDistribution(future.getLatency());
                        String hashCodeString = new String(response.getBytes());
                        hashCodeString = hashCodeString.substring(2, hashCodeString.length() - 1);
                        ByteBuf responseContent = ctx.alloc().directBuffer(hashCodeString.length() + 2);
                        responseContent.writeBytes(hashCodeString.getBytes("utf-8"));
                        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, responseContent);
                        ctx.writeAndFlush(httpResponse).addListener(future1 -> {
                            ctx.close();
                        });
                    } catch (Exception e) {
                        logger.error("", e);
                    }
                }, ctx.executor());
                agentClient.request(contentBuf, future);

            } catch (Exception e) {
                logger.error("", e);
            }
        }

    }

    private void responseFailure (ChannelHandlerContext ctx) throws UnsupportedEncodingException {
        DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK, Unpooled.wrappedBuffer("Failure".getBytes("utf-8")));
        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);

    }

    private void parseParameter(ByteBuf content) {
        byte c;
        int equalCnt = 0;

        while (true) {
            c = content.readByte();
            if (c == '=') {
                if (++equalCnt == 4) {
                    break;
                }
            }
        }
    }

    /**
     * 设置HTTP返回头信息
     */
    private void setHeaders(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
//        if (HttpUtil.isKeepAlive(request)) {
//            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
//        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        parseTempBuf.release();
//        contentBuf.release();
    }

    //@Override
    //public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    //    ctx.flush();//刷新后才将数据发出到SocketChannel
    //}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

}
