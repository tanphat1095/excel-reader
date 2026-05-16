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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellReference;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Builder
public class ExcelReader {
    private final CellTypeResolverFactory cellTypeResolverFactory;
    private final CellValueResolverFactory cellValueResolverFactory;
    private final SourceToTargetResolverFactory sourceToTargetResolverFactory;
    private final String datePattern;

    public ExcelReader() {
        this.datePattern = "dd-MM-yyyy";
        this.cellTypeResolverFactory = new CellTypeResolverFactory();
        this.cellValueResolverFactory = new CellValueResolverFactory();
        this.sourceToTargetResolverFactory = new SourceToTargetResolverFactory(datePattern);
    }

    public <T> List<T> read(Sheet sheet, CellReference cellReference, Class<T> clazz) {
        try {
            if (Map.class.isAssignableFrom(clazz))
                throw new ExcelCreateResultException("The result type Map is not supported. Please follow the Java Bean standard");
            Iterator<Row> rowIterator = sheet.iterator();
            int index = cellReference.getRow();
            List<T> result = new ArrayList<>();
            Map<Integer, FieldResultMetadata> fieldResultMetadataMap = getFieldResultMetadataMap(clazz);
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() < index) continue;
                T instance = clazz.getDeclaredConstructor().newInstance();
                for (Map.Entry<Integer, FieldResultMetadata> entry : fieldResultMetadataMap.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    Class<?> sourceType =  Optional.ofNullable(cellTypeResolverFactory.getCellTypeResolver(cell))
                            .orElseThrow(() -> new ExcelReaderException("There are no resolver for the CellType " + cell.getCellType().name()))
                            .resolve(cell);
                    processCell(sourceType, entry.getValue().type(), cell, instance, entry.getValue());

                }
                result.add(instance);
            }
            return result;
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new ExcelCreateResultException(e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ExcelCreateResultException("The result class must have a no-arg constructor");
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    <S, T, O> void processCell(Class<S> sourceClass, Class<T> targetClass, Cell cell, O instance, FieldResultMetadata fieldResultMetadata) throws InvocationTargetException, IllegalAccessException {
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
                fieldResultMetadataMap.put(excelColumn.index(), new FieldResultMetadata(field.getName(), field.getType(), propertyDescriptor.getWriteMethod()));
            } catch (NoSuchFieldException ex) {
                throw new ExcelCreateResultException("The class " + clazz.getName() + " does not have a field named " + propertyDescriptor.getName());
            }
        }
        return fieldResultMetadataMap;
    }

}
