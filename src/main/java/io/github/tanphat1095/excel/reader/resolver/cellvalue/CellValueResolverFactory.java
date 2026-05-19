package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import io.github.tanphat1095.excel.reader.resolver.ResolverRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CellValueResolverFactory extends ResolverRegistry<CellValueResolver<?>> {

    // Cache: source Class → first matching resolver. Optional.empty() means "no resolver found".
    private final Map<Class<?>, Optional<CellValueResolver<?>>> cache = new HashMap<>();

    public CellValueResolverFactory() {
        register(new StringResolver());
        register(new DateResolver());
        register(new BooleanResolver());
        register(new DoubleResolver());
    }

    @Override
    protected void onRegister() {
        cache.clear();
    }

    public CellValueResolver<?> getCellValueResolver(Class<?> source) {
        return cache.computeIfAbsent(source, k -> find(r -> r.supports(k))).orElse(null);
    }
}
