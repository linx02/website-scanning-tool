package com.leads.leadsgen.services;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Registers a new SSE client and returns the emitter.
     *
     * @return  SseEmitter instance
     */
    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter();
        String emitterId = UUID.randomUUID().toString();
        emitters.put(emitterId, emitter);

        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    emitter.send(SseEmitter.event().data("keep-alive"));
                } catch (Exception e) {
                    emitters.remove(emitterId);
                    timer.cancel();
                }
            }
        }, 0, 15000);

        emitter.onCompletion(() -> emitters.remove(emitterId));
        emitter.onTimeout(() -> emitters.remove(emitterId));

        return emitter;
    }

    /**
     * Sends a status update to all connected SSE clients.
     *
     * @param domain          Domain being scanned
     * @param status          Current status
     * @param additionalData  Additional data to send
     */
    public void broadcastStatus(String domain, String status, Map<String, String> additionalData) {
        List<String> deadEmitterIds = new ArrayList<>();

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                Map<String, String> payload = new HashMap<>(additionalData);
                payload.put("domain", domain);
                payload.put("status", status);

                emitter.send(payload);
            } catch (Exception e) {
                deadEmitterIds.add(emitterId);
            }
        }

        for (String id : deadEmitterIds) {
            emitters.remove(id);
        }
    }
}