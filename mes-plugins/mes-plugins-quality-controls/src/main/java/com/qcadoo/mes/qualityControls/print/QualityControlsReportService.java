/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo MES
 * Version: 0.3.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */

package com.qcadoo.mes.qualityControls.print;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.qcadoo.mes.api.DataDefinitionService;
import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.api.TranslationService;
import com.qcadoo.mes.model.DataDefinition;
import com.qcadoo.mes.model.search.Restrictions;
import com.qcadoo.mes.model.search.SearchResult;
import com.qcadoo.mes.model.types.internal.DateType;
import com.qcadoo.mes.utils.pdf.PdfUtil;
import com.qcadoo.mes.view.ComponentState;
import com.qcadoo.mes.view.ComponentState.MessageType;
import com.qcadoo.mes.view.ViewDefinitionState;
import com.qcadoo.mes.view.components.FieldComponentState;
import com.qcadoo.mes.view.components.form.FormComponentState;
import com.qcadoo.mes.view.components.grid.GridComponentState;

@Service
public class QualityControlsReportService {

    @Autowired
    private TranslationService translationService;

    @Autowired
    private DataDefinitionService dataDefinitionService;

    public final void printQualityControlReport(final ViewDefinitionState viewDefinitionState, final ComponentState state,
            final String[] args) {
        if (state instanceof FormComponentState) {
            FieldComponentState dateFrom = (FieldComponentState) viewDefinitionState.getComponentByReference("dateFrom");
            FieldComponentState dateTo = (FieldComponentState) viewDefinitionState.getComponentByReference("dateTo");
            if (dateFrom != null && dateTo != null && dateFrom.getFieldValue() != null && dateTo.getFieldValue() != null) {
                viewDefinitionState.redirectTo("/qualityControl/qualityControlByDates." + args[0] + "?type=" + args[1]
                        + "&dateFrom=" + dateFrom.getFieldValue() + "&dateTo=" + dateTo.getFieldValue(), true, false);
            } else {
                state.addMessage(translationService.translate("qualityControl.report.invalidDates", state.getLocale()),
                        MessageType.FAILURE);
            }
        } else {
            state.addMessage(translationService.translate("qualityControl.report.invalidDates", state.getLocale()),
                    MessageType.FAILURE);
        }
    }

    public final void printQualityControlReportForOrder(final ViewDefinitionState viewDefinitionState,
            final ComponentState state, final String[] args) {
        if (!(state instanceof GridComponentState)) {
            throw new IllegalStateException("method only for grid");
        }
        GridComponentState gridState = (GridComponentState) state;
        if (gridState.getSelectedEntitiesId().size() == 0) {
            state.addMessage(translationService.translate("core.grid.noRowSelectedError", state.getLocale()), MessageType.FAILURE);
            return;
        }
        StringBuilder redirectUrl = new StringBuilder();
        redirectUrl.append("/qualityControl/qualityControlReport.");
        redirectUrl.append(args[0]);
        redirectUrl.append("?type=");
        redirectUrl.append(args[1]);
        for (Long entityId : gridState.getSelectedEntitiesId()) {
            redirectUrl.append("&id=");
            redirectUrl.append(entityId);
        }
        viewDefinitionState.redirectTo(redirectUrl.toString(), true, false);
    }

    public final void addQualityControlReportHeader(final Document document, final Map<String, Object> model, final Locale locale)
            throws DocumentException {
        if (!model.containsKey("entities")) {
            Paragraph firstParagraphTitle = new Paragraph(new Phrase(translationService.translate(
                    "qualityControls.qualityControl.report.paragrah", locale), PdfUtil.getArialBold11Light()));
            firstParagraphTitle.add(new Phrase(" " + model.get("dateFrom") + " - " + model.get("dateTo"), PdfUtil
                    .getArialBold11Light()));
            firstParagraphTitle.setSpacingBefore(20);
            document.add(firstParagraphTitle);

        }
        Paragraph secondParagraphTitle = new Paragraph(new Phrase(translationService.translate(
                "qualityControls.qualityControl.report.paragrah2", locale), PdfUtil.getArialBold11Light()));
        secondParagraphTitle.setSpacingBefore(20);
        document.add(secondParagraphTitle);
    }

    public final Map<Entity, List<Entity>> getQualityOrdersForProduct(final List<Entity> orders) {
        Map<Entity, List<Entity>> productOrders = new HashMap<Entity, List<Entity>>();
        for (Entity entity : orders) {
            Entity product = entity.getBelongsToField("order").getBelongsToField("product");
            List<Entity> ordersList = new ArrayList<Entity>();
            if (productOrders.containsKey(product)) {
                ordersList = productOrders.get(product);
            }
            ordersList.add(entity);
            productOrders.put(product, ordersList);
        }
        return productOrders;
    }

    public final Map<Entity, List<BigDecimal>> getQualityOrdersQuantitiesForProduct(final List<Entity> orders) {
        Map<Entity, List<BigDecimal>> quantities = new HashMap<Entity, List<BigDecimal>>();
        for (Entity entity : orders) {
            Entity product = entity.getBelongsToField("order").getBelongsToField("product");
            List<BigDecimal> quantitiesList = new ArrayList<BigDecimal>();
            if (quantities.containsKey(product)) {
                quantitiesList = quantities.get(product);
                quantitiesList.set(0, quantitiesList.get(0).add((BigDecimal) entity.getField("controlledQuantity")));
                quantitiesList.set(1, quantitiesList.get(1).add((BigDecimal) entity.getField("rejectedQuantity")));
                quantitiesList.set(2, quantitiesList.get(2).add((BigDecimal) entity.getField("acceptedDefectsQuantity")));
            } else {
                quantitiesList.add(0, (BigDecimal) entity.getField("controlledQuantity"));
                quantitiesList.add(1, (BigDecimal) entity.getField("rejectedQuantity"));
                quantitiesList.add(2, (BigDecimal) entity.getField("acceptedDefectsQuantity"));
            }
            quantities.put(product, quantitiesList);
        }
        return quantities;
    }

    public final Map<Entity, List<BigDecimal>> getQualityOrdersResultsQuantitiesForProduct(final List<Entity> orders) {
        Map<Entity, List<BigDecimal>> quantities = new HashMap<Entity, List<BigDecimal>>();
        for (Entity entity : orders) {
            Entity product = entity.getBelongsToField("order").getBelongsToField("product");
            List<BigDecimal> quantitiesList = new ArrayList<BigDecimal>();
            if (quantities.containsKey(product)) {
                quantitiesList = quantities.get(product);
                quantitiesList.set(0, quantitiesList.get(0).add(BigDecimal.ONE));
                if ("01correct".equals(entity.getField("controlResult"))) {
                    quantitiesList.set(1, quantitiesList.get(1).add(BigDecimal.ONE));
                } else if ("02incorrect".equals(entity.getField("controlResult"))) {
                    quantitiesList.set(2, quantitiesList.get(2).add(BigDecimal.ONE));
                } else if ("03objection".equals(entity.getField("controlResult"))) {
                    quantitiesList.set(3, quantitiesList.get(3).add(BigDecimal.ONE));
                }
                if (entity.getBelongsToField("order").getField("doneQuantity") != null) {
                    quantitiesList.set(4,
                            quantitiesList.get(4).add((BigDecimal) entity.getBelongsToField("order").getField("doneQuantity")));
                } else {
                    quantitiesList
                            .set(4,
                                    quantitiesList.get(4).add(
                                            (BigDecimal) entity.getBelongsToField("order").getField("plannedQuantity")));
                }
            } else {
                quantitiesList.add(0, BigDecimal.ONE);
                if ("01correct".equals(entity.getField("controlResult"))) {
                    quantitiesList.add(1, BigDecimal.ONE);
                    quantitiesList.add(2, BigDecimal.ZERO);
                    quantitiesList.add(3, BigDecimal.ZERO);
                } else if ("02incorrect".equals(entity.getField("controlResult"))) {
                    quantitiesList.add(1, BigDecimal.ZERO);
                    quantitiesList.add(2, BigDecimal.ONE);
                    quantitiesList.add(3, BigDecimal.ZERO);
                } else if ("03objection".equals(entity.getField("controlResult"))) {
                    quantitiesList.add(1, BigDecimal.ZERO);
                    quantitiesList.add(2, BigDecimal.ZERO);
                    quantitiesList.add(3, BigDecimal.ONE);
                }
                if (entity.getBelongsToField("order").getField("doneQuantity") != null) {
                    quantitiesList.add(4, (BigDecimal) entity.getBelongsToField("order").getField("doneQuantity"));
                } else if (entity.getBelongsToField("order").getField("plannedQuantity") != null) {
                    quantitiesList.add(4, (BigDecimal) entity.getBelongsToField("order").getField("plannedQuantity"));
                } else {
                    quantitiesList.add(4, BigDecimal.ZERO);
                }
            }
            quantities.put(product, quantitiesList);
        }
        return quantities;
    }

    public final Map<Entity, List<Entity>> getQualityOrdersForOperation(final List<Entity> orders) {
        Map<Entity, List<Entity>> operationOrders = new HashMap<Entity, List<Entity>>();
        for (Entity entity : orders) {
            Entity operation = entity.getBelongsToField("operation");
            List<Entity> ordersList = new ArrayList<Entity>();
            if (operationOrders.containsKey(operation)) {
                ordersList = operationOrders.get(operation);
            }
            ordersList.add(entity);
            operationOrders.put(operation, ordersList);
        }
        return operationOrders;
    }

    public final Map<Entity, List<BigDecimal>> getQualityOrdersResultsQuantitiesForOperation(final List<Entity> orders) {
        Map<Entity, List<BigDecimal>> quantities = new HashMap<Entity, List<BigDecimal>>();
        for (Entity entity : orders) {
            Entity operation = entity.getBelongsToField("operation");
            List<BigDecimal> quantitiesList = new ArrayList<BigDecimal>();
            if (quantities.containsKey(operation)) {
                quantitiesList = quantities.get(operation);
                quantitiesList.set(0, quantitiesList.get(0).add(BigDecimal.ONE));
                if ("01correct".equals(entity.getField("controlResult"))) {
                    quantitiesList.set(1, quantitiesList.get(1).add(BigDecimal.ONE));
                } else if ("02incorrect".equals(entity.getField("controlResult"))) {
                    quantitiesList.set(2, quantitiesList.get(2).add(BigDecimal.ONE));
                } else if ("03objection".equals(entity.getField("controlResult"))) {
                    quantitiesList.set(3, quantitiesList.get(3).add(BigDecimal.ONE));
                }
            } else {
                quantitiesList.add(0, BigDecimal.ONE);
                if ("01correct".equals(entity.getField("controlResult"))) {
                    quantitiesList.add(1, BigDecimal.ONE);
                    quantitiesList.add(2, BigDecimal.ZERO);
                    quantitiesList.add(3, BigDecimal.ZERO);
                } else if ("02incorrect".equals(entity.getField("controlResult"))) {
                    quantitiesList.add(1, BigDecimal.ZERO);
                    quantitiesList.add(2, BigDecimal.ONE);
                    quantitiesList.add(3, BigDecimal.ZERO);
                } else if ("03objection".equals(entity.getField("controlResult"))) {
                    quantitiesList.add(1, BigDecimal.ZERO);
                    quantitiesList.add(2, BigDecimal.ZERO);
                    quantitiesList.add(3, BigDecimal.ONE);
                }
            }
            quantities.put(operation, quantitiesList);
        }
        return quantities;
    }

    @SuppressWarnings("unchecked")
    public final List<Entity> getOrderSeries(final Map<String, Object> model, final String type) {
        DataDefinition dataDef = dataDefinitionService.get("qualityControls", "qualityControl");
        if (model.containsKey("entities")) {
            if (!(model.get("entities") instanceof List<?>)) {
                throw new IllegalStateException("entities are not list");
            }
            List<Entity> entities = (List<Entity>) model.get("entities");
            for (Entity entity : entities) {
                if (!(Boolean) entity.getField("closed")) {
                    throw new IllegalStateException("quality controll is not closed");
                }
            }
            return entities;
        } else {
            try {
                SearchResult result = dataDef
                        .find()
                        .restrictedWith(
                                Restrictions.ge(dataDef.getField("date"),
                                        DateType.parseDate(model.get("dateFrom").toString(), false)))
                        .restrictedWith(
                                Restrictions.le(dataDef.getField("date"),
                                        DateType.parseDate(model.get("dateTo").toString(), true)))
                        .restrictedWith(Restrictions.eq("qualityControlType", type))
                        .restrictedWith(Restrictions.eq("closed", true)).list();
                return result.getEntities();
            } catch (ParseException e) {
                return Collections.emptyList();
            }
        }
    }

    public final Element prepareTitle(final Entity product, final Locale locale, final String type) {

        Paragraph title = new Paragraph();

        if (type.equals("product")) {
            title.add(new Phrase(translationService.translate("qualityControls.qualityControl.report.paragrah1", locale), PdfUtil
                    .getArialBold11Light()));
            String name = "";
            if (product != null) {
                name = product.getField("name").toString();
            }
            title.add(new Phrase(" " + name, PdfUtil.getArialBold11Dark()));
        }

        return title;
    }
}