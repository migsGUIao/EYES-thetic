package com.example.eyesthetic;

public class ClosetItem {
    private String name;
    private String type;
    private String color;
    private String gender;
    private String season;
    private String usage;
    private String imageUrl;
    private String id;

    public ClosetItem() {} // Required by Firestore

    public ClosetItem(String name, String type, String color, String gender, String season, String usage, String imageUrl) {
        this.name = name;
        this.type = type;
        this.color = color;
        this.gender = gender;
        this.season = season;
        this.usage = usage;
        this.imageUrl = imageUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }


}
