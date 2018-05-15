package com.yiran.agent;

import javax.validation.constraints.NotNull;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AgentServiceRequestFuture implements Future<AgentServiceResponse> {
    private static ScheduledExecutorService timeoutExecutorService = Executors.newScheduledThreadPool(10);
    private static Executor listenerExecutor = Executors.newFixedThreadPool(256);

    private final long requestId;
    private AgentClient agentClient;

    private CountDownLatch latch = new CountDownLatch(1);
    private AgentServiceResponse agentServiceResponse = null;
    private ScheduledFuture timeoutScheduledFuture;
    private Runnable listener = null;
    private AtomicBoolean isCancelled = new AtomicBoolean(false);
    private AtomicBoolean isDone = new AtomicBoolean(false);
    private Object lock = new Object();


    public AgentServiceRequestFuture(AgentClient agentClient, long requestId){
        this.requestId = requestId;
        this.agentClient = agentClient;
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

    public void done(AgentServiceResponse response){
        /*尝试停止超时任务的调度*/
        if(timeoutScheduledFuture != null){
            timeoutScheduledFuture.cancel(false);
        }
        synchronized (lock) {
            if (!isCancelled() && !isDone()) {
                this.agentServiceResponse = response;
                latch.countDown();
                isDone.set(true);  // 设置完成
                agentClient.requestDone();  // 请求数减一
                if (listener != null) {
                    /*执行监听线程*/
                    listenerExecutor.execute(listener);
                }
            }
        }
    }

    public void addListener(@NotNull Runnable listener) {
        synchronized (lock) {
            this.listener = listener;
            if (isDone()) {
                /*如果已经完成，则马上调用*/
                listenerExecutor.execute(listener);
            }
        }
    }

    public void addListener(@NotNull Runnable listener, long timeout, TimeUnit unit) {
        addListener(listener);
        /*使用计划任务来实现超时机制*/
        // 超时取消
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
