# excel-reader

A lightweight Java library for mapping Excel sheet data directly into Java objects using annotation-based column binding. Built on top of Apache POI with an extensible resolver architecture, so you can plug in custom type handling at any layer.

## Requirements

- Java 17+
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

## Reading Modes

Three overloads are available depending on how you want to handle memory.

**Collect into a List** — simplest, entire result held in memory:

```java
List<Employee> employees = reader.read(sheet, new CellReference("A2"), Employee.class);
```

**Process row-by-row via callback** — result objects are not accumulated, suitable for write-through pipelines such as batch database inserts:

```java
reader.read(sheet, new CellReference("A2"), Employee.class, employee -> {
    repository.save(employee);
});
```

**Lazy Stream** — functional-style processing, result set is never fully materialized in heap:

```java
reader.stream(sheet, new CellReference("A2"), Employee.class)
      .filter(Employee::isActive)
      .forEach(processor::process);
```

Note: all three modes still require the sheet to be loaded by the caller. `XSSFWorkbook` loads the entire file into memory at open time. For files with hundreds of thousands of rows, open the workbook with POI's event-based API and pass each sheet to the reader separately.

## Built-in Type Support

The following Excel cell type → Java field type combinations work out of the box.

### Numeric cell

| Java field type | Notes |
|---|---|
| `Double` | direct |
| `Integer` | truncates decimal part |
| `Long` | truncates decimal part |
| `BigDecimal` | via `BigDecimal.valueOf` |
| `String` | whole numbers formatted without `.0` (e.g. `42`, not `42.0`) |

### Numeric cell (date-formatted)

| Java field type | Notes |
|---|---|
| `LocalDate` | POI detects date-formatted cells automatically |
| `LocalDateTime` | preserves time component |
| `String` | formatted with `datePattern` (default `dd-MM-yyyy`) |

### String cell

| Java field type | Notes |
|---|---|
| `String` | direct |
| `LocalDate` | parsed with `datePattern` (default `dd-MM-yyyy`) |
| `LocalDateTime` | parsed with `datetimePattern` (default `dd-MM-yyyy HH:mm:ss`) |
| `Double` | parsed via `Double.parseDouble` |
| `Integer` | parsed, truncates decimal if present |
| `Long` | parsed, truncates decimal if present |
| `BigDecimal` | parsed via `new BigDecimal(...)` |
| `Boolean` | accepts `true/false`, `yes/no`, `1/0`, `y/n` (case-insensitive) |

### Boolean cell

| Java field type | Notes |
|---|---|
| `Boolean` | direct |
| `String` | `"true"` or `"false"` |

### Formula cell

The cached result value is read and mapped using the same rules as the corresponding non-formula cell type above.

### Blank, null, or error cell

Fields are left at their Java default value (`null` for objects, `0` for primitives, `false` for booleans). No exception is thrown.

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

### Custom date and datetime patterns

`datePattern` applies when a date column is stored as a plain string or when converting a date cell to `String`. `datetimePattern` applies when a datetime column is stored as a plain string or when mapping to `LocalDateTime`.

```java
ExcelReader reader = ExcelReader.builder()
        .datePattern("yyyy/MM/dd")
        .datetimePattern("yyyy/MM/dd HH:mm:ss")
        .cellTypeResolverFactory(new CellTypeResolverFactory())
        .cellValueResolverFactory(new CellValueResolverFactory())
        .sourceToTargetResolverFactory(new SourceToTargetResolverFactory("yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss"))
        .build();
```

Both patterns must be passed to `SourceToTargetResolverFactory` as well since that is where the string-parsing resolvers live.

## Extending the Library

All three factories extend `ResolverRegistry<R>`, which exposes two methods for adding resolvers.

`register(resolver)` appends to the end of the chain. Use this to add support for a type that is not handled by any built-in resolver. If the new resolver's `supports()` condition overlaps with a built-in, the built-in will still win.

`registerFirst(resolver)` inserts at the front of the chain. Use this when you want to override the default behavior for a type that is already supported.

### Custom CellTypeResolver

Use this when you need to handle an Excel cell type not covered by default, or override how an existing cell type is interpreted. The example below treats `ERROR` cells as empty strings instead of being silently skipped.

```java
public class CellErrorAsEmptyResolver implements CellTypeResolver {

    @Override
    public boolean supports(CellType cellType) {
        return cellType == CellType.ERROR;
    }

    @Override
    public Class<?> resolve(Cell cell) {
        return String.class;
    }
}
```

Register it:

```java
CellTypeResolverFactory cellTypeFactory = new CellTypeResolverFactory();
cellTypeFactory.register(new CellErrorAsEmptyResolver());

ExcelReader reader = ExcelReader.builder()
        .datePattern("dd-MM-yyyy")
        .datetimePattern("dd-MM-yyyy HH:mm:ss")
        .cellTypeResolverFactory(cellTypeFactory)
        .cellValueResolverFactory(new CellValueResolverFactory())
        .sourceToTargetResolverFactory(new SourceToTargetResolverFactory("dd-MM-yyyy"))
        .build();
```

### Custom CellValueResolver

Use this when you need to extract a value from a cell in a non-standard way. If the `supports()` condition overlaps with an existing built-in, use `registerFirst()` so your resolver takes priority.

```java
public class TrimmingStringResolver implements CellValueResolver<String> {

    @Override
    public boolean supports(Class<?> source) {
        return String.class == source;
    }

    @Override
    public String resolve(Cell cell) {
        return cell.getStringCellValue().trim();
    }
}
```

Register it (overrides the built-in `StringResolver`):

```java
CellValueResolverFactory cellValueFactory = new CellValueResolverFactory();
cellValueFactory.registerFirst(new TrimmingStringResolver());
```

### Custom SourceToTargetResolver

Use this to support a field type that is not built in, or to override existing conversion logic. The example below maps a numeric cell to a domain `Money` type.

```java
public class DoubleToMoneyResolver implements SourceToTargetResolver<Double, Money> {

    @Override
    public boolean supports(Class<?> source, Class<?> target) {
        return Double.class == source && Money.class == target;
    }

    @Override
    public Money resolve(Double source) {
        return source == null ? null : Money.of(source, Currency.getInstance("USD"));
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
transferFactory.register(new DoubleToMoneyResolver());
transferFactory.register(new StringToDepartmentResolver());

ExcelReader reader = ExcelReader.builder()
        .datePattern("dd-MM-yyyy")
        .datetimePattern("dd-MM-yyyy HH:mm:ss")
        .cellTypeResolverFactory(new CellTypeResolverFactory())
        .cellValueResolverFactory(new CellValueResolverFactory())
        .sourceToTargetResolverFactory(transferFactory)
        .build();
```

## Exception Handling

| Exception                    | When it is thrown                                                                                          |
|------------------------------|------------------------------------------------------------------------------------------------------------|
| `ExcelReaderException`       | No resolver found for a cell type or source-to-target type pair; value string cannot be parsed to target type |
| `ExcelCreateResultException` | Target class has no public no-arg constructor; `Map` passed as result type; introspection failure          |

Both are unchecked exceptions (`RuntimeException`). Blank, null, and error cells do not throw — they silently leave the field at its Java default.

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)
