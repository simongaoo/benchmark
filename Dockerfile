# benchmark
#
# VERSION               0.0.1
FROM openjdk:latest

ADD . /benchmark/

WORKDIR /benchmark

# run command
# CMD [ "java", "-jar", "lib/log-receiver-0.0.1.jar" ]