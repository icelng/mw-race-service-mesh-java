package com.yiran.agent;

import java.util.concurrent.ConcurrentHashMap;

public class AgentServiceRequestHolder {

    private static ConcurrentHashMap<Long, AgentServiceRequestFuture> processingRequest = new ConcurrentHashMap<>();

    public static void put(Long requestId, AgentServiceRequestFuture future) {
        processingRequest.put(requestId, future);
    }

    public static AgentServiceRequestFuture get(Long requestId) {
        return processingRequest.get(requestId);
    }

    public static void remove(Long requestId) {
        processingRequest.remove(requestId);
    }
}
