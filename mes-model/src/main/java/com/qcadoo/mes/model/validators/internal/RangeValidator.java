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

package com.qcadoo.mes.model.validators.internal;

import java.math.BigDecimal;
import java.util.Date;

import com.qcadoo.mes.api.Entity;
import com.qcadoo.mes.model.DataDefinition;
import com.qcadoo.mes.model.FieldDefinition;
import com.qcadoo.mes.model.validators.FieldValidator;

public final class RangeValidator implements FieldValidator {

    private static final String OUT_OF_RANGE_ERROR_SMALL = "core.validate.field.error.outOfRange.toSmall";

    private static final String OUT_OF_RANGE_ERROR_LARGE = "core.validate.field.error.outOfRange.toLarge";

    private String errorMessageSmall = OUT_OF_RANGE_ERROR_SMALL;

    private String errorMessageLarge = OUT_OF_RANGE_ERROR_LARGE;

    private String customErrorMessage;

    private final Object from;

    private final Object to;

    private final boolean inclusive;

    public RangeValidator(final Object from, final Object to, final boolean inclusive) {
        this.from = from;
        this.to = to;
        this.inclusive = inclusive;
    }

    @Override
    public boolean validate(final DataDefinition dataDefinition, final FieldDefinition fieldDefinition, final Object value,
            final Entity validatedEntity) {
        if (value == null) {
            return true;
        }

        Class<?> fieldClass = fieldDefinition.getType().getType();

        if (fieldClass.equals(String.class)) {
            return validateStringRange(fieldDefinition, (String) value, validatedEntity);
        } else if (fieldClass.equals(Integer.class) || fieldClass.equals(BigDecimal.class)) {
            return validateNumberRange(fieldDefinition, (Number) value, validatedEntity);
        } else if (fieldClass.equals(Date.class)) {
            return validateDateRange(fieldDefinition, (Date) value, validatedEntity);
        }

        return true;
    }

    private boolean validateDateRange(final FieldDefinition fieldDefinition, final Date value, final Entity validatedEntity) {
        if (from != null && ((!inclusive && !value.after((Date) from)) || (inclusive && value.before((Date) from)))) {
            addToSmallError(fieldDefinition, validatedEntity);
            return false;
        }
        if (to != null && ((!inclusive && !value.before((Date) to)) || (inclusive && value.after((Date) to)))) {
            addToLargeError(fieldDefinition, validatedEntity);
            return false;
        }
        return true;
    }

    private boolean validateNumberRange(final FieldDefinition fieldDefinition, final Number value, final Entity validatedEntity) {
        if (from != null
                && ((!inclusive && value.doubleValue() <= ((Number) from).doubleValue()) || (inclusive && value.doubleValue() < ((Number) from)
                        .doubleValue()))) {
            addToSmallError(fieldDefinition, validatedEntity);
            return false;
        }
        if (to != null
                && ((!inclusive && value.doubleValue() >= ((Number) to).doubleValue()) || (inclusive && value.doubleValue() > ((Number) to)
                        .doubleValue()))) {
            addToLargeError(fieldDefinition, validatedEntity);
            return false;
        }
        return true;
    }

    private boolean validateStringRange(final FieldDefinition fieldDefinition, final String value, final Entity validatedEntity) {
        if (from != null
                && ((!inclusive && value.compareTo((String) from) < 0) || (inclusive && value.compareTo((String) from) <= 0))) {
            addToSmallError(fieldDefinition, validatedEntity);
            return false;
        }
        if (to != null && ((!inclusive && value.compareTo((String) to) > 0) || (inclusive && value.compareTo((String) to) >= 0))) {
            addToLargeError(fieldDefinition, validatedEntity);
            return false;
        }
        return true;
    }

    private void addToSmallError(final FieldDefinition fieldDefinition, final Entity validatedEntity) {
        if (customErrorMessage != null) {
            validatedEntity.addError(fieldDefinition, customErrorMessage, String.valueOf(from), String.valueOf(to));
        } else {
            validatedEntity.addError(fieldDefinition, errorMessageSmall, String.valueOf(from), String.valueOf(to));
        }
    }

    private void addToLargeError(final FieldDefinition fieldDefinition, final Entity validatedEntity) {
        if (customErrorMessage != null) {
            validatedEntity.addError(fieldDefinition, customErrorMessage, String.valueOf(from), String.valueOf(to));
        } else {
            validatedEntity.addError(fieldDefinition, errorMessageLarge, String.valueOf(from), String.valueOf(to));
        }
    }

    @Override
    public boolean validate(final DataDefinition dataDefinition, final FieldDefinition fieldDefinition, final Entity entity) {
        return true;
    }

    @Override
    public FieldValidator customErrorMessage(final String errorMessage) {
        this.customErrorMessage = errorMessage;
        return this;
    }
}