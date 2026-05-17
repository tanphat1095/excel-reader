# excel-reader

A lightweight Java library for mapping Excel sheet data directly into Java objects using annotation-based column binding. Built on top of Apache POI with an extensible resolver architecture, so you can plug in custom type handling at any layer.

## Requirements

- Java 21+
- Apache POI is included as a transitive dependency

## Installation

```xml
<dependency>
    <groupId>io.github.tanphat1095</groupId>
    <artifactId>excel-reader</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### 1. Annotate your class

Annotate each field with `@ExcelColumn(index = N)` where `N` is the zero-based column index.

```java
import io.github.tanphat1095.excel.reader.annotation.ExcelColumn;

public class Employee {

    @ExcelColumn(index = 0)
    private String name;

    @ExcelColumn(index = 1)
    private Double salary;

    @ExcelColumn(index = 2)
    private LocalDate hireDate;

    @ExcelColumn(index = 3)
    private Boolean active;

    // no-arg constructor + getters + setters required
}
```

The target class must follow the Java Bean convention: a public no-arg constructor and standard getter/setter methods. `Map` is not supported as a result type.

### 2. Read the sheet

```java
try (Workbook workbook = new XSSFWorkbook(new FileInputStream("data.xlsx"))) {
    Sheet sheet = workbook.getSheet("Employees");

    ExcelReader reader = new ExcelReader();

    // Start reading from cell A2 (row index 1, skipping the header row)
    List<Employee> employees = reader.read(sheet, new CellReference("A2"), Employee.class);
}
```

The `CellReference` defines the first data cell. Rows above it are skipped, making it straightforward to skip header rows.

## Built-in Type Support

The following Excel cell types and Java field type combinations work out of the box.

| Excel cell type | Java field type  | Notes                                              |
|-----------------|------------------|----------------------------------------------------|
| String          | `String`         |                                                    |
| Numeric         | `Double`         |                                                    |
| Numeric (date)  | `LocalDate`      | POI detects date-formatted cells automatically     |
| String          | `LocalDate`      | Default pattern: `dd-MM-yyyy` (configurable)       |
| Boolean         | `Boolean`        |                                                    |

## How It Works

Reading is split across three independent resolver layers. Each layer is independently extensible.

```
Excel Cell
    |
    v
CellTypeResolver        -- determines which Java type represents this cell (String, Double, Date, Boolean)
    |
    v
CellValueResolver       -- extracts the raw value from the cell as that Java type
    |
    v
SourceToTargetResolver  -- converts the raw value to the field's declared type
    |
    v
Java field set via setter
```

## Configuration

### Custom date pattern

When a date column is stored as a plain string (not a date-formatted Excel cell), the library parses it using a configurable pattern. The default is `dd-MM-yyyy`.

```java
ExcelReader reader = ExcelReader.builder()
        .datePattern("yyyy/MM/dd")
        .cellTypeResolverFactory(new CellTypeResolverFactory())
        .cellValueResolverFactory(new CellValueResolverFactory())
        .sourceToTargetResolverFactory(new SourceToTargetResolverFactory("yyyy/MM/dd"))
        .build();
```

Note: the `datePattern` must be passed to `SourceToTargetResolverFactory` as well since that is where `StringToLocalDateResolver` lives.

## Extending the Library

All three factories expose a `register()` method. Call it to add your custom resolver before calling `read()`.

### Custom CellTypeResolver

Use this when you need to handle an Excel cell type that is not covered by default, or when you want to distinguish cells of the same type by some other condition.

```java
public class CellFormulaResolver implements CellTypeResolver {

    @Override
    public boolean supports(CellType cellType) {
        return cellType == CellType.FORMULA;
    }

    @Override
    public Class<?> resolve(Cell cell) {
        // Treat all formula cells as String
        return String.class;
    }
}
```

Register it:

```java
CellTypeResolverFactory cellTypeFactory = new CellTypeResolverFactory();
cellTypeFactory.register(new CellFormulaResolver());

ExcelReader reader = ExcelReader.builder()
        .datePattern("dd-MM-yyyy")
        .cellTypeResolverFactory(cellTypeFactory)
        .cellValueResolverFactory(new CellValueResolverFactory())
        .sourceToTargetResolverFactory(new SourceToTargetResolverFactory("dd-MM-yyyy"))
        .build();
```

### Custom CellValueResolver

Use this when you need to extract a value from a cell in a non-standard way for a given source type.

```java
public class RichTextResolver implements CellValueResolver<String> {

    @Override
    public boolean supports(Class<?> source) {
        return String.class == source;
    }

    @Override
    public String resolve(Cell cell) {
        // Strip leading/trailing whitespace during extraction
        return cell.getStringCellValue().trim();
    }
}
```

Register it:

```java
CellValueResolverFactory cellValueFactory = new CellValueResolverFactory();
cellValueFactory.register(new RichTextResolver());
```

Resolvers are matched in registration order, so a newly registered resolver that matches first will take precedence over the built-in ones. Register before creating the `ExcelReader` if you want to override a default.

### Custom SourceToTargetResolver

Use this to support a field type that is not built in, such as `BigDecimal`, `Long`, `Enum`, or any domain type.

**Example: Double to BigDecimal**

```java
public class DoubleToBigDecimalResolver implements SourceToTargetResolver<Double, BigDecimal> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Double.class == source && BigDecimal.class == target;
    }

    @Override
    public BigDecimal resolve(Double source) {
        if (source == null) return null;
        return BigDecimal.valueOf(source);
    }
}
```

**Example: String to Enum**

```java
public class StringToDepartmentResolver implements SourceToTargetResolver<String, Department> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return String.class == source && Department.class == target;
    }

    @Override
    public Department resolve(String source) {
        if (source == null || source.isBlank()) return null;
        return Department.valueOf(source.trim().toUpperCase());
    }
}
```

Register and build:

```java
SourceToTargetResolverFactory transferFactory = new SourceToTargetResolverFactory("dd-MM-yyyy");
transferFactory.register(new DoubleToBigDecimalResolver());
transferFactory.register(new StringToDepartmentResolver());

ExcelReader reader = ExcelReader.builder()
        .datePattern("dd-MM-yyyy")
        .cellTypeResolverFactory(new CellTypeResolverFactory())
        .cellValueResolverFactory(new CellValueResolverFactory())
        .sourceToTargetResolverFactory(transferFactory)
        .build();
```

And the corresponding class:

```java
public class Employee {

    @ExcelColumn(index = 0)
    private String name;

    @ExcelColumn(index = 1)
    private BigDecimal salary;

    @ExcelColumn(index = 2)
    private Department department;

    // no-arg constructor + getters + setters
}
```

## Exception Handling

| Exception                    | When it is thrown                                                                                      |
|------------------------------|--------------------------------------------------------------------------------------------------------|
| `ExcelReaderException`       | No resolver found for a cell type or source-to-target type pair; date string in wrong format           |
| `ExcelCreateResultException` | Target class has no public no-arg constructor; `Map` passed as result type; introspection failure      |

Both are unchecked exceptions (`RuntimeException`).

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)
