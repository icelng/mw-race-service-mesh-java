package com.yiran;

import com.alibaba.fastjson.JSON;
import com.yiran.agent.AgentServiceRequest;
import com.yiran.dubbo.DubboConnectManager;
import com.yiran.registry.ServiceInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 单例,主要做服务协议转换
 */
public class ServiceSwitcher {
    private static Logger logger = LoggerFactory.getLogger(ServiceSwitcher.class);
    private static Channel rpcClientChannel = null;
    private static DubboConnectManager dubboConnectManager;
    private static CountDownLatch rpcChannelReady = new CountDownLatch(1);
    /*使用可并发Hash表*/
    private static ConcurrentHashMap<String, AgentServiceRequest> processingRequest = new ConcurrentHashMap<>();

    /*一个转换两个表,使用普通Hash表，但是要保证一次操作两个表*/
    /*service*/
    private static Map<Integer, ServiceInfo> serviceIdMap = new HashMap<>();
    private static Map<String, ServiceInfo> serviceNameMap = new HashMap<>();
    private static long writeCnt = 0;
    private static Channel agentChannel;


    /**
     * 设置实现Dubbo协议的Netty客户端通道(Channel)
     * @param channel
     * 客户端Channel
     */
    public static void setRpcClientChannel(Channel channel){
        //dubboConnectManager = connectManager;
        rpcClientChannel = channel;
        rpcChannelReady.countDown();
    }


    /**
     * 透传
     */
    public static void responseFromDubbo(ByteBuf data) throws UnsupportedEncodingException {

        /*向客户端发送响应*/
        if (agentChannel != null) {
            agentChannel.writeAndFlush(data);

        } else {
            logger.error("The agent channel is null!!!");
        }

    }


    public static void setAgentChannel (Channel channel) {
        agentChannel = channel;
    }



    /**
     * 添加服务支持
     * @param service
     */
    synchronized public static void addSupportedService(ServiceInfo service){
        serviceIdMap.put(service.getServiceId(), service);
        serviceNameMap.put(service.getServiceName(), service);
    }

}
