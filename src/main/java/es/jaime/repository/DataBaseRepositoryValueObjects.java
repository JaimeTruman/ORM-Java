package es.jaime.repository;

import es.jaime.connection.DatabaseConnection;
import es.jaime.mapper.EntityMapper;
import es.jaime.utils.IntrospectionUtils;
import es.jaimetruman.delete.Delete;
import es.jaimetruman.insert.Insert;
import es.jaimetruman.insert.InsertOptionFinal;
import es.jaimetruman.select.Select;
import es.jaimetruman.update.Update;
import es.jaimetruman.update.UpdateOptionFull1;
import es.jaimetruman.update.UpdateOptionInitial;
import lombok.SneakyThrows;

import java.util.*;
import java.util.function.Function;

public abstract class DataBaseRepositoryValueObjects<T> extends Repostitory<T> {
    protected final DatabaseConnection databaseConnection;

    private final EntityMapper entityMapper;
    private final String table;
    private final String idField;
    private final List<String> fieldsNames;
    protected final InsertOptionFinal insertQueryOnSave;
    protected final UpdateOptionInitial updateQueryOnSave;

    protected DataBaseRepositoryValueObjects(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
        this.entityMapper = entityMapper();
        this.table = entityMapper.getTable();
        this.idField = entityMapper.getIdField();
        this.fieldsNames = IntrospectionUtils.getFieldsNames(entityMapper.getClassToMap());
        this.insertQueryOnSave = Insert.table(table).fields(fieldsNames.toArray(new String[0]));
        this.updateQueryOnSave = Update.table(table);
    }

    @Override
    @SneakyThrows
    protected List<T> all() {
        return buildListFromQuery(Select.from(entityMapper.getTable()));
    }

    @Override
    protected Optional<T> findById(Object id) {
        return buildObjectFromQuery(
                Select.from(table).where(idField).equal(idValueObjectToIdPrimitive().apply(id))
        );
    }

    @Override
    @SneakyThrows
    protected void deleteById(Object id) {
        databaseConnection.sendUpdate(
                Delete.from(table).where(idField).equal(idValueObjectToIdPrimitive().apply(id))
        );
    }

    @Override
    protected void save(T toPersist) {
        Object idValueObject = toValueObjects(toPersist).get(idField);
        boolean exists = findById(idValueObject).isPresent();

        if(exists){
            updateExistingObject(toPersist, idValueObject);
        }else{
            persistNewObject(toPersist);
        }
    }

    @SneakyThrows
    private void updateExistingObject(T toUpdate, Object idValueObject){
        Object idPrimtive = idValueObjectToIdPrimitive().apply(idValueObject);
        UpdateOptionFull1 updateQuery = this.updateQueryOnSave.set(idField, idPrimtive);
        Map<String, Object> primitives = toPrimitives(toUpdate);

        for(String fieldName : fieldsNames){
            if(fieldName.equalsIgnoreCase(idField)) continue;

            Object value = primitives.get(fieldName);

            updateQuery = updateQuery.andSet(fieldName, value);
        }

        databaseConnection.sendUpdate(
                updateQuery.where(idField).equal(idPrimtive)
        );
    }

    @SneakyThrows
    private void persistNewObject(T toPersist){
        List<Object> valuesToAddInQuery = new ArrayList<>();
        Map<String, Object> toPrimitves = toPrimitives(toPersist);

        for(String fieldName : fieldsNames){
            Object value = toPrimitves.get(fieldName);

            valuesToAddInQuery.add(value);
        }

        databaseConnection.sendUpdate(
                insertQueryOnSave.values(valuesToAddInQuery.toArray(new Object[0]))
        );
    }

    public abstract Function<Object, Object> idValueObjectToIdPrimitive();
    public abstract Map<String, Object> toValueObjects(T aggregate);
    public abstract Map<String, Object> toPrimitives(T aggregate);
}
