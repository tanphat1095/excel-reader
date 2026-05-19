package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;

public class StringToIntegerResolver implements SourceToTargetResolver<String, Integer> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return String.class == source && Integer.class == target;
    }

    @Override
    public Integer resolve(String source) {
        if (source == null || source.isBlank()) return null;
        try {
            return (int) Double.parseDouble(source.trim());
        } catch (NumberFormatException e) {
            throw new ExcelReaderException("Cannot parse '" + source.trim() + "' as Integer");
        }
    }
}
