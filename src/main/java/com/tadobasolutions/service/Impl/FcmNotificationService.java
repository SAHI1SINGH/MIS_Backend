package com.tadobasolutions.service.Impl;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmNotificationService {



    public void sendToToken(String fcmToken, String title, String body) {
        sendToToken(fcmToken, title, body, null);
    }

    public void sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        if (fcmToken == null || fcmToken.isBlank()) {
            log.warn("FCM token is null/blank, skipping notification");
            return;
        }
        try {
            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setSound("default")
                                    .build())
                            .build());

            if (data != null) builder.putAllData(data);

            String response = FirebaseMessaging.getInstance().send(builder.build());
            log.info("✅ FCM sent: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("❌ FCM send failed for token {}: {}", fcmToken, e.getMessage());
        }
    }

    // ── Multiple tokens ───────────────────────────────────────────────────────

    public void sendToTokens(List<String> fcmTokens, String title, String body) {
        sendToTokens(fcmTokens, title, body, null);
    }

    public void sendToTokens(List<String> fcmTokens, String title, String body, Map<String, String> data) {
        if (fcmTokens == null || fcmTokens.isEmpty()) return;

        MulticastMessage.Builder builder = MulticastMessage.builder()
                .addAllTokens(fcmTokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setSound("default")
                                .build())
                        .build());

        if (data != null) builder.putAllData(data);

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
            log.info("✅ FCM multicast: {} success, {} failure",
                    response.getSuccessCount(), response.getFailureCount());
        } catch (FirebaseMessagingException e) {
            log.error("❌ FCM multicast failed: {}", e.getMessage());
        }
    }

    // ── Topic ─────────────────────────────────────────────────────────────────

    public void sendToTopic(String topic, String title, String body) {
        try {
            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ FCM topic '{}' sent: {}", topic, response);
        } catch (FirebaseMessagingException e) {
            log.error("❌ FCM topic send failed: {}", e.getMessage());
        }
    }
}