package io.github.tanphat1095.excel.reader.resolver.transfer;

public class SameBooleanResolver extends SameTypeResolver<Boolean> {
    @Override
    boolean supports(Class<?> source) {
        return Boolean.class == source;
    }
}
