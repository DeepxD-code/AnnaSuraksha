package com.annasuraksha.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    private static ApplicationContext ctx;
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        ctx = applicationContext;
    }
    public static ApplicationContext getApplicationContext() { return ctx; }
}
