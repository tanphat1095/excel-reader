package io.github.tanphat1095.excel.reader.resolver.transfer;

public class BooleanToStringResolver implements SourceToTargetResolver<Boolean, String> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Boolean.class == source && String.class == target;
    }

    @Override
    public String resolve(Boolean source) {
        return source == null ? null : source.toString();
    }
}
