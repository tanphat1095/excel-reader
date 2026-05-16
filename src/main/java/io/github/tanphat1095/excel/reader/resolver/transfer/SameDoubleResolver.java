package io.github.tanphat1095.excel.reader.resolver.transfer;

public class SameDoubleResolver extends SameTypeResolver<Double>{
    @Override
    boolean supports(Class<?> source) {
        return Double.class == source;
    }
}
