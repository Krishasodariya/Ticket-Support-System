package com.ticketsystem.frontend.service;

import com.ticketsystem.dto.request.CreateCommentRequest;
import com.ticketsystem.frontend.model.CommentFX;
import com.ticketsystem.frontend.util.ApiClient;

import java.util.Arrays;
import java.util.List;

public class CommentApiService {
    public List<CommentFX> getComments(String ticketId) throws Exception {
        CommentFX[] arr = ApiClient.get("/tickets/" + ticketId + "/comments", CommentFX[].class);
        return Arrays.asList(arr);
    }

    public CommentFX createComment(String ticketId, String content, boolean isInternal) throws Exception {
        CreateCommentRequest req = new CreateCommentRequest();
        req.setContent(content);
        req.setInternal(isInternal);
        return ApiClient.post("/tickets/" + ticketId + "/comments", req, CommentFX.class);
    }
}
