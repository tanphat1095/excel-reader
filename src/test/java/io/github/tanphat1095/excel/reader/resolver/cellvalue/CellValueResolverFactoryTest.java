package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellValueResolverFactoryTest {

    // --- Custom resolver that targets a brand-new type (no built-in conflict) ---
    static class IntegerResolver implements CellValueResolver<Integer> {
        @Override public boolean supports(Class<?> source) { return Integer.class == source; }
        @Override public Integer resolve(Cell cell) { return (int) cell.getNumericCellValue(); }
    }

    // --- Custom resolver whose supports() conflicts with the built-in StringResolver ---
    static class TrimmingStringResolver implements CellValueResolver<String> {
        @Override public boolean supports(Class<?> source) { return String.class == source; }
        @Override public String resolve(Cell cell) { return cell.getStringCellValue().trim(); }
    }

    private CellValueResolverFactory factory;

    @BeforeEach
    void setUp() {
        factory = new CellValueResolverFactory();
    }

    @Nested
    class Register {

        @Test
        void shouldFindCustomResolverForNewType() {
            factory.register(new IntegerResolver());

            CellValueResolver<?> resolver = factory.getCellValueResolver(Integer.class);

            assertNotNull(resolver);
            assertInstanceOf(IntegerResolver.class, resolver);
        }

        @Test
        void shouldNotOverrideBuiltInWhenSupportsConflicts() {
            // register() adds to the end — built-in StringResolver is already at the front
            factory.register(new TrimmingStringResolver());

            CellValueResolver<?> resolver = factory.getCellValueResolver(String.class);

            assertNotNull(resolver);
            assertFalse(resolver instanceof TrimmingStringResolver,
                    "register() must not shadow the built-in StringResolver");
        }
    }

    @Nested
    class RegisterFirst {

        @Test
        void shouldOverrideBuiltInWhenSupportsConflicts() {
            factory.registerFirst(new TrimmingStringResolver());

            CellValueResolver<?> resolver = factory.getCellValueResolver(String.class);

            assertNotNull(resolver);
            assertInstanceOf(TrimmingStringResolver.class, resolver,
                    "registerFirst() must give the custom resolver priority over the built-in");
        }

        @Test
        void shouldAlsoWorkForNewTypes() {
            factory.registerFirst(new IntegerResolver());

            CellValueResolver<?> resolver = factory.getCellValueResolver(Integer.class);

            assertNotNull(resolver);
            assertInstanceOf(IntegerResolver.class, resolver);
        }
    }
}
