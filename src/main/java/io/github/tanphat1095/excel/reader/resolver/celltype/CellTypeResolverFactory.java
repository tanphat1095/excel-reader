package io.github.tanphat1095.excel.reader.resolver.celltype;

import io.github.tanphat1095.excel.reader.resolver.ResolverRegistry;
import org.apache.poi.ss.usermodel.Cell;

public class CellTypeResolverFactory extends ResolverRegistry<CellTypeResolver> {

    public CellTypeResolverFactory() {
        register(new CellStringResolver());
        register(new CellNumericResolver());
        register(new CellBooleanResolver());
    }

    public CellTypeResolver getCellTypeResolver(Cell cell) {
        return find(r -> r.supports(cell.getCellType())).orElse(null);
    }
}
