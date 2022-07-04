package com.example.jukapp;

public class Message {
    private String Author;
    private String textOfMessage;
    private long date;
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getAuthor() {
        return Author;
    }

    public void setAuthor(String author) {
        Author = author;
    }

    public String getTextOfMessage() {
        return textOfMessage;
    }

    public void setTextOfMessage(String textOfMessage) {
        this.textOfMessage = textOfMessage;
    }

    public Message(String author, String textOfMessage, long date,String imageUrl) {
        Author = author;
        this.textOfMessage = textOfMessage;
        this.date = date;
        this.imageUrl=imageUrl;
    }

    public Message() {
    }
}
