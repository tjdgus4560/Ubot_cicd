package com.autric.upbit.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class AppJwtProperties {

    private String secret;
    private long accessExpired;
    private long refreshExpired;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessExpired() {
        return accessExpired;
    }

    public void setAccessExpired(long accessExpired) {
        this.accessExpired = accessExpired;
    }

    public long getRefreshExpired() {
        return refreshExpired;
    }

    public void setRefreshExpired(long refreshExpired) {
        this.refreshExpired = refreshExpired;
    }
}
