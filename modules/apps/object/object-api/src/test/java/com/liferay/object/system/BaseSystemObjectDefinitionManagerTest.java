/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.system;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.language.LanguageImpl;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.mockito.Mockito;

/**
 * @author Nathaly Gomes
 */
public class BaseSystemObjectDefinitionManagerTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Before
	public void setUp() {
		LanguageUtil languageUtil = new LanguageUtil();

		languageUtil.setLanguage(new LanguageImpl());
	}

	@Test
	public void testGetDescriptionMap() {
		Assert.assertNull(_getDescriptionMap(Collections.emptyMap()));
		Assert.assertNull(
			_getDescriptionMap(Collections.singletonMap("label", "label")));
	}

	@Test
	public void testGetDescriptionMapWithDescriptionKey() {
		Map<Locale, String> descriptionMap = _getDescriptionMap(
			Collections.singletonMap("description", "description"));

		Assert.assertNotNull(descriptionMap);
		Assert.assertFalse(descriptionMap.isEmpty());
	}

	private Map<Locale, String> _getDescriptionMap(
		Map<String, String> labelKeys) {

		BaseSystemObjectDefinitionManager baseSystemObjectDefinitionManager =
			Mockito.mock(
				BaseSystemObjectDefinitionManager.class,
				Mockito.withSettings(
				).defaultAnswer(
					Mockito.CALLS_REAL_METHODS
				));

		Mockito.when(
			baseSystemObjectDefinitionManager.getLabelKeys()
		).thenReturn(
			labelKeys
		);

		return baseSystemObjectDefinitionManager.getDescriptionMap();
	}

}