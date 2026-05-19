package io.github.tanphat1095.excel.reader.resolver.transfer;

import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SourceToTargetResolverFactoryTest {

    // --- Custom resolver for a brand-new target type (Double -> ZonedDateTime, genuinely unsupported) ---
    static class DoubleToZonedDateTimeResolver implements SourceToTargetResolver<Double, java.time.ZonedDateTime> {
        @Override
        public boolean supports(Class<?> source, Class<?> target) {
            return Double.class == source && java.time.ZonedDateTime.class == target;
        }
        @Override
        public java.time.ZonedDateTime resolve(Double source) {
            return null; // marker — conversion logic is irrelevant for registration tests
        }
    }

    // --- Custom resolver whose supports() conflicts with built-in StringToStringResolver ---
    static class UpperCaseStringResolver implements SourceToTargetResolver<String, String> {
        @Override
        public boolean supports(Class<?> source, Class<?> target) {
            return String.class == source && String.class == target;
        }
        @Override
        public String resolve(String source) {
            return source == null ? null : source.toUpperCase();
        }
    }

    private static final String DATE_PATTERN = "dd-MM-yyyy";

    private SourceToTargetResolverFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SourceToTargetResolverFactory(DATE_PATTERN);
    }

    @Nested
    class Register {

        @Test
        void shouldFindCustomResolverForNewTargetType() {
            // ZonedDateTime has no built-in resolver — safe "new type" test
            factory.register(new DoubleToZonedDateTimeResolver());

            SourceToTargetResolver<?, ?> resolver = factory.getResolver(Double.class, java.time.ZonedDateTime.class);

            assertNotNull(resolver);
            assertInstanceOf(DoubleToZonedDateTimeResolver.class, resolver);
        }

        @Test
        void customResolverShouldBeFoundAfterRegister() {
            factory.register(new DoubleToZonedDateTimeResolver());

            SourceToTargetResolver<?, ?> resolver = factory.getResolver(Double.class, java.time.ZonedDateTime.class);

            assertNotNull(resolver);
            assertInstanceOf(DoubleToZonedDateTimeResolver.class, resolver);
        }

        @Test
        void shouldNotOverrideBuiltInWhenSupportsConflicts() {
            // register() adds to the end — built-in StringToStringResolver is at the front
            factory.register(new UpperCaseStringResolver());

            SourceToTargetResolver<?, ?> resolver = factory.getResolver(String.class, String.class);

            assertNotNull(resolver);
            assertFalse(resolver instanceof UpperCaseStringResolver,
                    "register() must not shadow the built-in StringToStringResolver");
        }

        @Test
        void shouldThrowWhenNoResolverFoundForType() {
            // ZonedDateTime has no built-in resolver and we have not registered one
            assertThrows(ExcelReaderException.class,
                    () -> factory.getResolver(Double.class, java.time.ZonedDateTime.class),
                    "getResolver() must throw when no resolver supports the requested type pair");
        }
    }

    @Nested
    class RegisterFirst {

        @Test
        void shouldOverrideBuiltInWhenSupportsConflicts() {
            factory.registerFirst(new UpperCaseStringResolver());

            SourceToTargetResolver<?, ?> resolver = factory.getResolver(String.class, String.class);

            assertNotNull(resolver);
            assertInstanceOf(UpperCaseStringResolver.class, resolver,
                    "registerFirst() must give the custom resolver priority over the built-in");
        }

        @Test
        void customResolverShouldProduceDifferentResultThanBuiltIn() {
            factory.registerFirst(new UpperCaseStringResolver());

            @SuppressWarnings("unchecked")
            SourceToTargetResolver<String, String> resolver =
                    (SourceToTargetResolver<String, String>) factory.getResolver(String.class, String.class);

            assertEquals("HELLO", resolver.resolve("hello"),
                    "Custom UpperCaseStringResolver must be invoked, not the built-in pass-through");
        }

        @Test
        void shouldAlsoWorkForNewTargetTypes() {
            factory.registerFirst(new DoubleToZonedDateTimeResolver());

            SourceToTargetResolver<?, ?> resolver = factory.getResolver(Double.class, java.time.ZonedDateTime.class);

            assertNotNull(resolver);
            assertInstanceOf(DoubleToZonedDateTimeResolver.class, resolver);
        }
    }
}
