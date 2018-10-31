package com.github.admin.addon.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@ApplicationScoped
public class LoggerProducer implements Serializable {

    private Map<Class<?>, Logger> loggerCache = new ConcurrentHashMap<>();


    @Produces
    public Logger produce(InjectionPoint ip) {
        Class<?> declaringClass = ip.getMember().getDeclaringClass();
        Logger logger = loggerCache.get(declaringClass);
        if(logger == null) {
            logger = Logger.getLogger(declaringClass.getName());
            loggerCache.put(declaringClass,logger);
        }
        return logger;
    }
}
