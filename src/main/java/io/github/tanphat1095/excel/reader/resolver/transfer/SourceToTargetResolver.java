package io.github.tanphat1095.excel.reader.resolver.transfer;

public interface SourceToTargetResolver<S,T>{
    boolean supports(Class<?> source, Class<?> target);
    T resolve(S source);
}
