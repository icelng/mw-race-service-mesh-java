package com.yiran;

import com.yiran.agent.AgentClient;
import com.yiran.agent.AgentClientManager;
import com.yiran.registry.Endpoint;
import com.yiran.registry.EtcdRegistry;
import com.yiran.registry.IRegistry;
import com.yiran.registry.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 负责服务发现，负责负载均衡，负责agent客户端的管理
 */
public class LoadBalance {
    private static float MAX_PPL = 999999999;
    private static Logger logger = LoggerFactory.getLogger(LoadBalance.class);
    private static int TOKEN_BUCKET_CAPACITY = 32;

    private IRegistry registry;
    private AgentClientManager agentClientManager;
    private ConcurrentHashMap<String, HashSet<String>>  serviceNameToAgentClientsMap;
    private ConcurrentHashMap<String, AgentClient> clientNameToAgentClientMap;
    private ConcurrentHashMap<Integer, List<String>> loadLevelToAgentClientsMap;
    private final Object lock = new Object();
    private int tryCount = 0;
    private AtomicLong requestRateCalPeriod = new AtomicLong(0);
    private AtomicLong responseRateSetPeriod = new AtomicLong(0);
    private long lastNanoTime = 0;
    private float requestRate = 6000;  // 初始6000
    private Semaphore tokenBucket;
    private Object tokenBucketLock = new Object();

    private ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService scheduledPrintRate = Executors.newSingleThreadScheduledExecutor();


    public LoadBalance(String registryAddress, AgentClientManager agentClientManager){
        registry = new EtcdRegistry(registryAddress);
        serviceNameToAgentClientsMap = new ConcurrentHashMap<>();
        clientNameToAgentClientMap = new ConcurrentHashMap<>();
        loadLevelToAgentClientsMap = new ConcurrentHashMap<>();
        this.agentClientManager = agentClientManager;
        tokenBucket = new Semaphore(TOKEN_BUCKET_CAPACITY);
        scheduledExecutor.scheduleAtFixedRate(() -> {
            synchronized (tokenBucketLock) {
                if (tokenBucket.availablePermits() < TOKEN_BUCKET_CAPACITY) {
                    int supplementNum = Math.min(TOKEN_BUCKET_CAPACITY, TOKEN_BUCKET_CAPACITY - tokenBucket.availablePermits());
                    tokenBucket.release(supplementNum);
                }
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
        scheduledPrintRate.scheduleAtFixedRate(() -> {
            logger.info("The request rate is {}", requestRate);
        }, 0, 1, TimeUnit.SECONDS);
    }

    public boolean findService(String serviceName) throws Exception {
        List<Endpoint> endpoints = registry.find(serviceName);
        if (endpoints == null) {
            return false;
        }
        for (Endpoint endpoint : endpoints) {
            createAgentClient(endpoint);
        }
        return true;
    }

    public AgentClient createAgentClient(Endpoint endpoint) throws InterruptedException {
        String clientName = endpoint.getHost() + ":" + String.valueOf(endpoint.getPort());
        AgentClient agentClient = clientNameToAgentClientMap.getOrDefault(clientName, null);
        if(agentClient == null){
            agentClient = agentClientManager.newClient();
            clientNameToAgentClientMap.put(clientName, agentClient);
            agentClient.setLoadLevel(endpoint.getLoadLevel());
            agentClient.connect(endpoint.getHost(), endpoint.getPort());
        }

        ServiceInfo service = endpoint.getSupportedService();
        agentClient.addSupportedService(service);
        String serviceName = service.getServiceName();

        List<String> loadLevelAgentClients = loadLevelToAgentClientsMap.computeIfAbsent(endpoint.getLoadLevel(), k -> new ArrayList<>());
        loadLevelAgentClients.add(clientName);

        HashSet<String> serviceNameAgentClients = serviceNameToAgentClientsMap.computeIfAbsent(serviceName, k -> new HashSet<>());
        serviceNameAgentClients.add(clientName);


        return agentClient;
    }

    /**
     * 查找最优的provider对应的agentClient
     * 如果得出最优的客户端，最好要进行请求。如果不请求，则一定要调用放弃强求放弃方法
     * 计算得到最优的客户端的过程中，如果没有带上服务发现，其时间是很短的。
     * @param serviceName
     * @return
     */
    synchronized public AgentClient findOptimalAgentClient(String serviceName) throws Exception {
        HashSet<String> agentClientNames;
        do {
            /*查询支持指定服务名的客户端集合*/
            agentClientNames = serviceNameToAgentClientsMap.getOrDefault(serviceName, null);
            if(agentClientNames == null || agentClientNames.size() == 0){
                synchronized (lock) {
                    agentClientNames = serviceNameToAgentClientsMap.getOrDefault(serviceName, null);
                    if(agentClientNames == null || agentClientNames.size() == 0){
                        logger.info("Service {} not found!Checking out etcd..tryCount:{}", serviceName, ++tryCount);
                        /*如果针对服务名找不到对应的客户端，则向ETCD中心查询服务*/
                        findService(serviceName);
                    }
                }
            }
        } while (agentClientNames == null || agentClientNames.size() == 0);

        ///*现在已经找到服务名对应agent客户端的集合，下面选出最优的agent客户端*/
        AgentClient optimalAgentClient = null;
        float minPPL = MAX_PPL;  //  minimum processingRequestNum per loadLevel
        for (String clientName : agentClientNames) {
            AgentClient agentClient = clientNameToAgentClientMap.get(clientName);
            if(agentClient == null){
                logger.error("AgentClient {} not found!", clientName);
                return null;
            }

            /*计算最小的ppl*/
            long processingRequestNum = agentClient.getProcessingRequestNum().get();
            int loadLevel = agentClient.getLoadLevel();
            float currentPPL = ((float) processingRequestNum)/((float) loadLevel * 10);
            if (currentPPL < minPPL && processingRequestNum < 200) {
                minPPL = currentPPL;
                optimalAgentClient = agentClient;
            }
        }
        // optimalAgentClient = getOptimalByRandom();

        /*限流*/
        //if (AgentClient.getTotalProccessingNum() > 512) {
        //    return null;
        //}

        if (optimalAgentClient != null) {
            /*这里提前增加了请求数*/
            optimalAgentClient.requestReady();
        }

        return optimalAgentClient;
    }

    private AgentClient getOptimalByRandom() throws Exception {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int randomNum = random.nextInt(70);
        int selectedLoadLevel = 0;

        if (randomNum >= 0 && randomNum < 10) {
            selectedLoadLevel = 1;
        } else if (randomNum >= 10 && randomNum < 40) {
            selectedLoadLevel = 2;
        } else if (randomNum >= 40 && randomNum < 70) {
            selectedLoadLevel = 3;
        }

        List<String> loadLevelAgentClients = loadLevelToAgentClientsMap.getOrDefault(selectedLoadLevel, null);
        if (loadLevelAgentClients == null || loadLevelAgentClients.size() == 0) {
            logger.error("No agentClient for loadLevel:{} when getting optimal client!", selectedLoadLevel);
            return null;
        }

//        String agentClientName = loadLevelAgentClients.get(random.nextInt(loadLevelAgentClients.size()));
        String agentClientName = loadLevelAgentClients.get(0);
        AgentClient agentClient = clientNameToAgentClientMap.getOrDefault(agentClientName, null);

        //while (agentClient.getProcessingRequestNum().get() > 200) {
        //    loadLevelAgentClients = loadLevelToAgentClientsMap.getOrDefault((++selectedLoadLevel) % 3 + 1, null);
        //    if (loadLevelAgentClients == null || loadLevelAgentClients.size() == 0) {
        //        logger.error("No agentClient for loadLevel:{} when getting optimal client!", selectedLoadLevel);
        //        return null;
        //    }

        //    agentClientName = loadLevelAgentClients.get(0);
        //    agentClient = clientNameToAgentClientMap.getOrDefault(agentClientName, null);
        //}

        return agentClient;
    }

    public void calRequestRate() {

        if (requestRateCalPeriod.addAndGet(1) == 1024) {
            long intervalNanoTime = System.nanoTime() - lastNanoTime;
            lastNanoTime += intervalNanoTime;
            requestRate = (float) 1024 / (float) intervalNanoTime * 1000000000;
            //if (requestRate > 8000) {
            //    logger.info("The request rate(QPS:{}) is higher than 8000", requestRate);
            //}
            requestRateCalPeriod.set(0);
        }

    }

    public boolean tryAcquireToken () {
        return tokenBucket.tryAcquire();
    };

    public void acquireToken() {
        tokenBucket.acquireUninterruptibly();
    }

    public void supplementToken() {
        synchronized (tokenBucketLock) {
            if (tokenBucket.availablePermits() < TOKEN_BUCKET_CAPACITY) {
                tokenBucket.release();
            }
        }
    }

    public float getRequestRate() {
        return requestRate;
    }

    public boolean isNeedToSetRespRate() {

        if (responseRateSetPeriod.getAndAdd(1) == 512) {
            responseRateSetPeriod.set(0);
            return true;
        } else {
            return false;
        }

    }

}
