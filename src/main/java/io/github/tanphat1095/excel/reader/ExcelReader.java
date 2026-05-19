package io.github.tanphat1095.excel.reader;

import io.github.tanphat1095.excel.reader.annotation.ExcelColumn;
import io.github.tanphat1095.excel.reader.exception.ExcelCreateResultException;
import io.github.tanphat1095.excel.reader.exception.ExcelReaderException;
import io.github.tanphat1095.excel.reader.metadata.FieldResultMetadata;
import io.github.tanphat1095.excel.reader.resolver.celltype.CellTypeResolverFactory;
import io.github.tanphat1095.excel.reader.resolver.cellvalue.CellValueResolverFactory;
import io.github.tanphat1095.excel.reader.resolver.transfer.SourceToTargetResolverFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@AllArgsConstructor
@Builder
public class ExcelReader {
    private final CellTypeResolverFactory cellTypeResolverFactory;
    private final CellValueResolverFactory cellValueResolverFactory;
    private final SourceToTargetResolverFactory sourceToTargetResolverFactory;
    private final String datePattern;
    private final String datetimePattern;

    public ExcelReader() {
        this.datePattern = "dd-MM-yyyy";
        this.datetimePattern = "dd-MM-yyyy HH:mm:ss";
        this.cellTypeResolverFactory = new CellTypeResolverFactory();
        this.cellValueResolverFactory = new CellValueResolverFactory();
        this.sourceToTargetResolverFactory = new SourceToTargetResolverFactory(datePattern, datetimePattern);
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Reads all matching rows into a List. The entire result is held in memory.
     * For very large sheets, prefer {@link #stream} or {@link #read(Sheet, CellReference, Class, Consumer)}.
     */
    public <T> List<T> read(Sheet sheet, CellReference cellReference, Class<T> clazz) {
        int estimated = Math.max(sheet.getLastRowNum() - cellReference.getRow() + 1, 16);
        List<T> result = new ArrayList<>(estimated);
        read(sheet, cellReference, clazz, result::add);
        return result;
    }

    /**
     * Streams rows lazily. Each row is processed on demand, so the full result set is never
     * accumulated in heap. The underlying Sheet is still DOM-loaded; use this to avoid holding
     * 100k result objects in memory at once.
     */
    public <T> Stream<T> stream(Sheet sheet, CellReference cellReference, Class<T> clazz) {
        Iterator<T> rowIter = new RowMappingIterator<>(sheet, cellReference, clazz);
        Iterable<T> iterable = () -> rowIter;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Processes each row via a callback without accumulating results in memory.
     * Useful for write-through pipelines (e.g., batch DB inserts).
     */
    public <T> void read(Sheet sheet, CellReference cellReference, Class<T> clazz, Consumer<T> rowHandler) {
        processSheet(sheet, cellReference, clazz, rowHandler);
    }

    // -------------------------------------------------------------------------
    // Core processing
    // -------------------------------------------------------------------------

    private <T> void processSheet(Sheet sheet, CellReference cellReference, Class<T> clazz, Consumer<T> rowHandler) {
        if (Map.class.isAssignableFrom(clazz))
            throw new ExcelCreateResultException("The result type Map is not supported. Please follow the Java Bean standard");
        try {
            Map<Integer, FieldResultMetadata> fieldResultMetadataMap = getFieldResultMetadataMap(clazz);
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);

            int startRow = cellReference.getRow();
            Iterator<Row> rowIterator = sheet.iterator();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() < startRow) continue;

                T instance = constructor.newInstance();
                for (Map.Entry<Integer, FieldResultMetadata> entry : fieldResultMetadataMap.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());

                    // null cell or BLANK → leave field at Java default (null / 0 / false)
                    if (cell == null
                            || cell.getCellType() == CellType.BLANK
                            || cell.getCellType() == CellType.ERROR) {
                        continue;
                    }

                    Class<?> sourceType = Optional.ofNullable(cellTypeResolverFactory.getCellTypeResolver(cell))
                            .orElseThrow(() -> new ExcelReaderException(
                                    "There are no resolver for the CellType " + cell.getCellType().name()))
                            .resolve(cell);

                    // FORMULA evaluating to BLANK or ERROR → treat as null field
                    if (sourceType == null) continue;

                    processCell(sourceType, entry.getValue().type(), cell, instance, entry.getValue());
                }
                rowHandler.accept(instance);
            }
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new ExcelCreateResultException(e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ExcelCreateResultException("The result class must have a no-arg constructor");
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    <S, T, O> void processCell(Class<S> sourceClass, Class<T> targetClass, Cell cell, O instance,
                                FieldResultMetadata fieldResultMetadata)
            throws InvocationTargetException, IllegalAccessException {
        S sourceValue = (S) cellValueResolverFactory.getCellValueResolver(sourceClass).resolve(cell);
        T value = sourceToTargetResolverFactory.getResolver(sourceClass, targetClass).resolve(sourceValue);
        fieldResultMetadata.setter().invoke(instance, value);
    }

    Map<Integer, FieldResultMetadata> getFieldResultMetadataMap(Class<?> clazz) throws IntrospectionException {
        PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(clazz, Object.class).getPropertyDescriptors();
        Map<Integer, FieldResultMetadata> fieldResultMetadataMap = new HashMap<>();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            try {
                Field field = clazz.getDeclaredField(propertyDescriptor.getName());
                ExcelColumn excelColumn = field.getAnnotation(ExcelColumn.class);
                if (excelColumn == null) continue;
                fieldResultMetadataMap.put(excelColumn.index(),
                        new FieldResultMetadata(field.getName(), field.getType(), propertyDescriptor.getWriteMethod()));
            } catch (NoSuchFieldException ex) {
                throw new ExcelCreateResultException(
                        "The class " + clazz.getName() + " does not have a field named " + propertyDescriptor.getName());
            }
        }
        return fieldResultMetadataMap;
    }

    // -------------------------------------------------------------------------
    // Lazy iterator backing stream()
    // -------------------------------------------------------------------------

    private class RowMappingIterator<T> implements Iterator<T> {
        private final Iterator<Row> rows;
        private final int startRow;
        private final Class<T> clazz;
        private final Map<Integer, FieldResultMetadata> fieldMap;
        private final Constructor<T> constructor;
        private Row nextRow;

        RowMappingIterator(Sheet sheet, CellReference cellReference, Class<T> clazz) {
            try {
                this.clazz = clazz;
                this.startRow = cellReference.getRow();
                this.fieldMap = getFieldResultMetadataMap(clazz);
                this.constructor = clazz.getDeclaredConstructor();
                this.constructor.setAccessible(true);
                this.rows = sheet.iterator();
                advance();
            } catch (IntrospectionException | NoSuchMethodException e) {
                throw new ExcelCreateResultException(e.getMessage());
            }
        }

        private void advance() {
            nextRow = null;
            while (rows.hasNext()) {
                Row r = rows.next();
                if (r.getRowNum() >= startRow) { nextRow = r; break; }
            }
        }

        @Override
        public boolean hasNext() { return nextRow != null; }

        @Override
        public T next() {
            try {
                T instance = constructor.newInstance();
                for (Map.Entry<Integer, FieldResultMetadata> entry : fieldMap.entrySet()) {
                    Cell cell = nextRow.getCell(entry.getKey());
                    if (cell == null || cell.getCellType() == CellType.BLANK || cell.getCellType() == CellType.ERROR)
                        continue;
                    Class<?> sourceType = Optional.ofNullable(cellTypeResolverFactory.getCellTypeResolver(cell))
                            .orElseThrow(() -> new ExcelReaderException(
                                    "There are no resolver for the CellType " + cell.getCellType().name()))
                            .resolve(cell);
                    if (sourceType == null) continue;
                    processCell(sourceType, entry.getValue().type(), cell, instance, entry.getValue());
                }
                advance();
                return instance;
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new ExcelCreateResultException(e.getMessage());
            }
        }
    }
}
