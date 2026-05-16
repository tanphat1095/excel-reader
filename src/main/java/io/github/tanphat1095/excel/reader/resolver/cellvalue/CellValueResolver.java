package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import org.apache.poi.ss.usermodel.Cell;

public interface CellValueResolver<S>{
    boolean supports(Class<?> source);
    S resolve(Cell cell);

}
