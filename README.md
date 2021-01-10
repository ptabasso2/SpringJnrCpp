# SpringBoot / JNR Tracing from Java to Native code

## Instructions


A detailed step-by-step showing how tracing can be implemented for a SpringBoot app loading a C++ library.
Tracing will be implemented on both layers.

The environment used in this tutorial is based on a linux Ubuntu (20.04) virtual machine running on Mac OS X. 
If you simply need to run the environment you might want to use the `Dockerfile.springjnr` and `docker-compose.yml` files provided and skip the tutorial details.<br>
(And then jump straight to the last two sections "Running the app" and "Testing the app").

Build and spin up both the DD Agent container and the application container using the following commands. 
You need to provide your API key

```sh
pejman@macosx:~ $ docker build -f Dockerfile.springjnr -t springjnrimg .
pejman@macosx:~ $ export DD_API_KEY=cdxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
pejman@macosx:~ $ docker-compose up -d
Creating springjnr ... done
Creating dd-agent  ... done
pejman@macosx:~ $ docker exec -it springjnr bash
[root@ubuntu:~]$ 
```

<br>

### Preliminary tasks and first time steps

***Clone this repository***

```sh
[root@ubuntu:~]$ git clone https://github.com/ptabasso2/SpringJnrCpp
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
export LD_LIBRARY_PATH=/root/dd-opentracing-cpp/.build:/root/opentracing-cpp/.build/output:/root/SpringJnrCpp/cpp/lib
```

```sh
[root@ubuntu:~]$ source ~/.bashrc
```

***Initial directory structure***

```sh
[root@ubuntu:~]$ echo $HOME
/root
[root@ubuntu:~]$ cd SpringJnrCpp
[root@ubuntu:~/SpringJnrCpp]$ tree
.
├── cpp
│   ├── c
│   │   ├── springjnr.cpp
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
            │               └── springjnr
            │                   ├── SpringController.java
            │                   ├── INative.java
            │                   └── SpringjnrApplication.java
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
SpringJnrCpp
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


### Build the Springboot app

```sh
[root@ubuntu:~/SpringJnrCpp/springboot]$ gradle build
```

This build the final artifact `springjnr-0.0.1-SNAPSHOT.jar` placed under `$HOME/SpringJnrCpp/springboot/build/libs`


### Build the c++ lib

```sh
[root@ubuntu:~/SpringJnrCpp/springboot]$ cd ../cpp
[root@ubuntu:~/SpringJnrCpp/cpp]$ make
```

This will place the `libspringjnr.so` library in the `$HOME/SpringJnrCpp/cpp/lib` directory

### Running the app

Setting the `LD_LIBRARY_PATH` variable to point to the location of the newly created library. It actually tells the spring boot app where to locate it.
If not specified, it will fail at startup. 

```sh
[root@ubuntu:~/SpringJnrCpp/springboot]$ java -jar ./build/libs/springjnr-0.0.1-SNAPSHOT.jar
```

### Testing the app

Open a new terminal and run the following command

```sh
[root@ubuntu:~]$ curl localhost:8080/
C++ ended job done...
```

