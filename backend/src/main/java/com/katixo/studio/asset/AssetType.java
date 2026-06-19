package com.katixo.studio.asset;

/** Media kind of a stored asset. Stored as lowercase text. */
public enum AssetType {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    TEXT("text");

    private final String value;

    AssetType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AssetType fromValue(String value) {
        for (AssetType t : values()) {
            if (t.value.equals(value)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown asset type: " + value);
    }
}
