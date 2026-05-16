package io.github.tanphat1095.excel.reader.resolver.transfer;


import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;
import lombok.AllArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SourceToTargetResolverFactory {

    private final List<SourceToTargetResolver<?,?>> resolvers;
    private final String datePattern;
    public SourceToTargetResolverFactory(String datePattern) {
        this(new ArrayList<>(), datePattern);
        register(new StringToStringResolver());
        register(new StringToLocalDateResolver(DateTimeFormatter.ofPattern(this.datePattern)));
        register(new DateToLocalDateResolver());
        register(new SameDoubleResolver());
        resolvers.add(new SameBooleanResolver());
    }

    public void register(SourceToTargetResolver<?,?> resolver) {
        resolvers.add(resolver);
    }

    @SuppressWarnings("unchecked")
    public <S, T, P extends SourceToTargetResolver<S,T>> P getResolver(Class<S> source, Class<T> target){
        return (P) this.resolvers.stream()
                .filter(resolver -> resolver.supports(source, target))
                .findFirst()
                .orElseThrow(() -> new ExcelReaderException("There are no resolver for transfer from " + source.getName() + " to " + target.getName()));
    }

}
