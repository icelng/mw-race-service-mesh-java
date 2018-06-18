package com.yiran.agent;

import com.google.common.util.concurrent.ListenableFuture;
import com.yiran.dubbo.model.HardRequest;
import com.yiran.dubbo.model.JsonUtils;
import com.yiran.dubbo.model.Request;
import com.yiran.dubbo.model.RpcInvocation;
import com.yiran.registry.ServiceInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class AgentClient {
    private String name;
    private String host;
    private int port;
    private long writeCnt = 0;
    private Channel channel;
    private static AtomicLong totalProcessingNum = new AtomicLong(0);
    private static AtomicLong requestId = new AtomicLong(0);

    /*表示正在处理的请求数，负载均衡用*/
    private AtomicLong processingRequestNum;
    private int loadLevel;

    /*支持的服务*/
    private ConcurrentHashMap<String, ServiceInfo> supportedServiceMap;

    private Bootstrap bootstrap;


    private static Logger logger = LoggerFactory.getLogger(AgentClient.class);


    public AgentClient(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public void connect(String host, int port) throws InterruptedException {
        logger.info("Connecting to provider-agent, host:{}  port:{}", host, port);
        channel = bootstrap.connect(host, port).channel();
        logger.info("Connected successfully!");
        this.host = host;
        this.port = port;
        this.name = host + ":" + String.valueOf(port);
        processingRequestNum = new AtomicLong(0);
        supportedServiceMap = new ConcurrentHashMap<>();
    }


    /**
     * 异步发起请求
     * @param serviceId
     * @param methodId
     * @param parameterTypes
     * @param parameters
     * @return
     */
    public ListenableFuture asynRequest(int serviceId, byte methodId, List<Integer> parameterTypes, List<byte[]> parameters){

        return null;
    }

    public void request(Map<String, String> argumentsMap, AgentServiceRequestFuture future) throws IOException {
        long reqId = requestId.addAndGet(1);

        future.setAgentClient(this);

        RpcInvocation invocation = new RpcInvocation();
        invocation.setMethodName(argumentsMap.get("method"));
        invocation.setAttachment("path", argumentsMap.get("interface"));
        String parameterTypeName = argumentsMap.get("parameterTypesString");
        invocation.setParameterTypes(parameterTypeName);    // Dubbo内部用"Ljava/lang/String"来表示参数类型是String
        String parameter = argumentsMap.get("parameter");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        JsonUtils.writeObject(parameter, writer);
        invocation.setArguments(out.toByteArray());

        Request request = Request.get();
        request.setVersion("2.0.0");
        request.setTwoWay(true);
        request.setData(invocation);
        request.setId(reqId);


        AgentServiceRequestHolder.put(reqId, future);

        channel.writeAndFlush(request);  // 开始发送报文
    }

    public void hardRequest(ByteBuf parameter, AgentServiceRequestFuture future) {
        long reqId = requestId.addAndGet(1);

        future.setAgentClient(this);
        AgentServiceRequestHolder.put(reqId, future);

        HardRequest request = HardRequest.get();
        request.setParameter(parameter);
        request.setReqId(reqId);

        channel.writeAndFlush(request);
    }

    public AgentServiceRequestFuture request(Channel httpChannel, ByteBuf data) throws Exception {
        long reqId = requestId.addAndGet(1);

        AgentServiceRequest agentServiceRequest = new AgentServiceRequest();
        agentServiceRequest.setRequestId(reqId);
        agentServiceRequest.setData(data);

        AgentServiceRequestFuture future = new AgentServiceRequestFuture(this, agentServiceRequest, httpChannel);
        AgentServiceRequestHolder.put(reqId, future);

        channel.writeAndFlush(agentServiceRequest);  // 开始发送报文

        return future;
    }

    public AtomicLong getProcessingRequestNum() {
        return processingRequestNum;
    }

    public void addSupportedService(ServiceInfo service){
        supportedServiceMap.put(service.getServiceName(), service);
    }

    public ServiceInfo getSupportedService(String serviceName){
        return supportedServiceMap.getOrDefault(serviceName, null);
    }

    public int getLoadLevel() {
        return loadLevel;
    }

    public void setLoadLevel(int loadLevel) {
        this.loadLevel = loadLevel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static long getTotalProcessingNum() {
        return totalProcessingNum.get();
    }

    public void requestReady(){
        totalProcessingNum.addAndGet(1);
        this.processingRequestNum.addAndGet(1);
    }

    public void requestDone(){
        totalProcessingNum.decrementAndGet();
        this.processingRequestNum.decrementAndGet();
    }
}
