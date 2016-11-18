package de.asideas.crowdsource.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

    @Autowired(required = false)
    private List<HandlerInterceptorAdapter> handlerInterceptorAdapters = new ArrayList<>();

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        handlerInterceptorAdapters.forEach(registry::addInterceptor);
        super.addInterceptors(registry);
    }
}
