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

package com.qcadoo.view.internal.patterns;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.springframework.test.util.ReflectionTestUtils.getField;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.qcadoo.localization.api.TranslationService;
import com.qcadoo.mes.beans.sample.CustomEntityService;
import com.qcadoo.view.internal.ComponentDefinition;
import com.qcadoo.view.internal.ComponentPattern;
import com.qcadoo.view.internal.ComponentState;
import com.qcadoo.view.internal.ContainerPattern;
import com.qcadoo.view.internal.ViewDefinition;
import com.qcadoo.view.internal.ViewDefinitionState;
import com.qcadoo.view.internal.components.TextInputComponentPattern;
import com.qcadoo.view.internal.components.form.FormComponentPattern;
import com.qcadoo.view.internal.components.form.FormComponentState;
import com.qcadoo.view.internal.components.window.WindowComponentPattern;
import com.qcadoo.view.internal.internal.ComponentCustomEvent;
import com.qcadoo.view.internal.internal.EventHandlerHolder;
import com.qcadoo.view.internal.patterns.AbstractComponentPattern;
import com.qcadoo.view.internal.patterns.AbstractContainerPattern;

public class ComponentPatternTest extends AbstractPatternTest {

    @Test
    @SuppressWarnings("unchecked")
    public void shouldHaveValidInstance() throws Exception {
        // given
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        TranslationService translationService = mock(TranslationService.class);
        ViewDefinition viewDefinition = mock(ViewDefinition.class);
        ComponentDefinition componentDefinition = getComponentDefinition("testName", null);
        componentDefinition.setTranslationService(translationService);
        componentDefinition.setViewDefinition(viewDefinition);
        AbstractComponentPattern pattern = new FormComponentPattern(componentDefinition);
        CustomEntityService object = mock(CustomEntityService.class);
        pattern.addCustomEvent(new ComponentCustomEvent("save", object, "saveForm"));

        // when
        ComponentState state = pattern.createComponentState(viewDefinitionState);

        // then
        assertTrue(state instanceof FormComponentState);

        EventHandlerHolder eventHandlerHolder = (EventHandlerHolder) getField(state, "eventHandlerHolder");
        Map<String, List<Object>> eventHandlers = (Map<String, List<Object>>) getField(eventHandlerHolder, "eventHandlers");

        List<Object> handlers = eventHandlers.get("save");

        assertNotNull(handlers);
        assertEquals(2, handlers.size());
        assertEquals(object, getField(handlers.get(1), "obj"));
        assertEquals("saveForm", ((Method) getField(handlers.get(1), "method")).getName());
    }

    @Test
    public void shouldHaveName() throws Exception {
        // given
        ComponentPattern pattern = new FormComponentPattern(getComponentDefinition("testName", null));

        // when
        String name = pattern.getName();

        // then
        Assert.assertEquals("testName", name);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailWithoutName() throws Exception {
        // given
        ViewDefinitionState viewDefinitionState = mock(ViewDefinitionState.class);
        ComponentDefinition componentDefinition = new ComponentDefinition();
        ComponentPattern pattern = new FormComponentPattern(componentDefinition);

        // when
        pattern.createComponentState(viewDefinitionState);
    }

    @Test
    public void shouldReturnValidPath() throws Exception {
        // given
        ContainerPattern root = new WindowComponentPattern(getComponentDefinition("rootName", null));
        ContainerPattern child1 = new WindowComponentPattern(getComponentDefinition("child1", root, null));
        ComponentPattern child2 = new TextInputComponentPattern(getComponentDefinition("child2", root, null));
        ComponentPattern child11 = new TextInputComponentPattern(getComponentDefinition("child11", child1, null));

        // when
        String rootPathName = root.getPath();
        String child1PathName = child1.getPath();
        String child2PathName = child2.getPath();
        String child11PathName = child11.getPath();

        // then
        Assert.assertEquals("rootName", rootPathName);
        Assert.assertEquals("rootName.child1", child1PathName);
        Assert.assertEquals("rootName.child2", child2PathName);
        Assert.assertEquals("rootName.child1.child11", child11PathName);
    }

    @Test
    public void shouldAddItselfToParentOnInitialize() throws Exception {
        // given
        ViewDefinition viewDefinition = mock(ViewDefinition.class);
        AbstractContainerPattern parent = new WindowComponentPattern(getComponentDefinition("test", viewDefinition));

        ComponentPattern pattern = new TextInputComponentPattern(getComponentDefinition("testName", "testField", null, parent,
                viewDefinition));

        // when
        pattern.initialize();

        // then
        assertEquals(pattern, parent.getFieldEntityIdChangeListeners().get("testField"));
    }

    @Test
    public void shouldHaveDefaultEnabledFlag() throws Exception {
        // given
        ComponentDefinition componentDefinition = getComponentDefinition("testName", null);
        componentDefinition.setDefaultEnabled(false);
        AbstractComponentPattern pattern = new TextInputComponentPattern(componentDefinition);

        // then
        assertFalse(pattern.isDefaultEnabled());
    }

    @Test
    public void shouldHaveDefaultVisibleFlag() throws Exception {
        // given
        ComponentDefinition componentDefinition = getComponentDefinition("testName", null);
        componentDefinition.setDefaultVisible(false);
        AbstractComponentPattern pattern = new TextInputComponentPattern(componentDefinition);

        // then
        assertFalse(pattern.isDefaultVisible());
    }

    @Test
    public void shouldHaveHasDescriptionFlag() throws Exception {
        // given
        ComponentDefinition componentDefinition = getComponentDefinition("testName", null);
        componentDefinition.setHasDescription(true);
        AbstractComponentPattern pattern = new TextInputComponentPattern(componentDefinition);

        // then
        assertTrue(pattern.isHasDescription());
    }

    @Test
    public void shouldHaveReferenceName() throws Exception {
        // given
        ComponentDefinition componentDefinition = getComponentDefinition("testName", null);
        componentDefinition.setReference("uniqueReferenceName");
        AbstractComponentPattern pattern = new TextInputComponentPattern(componentDefinition);

        // then
        assertEquals("uniqueReferenceName", pattern.getReference());
    }

    @Test
    public void shouldHaveListenersOnEmptyOptions() throws Exception {
        // given
        TranslationService translationService = mock(TranslationService.class);
        ViewDefinition viewDefinition = mock(ViewDefinition.class);
        ComponentDefinition componentDefinition = getComponentDefinition("testName", null);
        componentDefinition.setTranslationService(translationService);
        componentDefinition.setViewDefinition(viewDefinition);
        AbstractComponentPattern pattern = new TextInputComponentPattern(componentDefinition);

        // when
        Map<String, Object> model = pattern.prepareView(Locale.ENGLISH);

        // then
        JSONObject options = (JSONObject) model.get("jsOptions");

        assertEquals(4, options.length());
    }

    @Test
    public void shouldHaveOptionsWhenAdded() throws Exception {
        // given
        AbstractComponentPattern pattern = new WindowComponentPattern(getComponentDefinition("testName", null));

        // when
        Map<String, Object> model = pattern.prepareView(Locale.ENGLISH);

        // then
        JSONObject options = (JSONObject) model.get("jsOptions");
        assertTrue(options.getBoolean("hasRibbon"));
    }

    @Test
    public void shouldHaveListenersInOptions() throws Exception {
        // given
        ViewDefinition viewDefinition = mock(ViewDefinition.class);

        AbstractContainerPattern parent = new WindowComponentPattern(getComponentDefinition("f1", viewDefinition));

        ComponentPatternMock child1 = new ComponentPatternMock(getComponentDefinition("t1", "t1", null, parent, viewDefinition));

        ComponentPatternMock child2 = new ComponentPatternMock(getComponentDefinition("t2", "t2", null, parent, viewDefinition));

        ComponentPatternMock child3 = new ComponentPatternMock(getComponentDefinition("t3", null, "t3", parent, viewDefinition));

        parent.addChild(child1);
        parent.addChild(child2);
        parent.addChild(child3);

        parent.initialize();
        child1.initialize();
        child2.initialize();
        child3.initialize();

        // when
        Map<String, Object> model = parent.prepareView(Locale.ENGLISH);

        // then
        JSONObject options = (JSONObject) model.get("jsOptions");
        JSONArray listenersArray = options.getJSONArray("listeners");
        assertEquals(3, listenersArray.length());
        assertTrue(JsonArrayContain(listenersArray, "f1.t1"));
        assertTrue(JsonArrayContain(listenersArray, "f1.t2"));
        assertTrue(JsonArrayContain(listenersArray, "f1.t3"));
    }

    private boolean JsonArrayContain(final JSONArray array, final String value) throws JSONException {
        for (int i = 0; i < array.length(); i++) {
            if (array.getString(i).equals(value)) {
                return true;
            }
        }
        return false;
    }
}