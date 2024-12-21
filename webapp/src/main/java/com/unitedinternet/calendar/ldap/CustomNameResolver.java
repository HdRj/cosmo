package com.unitedinternet.calendar.ldap;



import com.unboundid.ldap.sdk.NameResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CustomNameResolver extends NameResolver {

    private final String localNetworkPrefix; // Example: "192.168.30."

    public CustomNameResolver(String localNetworkPrefix) {
        this.localNetworkPrefix = localNetworkPrefix;
    }

    @Override
    public InetAddress getByName(String host) throws UnknownHostException {
        List<InetAddress> prioritizedAddresses = resolveAndPrioritize(host);
        if (!prioritizedAddresses.isEmpty()) {
            return prioritizedAddresses.get(0); // Return the first (prioritized) address.
        }
        throw new UnknownHostException("No suitable IP address found for hostname: " + host);
    }

    @Override
    public InetAddress[] getAllByName(String host) throws UnknownHostException {
        List<InetAddress> prioritizedAddresses = resolveAndPrioritize(host);
        return prioritizedAddresses.toArray(new InetAddress[0]);
    }

    @Override
    public void toString(StringBuilder stringBuilder) {

    }

    private List<InetAddress> resolveAndPrioritize(String host) throws UnknownHostException {
        // Resolve all addresses for the given hostname.
        InetAddress[] allAddresses = InetAddress.getAllByName(host);

        // Separate into "same network" and "other network" groups.
        List<InetAddress> sameNetwork = new ArrayList<>();
        List<InetAddress> otherNetwork = new ArrayList<>();

        for (InetAddress address : allAddresses) {
            String ip = address.getHostAddress();
            if (ip.startsWith(localNetworkPrefix)) {
                sameNetwork.add(address);
            } else {
                otherNetwork.add(address);
            }
        }

        // Combine "same network" and "other network" lists.
        List<InetAddress> sortedAddresses = new ArrayList<>();
        sortedAddresses.addAll(sameNetwork);
        sortedAddresses.addAll(otherNetwork);

        return sortedAddresses;
    }

    public String resolve(String host) throws UnknownHostException {
        InetAddress resolvedAddress = getByName(host);
        return resolvedAddress.getHostAddress(); // Return the resolved IP as a string.
    }
}
