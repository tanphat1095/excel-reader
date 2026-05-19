package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@AllArgsConstructor
public class StringToLocalDateTimeResolver implements SourceToTargetResolver<String, LocalDateTime> {

    private final DateTimeFormatter formatter;

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return String.class == source && LocalDateTime.class == target;
    }

    @Override
    public LocalDateTime resolve(String source) {
        if (source == null || source.isBlank()) return null;
        try {
            return LocalDateTime.parse(source.trim(), formatter);
        } catch (DateTimeParseException e) {
            throw new ExcelReaderException("Cannot parse '" + source.trim() + "' as LocalDateTime: " + e.getMessage());
        }
    }
}
