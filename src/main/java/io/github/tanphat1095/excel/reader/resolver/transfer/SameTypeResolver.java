package io.github.tanphat1095.excel.reader.resolver.transfer;


public abstract class SameTypeResolver<T> implements SourceToTargetResolver<T, T>{

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return supports(source) && supports(target);
    }

    abstract boolean supports(Class<?> source);

    @Override
    public T resolve(T source) {
        return source;
    }
}
