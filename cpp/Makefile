# Remove file macro

define rm-test
    @test -z "$1" || rm -rf "$1"
endef

# Depending on architecture we want to use either
# dylib or so for library extention
#
ARCH=$(shell uname -s | tr '[:upper:]' '[:lower:]')
ifeq ($(ARCH),darwin)
  EXT=dylib
else
  EXT=so
endif

# Since release 9 of JDK, server is no longer
# part of jre. We have to look for it inside
# lib/server instead of jre/lib/server
# however, for jdk 1.8 we still have to look inside
# jre - this is why we have this fancy directory
# checking here
#
JAVA_SERVER_DIR=${JAVA_HOME}/jre/lib/server
ifneq "$(wildcard $(JAVA_SERVER_DIR) )" ""
  JAVA_SERVER_LIB=${JAVA_HOME}/jre/lib/server
else
  JAVA_SERVER_LIB=${JAVA_HOME}/lib/server
endif

# depending on architecture we have to set linker settings
# while linking with libjvm.so/libjvm.dylib
# Be careful with mac os!!
#
# If you don't use -rpath while linking, your code
# will be linked with /usr/local/lib/libjvm.dylib
# This will be a dissaster if your custom installation
# inside /Library/Java/JavaVirtualMachines/
#
ifeq ($(ARCH),darwin)
  CC=clang
  CXX=clang++
  LD_FLAGS_JAVA=-L${JAVA_SERVER_LIB} -rpath ${JAVA_SERVER_LIB} -ljvm
  LD_FLAGS=-L/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk
  LD_FLAGS+=-arch x86_64
  LD_FLAGS+=-macosx_version_min 10.14.0 -lSystem
  LD_FLAGS+=$(LD_FLAGS_JAVA)
else
  CC=gcc
  CXX=g++
  LD_FLAGS=-Wl,-rpath,${JAVA_SERVER_LIB} -L${JAVA_SERVER_LIB} -ljvm
endif

JVM_INCLUDE=-I${JAVA_HOME}/include -I${JAVA_HOME}/include/$(ARCH) \


all: compilec

compilec:
        g++ -std=c++14 -g -shared -fpic c/springjnr.cpp -o lib/libspringjnr.so -ldd_opentracing -lopentracing

clean: clean-common
        $(call rm-test,c/passHashMap.h)

clean-common:
        @test -z "target/*" || rm -rf target/*
        @test -z "lib/*" || rm -rf lib/*


