#!/usr/bin/env bash

java \
  -agentpath:/tmp/YourKit-JavaProfiler-2017.02/bin/linux-x86-64/libyjpagent.so \
  -cp /data/app/server.jar \
  org.ecplipse.jetty.threadtest.ThreadCountRepro
