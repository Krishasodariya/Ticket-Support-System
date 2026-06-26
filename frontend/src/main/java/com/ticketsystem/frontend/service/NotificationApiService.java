package com.ticketsystem.frontend.service;

import com.ticketsystem.frontend.model.NotificationFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class NotificationApiService {

    public List<NotificationFX> getMyNotifications() throws Exception {
        NotificationFX[] arr = ApiClient.get("/notifications/my", NotificationFX[].class);
        return Arrays.asList(arr);
    }

    public NotificationFX markAsRead(String id) throws Exception {
        return ApiClient.patch("/notifications/" + id + "/read", Map.of(), NotificationFX.class);
    }

    // [Nzchupa | 2026-06-12] TS-001: Hilfsmethode — alle ungelesenen Nachrichten als gelesen markieren
    // Helper — marks all unread notifications as read; errors are silently ignored (best-effort)
    public void markAllAsRead(List<NotificationFX> notifications) {
        for (NotificationFX n : notifications) {
            try {
                if (!n.isRead()) {
                    markAsRead(n.getId());
                }
            } catch (Exception ignored) { }
        }
    }

    // [Nzchupa | 2026-06-26] KAT-91: Einzelne Benachrichtigung löschen
    // Deletes a single notification via the new DELETE endpoint
    public void deleteNotification(String id) throws Exception {
        ApiClient.delete("/notifications/" + id);
    }

    @SuppressWarnings("unchecked")
    public long getUnreadCount() throws Exception {
        Map<String, Object> result = ApiClient.get("/notifications/unread-count", Map.class);
        Object unread = result.get("unread");
        if (unread instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }
}
