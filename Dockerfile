FROM quay.io/wildfly/wildfly:latest

USER root
RUN dnf update -y || microdnf update -y

USER jboss

COPY --chown=jboss:root hercey.war /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]