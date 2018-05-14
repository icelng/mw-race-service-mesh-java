package com.yiran;

import com.yiran.agent.AgentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HelloController {

    private Object lock = new Object();
    private static Logger logger = LoggerFactory.getLogger(HelloController.class);
    private LoadBalance loadBalance = new LoadBalance(System.getProperty("etcd.url"));

    @RequestMapping(value = "")
    public Object invoke(@RequestParam("interface") String interfaceName,
                         @RequestParam("method") String method,
                         @RequestParam("parameterTypesString") String parameterTypesString,
                         @RequestParam("parameter") String parameter) throws Exception {
        return consumer(interfaceName,method,parameterTypesString,parameter);
    }

    public Integer consumer(String interfaceName,String method,String parameterTypesString,String parameter) throws Exception {
        AgentClient agentClient;
        int hashCode;
        try {
            agentClient = loadBalance.findOptimalAgentClient(interfaceName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        hashCode = agentClient.serviceRequest(interfaceName, method, parameterTypesString, parameter);
        float ppl = ((float)agentClient.getProcessingRequestNum().get())/((float) agentClient.getLoadLevel());
        logger.info("Get result from:{}  loadLevel:{}  ppl:{}", agentClient.getName(), agentClient.getLoadLevel(), ppl);
        return hashCode;  // 直接返回
    }
}
