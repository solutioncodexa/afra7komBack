package com.afra7kom.backend.entity;

public enum PackType {
    PACK("Pack"),
    BUFFET("Buffet"),
    PACK_BUFFET("Pack + Buffet"),
    MATERIEL("Matériel"),
    CADEAU("Cadeau"),
    GATEAU("Gâteau");
    
    private final String displayName;
    
    PackType(String displayName) {
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
