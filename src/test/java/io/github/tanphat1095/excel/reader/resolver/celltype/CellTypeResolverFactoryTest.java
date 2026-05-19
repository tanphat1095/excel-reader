package io.github.tanphat1095.excel.reader.resolver.celltype;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CellTypeResolverFactoryTest {

    // --- Custom resolver for BLANK cells (genuinely no built-in — safe "new type" for register() tests) ---
    static class CellBlankResolver implements CellTypeResolver {
        @Override public boolean supports(CellType cellType) { return cellType == CellType.BLANK; }
        @Override public Class<?> resolve(Cell cell) { return String.class; }
    }

    // --- Custom resolver whose supports() conflicts with the built-in CellStringResolver ---
    static class CustomStringTypeResolver implements CellTypeResolver {
        @Override public boolean supports(CellType cellType) { return cellType == CellType.STRING; }
        @Override public Class<?> resolve(Cell cell) { return Integer.class; } // marker class
    }

    private Workbook workbook;
    private CellTypeResolverFactory factory;

    @BeforeEach
    void setUp() {
        workbook = new XSSFWorkbook();
        factory = new CellTypeResolverFactory();
    }

    @AfterEach
    void tearDown() throws IOException {
        workbook.close();
    }

    private Cell createCell(CellType cellType) {
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0);
        return row.createCell(0, cellType);
    }

    // CellType.FORMULA requires an actual formula string — createCell(FORMULA) leaves getCellType() as BLANK.
    private Cell createFormulaCell() {
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellFormula("1+1");
        return cell;
    }

    // -------------------------------------------------------------------------
    // Built-in behaviour
    // -------------------------------------------------------------------------

    @Test
    void builtInShouldResolveStringCellToStringClass() {
        Cell stringCell = createCell(CellType.STRING);

        Class<?> resolved = factory.getCellTypeResolver(stringCell).resolve(stringCell);

        assertEquals(String.class, resolved);
    }

    @Test
    void builtInCellFormulaResolverShouldHandleFormulaCells() {
        Cell formulaCell = createFormulaCell();

        CellTypeResolver resolver = factory.getCellTypeResolver(formulaCell);

        assertNotNull(resolver);
        assertInstanceOf(io.github.tanphat1095.excel.reader.resolver.celltype.CellFormulaResolver.class, resolver,
                "CellFormulaResolver must be registered as a built-in");
    }

    @Test
    void shouldReturnNullWhenNoCellTypeResolverFound() {
        // BLANK has no built-in resolver
        Cell blankCell = createCell(CellType.BLANK);

        assertNull(factory.getCellTypeResolver(blankCell));
    }

    // -------------------------------------------------------------------------
    // register()
    // -------------------------------------------------------------------------

    @Nested
    class Register {

        @Test
        void shouldFindCustomResolverForNewCellType() {
            // BLANK has no built-in — safe new-type test
            factory.register(new CellBlankResolver());
            Cell blankCell = createCell(CellType.BLANK);

            CellTypeResolver resolver = factory.getCellTypeResolver(blankCell);

            assertNotNull(resolver);
            assertInstanceOf(CellBlankResolver.class, resolver);
        }

        @Test
        void shouldNotOverrideBuiltInWhenSupportsConflicts() {
            factory.register(new CustomStringTypeResolver());
            Cell stringCell = createCell(CellType.STRING);

            CellTypeResolver resolver = factory.getCellTypeResolver(stringCell);

            assertNotNull(resolver);
            assertFalse(resolver instanceof CustomStringTypeResolver,
                    "register() must not shadow the built-in CellStringResolver");
        }
    }

    // -------------------------------------------------------------------------
    // registerFirst()
    // -------------------------------------------------------------------------

    @Nested
    class RegisterFirst {

        @Test
        void shouldOverrideBuiltInWhenSupportsConflicts() {
            factory.registerFirst(new CustomStringTypeResolver());
            Cell stringCell = createCell(CellType.STRING);

            CellTypeResolver resolver = factory.getCellTypeResolver(stringCell);

            assertNotNull(resolver);
            assertInstanceOf(CustomStringTypeResolver.class, resolver,
                    "registerFirst() must give the custom resolver priority over the built-in");
        }

        @Test
        void customResolverShouldProduceDifferentTypeThanBuiltIn() {
            factory.registerFirst(new CustomStringTypeResolver());
            Cell stringCell = createCell(CellType.STRING);

            Class<?> resolved = factory.getCellTypeResolver(stringCell).resolve(stringCell);

            assertEquals(Integer.class, resolved,
                    "Custom resolver must be invoked, not the built-in CellStringResolver");
        }

        @Test
        void shouldAlsoWorkForNewCellTypes() {
            factory.registerFirst(new CellBlankResolver());
            Cell blankCell = createCell(CellType.BLANK);

            CellTypeResolver resolver = factory.getCellTypeResolver(blankCell);

            assertNotNull(resolver);
            assertInstanceOf(CellBlankResolver.class, resolver);
        }
    }

    // -------------------------------------------------------------------------
    // Cache invalidation
    // -------------------------------------------------------------------------

    @Test
    void cacheShouldBeInvalidatedAfterRegisterFirst() {
        Cell stringCell = createCell(CellType.STRING);

        // Warm cache — built-in wins
        CellTypeResolver before = factory.getCellTypeResolver(stringCell);
        assertFalse(before instanceof CustomStringTypeResolver);

        // Override — cache must be cleared
        factory.registerFirst(new CustomStringTypeResolver());

        CellTypeResolver after = factory.getCellTypeResolver(stringCell);
        assertInstanceOf(CustomStringTypeResolver.class, after,
                "Lookup cache must be invalidated after registerFirst()");
    }
}
