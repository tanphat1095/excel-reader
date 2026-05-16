package io.github.tanphat1095.excel.reader.resolver.cellvalue;

import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;


@AllArgsConstructor
public class StringResolver implements CellValueResolver<String>{

    @Override
    public boolean supports(Class<?> source) {
        return String.class == source;
    }

    @Override
    public String resolve(Cell cell) {
        return cell.getStringCellValue();
    }
}
