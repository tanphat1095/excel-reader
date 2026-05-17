package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;
import io.github.tanphat1095.excel.reader.resolver.ResolverRegistry;

import java.time.format.DateTimeFormatter;

public class SourceToTargetResolverFactory extends ResolverRegistry<SourceToTargetResolver<?, ?>> {

    public SourceToTargetResolverFactory(String datePattern) {
        register(new StringToStringResolver());
        register(new StringToLocalDateResolver(DateTimeFormatter.ofPattern(datePattern)));
        register(new DateToLocalDateResolver());
        register(new SameDoubleResolver());
        register(new SameBooleanResolver());
    }

    @SuppressWarnings("unchecked")
    public <S, T, P extends SourceToTargetResolver<S, T>> P getResolver(Class<S> source, Class<T> target) {
        return (P) find(r -> r.supports(source, target))
                .orElseThrow(() -> new ExcelReaderException(
                        "There are no resolver for transfer from " + source.getName() + " to " + target.getName()));
    }
}
