package com.yiran.agent;

import com.yiran.ServiceSwitcher;
import com.yiran.registry.ServiceInfo;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

public class AgentClient {
    private String name;
    private String host;
    private int port;
    private Channel channel;
    private AtomicLong requestId;

    /*表示正在处理的请求数，负载均衡用*/
    private AtomicLong processingRequestNum;
    private int loadLevel;

    /*支持的服务*/
    private ConcurrentHashMap<String, ServiceInfo> supportedServiceMap;

    private Bootstrap bootstrap;


    private static Logger logger = LoggerFactory.getLogger(AgentClient.class);


    public AgentClient(String host, int port){
        this.host = host;
        this.port = port;
        this.name = host + ":" + String.valueOf(port);
        requestId = new AtomicLong(100);
        processingRequestNum = new AtomicLong(0);
    }

    public void run() throws InterruptedException {

        EventLoopGroup workerGroup = new NioEventLoopGroup(10);
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ConsumerAgentDecoder());
                ch.pipeline().addLast(new ConsumerAgentEncoder());
                ch.pipeline().addLast(new ConsumerAgentClientHandler());
            }
        });

        /*连接*/
        channel = bootstrap.connect(host, port).sync().channel();

    }

    public AgentServiceResponse request(int serviceId, byte methodId, List<Integer> parameterTypes, List<byte[]> parameters) throws ExecutionException, InterruptedException {

        AgentServiceRequest agentServiceRequest = new AgentServiceRequest();
        agentServiceRequest.setServiceId(serviceId);
        agentServiceRequest.setMethodId(methodId);
        agentServiceRequest.setTwoWay(false);
        agentServiceRequest.setTableType((byte) 1);
        agentServiceRequest.setParameterTypes(new ArrayList<Integer>(parameterTypes));
        agentServiceRequest.setParameters(new ArrayList<byte[]>(parameters));
        agentServiceRequest.setRequestId(requestId.addAndGet(1));

        AgentServiceRequestFuture future = new AgentServiceRequestFuture();
        AgentServiceRequestHolder.put(String.valueOf(agentServiceRequest.getRequestId()), future);

        channel.writeAndFlush(agentServiceRequest);  // 开始发送报文
        AgentServiceResponse response = (AgentServiceResponse) future.get(); // 阻塞获取
        processingRequestNum.decrementAndGet();  // 请求数减一

        return response;
    }

    public Object serviceRequest(String interfaceName, String method, String parameterTypesString, String parameter) throws Exception {
        ServiceInfo serviceInfo = supportedServiceMap.get(interfaceName);
        if (serviceInfo == null) {
            throw new Exception("Service not found when requesting!!");
        }
        int serviceId = serviceInfo.getServiceId();
        int methodId = serviceInfo.getMethodId(method);
        int parameterTypeId = serviceInfo.getParameterTypeId(parameterTypesString);

        List<Integer> parameterTypes = new ArrayList<>();
        List<byte[]> parameters = new ArrayList<>();
        parameterTypes.add(parameterTypeId);
        parameters.add(parameter.getBytes("UTF-8"));
        AgentServiceResponse agentServiceResponse = this.request(serviceId, (byte) methodId, parameterTypes, parameters);
        return agentServiceResponse.getReturnValue();
    }

    public AtomicLong getProcessingRequestNum() {
        return processingRequestNum;
    }

//    public static void main(String args[]) throws InterruptedException, UnsupportedEncodingException, ExecutionException {
//        System.out.println("starting client");
//        AgentClient agentClient = new AgentClient("127.0.0.1", 2334);
//        agentClient.run();
//
//        int count1 = 0;
//        while(true) {
//            String sendString = "hhhhhhhh-count:" + count1++;
//            byte[] hashCodeBytes = (byte[]) agentClient.serviceRequest("com.alibaba.performance.dubbomesh.provider.IHelloService", "method", "Ljava/lang/String;", sendString);
//            int hashCode = Bytes.bytes2int(hashCodeBytes, 0);
//            logger.info("<-------------------------->");
//            logger.info("sendString:" + sendString);
//            logger.info("localHashCode:" + sendString.hashCode());
//            logger.info("hashCode:" + hashCode);
//
//            Thread.sleep(1);
//
//        }
//    }

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

    public void requestReady(){
        this.processingRequestNum.addAndGet(1);
    }

    public void discardRequest(){
        this.processingRequestNum.decrementAndGet();
    }
}
