package vn.commentary.backend.user.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkPolicyTest {
    @Test
    void privateAndLoopbackAddressesAreAllowed() {
        for (String ip : new String[]{"127.0.0.1", "::1", "10.1.2.3", "172.16.0.5", "172.31.255.1",
                "192.168.1.20", "::ffff:192.168.0.9", "169.254.1.1", "fd12:3456::1", "fe80::1%eth0"}) {
            assertTrue(NetworkPolicy.isPrivateOrLoopback(ip), ip);
        }
    }

    @Test
    void publicOrMalformedAddressesAreRejected() {
        for (String ip : new String[]{"8.8.8.8", "172.32.0.1", "172.15.0.1", "192.169.1.1", "203.0.113.5",
                "2001:db8::1", "", null, "abc", "300.1.1.1", "1.2.3"}) {
            assertFalse(NetworkPolicy.isPrivateOrLoopback(ip), String.valueOf(ip));
        }
    }

    @Test
    void lanAddressesNeverFail() {
        assertNotNull(NetworkPolicy.lanAddresses());
    }
}
