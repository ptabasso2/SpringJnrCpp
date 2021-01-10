package com.datadog.pej.springjnr;


import datadog.opentracing.DDTracer;
import datadog.trace.api.DDTags;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.Tracer;

import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class SpringController {

    private static Logger log = LoggerFactory.getLogger(SpringController.class);

    @Autowired
    @Qualifier("jnrTracer")
    Tracer tracer;


    @Autowired
    @Qualifier("jnrNativeInterface")
    INative iNative;


    @RequestMapping("/")
    public String home() {


        System.out.println("library: " + System.getProperty("java.library.path"));
        Tracer tracer = DDTracer.builder().build();
        ScopeManager sm = tracer.scopeManager();
        Tracer.SpanBuilder tb = tracer.buildSpan("servlet.request");

        Map<String,String> map=new HashMap<>();

        Span span = tb.start();
        try(Scope scope = sm.activate(span)){
            tracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, new TextMapAdapter(map));
            span.setTag(DDTags.SERVICE_NAME, "cpp");
            span.setTag(DDTags.RESOURCE_NAME, "GET /test");
            span.setTag(DDTags.SPAN_TYPE, "web");

            try {
                System.out.println("Tracing in the Java layer and calling C++");

                /* We have to convert HashMap to Array */
                String[] keys = map.keySet().toArray(new String[0]);
                String[] values = new String[keys.length];
                int idx = 0;
                for (String key : keys) {
                    values[idx++] = map.get(key);
                }


                /* Pausing before calling native code */
                try {
                    Thread.sleep(2000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                /* Call to shared library */
                int cppResult=iNative.runcppkv(2,4,keys,values);

                Thread.sleep(20);
            } catch(InterruptedException e){
                e.printStackTrace();
            }
            span.finish();
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("Handling home");
        return "C++ ended job done...\n";
    }


}
  
