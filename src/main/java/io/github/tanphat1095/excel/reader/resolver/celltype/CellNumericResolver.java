package io.github.tanphat1095.excel.reader.resolver.celltype;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import java.util.Date;

public class CellNumericResolver implements CellTypeResolver{
    @Override
    public boolean supports(CellType cellType) {
        return cellType == CellType.NUMERIC;
    }

    @Override
    public Class<?> resolve(Cell cell) {
        if(DateUtil.isCellDateFormatted(cell))
            return Date.class;
        return Double.class;
    }
}
