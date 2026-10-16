FROM registry.redhat.io/jboss-eap-8/eap8-openjdk17-builder-openshift-rhel8:latest AS builder

ENV GALLEON_PROVISION_FEATURE_PACKS="org.jboss.eap:wildfly-ee-galleon-pack,org.jboss.eap.cloud:eap-cloud-galleon-pack"
# FIX: Changed 'admin-console' to 'management-console'
ENV GALLEON_PROVISION_LAYERS="jaxrs-server,management-console"
ENV GALLEON_PROVISION_CHANNELS="org.jboss.eap.channels:eap-8.0"

RUN /usr/local/s2i/assemble

FROM registry.access.redhat.com/ubi9/openjdk-17:1.20

ENV JBOSS_HOME=/opt/jboss-eap/
COPY --from=builder --chown=185:0 /opt/server /opt/jboss-eap

COPY --chown=185:0 hercey.war $JBOSS_HOME/standalone/deployments/

RUN $JBOSS_HOME/bin/add-user.sh -u admin -p "Admin#12345" --silent

RUN if grep -q 'console-enabled=' $JBOSS_HOME/standalone/configuration/standalone.xml; then \
    sed -i 's/console-enabled="[^"]*"/console-enabled="true"/g' $JBOSS_HOME/standalone/configuration/standalone.xml; \
    else \
    sed -i 's/<http-interface /<http-interface console-enabled="true" /g' $JBOSS_HOME/standalone/configuration/standalone.xml; \
    fi

RUN chmod -R ug+rwX $JBOSS_HOME
EXPOSE 8080 9990
USER 185

CMD ["/opt/jboss-eap/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "-Dwildfly.management.console.enabled=true"]