package com.yiran.agent;

import java.util.concurrent.*;

public class AgentServiceRequestFuture implements Future<Object> {
    private CountDownLatch latch = new CountDownLatch(1);
    private AgentServiceResponse agentServiceResponse;

    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isDone() {
        return false;
    }

    public Object get() throws InterruptedException, ExecutionException {
        latch.await();
        return agentServiceResponse;
    }

    public Object get(long timeout, TimeUnit unit) throws InterruptedException{
        latch.await(timeout, unit);
        return agentServiceResponse;
    }

    public void done(AgentServiceResponse response){
        this.agentServiceResponse = response;
        latch.countDown();
    }

}
