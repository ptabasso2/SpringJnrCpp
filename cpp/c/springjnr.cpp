#include <stdio.h>
#include <datadog/opentracing.h>
#include <opentracing/tracer.h>
#include <opentracing/propagation.h>
#include <iostream>
#include <string>
#include "text_map_carrier.h"

using namespace datadog::opentracing;

extern "C" int runcppkv(int m, int n, char** keys, char** values);
extern "C" int sum(int m, int n);


int runcppkv(int m, int n, char** keys, char** values){
  int result;
  printf("Printing key-values pairs in CPP:\n");
  for (int i=0;*(keys+i)!=nullptr;i++){
    std::cout<<*(keys+i)<<": "<<*(values+i)<<std::endl;
  }
  datadog::opentracing::TracerOptions tracer_options{"localhost", 8126, "cppservice"};
  auto tracer = datadog::opentracing::makeTracer(tracer_options);
  {
    std::unordered_map<std::string, std::string> text_map;
    for (int i=0;*(keys+i)!=nullptr;i++){
      text_map[std::string(*(keys+i))]=std::string(*(values+i));
    }

    TextMapCarrier carrier(text_map);
    auto span_context=tracer->Extract(carrier);
    auto span=tracer->StartSpan("nativeCode", {ChildOf(span_context->get())});

    span->SetTag("cppspan", 123);
    result= sum(m,n);
    span->Finish();
  }
  tracer->Close();
  return result;
}

int sum(int m, int n){
        int result = m + n;

        printf("C++ called!! This message printed by CPP! result=%d\n", result);
        fflush(stdout);
  return result;
}

