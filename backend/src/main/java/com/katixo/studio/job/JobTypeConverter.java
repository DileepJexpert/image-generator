package com.katixo.studio.job;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link JobType} to its lowercase text value in the database. */
@Converter(autoApply = true)
public class JobTypeConverter implements AttributeConverter<JobType, String> {

    @Override
    public String convertToDatabaseColumn(JobType attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public JobType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : JobType.fromValue(dbData);
    }
}
