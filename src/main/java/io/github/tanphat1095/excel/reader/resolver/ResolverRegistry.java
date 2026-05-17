package io.github.tanphat1095.excel.reader.resolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class ResolverRegistry<R> {

    private final List<R> resolvers = new ArrayList<>();

    /**
     * Adds a resolver to the end of the chain.
     * Built-in resolvers registered at construction time will take priority over resolvers added here
     * if their {@code supports()} conditions overlap.
     * Use {@link #registerFirst(Object)} to override a built-in.
     */
    public void register(R resolver) {
        resolvers.add(resolver);
    }

    /**
     * Inserts a resolver at the front of the chain, giving it priority over all previously
     * registered resolvers including built-ins.
     * Use this when you want to override the default handling for a type that is already supported.
     */
    public void registerFirst(R resolver) {
        resolvers.add(0, resolver);
    }

    protected Optional<R> find(Predicate<R> predicate) {
        return resolvers.stream().filter(predicate).findFirst();
    }
}
