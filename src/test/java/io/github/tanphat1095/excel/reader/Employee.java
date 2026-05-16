package io.github.tanphat1095.excel.reader;

import io.github.tanphat1095.excel.reader.annotation.ExcelColumn;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class Employee {

    @ExcelColumn(index = 0)
    private String name;

    @ExcelColumn(index = 1)
    private Double age;

    @ExcelColumn(index = 2)
    private LocalDate birthDate;

    @ExcelColumn(index = 3)
    private Boolean active;
}
