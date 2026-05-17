package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import io.github.tanphat1095.excel.reader.resolver.ResolverRegistry;

public class CellValueResolverFactory extends ResolverRegistry<CellValueResolver<?>> {

    public CellValueResolverFactory() {
        register(new StringResolver());
        register(new DateResolver());
        register(new BooleanResolver());
        register(new DoubleResolver());
    }

    public CellValueResolver<?> getCellValueResolver(Class<?> source) {
        return find(r -> r.supports(source)).orElse(null);
    }
}
