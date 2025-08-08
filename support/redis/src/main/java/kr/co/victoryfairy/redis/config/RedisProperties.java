package kr.co.victoryfairy.redis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisProperties {

    private String host;
    private int port;
    private int database;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getDatabase() {
        return database;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDatabase(int database) {
        this.database = database;
    }
}
