package com.hercey.config;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class AppConfig extends ResourceConfig {

    public AppConfig() {
        packages("com.hercey.controller");
        packages("com.hercey.middleware");
        register(MultiPartFeature.class);
    }
}