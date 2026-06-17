package com.katixo.studio.job;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Maps {@link JobStatus} to its lowercase text value in the database. */
@Converter(autoApply = true)
public class JobStatusConverter implements AttributeConverter<JobStatus, String> {

    @Override
    public String convertToDatabaseColumn(JobStatus attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public JobStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : JobStatus.fromValue(dbData);
    }
}
