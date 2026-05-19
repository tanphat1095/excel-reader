package io.github.tanphat1095.excel.reader.resolver.transfer;

public class DoubleToLongResolver implements SourceToTargetResolver<Double, Long> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Double.class == source && Long.class == target;
    }

    @Override
    public Long resolve(Double source) {
        return source == null ? null : source.longValue();
    }
}
