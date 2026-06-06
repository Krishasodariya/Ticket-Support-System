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
