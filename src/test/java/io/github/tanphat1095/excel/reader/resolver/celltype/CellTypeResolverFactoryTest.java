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

    // --- Custom resolver for FORMULA cells (no built-in conflict) ---
    static class CellFormulaResolver implements CellTypeResolver {
        @Override public boolean supports(CellType cellType) { return cellType == CellType.FORMULA; }
        @Override public Class<?> resolve(Cell cell) { return String.class; }
    }

    // --- Custom resolver whose supports() conflicts with the built-in CellStringResolver ---
    static class CustomStringTypeResolver implements CellTypeResolver {
        @Override public boolean supports(CellType cellType) { return cellType == CellType.STRING; }
        // Returns a different marker class to distinguish from the built-in
        @Override public Class<?> resolve(Cell cell) { return Integer.class; }
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

    // CellType.FORMULA ở POI yêu cầu phải set formula string mới thực sự là FORMULA type.
    // createCell(CellType.FORMULA) không set formula → getCellType() trả về BLANK.
    private Cell createFormulaCell() {
        Sheet sheet = workbook.createSheet();
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellFormula("1+1");
        return cell;
    }

    @Nested
    class Register {

        @Test
        void shouldFindCustomResolverForNewCellType() {
            factory.register(new CellFormulaResolver());
            Cell formulaCell = createFormulaCell();

            CellTypeResolver resolver = factory.getCellTypeResolver(formulaCell);

            assertNotNull(resolver);
            assertInstanceOf(CellFormulaResolver.class, resolver);
        }

        @Test
        void shouldNotOverrideBuiltInWhenSupportsConflicts() {
            // register() adds to the end — built-in CellStringResolver is already at the front
            factory.register(new CustomStringTypeResolver());
            Cell stringCell = createCell(CellType.STRING);

            CellTypeResolver resolver = factory.getCellTypeResolver(stringCell);

            assertNotNull(resolver);
            assertFalse(resolver instanceof CustomStringTypeResolver,
                    "register() must not shadow the built-in CellStringResolver");
        }

        @Test
        void builtInShouldResolveStringCellToStringClass() {
            Cell stringCell = createCell(CellType.STRING);

            CellTypeResolver resolver = factory.getCellTypeResolver(stringCell);
            Class<?> resolvedType = resolver.resolve(stringCell);

            assertEquals(String.class, resolvedType);
        }
    }

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

            CellTypeResolver resolver = factory.getCellTypeResolver(stringCell);
            Class<?> resolvedType = resolver.resolve(stringCell);

            // CustomStringTypeResolver returns Integer.class as a marker,
            // while the built-in CellStringResolver returns String.class
            assertEquals(Integer.class, resolvedType,
                    "Custom resolver must be invoked, not the built-in CellStringResolver");
        }

        @Test
        void shouldAlsoWorkForNewCellTypes() {
            factory.registerFirst(new CellFormulaResolver());
            Cell formulaCell = createFormulaCell();

            CellTypeResolver resolver = factory.getCellTypeResolver(formulaCell);

            assertNotNull(resolver);
            assertInstanceOf(CellFormulaResolver.class, resolver);
        }
    }

    @Test
    void shouldReturnNullWhenNoCellTypeResolverFound() {
        // BLANK cell type has no built-in resolver
        Cell blankCell = createCell(CellType.BLANK);

        CellTypeResolver resolver = factory.getCellTypeResolver(blankCell);

        assertNull(resolver);
    }
}
