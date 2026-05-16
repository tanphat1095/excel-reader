package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import org.apache.poi.ss.usermodel.Cell;

import java.util.Date;

public class DateResolver implements CellValueResolver<Date> {

    @Override
    public boolean supports(Class<?> source) {
        return Date.class == source;
    }

    @Override
    public Date resolve(Cell cell) {
        return cell.getDateCellValue();
    }
}
