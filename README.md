# SpringBoot / JNI Tracing from Java to Native code

## Instructions


A detailed step-by-step showing how tracing can be implemented for a SpringBoot app loading a C++ library.
Tracing will be implemented on both layers.

The environment used in this tutorial is based on a linux Ubuntu (20.04) virtual machine running on Mac OS X. 
If you simply need to run the environment you might want to use the `Dockerfile.springjni` and `docker-compose.yml` files provided and skip the tutorial details.<br>
(And then jump straight to the last two sections "Running the app" and "Testing the app").

Build and spin up both the DD Agent container and the application container using the following commands. 
You need to provide your API key

```sh
pejman@macosx:~ $ docker build -f Dockerfile.springjni -t springjniimg .
pejman@macosx:~ $ export DD_API_KEY=cdxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
pejman@macosx:~ $ docker-compose up -d
Creating springjni ... done
Creating dd-agent  ... done
pejman@macosx:~ $ docker exec -it springjni bash
[root@ubuntu:~]$ 
```

<br>

### Preliminary tasks and first time steps

***Clone this repository***

```sh
[root@ubuntu:~]$ git clone https://github.com/ptabasso2/SpringJniCpp
```

***Jdk, gradle + various utilities (build tools) and setting environment variables***

```sh
[root@ubuntu:~]$ apt update && apt -y install net-tools iputils-ping curl vim procps netcat wget gnupg2 apt-transport-https sudo lsof unzip git zip tree build-essential cmake gdb openjdk-13-jdk

[root@ubuntu:~]$ wget https://services.gradle.org/distributions/gradle-6.5.1-bin.zip -P /tmp && unzip -d /opt/gradle /tmp/gradle-6.5.1-bin.zip && ln -s /opt/gradle/gradle-6.5.1 /opt/gradle/latest
```

Add the following lines to your `.bashrc` file

```sh
export JAVA_HOME=/usr/lib/jvm/java-13-openjdk-amd64
export GRADLE_HOME=/opt/gradle/latest
export PATH=/opt/gradle/latest/bin:${PATH}
export LD_LIBRARY_PATH=/root/dd-opentracing-cpp/.build:/root/SpringJniCpp/cpp/lib
```

```sh
[root@ubuntu:~]$ source ~/.bashrc
```

***Initial directory structure***

```sh
[root@ubuntu:~]$ echo $HOME
/root
[root@ubuntu:~]$ cd SpringJniCpp
[root@ubuntu:~/SpringJniCpp]$ tree
.
├── cpp
│   ├── c
│   │   ├── springjni.cpp
│   │   └── text_map_carrier.h
│   ├── lib
│   └── Makefile
└── springboot
    ├── build.gradle
    ├── gradle
    │   └── wrapper
    │       ├── gradle-wrapper.jar
    │       └── gradle-wrapper.properties
    ├── gradlew
    ├── settings.gradle
    └── src
        └── main
            ├── java
            │   └── com
            │       └── datadog
            │           └── pej
            │               └── springjni
            │                   ├── SpringController.java
            │                   └── SpringjniApplication.java
            └── resources
                └── application.properties

```



***Install the C++ opentracing library and the C++ Datadog tracing library***

*Opentracing*

```sh
[root@ubuntu:~]$ git clone https://github.com/opentracing/opentracing-cpp.git
[root@ubuntu:~]$ cd opentracing-cpp
[root@ubuntu:~/opentracing-cpp]$ mkdir .build
[root@ubuntu:~/opentracing-cpp]$ cd .build
[root@ubuntu:~/opentracing-cpp/.build]$ cmake ..
[root@ubuntu:~/opentracing-cpp/.build]$ make
[root@ubuntu:~/opentracing-cpp/.build]$ sudo make install
```

*DD Tracing api*

```sh
[root@ubuntu:~]$ git clone https://github.com/DataDog/dd-opentracing-cpp
[root@ubuntu:~]$ cd dd-opentracing-cpp
[root@ubuntu:~/dd-opentracing-cpp]$ sudo scripts/install_dependencies.sh
[root@ubuntu:~/dd-opentracing-cpp]$ mkdir .build
[root@ubuntu:~/dd-opentracing-cpp]$ cd .build
[root@ubuntu:~/dd-opentracing-cpp/.build]$ cmake ..
[root@ubuntu:~/dd-opentracing-cpp/.build]$ make
[root@ubuntu:~/dd-opentracing-cpp/.build]$ sudo make install
```



The directory should normally look like this:

```sh
[root@ubuntu:~]$ tree
opentracing-cpp
dd-opentracing-cpp
SpringJniCpp
├── cpp
└── springboot
```


### Spin up the Datadog Agent (Provide your API key  to the  below command)


```sh
[root@ubuntu:~]$ DOCKER_CONTENT_TRUST=1 docker run -d --rm --name datadog_agent -h datadog \ 
-v /var/run/docker.sock:/var/run/docker.sock:ro -v /proc/:/host/proc/:ro -v /sys/fs/cgroup/:/host/sys/fs/cgroup:ro \
-p 8126:8126 -p 8125:8125/udp -e DD_API_KEY=<Api key to enter> -e DD_APM_ENABLED=true \
-e DD_APM_NON_LOCAL_TRAFFIC=true -e DD_PROCESS_AGENT_ENABLED=true -e DD_DOGSTATSD_NON_LOCAL_TRAFFIC="true" \ 
-e DD_LOG_LEVEL=debug datadog/agent:7
```


### Generate the c++ header file

First we will need to build a fat jar containing all the class files that are coming via the project dependencies.  

```sh
[root@ubuntu:~/SpringJniCpp]$ cd springboot
[root@ubuntu:~/SpringJniCpp/springboot]$ gradle fatJar 
```

This will create the jar file `springjni-all-0.0.1-SNAPSHOT.jar` placed under  `$HOME/SpringJniCpp/springboot/build/libs`
Now let's generate the header file:

```sh
[root@ubuntu:~/SpringJniCpp/springboot]$ javac -h ../cpp/c \
-cp $HOME/SpringJniCpp/springboot/build/libs/springjni-all-0.0.1-SNAPSHOT.jar \
-d $HOME/SpringJniCpp/springboot/build/classes/java/main/com/datadog/pej/springjni src/main/java/com/datadog/pej/springjni/SpringController.java
```

The header file will be placed under the `$HOME/cpp/c` directory and is named: `com_datadog_pej_springjni_SpringController.h`


```sh
[root@ubuntu:~/SpringJniCpp/springboot]$ ls -lrt ../cpp/c
total 12
-rw-r--r-- 1 pej pej  988 Dec 13 08:27 text_map_carrier.h
-rw-r--r-- 1 pej pej 2230 Dec 14 05:46 springjni.cpp
-rw-rw-r-- 1 pej pej  620 Dec 15 23:49 com_datadog_pej_springjni_SpringController.h
```


### Build the Springboot app

```sh
[root@ubuntu:~/SpringJniCpp/springboot]$ gradle build
```

This build the final artifact `springjni-0.0.1-SNAPSHOT.jar` placed under `$HOME/SpringJniCpp/springboot/build/libs`


### Build the c++ lib

```sh
[root@ubuntu:~/SpringJniCpp/springboot]$ cd ../cpp
[root@ubuntu:~/SpringJniCpp/cpp]$ make
```

This will place the `libspringjni.so` library in the `$HOME/SpringJniCpp/cpp/lib` directory

### Running the app

Setting the `LD_LIBRARY_PATH` variable to point to the location of the newly created library. It actually tells the spring boot app where to locate it.
If not specified, it will fail at startup. 

```sh
[root@ubuntu:~/SpringJniCpp/springboot]$ export LD_LIBRARY_PATH=/root/dd-opentracing-cpp/.build:/root/SpringJniCpp/cpp/lib
[root@ubuntu:~/SpringJniCpp/springboot]$ java -jar ./build/libs/springjni-0.0.1-SNAPSHOT.jar

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.3.5.RELEASE)

2021-01-02 14:44:07.513  INFO 199 --- [           main] c.d.pej.springjni.SpringjniApplication   : Starting SpringjniApplication on ubuntu with PID 199 (/root/SpringJniCpp/springboot/build/libs/springjni-0.0.1-SNAPSHOT.jar started by root in /root/SpringJniCpp/springboot)
2021-01-02 14:44:07.529  INFO 199 --- [           main] c.d.pej.springjni.SpringjniApplication   : No active profile set, falling back to default profiles: default
2021-01-02 14:44:09.029  INFO 199 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8080 (http)
2021-01-02 14:44:09.043  INFO 199 --- [           main] o.apache.catalina.core.StandardService   : Starting service [Tomcat]
2021-01-02 14:44:09.044  INFO 199 --- [           main] org.apache.catalina.core.StandardEngine  : Starting Servlet engine: [Apache Tomcat/9.0.36]
2021-01-02 14:44:09.124  INFO 199 --- [           main] o.a.c.c.C.[Tomcat].[localhost].[/]       : Initializing Spring embedded WebApplicationContext
2021-01-02 14:44:09.125  INFO 199 --- [           main] w.s.c.ServletWebServerApplicationContext : Root WebApplicationContext: initialization completed in 1466 ms
2021-01-02 14:44:09.553  INFO 199 --- [           main] datadog.trace.core.StatusLogger          : DATADOG TRACER CONFIGURATION {"version":"0.68.0~ca80da2a6","os_name":"Linux","os_version":"5.4.0-58-generic","architecture":"amd64","lang":"jvm","lang_version":"13.0.4","jvm_vendor":"Private Build","jvm_version":"13.0.4+8-Ubuntu-120.04","java_class_version":"57.0","http_nonProxyHosts":"null","http_proxyHost":"null","enabled":true,"service":"springjni-0.0.1-SNAPSHOT","agent_url":"http://localhost:8126","agent_error":true,"debug":false,"analytics_enabled":false,"sampling_rules":[{},{}],"priority_sampling_enabled":true,"logs_correlation_enabled":false,"profiling_enabled":false,"dd_version":"0.68.0~ca80da2a6","health_checks_enabled":true,"configuration_file":"no config file present","runtime_id":"ca79777e-c2d8-42d8-822d-0473bc8cd637"}
2021-01-02 14:44:09.696  INFO 199 --- [           main] o.s.s.concurrent.ThreadPoolTaskExecutor  : Initializing ExecutorService 'applicationTaskExecutor'
2021-01-02 14:44:09.971  INFO 199 --- [           main] o.s.s.c.ThreadPoolTaskScheduler          : Initializing ExecutorService 'taskScheduler'
2021-01-02 14:44:09.977  INFO 199 --- [           main] o.s.b.a.e.web.EndpointLinksResolver      : Exposing 2 endpoint(s) beneath base path '/actuator'
2021-01-02 14:44:10.009  INFO 199 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8080 (http) with context path ''
2021-01-02 14:44:10.038  INFO 199 --- [           main] c.d.pej.springjni.SpringjniApplication   : Started SpringjniApplication in 3.007 seconds (JVM running for 3.491)
```

### Testing the app

Open a new terminal and run the following command

```sh
[root@ubuntu:~]$ curl localhost:8080/
C++ ended job done...
```

