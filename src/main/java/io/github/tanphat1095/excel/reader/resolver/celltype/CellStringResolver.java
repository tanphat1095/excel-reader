package io.github.tanphat1095.excel.reader.resolver.celltype;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

public class CellStringResolver implements CellTypeResolver{
    @Override
    public boolean supports(CellType cellType) {
        return cellType == CellType.STRING;
    }

    @Override
    public Class<?> resolve(Cell cell) {
        return String.class;
    }
}
