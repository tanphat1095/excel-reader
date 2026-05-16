package io.github.tanphat1095.excel.reader.resolver.celltype;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

public class CellBooleanResolver implements CellTypeResolver{
    @Override
    public boolean supports(CellType cellType) {
        return cellType == CellType.BOOLEAN;
    }

    @Override
    public Class<?> resolve(Cell cell) {
        return Boolean.class;
    }
}
