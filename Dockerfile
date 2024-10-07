# Image
FROM alpine:3.19


# Install requirements

RUN apk update && \
    apk add \
        bash \
        openjdk17-jdk \
        maven \
        git \
        zip;

RUN export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))";

# Maven on cosmo

WORKDIR /home/cosmo
COPY . /home/cosmo/

RUN mvn clean && \
    mvn package -Dmaven.test.skip && \
    mvn install -Dmaven.test.skip;


# Maven on webapp

WORKDIR /home/cosmo/webapp

RUN mvn clean && \
    mvn package spring-boot:repackage;


# Fix yaml

ENV SPRINGBOOT_PORT=43345
ENV SPRINGBOOT_HEALTHPORT=50801

COPY application.yaml.test3 /tmp/application.yaml.test3

WORKDIR /home/cosmo/webapp/target

RUN mkdir -p BOOT-INF/classes/ && \
    cp /tmp/application.yaml.test3 BOOT-INF/classes/application.yaml && \
    zip -r webapp.jar BOOT-INF/classes/application.yaml;


# Run

WORKDIR /home/cosmo/webapp

CMD ["java -jar target/webapp.jar"]
# CMD ["/bin/bash"]

