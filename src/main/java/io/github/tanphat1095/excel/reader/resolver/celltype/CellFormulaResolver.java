package io.github.tanphat1095.excel.reader.resolver.celltype;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.util.Date;

public class CellFormulaResolver implements CellTypeResolver {

    @Override
    public boolean supports(CellType cellType) {
        return cellType == CellType.FORMULA;
    }

    @Override
    public Class<?> resolve(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> String.class;
            case NUMERIC -> DateUtil.isCellDateFormatted(cell) ? Date.class : Double.class;
            case BOOLEAN -> Boolean.class;
            default -> null; // BLANK or ERROR formula result → caller treats as null field
        };
    }
}
