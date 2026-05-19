package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;
import io.github.tanphat1095.excel.reader.resolver.ResolverRegistry;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SourceToTargetResolverFactory extends ResolverRegistry<SourceToTargetResolver<?, ?>> {

    // Cache key: "source.name#target.name" → first matching resolver
    private final Map<String, Optional<SourceToTargetResolver<?, ?>>> cache = new HashMap<>();

    public SourceToTargetResolverFactory(String datePattern) {
        this(datePattern, "dd-MM-yyyy HH:mm:ss");
    }

    public SourceToTargetResolverFactory(String datePattern, String datetimePattern) {
        DateTimeFormatter dateFmt     = DateTimeFormatter.ofPattern(datePattern);
        DateTimeFormatter datetimeFmt = DateTimeFormatter.ofPattern(datetimePattern);

        // Same-type pass-through
        register(new StringToStringResolver());
        register(new SameDoubleResolver());
        register(new SameBooleanResolver());

        // Date / LocalDate
        register(new StringToLocalDateResolver(dateFmt));
        register(new DateToLocalDateResolver());

        // LocalDateTime
        register(new StringToLocalDateTimeResolver(datetimeFmt));
        register(new DateToLocalDateTimeResolver());

        // Numeric targets
        register(new DoubleToIntegerResolver());
        register(new DoubleToLongResolver());
        register(new DoubleToBigDecimalResolver());

        // String targets (from non-string cells)
        register(new DoubleToStringResolver());
        register(new BooleanToStringResolver());
        register(new DateToStringResolver(dateFmt));

        // Lenient String → numeric / boolean
        register(new StringToDoubleResolver());
        register(new StringToIntegerResolver());
        register(new StringToLongResolver());
        register(new StringToBigDecimalResolver());
        register(new StringToBooleanResolver());
    }

    @Override
    protected void onRegister() {
        cache.clear();
    }

    @SuppressWarnings("unchecked")
    public <S, T, P extends SourceToTargetResolver<S, T>> P getResolver(Class<S> source, Class<T> target) {
        String key = source.getName() + "#" + target.getName();
        return (P) cache.computeIfAbsent(key, k -> find(r -> r.supports(source, target)))
                .orElseThrow(() -> new ExcelReaderException(
                        "There are no resolver for transfer from " + source.getName() + " to " + target.getName()));
    }
}
