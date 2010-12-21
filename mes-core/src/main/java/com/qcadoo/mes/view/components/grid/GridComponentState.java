package com.qcadoo.mes.view.components.grid;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.model.DataDefinition;
import com.qcadoo.mes.model.FieldDefinition;
import com.qcadoo.mes.model.search.Restrictions;
import com.qcadoo.mes.model.search.SearchCriteriaBuilder;
import com.qcadoo.mes.model.search.SearchResult;
import com.qcadoo.mes.model.types.BelongsToType;
import com.qcadoo.mes.model.types.FieldType;
import com.qcadoo.mes.view.states.AbstractComponentState;

public final class GridComponentState extends AbstractComponentState {

    public static final String JSON_SELECTED_ENTITY_ID = "selectedEntityId";

    public static final String JSON_BELONGS_TO_ENTITY_ID = "belongsToEntityId";

    public static final String JSON_FIRST_ENTITY = "firstEntity";

    public static final String JSON_MAX_ENTITIES = "maxEntities";

    public static final String JSON_TOTAL_ENTITIES = "totalEntities";

    public static final String JSON_ORDER = "order";

    public static final String JSON_ORDER_COLUMN = "column";

    public static final String JSON_ORDER_DIRECTION = "direction";

    public static final String JSON_FILTERS = "filters";

    public static final String JSON_FILTERS_ENABLED = "filtersEnabled";

    public static final String JSON_ENTITIES = "entities";

    private final GridEventPerformer eventPerformer = new GridEventPerformer();

    private final Map<String, GridComponentColumn> columns;

    private final FieldDefinition belongsToFieldDefinition;

    private Long selectedEntityId;

    private Long belongsToEntityId;

    private List<Entity> entities;

    private int totalEntities;

    private int firstResult;

    private int maxResults = Integer.MAX_VALUE;

    private boolean filtersEnabled = true;

    private String orderColumn;

    private String orderDirection;

    private final Map<String, String> filters = new HashMap<String, String>();

    public GridComponentState(final FieldDefinition scopeField, final Map<String, GridComponentColumn> columns,
            final String orderColumn, final String orderDirection) {
        this.belongsToFieldDefinition = scopeField;
        this.columns = columns;
        this.orderColumn = orderColumn;
        this.orderDirection = orderDirection;
        registerEvent("refresh", eventPerformer, "refresh");
        registerEvent("select", eventPerformer, "selectEntity");
        registerEvent("remove", eventPerformer, "removeSelectedEntity");
        registerEvent("moveUp", eventPerformer, "moveUpSelectedEntity");
        registerEvent("moveDown", eventPerformer, "moveDownSelectedEntity");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initializeContext(final JSONObject json) throws JSONException {
        Iterator<String> iterator = json.keys();
        while (iterator.hasNext()) {
            String field = iterator.next();
            if (JSON_BELONGS_TO_ENTITY_ID.equals(field)) {
                onScopeEntityIdChange(json.getLong(field));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initializeContent(final JSONObject json) throws JSONException {
        if (json.has(JSON_SELECTED_ENTITY_ID) && !json.isNull(JSON_SELECTED_ENTITY_ID)) {
            selectedEntityId = json.getLong(JSON_SELECTED_ENTITY_ID);
        }
        if (json.has(JSON_BELONGS_TO_ENTITY_ID) && !json.isNull(JSON_BELONGS_TO_ENTITY_ID)) {
            belongsToEntityId = json.getLong(JSON_BELONGS_TO_ENTITY_ID);
        }
        if (json.has(JSON_FIRST_ENTITY) && !json.isNull(JSON_FIRST_ENTITY)) {
            firstResult = json.getInt(JSON_FIRST_ENTITY);
        }
        if (json.has(JSON_MAX_ENTITIES) && !json.isNull(JSON_MAX_ENTITIES)) {
            maxResults = json.getInt(JSON_MAX_ENTITIES);
        }
        if (json.has(JSON_FILTERS_ENABLED) && !json.isNull(JSON_FILTERS_ENABLED)) {
            filtersEnabled = json.getBoolean(JSON_FILTERS_ENABLED);
        }
        if (json.has(JSON_ORDER) && !json.isNull(JSON_ORDER)) {
            JSONObject orderJson = json.getJSONObject(JSON_ORDER);
            if (orderJson.has(JSON_ORDER_COLUMN) && orderJson.has(JSON_ORDER_DIRECTION)) {
                orderColumn = orderJson.getString(JSON_ORDER_COLUMN);
                orderDirection = orderJson.getString(JSON_ORDER_DIRECTION);
            }
        }
        if (json.has(JSON_FILTERS) && !json.isNull(JSON_FILTERS)) {
            JSONObject filtersJson = json.getJSONObject(JSON_FILTERS);
            Iterator<String> filtersKeys = filtersJson.keys();
            while (filtersKeys.hasNext()) {
                String column = filtersKeys.next();
                filters.put(column, filtersJson.getString(column));
            }
        }

        if (belongsToFieldDefinition != null && belongsToEntityId == null) {
            setEnabled(false);
        }

        requestRender();
        requestUpdateState();
    }

    @Override
    public void onFieldEntityIdChange(final Long entityId) {
        setSelectedEntityId(entityId);
    }

    @Override
    public void onScopeEntityIdChange(final Long scopeEntityId) {
        if (belongsToFieldDefinition != null) {
            this.belongsToEntityId = scopeEntityId;
            setEnabled(scopeEntityId != null);
        } else {
            throw new IllegalStateException("Grid doesn't have scopeField, it cannot set scopeEntityId");
        }
    }

    @Override
    protected JSONObject renderContent() throws JSONException {
        if (entities == null) {
            eventPerformer.reload();
        }

        if (entities == null) {
            throw new IllegalStateException("Cannot load entities for grid component");
        }

        JSONObject json = new JSONObject();
        json.put(JSON_SELECTED_ENTITY_ID, selectedEntityId);
        json.put(JSON_BELONGS_TO_ENTITY_ID, belongsToEntityId);
        json.put(JSON_FIRST_ENTITY, firstResult);
        json.put(JSON_MAX_ENTITIES, maxResults);
        json.put(JSON_FILTERS_ENABLED, filtersEnabled);
        json.put(JSON_TOTAL_ENTITIES, totalEntities);

        if (orderColumn != null) {
            JSONObject jsonOrder = new JSONObject();
            jsonOrder.put(JSON_ORDER_COLUMN, orderColumn);
            jsonOrder.put(JSON_ORDER_DIRECTION, orderDirection);
            json.put(JSON_ORDER, jsonOrder);
        }

        JSONObject jsonFilters = new JSONObject();
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            jsonFilters.put(entry.getKey(), entry.getValue());
        }

        json.put(JSON_FILTERS, jsonFilters);

        JSONArray jsonEntities = new JSONArray();
        for (Entity entity : entities) {
            jsonEntities.put(convertEntityToJson(entity));
        }

        json.put(JSON_ENTITIES, jsonEntities);

        return json;
    }

    private JSONObject convertEntityToJson(final Entity entity) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", entity.getId());
        JSONObject fields = new JSONObject();
        for (GridComponentColumn column : columns.values()) {
            fields.put(column.getName(), column.getValue(entity, getLocale()));
        }
        json.put("fields", fields);

        return json;
    }

    public void setSelectedEntityId(final Long selectedEntityId) {
        this.selectedEntityId = selectedEntityId;
        notifyEntityIdChangeListeners(selectedEntityId);
    }

    public Long getSelectedEntityId() {
        return selectedEntityId;
    }

    @Override
    public Object getFieldValue() {
        return getSelectedEntityId();
    }

    @Override
    public void setFieldValue(final Object value) {
        setSelectedEntityId((Long) value);
    }

    private String translateMessage(final String key) {
        List<String> codes = Arrays.asList(new String[] { getTranslationPath() + "." + key, "core.message." + key });
        return getTranslationService().translate(codes, getLocale());
    }

    protected class GridEventPerformer {

        public void refresh(final String[] args) {
            // nothing interesting here
        }

        public void selectEntity(final String[] args) {
            notifyEntityIdChangeListeners(getSelectedEntityId());
        }

        public void removeSelectedEntity(final String[] args) {
            Entity entity = getDataDefinition().get(selectedEntityId);
            if (entity == null) {
                addMessage(translateMessage("entityNotFound"), MessageType.FAILURE);
            } else {
                getDataDefinition().delete(selectedEntityId);
                addMessage(translateMessage("deleteMessage"), MessageType.SUCCESS);
            }
            setSelectedEntityId(null);
        }

        public void moveUpSelectedEntity(final String[] args) {
            getDataDefinition().move(selectedEntityId, -1);
            addMessage(translateMessage("moveMessage"), MessageType.SUCCESS);
        }

        public void moveDownSelectedEntity(final String[] args) {
            getDataDefinition().move(selectedEntityId, 1);
            addMessage(translateMessage("moveMessage"), MessageType.SUCCESS);
        }

        private void reload() {
            if (belongsToFieldDefinition == null || belongsToEntityId != null) {
                SearchCriteriaBuilder criteria = getDataDefinition().find();
                if (belongsToFieldDefinition != null) {
                    criteria.restrictedWith(Restrictions.belongsTo(belongsToFieldDefinition, belongsToEntityId));
                }

                if (filtersEnabled) {
                    addFilters(criteria);
                }

                addOrder(criteria);
                addPaging(criteria);

                SearchResult result = criteria.list();

                if (repeatWithFixedFirstResult(result)) {
                    addPaging(criteria);
                    result = criteria.list();
                }

                entities = result.getEntities();
                totalEntities = result.getTotalNumberOfEntities();
            } else {
                entities = Collections.emptyList();
                totalEntities = 0;
            }
        }

        private void addPaging(final SearchCriteriaBuilder criteria) {
            criteria.withFirstResult(firstResult);
            criteria.withMaxResults(maxResults);
        }

        private void addFilters(final SearchCriteriaBuilder criteria) {
            for (Map.Entry<String, String> filter : filters.entrySet()) {
                String field = getFieldNameByColumnName(filter.getKey());

                if (field != null) {
                    FieldType type = getFieldType(field);

                    if (type != null && String.class.isAssignableFrom(type.getType())) {
                        criteria.restrictedWith(Restrictions.eq(field, filter.getValue() + "*"));
                    } else if (type != null && Boolean.class.isAssignableFrom(type.getType())) {
                        criteria.restrictedWith(Restrictions.eq(field, "1".equals(filter.getValue())));
                    } else {
                        criteria.restrictedWith(Restrictions.eq(field, filter.getValue()));
                    }
                }
            }
        }

        private FieldType getFieldType(final String field) {
            String[] path = field.split("\\.");

            DataDefinition dataDefinition = getDataDefinition();

            for (int i = 0; i < path.length; i++) {
                if (dataDefinition.getField(path[i]) == null) {
                    return null;
                }

                FieldType fieldType = dataDefinition.getField(path[i]).getType();

                if (i < path.length - 1) {
                    if (fieldType instanceof BelongsToType) {
                        dataDefinition = ((BelongsToType) fieldType).getDataDefinition();
                        continue;
                    } else {
                        return null;
                    }
                }

                return fieldType;
            }

            return null;
        }

        private void addOrder(final SearchCriteriaBuilder criteria) {
            if (orderColumn != null) {
                String field = getFieldNameByColumnName(orderColumn);

                if (field != null) {
                    if ("asc".equals(orderDirection)) {
                        criteria.orderAscBy(field);
                    } else {
                        criteria.orderDescBy(field);
                    }
                }
            }
        }

        private String getFieldNameByColumnName(final String columnName) {
            GridComponentColumn column = columns.get(columnName);

            if (column == null) {
                return null;
            }

            if (StringUtils.hasText(column.getExpression())) {
                Matcher matcher = Pattern.compile("#(\\w+)\\['(\\w+)'\\]").matcher(column.getExpression());
                if (matcher.matches()) {
                    return matcher.group(1) + "." + matcher.group(2);
                }
            } else if (column.getFields().size() == 1) {
                return column.getFields().get(0).getName();
            }

            return null;
        }

        private boolean repeatWithFixedFirstResult(final SearchResult result) {
            if (result.getEntities().isEmpty() && result.getTotalNumberOfEntities() > 0) {
                while (firstResult >= result.getTotalNumberOfEntities()) {
                    firstResult -= maxResults;
                }
                return true;
            } else {
                return false;
            }
        }
    }

}
