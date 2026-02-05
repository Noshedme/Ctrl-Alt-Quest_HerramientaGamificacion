package com.ctrlaltquest.models;

public class StoreOffer {
    private int id;
    private String title;
    private String description;
    private double price;
    private boolean isPremium;
    private String imagePath; // Puede ser una ruta o un emoji

    public StoreOffer(int id, String title, String description, double price, boolean isPremium, String imagePath) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.isPremium = isPremium;
        this.imagePath = imagePath;
    }

    // --- GETTERS ---

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public String getImagePath() {
        return imagePath;
    }
}