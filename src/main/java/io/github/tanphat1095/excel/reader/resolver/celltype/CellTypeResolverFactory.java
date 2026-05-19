package io.github.tanphat1095.excel.reader.resolver.celltype;

import io.github.tanphat1095.excel.reader.resolver.ResolverRegistry;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CellTypeResolverFactory extends ResolverRegistry<CellTypeResolver> {

    // Cache: CellType → first matching resolver. Optional.empty() means "no resolver found".
    private final Map<CellType, Optional<CellTypeResolver>> cache = new HashMap<>();

    public CellTypeResolverFactory() {
        register(new CellStringResolver());
        register(new CellNumericResolver());
        register(new CellBooleanResolver());
        register(new CellFormulaResolver());
    }

    @Override
    protected void onRegister() {
        cache.clear();
    }

    public CellTypeResolver getCellTypeResolver(Cell cell) {
        return cache.computeIfAbsent(cell.getCellType(), k -> find(r -> r.supports(k))).orElse(null);
    }
}
