package io.github.tanphat1095.excel.reader.resolver.celltype;

import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class CellTypeResolverFactory {

    private final List<CellTypeResolver> resolvers;

    public CellTypeResolverFactory() {
        this(new ArrayList<>());
        register(new CellStringResolver());
        register(new CellNumericResolver());
        register(new CellBooleanResolver());
    }

    public void register(CellTypeResolver resolver) {
        resolvers.add(resolver);
    }

    public CellTypeResolver getCellTypeResolver(Cell cell) {
       return resolvers.stream().filter(r -> r.supports(cell.getCellType())).findFirst().orElse(null);
    }

}
