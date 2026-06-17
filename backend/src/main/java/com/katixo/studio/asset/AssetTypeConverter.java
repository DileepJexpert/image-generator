package com.katixo.studio.asset;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link AssetType} to its lowercase text value in the database. */
@Converter(autoApply = true)
public class AssetTypeConverter implements AttributeConverter<AssetType, String> {

    @Override
    public String convertToDatabaseColumn(AssetType attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public AssetType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AssetType.fromValue(dbData);
    }
}
