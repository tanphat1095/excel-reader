package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;

public class StringToDoubleResolver implements SourceToTargetResolver<String, Double> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return String.class == source && Double.class == target;
    }

    @Override
    public Double resolve(String source) {
        if (source == null || source.isBlank()) return null;
        try {
            return Double.parseDouble(source.trim());
        } catch (NumberFormatException e) {
            throw new ExcelReaderException("Cannot parse '" + source.trim() + "' as Double");
        }
    }
}
