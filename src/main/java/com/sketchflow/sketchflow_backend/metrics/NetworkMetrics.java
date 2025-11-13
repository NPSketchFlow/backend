package com.sketchflow.sketchflow_backend.metrics;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class NetworkMetrics {

    private final AtomicLong totalPacketsReceived = new AtomicLong();
    private final AtomicLong totalAcksSent = new AtomicLong();
    private final AtomicLong totalRetransmissions = new AtomicLong();
    private final AtomicLong totalPacketDrops = new AtomicLong();

    private final ConcurrentHashMap<String, ClientRtt> clientRtts = new ConcurrentHashMap<>();

    public void incrementPacketsReceived() { totalPacketsReceived.incrementAndGet(); }
    public void incrementAcksSent() { totalAcksSent.incrementAndGet(); }
    public void incrementRetransmissions() { totalRetransmissions.incrementAndGet(); }
    public void incrementPacketDrops() { totalPacketDrops.incrementAndGet(); }

    public void recordRtt(String clientId, long rttMs) {
        clientRtts.compute(clientId, (k, v) -> {
            if (v == null) return new ClientRtt(1, rttMs);
            long count = v.count + 1;
            double avg = ((v.avg * v.count) + rttMs) / count;
            return new ClientRtt(count, avg);
        });
    }

    @GetMapping("/api/metrics")
    public Map<String, Object> metrics() {
        return Map.of(
                "totalPacketsReceived", totalPacketsReceived.get(),
                "totalAcksSent", totalAcksSent.get(),
                "totalRetransmissions", totalRetransmissions.get(),
                "totalPacketDrops", totalPacketDrops.get(),
                "clientRtts", clientRtts
        );
    }

    public static record ClientRtt(long count, double avg) {}
}
