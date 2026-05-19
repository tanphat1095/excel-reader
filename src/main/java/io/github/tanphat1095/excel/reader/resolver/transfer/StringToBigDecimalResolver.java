package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;

import java.math.BigDecimal;

public class StringToBigDecimalResolver implements SourceToTargetResolver<String, BigDecimal> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return String.class == source && BigDecimal.class == target;
    }

    @Override
    public BigDecimal resolve(String source) {
        if (source == null || source.isBlank()) return null;
        try {
            return new BigDecimal(source.trim());
        } catch (NumberFormatException e) {
            throw new ExcelReaderException("Cannot parse '" + source.trim() + "' as BigDecimal");
        }
    }
}
