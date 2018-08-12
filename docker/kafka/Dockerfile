FROM wurstmeister/kafka:2.11-1.1.1
LABEL maintainer="wojda@woodenmill.io"

COPY prom-jmx-agent-config.yml /etc/kafka/prom-jmx-agent-config.yml
ADD http://central.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.10/jmx_prometheus_javaagent-0.10.jar /etc/kafka/jmx_prometheus_javaagent.jar
