package io.github.tanphat1095.excel.reader;

import io.github.tanphat1095.excel.reader.annotation.ExcelColumn;
import io.github.tanphat1095.excel.reader.resolver.celltype.CellTypeResolver;
import io.github.tanphat1095.excel.reader.resolver.celltype.CellTypeResolverFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests null cell, BLANK cell, ERROR cell, and FORMULA cell handling in ExcelReader,
 * plus the Consumer&lt;T&gt; and stream() overloads, and cache invalidation via registerFirst().
 */
class ExcelReaderCellHandlingTest {

    // -------------------------------------------------------------------------
    // Bean classes used by multiple tests
    // -------------------------------------------------------------------------

    public static class Row1 {
        @ExcelColumn(index = 0) private String name;
        @ExcelColumn(index = 1) private Double value;

        public Row1() {}
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
    }

    public static class StringRow {
        @ExcelColumn(index = 0) private String label;

        public StringRow() {}
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

    // -------------------------------------------------------------------------
    // Fixture
    // -------------------------------------------------------------------------

    private Workbook workbook;
    private ExcelReader reader;

    @BeforeEach
    void setUp() {
        workbook = new XSSFWorkbook();
        reader = new ExcelReader();
    }

    @AfterEach
    void tearDown() throws IOException {
        workbook.close();
    }

    // -------------------------------------------------------------------------
    // Null / BLANK / ERROR cells → field stays at Java default (null)
    // -------------------------------------------------------------------------

    @Nested
    class BlankAndNullCellHandling {

        @Test
        void nullCellShouldLeaveFieldNull() {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            // col 0 has a string value; col 1 is never created → null cell
            row.createCell(0, CellType.STRING).setCellValue("Alice");
            // col 1 intentionally absent

            List<Row1> result = reader.read(sheet, new CellReference("A1"), Row1.class);

            assertEquals(1, result.size());
            assertEquals("Alice", result.get(0).getName());
            assertNull(result.get(0).getValue(), "Null (absent) cell must leave Double field null");
        }

        @Test
        void blankCellShouldLeaveFieldNull() {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0, CellType.STRING).setCellValue("Bob");
            row.createCell(1, CellType.BLANK); // explicitly BLANK

            List<Row1> result = reader.read(sheet, new CellReference("A1"), Row1.class);

            assertEquals(1, result.size());
            assertNull(result.get(0).getValue(), "BLANK cell must leave Double field null");
        }

        @Test
        void errorCellShouldLeaveFieldNull() {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0, CellType.STRING).setCellValue("Carol");
            Cell errCell = row.createCell(1, CellType.ERROR);
            errCell.setCellErrorValue(FormulaError.DIV0.getCode());

            List<Row1> result = reader.read(sheet, new CellReference("A1"), Row1.class);

            assertEquals(1, result.size());
            assertNull(result.get(0).getValue(), "ERROR cell must leave Double field null");
        }
    }

    // -------------------------------------------------------------------------
    // FORMULA cell — cached result is read after evaluation
    // -------------------------------------------------------------------------

    @Nested
    class FormulaCellHandling {

        @Test
        void numericFormulaCellShouldBeReadAsDouble() {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0, CellType.STRING).setCellValue("Dave");
            Cell formulaCell = row.createCell(1);
            formulaCell.setCellFormula("10*5");

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            List<Row1> result = reader.read(sheet, new CellReference("A1"), Row1.class);

            assertEquals(1, result.size());
            assertEquals(50.0, result.get(0).getValue(),
                    "FORMULA cell with numeric cached result must be read as Double");
        }

        @Test
        void stringFormulaCellShouldBeReadAsString() {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            Cell formulaCell = row.createCell(0);
            formulaCell.setCellFormula("\"Hello\"");

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            List<StringRow> result = reader.read(sheet, new CellReference("A1"), StringRow.class);

            assertEquals(1, result.size());
            assertEquals("Hello", result.get(0).getLabel(),
                    "FORMULA cell with string cached result must be read as String");
        }
    }

    // -------------------------------------------------------------------------
    // Consumer<T> overload
    // -------------------------------------------------------------------------

    @Nested
    class ConsumerOverload {

        @Test
        void shouldInvokeCallbackForEachRow() {
            Sheet sheet = workbook.createSheet();
            for (int i = 0; i < 3; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0, CellType.STRING).setCellValue("Name" + i);
                row.createCell(1, CellType.NUMERIC).setCellValue(i * 10.0);
            }

            List<Row1> collected = new ArrayList<>();
            reader.read(sheet, new CellReference("A1"), Row1.class, collected::add);

            assertEquals(3, collected.size());
            assertEquals("Name0", collected.get(0).getName());
            assertEquals(20.0, collected.get(2).getValue());
        }

        @Test
        void shouldSkipRowsBeforeStartRef() {
            Sheet sheet = workbook.createSheet();
            // Row 0: header (skipped when startRef = A2)
            Row header = sheet.createRow(0);
            header.createCell(0, CellType.STRING).setCellValue("Header");
            // Row 1: first data row
            Row data = sheet.createRow(1);
            data.createCell(0, CellType.STRING).setCellValue("DataRow");
            data.createCell(1, CellType.NUMERIC).setCellValue(99.0);

            List<Row1> collected = new ArrayList<>();
            reader.read(sheet, new CellReference("A2"), Row1.class, collected::add);

            assertEquals(1, collected.size());
            assertEquals("DataRow", collected.get(0).getName());
        }
    }

    // -------------------------------------------------------------------------
    // stream() overload — lazy, correct elements
    // -------------------------------------------------------------------------

    @Nested
    class StreamOverload {

        @Test
        void streamShouldReturnAllRows() {
            Sheet sheet = workbook.createSheet();
            for (int i = 0; i < 5; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0, CellType.STRING).setCellValue("Item" + i);
                row.createCell(1, CellType.NUMERIC).setCellValue(i * 1.5);
            }

            List<Row1> result = reader.stream(sheet, new CellReference("A1"), Row1.class)
                    .collect(Collectors.toList());

            assertEquals(5, result.size());
            assertEquals("Item0", result.get(0).getName());
            assertEquals(6.0, result.get(4).getValue());
        }

        @Test
        void streamShouldSupportFiltering() {
            Sheet sheet = workbook.createSheet();
            for (int i = 0; i < 4; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0, CellType.STRING).setCellValue(i % 2 == 0 ? "even" : "odd");
                row.createCell(1, CellType.NUMERIC).setCellValue((double) i);
            }

            List<Row1> evens = reader.stream(sheet, new CellReference("A1"), Row1.class)
                    .filter(r -> "even".equals(r.getName()))
                    .collect(Collectors.toList());

            assertEquals(2, evens.size());
            assertTrue(evens.stream().allMatch(r -> "even".equals(r.getName())));
        }

        @Test
        void streamShouldSkipRowsBeforeStartRef() {
            Sheet sheet = workbook.createSheet();
            Row ignored = sheet.createRow(0);
            ignored.createCell(0, CellType.STRING).setCellValue("skip");

            Row data = sheet.createRow(1);
            data.createCell(0, CellType.STRING).setCellValue("keep");
            data.createCell(1, CellType.NUMERIC).setCellValue(1.0);

            List<Row1> result = reader.stream(sheet, new CellReference("A2"), Row1.class)
                    .collect(Collectors.toList());

            assertEquals(1, result.size());
            assertEquals("keep", result.get(0).getName());
        }
    }

    // -------------------------------------------------------------------------
    // Cache invalidation — registerFirst() after a lookup must clear the cache
    // -------------------------------------------------------------------------

    @Nested
    class CacheInvalidation {

        // Resolver that intercepts STRING cells and claims they produce Double
        static class StringAsDoubleResolver implements CellTypeResolver {
            @Override
            public boolean supports(CellType cellType) { return cellType == CellType.STRING; }

            @Override
            public Class<?> resolve(Cell cell) { return Double.class; }
        }

        @Test
        void cacheInvalidatedAfterRegisterFirstOnCellTypeFactory() {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            Cell stringCell = row.createCell(0, CellType.STRING);
            stringCell.setCellValue("hello");

            CellTypeResolverFactory cellTypeFactory = new CellTypeResolverFactory();

            // Warm the cache — built-in CellStringResolver wins
            CellTypeResolver before = cellTypeFactory.getCellTypeResolver(stringCell);
            assertFalse(before instanceof StringAsDoubleResolver);

            // Register at front after the cache is already warm
            cellTypeFactory.registerFirst(new StringAsDoubleResolver());

            // Cache must have been cleared — new resolver must win
            CellTypeResolver after = cellTypeFactory.getCellTypeResolver(stringCell);
            assertInstanceOf(StringAsDoubleResolver.class, after,
                    "getCellTypeResolver() must return the newly registered resolver after cache invalidation");
        }
    }
}
