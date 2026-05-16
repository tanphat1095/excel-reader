package io.github.tanphat1095.excel.reader.resolver.transfer;

public class StringToStringResolver extends SameTypeResolver<String>{
    @Override
    boolean supports(Class<?> source) {
        return String.class == source;
    }
}
