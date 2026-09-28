FROM quay.io/wildfly/wildfly:latest

COPY hercey.war /opt/jboss/wildfly/standalone/deployments/

USER root
RUN yum update -y

USER jboss

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]