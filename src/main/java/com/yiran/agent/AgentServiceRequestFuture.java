package com.yiran.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

import javax.validation.constraints.NotNull;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentServiceRequestFuture implements Future<AgentServiceResponse> {
    private static Logger logger = LoggerFactory.getLogger(AgentServiceRequestFuture.class);

    private static ScheduledExecutorService timeoutExecutorService = Executors.newScheduledThreadPool(4);

    private final long requestId;
    private AgentClient agentClient;

    private Executor listenerExecutor;
    private CountDownLatch latch = new CountDownLatch(1);
    private AgentServiceResponse agentServiceResponse = null;
    private ScheduledFuture timeoutScheduledFuture;
    private Runnable listener = null;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private AtomicBoolean isDone = new AtomicBoolean(false);
    private Object lock = new Object();

    private DeferredResult<ResponseEntity> result;


    public AgentServiceRequestFuture(AgentClient agentClient, long requestId, DeferredResult<ResponseEntity> result){
        this.requestId = requestId;
        this.agentClient = agentClient;
        this.result = result;
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

    public AgentServiceResponse get() throws InterruptedException{
        latch.await();
        return agentServiceResponse;
    }

    public AgentServiceResponse get(long timeout, TimeUnit unit) throws InterruptedException{
        latch.await(timeout, unit);
        return agentServiceResponse;
    }

    public void cancel() {
        /*尝试停止超时任务的调度*/
        if(timeoutScheduledFuture != null){
            timeoutScheduledFuture.cancel(false);
        }
        synchronized (lock) {
            if (!isDone() && !isCancelled()) {
                this.agentServiceResponse = null;
                latch.countDown();
                /*如果未完成，且未取消，则可以设置取消*/
                AgentServiceRequestHolder.remove(String.valueOf(requestId));
                isCancelled.set(true);  // 设置取消
                agentClient.requestDone();  // 请求数减一
                if (listener != null) {
                    /*执行监听线程*/
                    listenerExecutor.execute(listener);
                }
            }
        }
    }

    public void done(AgentServiceResponse response) {
        if (response != null) {
            int hashCode = Bytes.bytes2int(response.getReturnValue(), 0);
            ResponseEntity responseEntity = new ResponseEntity(hashCode, HttpStatus.OK);
            result.setResult(responseEntity);
        } else {
            logger.error("Request:{} error!", requestId);
        }
    }


//    public void done(AgentServiceResponse response){
//        /*尝试停止超时任务的调度*/
//        if(timeoutScheduledFuture != null){
//            timeoutScheduledFuture.cancel(false);
//        }
//        synchronized (lock) {
//            if (!isCancelled() && !isDone()) {
//                this.agentServiceResponse = response;
//                latch.countDown();
//                isDone.set(true);  // 设置完成
//                agentClient.requestDone();  // 请求数减一
//                if (listener != null) {
//                    /*执行监听线程*/
//                    listenerExecutor.execute(listener);
//                }
//            }
//        }
//    }

    public void addListener(@NotNull Runnable listener, @NotNull Executor listenerExecutor) {
        synchronized (lock) {
            this.listener = listener;
            this.listenerExecutor = listenerExecutor;
            if (isDone()) {
                /*如果已经完成，则马上调用*/
                listenerExecutor.execute(listener);
            }
        }
    }

    public void addListener(@NotNull Runnable listener, @NotNull Executor listenerExecutor, long timeout, TimeUnit unit) {
        addListener(listener, listenerExecutor);
        /*使用计划任务来实现超时机制*/
        // 超时取消
        if(isDone() || isCancelled()) {
            /*不用设置超时了*/
            return;
        }
        timeoutScheduledFuture = timeoutExecutorService.schedule((Runnable) this::cancel, timeout, unit);
    }

    public static void main(String args[]){
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            System.out.println("hhhhh");
        }, 2, TimeUnit.SECONDS);

    }

    public long getRequestId() {
        return requestId;
    }
}
