package com.unitedinternet.calendar.ldap;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CustomNameResolver {

    private final String localNetworkPrefix; // Example: "192.168.30."

    public CustomNameResolver(String localNetworkPrefix) {
        this.localNetworkPrefix = localNetworkPrefix;
    }

    /**
     * Resolve and prioritize IP addresses.
     *
     * @param hostname The hostname to resolve.
     * @return A prioritized list of IP addresses.
     * @throws UnknownHostException If hostname resolution fails.
     */
    public List<InetAddress> resolve(String hostname) throws UnknownHostException {
        // Get all IP addresses for the hostname
        InetAddress[] allAddresses = InetAddress.getAllByName(hostname);

        // Separate into "same network" and "other network" groups
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

        // Combine "same network" and "other network" lists
        List<InetAddress> sortedAddresses = new ArrayList<>();
        sortedAddresses.addAll(sameNetwork);
        sortedAddresses.addAll(otherNetwork);

        return sortedAddresses;
    }

    public static void main(String[] args) {
        try {
            // Define the local network prefix (192.168.30.x in this case)
            String localNetworkPrefix = "192.168.30.";

            // Create a resolver instance
            CustomNameResolver resolver = new CustomNameResolver(localNetworkPrefix);

            // Test with the hostname "dnstest.kempo.eu"
            List<InetAddress> sortedAddresses = resolver.resolve("dnstest.kempo.eu");

            // Print the sorted addresses
            System.out.println("Prioritized IP Addresses:");
            for (InetAddress address : sortedAddresses) {
                System.out.println(address.getHostAddress());
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
