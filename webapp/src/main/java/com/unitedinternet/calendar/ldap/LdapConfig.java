package com.unitedinternet.calendar.ldap;
import com.unboundid.ldap.sdk.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LdapConfig {

    @Value("${ldap.urls}")
    private String ldapUrls;

    @Value("${ldap.auth.manager.username}")
    private String username;

    @Value("${ldap.auth.manager.password}")
    private String password;

    @Value("${ldap.pool.size}")
    private int poolSize;

    @Value("${ldap.pool.health-check-interval}")
    private long healthCheckInterval;

    @Value("${ldap.connection.timeout}")
    private int connectionTimeout;

    @Bean
    public LDAPConnectionPool ldapConnectionPool() throws LDAPException {
        // Define the local network prefix
        String localNetworkPrefix = "192.168.30.";

        // Set custom NameResolver
        LDAPConnectionOptions options = new LDAPConnectionOptions();
        options.setNameResolver(new CustomLDAPNameResolver(localNetworkPrefix));
        options.setConnectTimeoutMillis(connectionTimeout);

        // Configure LDAP connection pool
        //return new LDAPConnectionPool("dnstest.kempo.eu", 389, options, null, 10);

        // Parse the LDAP URLs from the configuration
        String[] servers = ldapUrls.split(",");

        // Extract hostnames and ports from the URLs
        String[] hostnames = new String[servers.length];
        int[] ports = new int[servers.length];
        for (int i = 0; i < servers.length; i++) {
            String[] parts = servers[i].replace("ldap://", "").split(":");
            hostnames[i] = parts[0];
            ports[i] = Integer.parseInt(parts[1]);
        }

        // Configure FailoverServerSet
        FailoverServerSet failoverServerSet = new FailoverServerSet(hostnames, ports);

        // BindRequest for authentication
        BindRequest bindRequest = new SimpleBindRequest(username, password);

        // Create and configure the LDAP connection pool
        LDAPConnectionPool connectionPool = new LDAPConnectionPool(failoverServerSet, bindRequest, poolSize);
        connectionPool.setHealthCheckIntervalMillis(healthCheckInterval);

        return connectionPool;
    }
}
