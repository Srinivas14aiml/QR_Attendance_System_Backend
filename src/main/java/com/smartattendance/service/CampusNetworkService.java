package com.smartattendance.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

@Service
public class CampusNetworkService {

    private final List<String> allowedCidrs;

    public CampusNetworkService(@Value("${attendance.allowed-cidrs:192.168.0.0/16}") String allowedCidrs) {
        this.allowedCidrs = Arrays.stream(allowedCidrs.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public boolean isAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        return allowedCidrs.stream().anyMatch(cidr -> matches(ipAddress, cidr));
    }

    private boolean matches(String ipAddress, String cidr) {
        try {
            String[] parts = cidr.split("/");
            byte[] address = InetAddress.getByName(ipAddress).getAddress();
            byte[] network = InetAddress.getByName(parts[0]).getAddress();
            int prefix = Integer.parseInt(parts[1]);

            int fullBytes = prefix / 8;
            int remainingBits = prefix % 8;
            for (int index = 0; index < fullBytes; index++) {
                if (address[index] != network[index]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = (-1) << (8 - remainingBits);
            return (address[fullBytes] & mask) == (network[fullBytes] & mask);
        } catch (UnknownHostException | NumberFormatException ex) {
            return false;
        }
    }
}
