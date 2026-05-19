package io.github.tanphat1095.excel.reader.resolver.transfer;

import lombok.AllArgsConstructor;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@AllArgsConstructor
public class DateToStringResolver implements SourceToTargetResolver<Date, String> {

    private final DateTimeFormatter formatter;

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Date.class == source && String.class == target;
    }

    @Override
    public String resolve(Date source) {
        if (source == null) return null;
        return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().format(formatter);
    }
}
