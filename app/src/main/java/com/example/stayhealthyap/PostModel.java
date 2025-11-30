package com.example.stayhealthyap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostModel {
    private String username;
    private String userId;
    private String content;
    private int likes;
    private long timestamp;
    private boolean isReply;
    private String documentId;
    private String replyToId; // ID of the parent post
    private List<String> likedBy; // List of User IDs who liked this

    public PostModel() {}

    public PostModel(String username, String userId, String content, int likes, boolean isReply, String replyToId) {
        this.username = username;
        this.userId = userId;
        this.content = content;
        this.likes = likes;
        this.timestamp = new Date().getTime();
        this.isReply = isReply;
        this.replyToId = replyToId;
        this.likedBy = new ArrayList<>(); // Initialize empty list
    }

    // Getters
    public String getUsername() { return username; }
    public String getUserId() { return userId; }
    public String getContent() { return content; }
    public int getLikes() { return likes; }
    public long getTimestamp() { return timestamp; }
    public boolean isReply() { return isReply; }
    public String getDocumentId() { return documentId; }
    public String getReplyToId() { return replyToId; }

    // Safer getter for list to prevent crashes
    public List<String> getLikedBy() {
        if (likedBy == null) return new ArrayList<>();
        return likedBy;
    }

    // Setters
    public void setLikes(int likes) { this.likes = likes; }
    public void setReply(boolean reply) { isReply = reply; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }
}