package vn.commentary.backend.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.commentary.backend.user.dto.Dtos.NearbyMatch;
import vn.commentary.backend.user.dto.Dtos.ResolveResult;
import vn.commentary.backend.user.service.DiscoveryService;

import java.util.List;

/** Nhận diện đối tượng bằng QR, GPS hoặc Beacon (UC7.1, UC7.2.1). */
@RestController
@RequestMapping("/api/user")
public class UserDiscoveryController {
    private final DiscoveryService discovery;

    public UserDiscoveryController(DiscoveryService discovery) {
        this.discovery = discovery;
    }

    @GetMapping("/resolve/qr")
    public ResolveResult resolveQr(@RequestParam String code) {
        return discovery.resolveQr(code);
    }

    @GetMapping("/resolve/beacon")
    public ResolveResult resolveBeacon(@RequestParam String mac) {
        return discovery.resolveBeacon(mac);
    }

    @GetMapping("/nearby")
    public List<NearbyMatch> nearby(@RequestParam double lat, @RequestParam double lng) {
        return discovery.nearby(lat, lng);
    }
}
