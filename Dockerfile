FROM centos:7

RUN yum -y update && \
  yum -y install java-1.8.0-openjdk unzip curl

RUN cd /tmp && \
  curl -O https://www.yourkit.com/download/YourKit-JavaProfiler-2017.02-b75.zip && \
  unzip YourKit-JavaProfiler-2017.02-b75.zip

ADD jetty-thread-test/target/server.jar /data/app/
ADD run.sh /data/app/
ADD keys.jceks /data/app/

EXPOSE 10001
EXPOSE 1338

ENTRYPOINT ["/data/app/run.sh"]
