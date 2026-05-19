package io.github.tanphat1095.excel.reader.resolver.transfer;

public class DoubleToStringResolver implements SourceToTargetResolver<Double, String> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Double.class == source && String.class == target;
    }

    @Override
    public String resolve(Double source) {
        if (source == null) return null;
        // Avoid "1.0" for whole numbers → return "1"
        if (source == Math.floor(source) && !Double.isInfinite(source)) {
            return String.valueOf(source.longValue());
        }
        return String.valueOf(source);
    }
}
