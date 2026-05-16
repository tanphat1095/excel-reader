package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import org.apache.poi.ss.usermodel.Cell;

public class BooleanResolver implements  CellValueResolver<Boolean> {
    @Override
    public boolean supports(Class<?> source) {
        return Boolean.class.equals(source);
    }

    @Override
    public Boolean resolve(Cell cell) {
        return cell.getBooleanCellValue();
    }
}
