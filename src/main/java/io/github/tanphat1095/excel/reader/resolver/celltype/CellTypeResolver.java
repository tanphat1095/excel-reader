package io.github.tanphat1095.excel.reader.resolver.celltype;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

public interface CellTypeResolver {
    boolean supports(CellType cellType);
    Class<?> resolve(Cell cell);
}
