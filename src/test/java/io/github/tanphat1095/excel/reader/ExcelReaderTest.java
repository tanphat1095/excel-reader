package io.github.tanphat1095.excel.reader;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReaderTest {

    private static Sheet sheet;
    private static final ExcelReader reader = new ExcelReader();

    @BeforeAll
    static void setup() throws Exception {
        Workbook workbook = new XSSFWorkbook(new FileInputStream("sample.xlsx"));
        sheet = workbook.getSheet("Employees");
    }

    @Test
    void shouldReadNEmployeeRows() {
        List<Employee> employees = reader.read(sheet, new CellReference("A2"), Employee.class);
        assertEquals(50, employees.size());
    }

    @Test
    void shouldMapNameCorrectly() {
        List<Employee> employees = reader.read(sheet, new CellReference("A2"), Employee.class);
        assertEquals("Alice", employees.get(0).getName());
        assertEquals("Bob", employees.get(1).getName());
        assertEquals("Charlie", employees.get(2).getName());
    }

    @Test
    void shouldMapAgeCorrectly() {
        List<Employee> employees = reader.read(sheet, new CellReference("A2"), Employee.class);
        assertEquals(30.0, employees.get(0).getAge());
        assertEquals(25.0, employees.get(1).getAge());
        assertEquals(35.0, employees.get(2).getAge());
    }

    @Test
    void shouldMapBirthDateCorrectly() {
        List<Employee> employees = reader.read(sheet, new CellReference("A2"), Employee.class);
        assertEquals(LocalDate.of(1994, 6, 15), employees.get(0).getBirthDate());
        assertEquals(LocalDate.of(1999, 3, 20), employees.get(1).getBirthDate());
        assertEquals(LocalDate.of(1989, 11, 1), employees.get(2).getBirthDate());
    }

    @Test
    void shouldMapActiveCorrectly() {
        List<Employee> employees = reader.read(sheet, new CellReference("A2"), Employee.class);
        assertTrue(employees.get(0).getActive());
        assertFalse(employees.get(1).getActive());
        assertTrue(employees.get(2).getActive());
    }
}
