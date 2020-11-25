package ru.mycrg.datasets.dto;

import java.time.LocalDateTime;

public class ResourceDescription {

    private String title;
    private String details;
    private String type;
    private String owner;
    private String identifier;
    private Integer itemsCount;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastModified = LocalDateTime.now();

    public ResourceDescription() {
    }

    public ResourceDescription(String title, String type, String identifier, int count) {
        this.title = title;
        this.type = type;
        this.identifier = identifier;
        this.itemsCount = count;
        this.owner = "arh_grad_rk@mail.ru";
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }

    public Integer getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(Integer itemsCount) {
        this.itemsCount = itemsCount;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}
