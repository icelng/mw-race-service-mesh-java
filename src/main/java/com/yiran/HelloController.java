package com.yiran;

import com.yiran.agent.AgentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class HelloController {

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
        try {
             agentClient = loadBalance.findOptimalAgentClient(interfaceName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return null;
        }
        logger.info("Send parameter:", parameter);
        int hashCode = agentClient.serviceRequest(interfaceName, method, parameterTypesString, parameter);
        logger.info("Get the hash code:{}, the local hashcode is:{}" , hashCode, parameter.hashCode());

        return hashCode;  // 直接返回
    }
}
