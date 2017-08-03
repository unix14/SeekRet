package com.example.android.mychat;

import java.util.Date;

/**
 * Created by eyal on 25-Jul-17.
 */

public class ChatMessage {

    private String messageText;
    private String messageUser;
    private long messageTime;
    private String photoUrl;

    public ChatMessage(String messageText, String messageUser) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        messageTime = new Date().getTime();
    }

    public ChatMessage() {
    }

    public ChatMessage(ChatUser chatUser,String text) {
        messageUser = chatUser.getUserName();
        messageText = text;
        messageTime = new Date().getTime();
    }

    public ChatMessage(String messageText, String messageUser , String image) {
        this.messageText = messageText;
        this.messageUser = messageUser;
        messageTime = new Date().getTime();
        photoUrl = image;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getMessageUser() {
        return messageUser;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public long getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }
}
