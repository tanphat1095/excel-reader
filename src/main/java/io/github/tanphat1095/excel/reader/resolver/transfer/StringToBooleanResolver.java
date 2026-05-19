package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;

import java.util.Set;

public class StringToBooleanResolver implements SourceToTargetResolver<String, Boolean> {

    private static final Set<String> TRUE_VALUES  = Set.of("true", "yes", "1", "y");
    private static final Set<String> FALSE_VALUES = Set.of("false", "no", "0", "n");

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return String.class == source && Boolean.class == target;
    }

    @Override
    public Boolean resolve(String source) {
        if (source == null || source.isBlank()) return null;
        String lower = source.trim().toLowerCase();
        if (TRUE_VALUES.contains(lower))  return Boolean.TRUE;
        if (FALSE_VALUES.contains(lower)) return Boolean.FALSE;
        throw new ExcelReaderException(
                "Cannot parse '" + source.trim() + "' as Boolean. Accepted values: true/false, yes/no, 1/0, y/n");
    }
}
