package io.github.tanphat1095.excel.reader.resolver.transfer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class DateToLocalDateResolver implements SourceToTargetResolver<Date, LocalDate> {
    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Date.class == source && LocalDate.class == target;
    }

    @Override
    public LocalDate resolve(Date source) {
        if(source == null) return null;
        return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
