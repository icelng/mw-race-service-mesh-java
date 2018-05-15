package com.yiran;

import com.yiran.agent.AgentClient;
import com.yiran.agent.AgentServiceRequestFuture;
import com.yiran.agent.AgentServiceResponse;
import com.yiran.agent.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@RestController
public class HelloController {

    private static Logger logger = LoggerFactory.getLogger(HelloController.class);
    private LoadBalance loadBalance = new LoadBalance(System.getProperty("etcd.url"));
    private Executor listenerExecutor = Executors.newFixedThreadPool(16);

    @RequestMapping(value = "")
    public DeferredResult<ResponseEntity> invoke(@RequestParam("interface") String interfaceName,
                                                 @RequestParam("method") String method,
                                                 @RequestParam("parameterTypesString") String parameterTypesString,
                                                 @RequestParam("parameter") String parameter) throws Exception {
        DeferredResult<ResponseEntity> result = new DeferredResult<>();

        AgentClient agentClient;
        try {
            agentClient = loadBalance.findOptimalAgentClient(interfaceName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }

        AgentServiceRequestFuture future = agentClient.serviceRequest(interfaceName, method, parameterTypesString, parameter);
        future.addListener(() -> {
            try {
                AgentServiceResponse response = future.get();
                if (response != null) {
                    int hashCode = Bytes.bytes2int(response.getReturnValue(), 0);
                    ResponseEntity responseEntity = new ResponseEntity(hashCode, HttpStatus.OK);
                    result.setResult(responseEntity);
                } else {
                    logger.error("Request:{} error!", future.getRequestId());
                }
            } catch (InterruptedException e) {
                logger.error("", e);
            }

        }, listenerExecutor, 2, TimeUnit.SECONDS);

//        AgentServiceRequestFuture future = agentClient.serviceRequest(interfaceName, method, parameterTypesString, parameter);
//        AgentServiceResponse response = future.get(2, TimeUnit.SECONDS);
//        if(response == null) {
//            logger.error("Request: time out!", future.getRequestId());
//            return null;
//        }

        return result;
    }
}
