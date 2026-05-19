package io.github.tanphat1095.excel.reader.resolver.transfer;

public class DoubleToIntegerResolver implements SourceToTargetResolver<Double, Integer> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Double.class == source && Integer.class == target;
    }

    @Override
    public Integer resolve(Double source) {
        return source == null ? null : source.intValue();
    }
}
