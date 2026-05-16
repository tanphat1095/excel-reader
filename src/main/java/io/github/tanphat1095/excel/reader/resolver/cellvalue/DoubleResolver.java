package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import org.apache.poi.ss.usermodel.Cell;

public class DoubleResolver implements CellValueResolver<Double> {
    @Override
    public boolean supports(Class<?> clazz) {
        return Double.class.equals(clazz);
    }

    @Override
    public Double resolve(Cell cell) {
        return cell.getNumericCellValue();
    }
}
