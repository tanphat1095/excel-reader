package io.github.tanphat1095.excel.reader.resolver.transfer;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class DateToLocalDateTimeResolver implements SourceToTargetResolver<Date, LocalDateTime> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Date.class == source && LocalDateTime.class == target;
    }

    @Override
    public LocalDateTime resolve(Date source) {
        if (source == null) return null;
        return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
