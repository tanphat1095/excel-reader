package io.github.tanphat1095.excel.reader.metadata;

import java.lang.reflect.Method;

public record FieldResultMetadata(String name, Class<?> type, Method setter) {

}
