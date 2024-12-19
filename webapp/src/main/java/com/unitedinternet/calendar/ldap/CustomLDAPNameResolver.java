package com.unitedinternet.calendar.ldap;
import com.unboundid.ldap.sdk.NameResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class CustomLDAPNameResolver extends NameResolver {

    private final CustomNameResolver resolver;

    public CustomLDAPNameResolver(String localNetworkPrefix) {
        this.resolver = new CustomNameResolver(localNetworkPrefix);
    }

    @Override
    public InetAddress getByName(String host) throws UnknownHostException {
        // Return the first IP address from the sorted list
        List<InetAddress> sortedAddresses = resolver.resolve(host);
        return sortedAddresses.get(0);
    }

    @Override
    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        // Return all IP addresses in sorted order
        List<InetAddress> sortedAddresses = resolver.resolve(host);
        return sortedAddresses.toArray(new InetAddress[0]);
    }

    @Override
    public void toString(StringBuilder stringBuilder) {

    }
}
