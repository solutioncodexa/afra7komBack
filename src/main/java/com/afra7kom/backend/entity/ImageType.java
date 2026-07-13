package com.afra7kom.backend.entity;

public enum ImageType {
    UPLOAD("Upload"),
    URL("URL");
    
    private final String displayName;
    
    ImageType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
