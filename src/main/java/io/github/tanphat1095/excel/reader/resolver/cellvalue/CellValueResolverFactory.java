package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class CellValueResolverFactory {

    private final List<CellValueResolver<?>> resolvers;

    public CellValueResolverFactory(){
        this(new ArrayList<>());
        register(new StringResolver());
        register(new DateResolver());
        register(new BooleanResolver());
        register(new DoubleResolver());
    }

    public void register(CellValueResolver<?> resolver) {
        resolvers.add(resolver);
    }

    public CellValueResolver<?> getCellValueResolver(Class<?> source) {
        return resolvers.stream().filter(r -> r.supports(source)).findFirst().orElse(null);
    }
}
