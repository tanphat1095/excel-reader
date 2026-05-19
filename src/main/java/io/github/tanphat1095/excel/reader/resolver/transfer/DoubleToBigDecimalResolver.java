package io.github.tanphat1095.excel.reader.resolver.transfer;

import java.math.BigDecimal;

public class DoubleToBigDecimalResolver implements SourceToTargetResolver<Double, BigDecimal> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Double.class == source && BigDecimal.class == target;
    }

    @Override
    public BigDecimal resolve(Double source) {
        return source == null ? null : BigDecimal.valueOf(source);
    }
}
