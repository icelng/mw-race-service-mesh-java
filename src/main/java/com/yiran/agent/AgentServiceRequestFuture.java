package com.yiran.agent;

import com.yiran.dubbo.model.RpcResponse;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentServiceRequestFuture implements Future<RpcResponse> {
    private static Logger logger = LoggerFactory.getLogger(AgentServiceRequestFuture.class);

    private long requestId;
    private AgentClient agentClient;

    private CountDownLatch latch = new CountDownLatch(1);
    private RpcResponse agentServiceResponse = null;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private AtomicBoolean isDone = new AtomicBoolean(false);
    private Executor executor = null;
    private Runnable listener = null;

    private AgentServiceRequest agentServiceRequest;

    private Channel httpChannel;

    /*时延*/
    private long startTime;
    private float latency;

    public AgentServiceRequestFuture(AgentClient agentClient, AgentServiceRequest agentServiceRequest, Channel httpChannel){
        this.agentServiceRequest = agentServiceRequest;
        this.requestId = agentServiceRequest.getRequestId();
        this.agentClient = agentClient;
        this.httpChannel = httpChannel;
        startTime = System.nanoTime();
    }

    public AgentServiceRequestFuture () {
        startTime = System.nanoTime();
    }


    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return isCancelled.get();
    }

    public boolean isDone() {
        return isDone.get();
    }

    public RpcResponse get() throws InterruptedException{
        latch.await();
        return agentServiceResponse;
    }

    public RpcResponse get(long timeout, TimeUnit unit) throws InterruptedException{
        latch.await(timeout, unit);
        return agentServiceResponse;
    }

    public void addListener(Runnable listener, Executor executor) {
        this.listener = listener;
        this.executor = executor;
    }

    public void done2 (RpcResponse response) {
        latch.countDown();
        this.agentServiceResponse = response;
        if (agentClient != null) {
            agentClient.requestDone();
        }
        executor.execute(listener);
    }

    public float getLatency () {
        latency = (float) (System.nanoTime() - startTime) / 1000000.f;
        return latency;
    }

    public void setAgentClient(AgentClient agentClient) {
        this.agentClient = agentClient;
    }

    public void done(AgentServiceBaseMsg response) throws UnsupportedEncodingException {
        if (response != null) {
            int hashCode = response.getData().readInt();
            response.getData().release();
            String hashCodeString = String.valueOf(hashCode);
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK, Unpooled.wrappedBuffer(hashCodeString.getBytes("utf-8")));
            setHeaders(httpResponse);
            httpChannel.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            agentClient.requestDone();
        } else {
            logger.error("Request:{} error!", requestId);
        }
    }



    public long getRequestId() {
        return requestId;
    }

    private void setHeaders(FullHttpResponse response) {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
    }
}
