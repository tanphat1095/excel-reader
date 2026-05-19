package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;

public class StringToLongResolver implements SourceToTargetResolver<String, Long> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return String.class == source && Long.class == target;
    }

    @Override
    public Long resolve(String source) {
        if (source == null || source.isBlank()) return null;
        try {
            return (long) Double.parseDouble(source.trim());
        } catch (NumberFormatException e) {
            throw new ExcelReaderException("Cannot parse '" + source.trim() + "' as Long");
        }
    }
}
