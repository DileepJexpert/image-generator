package com.katixo.studio.config;

import com.katixo.ai.commons.gpu.GpuGuardConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.time.Duration;

/**
 * Tunables for the shared single-GPU guard, bound from {@code katixo.ai.gpu.*}.
 *
 * <p><b>Critical:</b> for the guard to actually serialize this app against katixo-docai, both apps
 * must run with {@code lock-mode: postgres} and point {@code lock-datasource} at the SAME database
 * (the shared "lock authority").
 */
@ConfigurationProperties(prefix = "katixo.ai.gpu")
public class GpuGuardProperties {

    private String lockMode = "postgres";
    private long lockKey = GpuGuardConfig.DEFAULT_LOCK_KEY;
    private Duration acquireTimeout = Duration.ofSeconds(120);
    private Duration pollInterval = Duration.ofMillis(250);
    private Duration maxHold = Duration.ofMinutes(15);

    @NestedConfigurationProperty
    private LockDatasource lockDatasource = new LockDatasource();

    public String getLockMode() { return lockMode; }
    public void setLockMode(String lockMode) { this.lockMode = lockMode; }
    public long getLockKey() { return lockKey; }
    public void setLockKey(long lockKey) { this.lockKey = lockKey; }
    public Duration getAcquireTimeout() { return acquireTimeout; }
    public void setAcquireTimeout(Duration acquireTimeout) { this.acquireTimeout = acquireTimeout; }
    public Duration getPollInterval() { return pollInterval; }
    public void setPollInterval(Duration pollInterval) { this.pollInterval = pollInterval; }
    public Duration getMaxHold() { return maxHold; }
    public void setMaxHold(Duration maxHold) { this.maxHold = maxHold; }
    public LockDatasource getLockDatasource() { return lockDatasource; }
    public void setLockDatasource(LockDatasource lockDatasource) { this.lockDatasource = lockDatasource; }

    public static class LockDatasource {
        private String url = "jdbc:postgresql://localhost:5432/katixo_gpu";
        private String username = "katixo";
        private String password = "katixo";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
