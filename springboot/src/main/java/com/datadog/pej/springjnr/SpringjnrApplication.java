package com.datadog.pej.springjnr;


import datadog.opentracing.DDTracer;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import jnr.ffi.LibraryLoader;


@SpringBootApplication
@EnableScheduling
public class SpringjnrApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringjnrApplication.class, args);
    }


    @Bean(name="jnrTracer")
    public Tracer tracer(){
        return DDTracer.builder().build();
    }

    @Bean(name="jnrNativeInterface")
    public INative inativeCreate(){
        return LibraryLoader.create(INative.class).load("springjnr");
    }


}

