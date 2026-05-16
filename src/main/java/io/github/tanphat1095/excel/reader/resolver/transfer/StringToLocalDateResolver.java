package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@AllArgsConstructor
public class StringToLocalDateResolver implements SourceToTargetResolver<String, LocalDate> {

    private final DateTimeFormatter formatter;

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return String.class == source && LocalDate.class == target;
    }

    @Override
    public LocalDate resolve(String source) {
        if (source == null || source.isEmpty())
            return null;
        try {
            return LocalDate.parse(source.trim(), formatter);
        } catch (DateTimeParseException ex) {
            throw new ExcelReaderException("The text: "+ ex.getParsedString() + " was in wrong format");
        } catch (Exception ex) {
            throw new ExcelReaderException(ex.getMessage());
        }
    }
}
