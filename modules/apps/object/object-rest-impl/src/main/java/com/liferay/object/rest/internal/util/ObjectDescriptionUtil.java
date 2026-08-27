/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.rest.internal.util;

import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectField;
import com.liferay.object.model.ObjectRelationship;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Validator;

/**
 * @author Nathaly Gomes
 */
public class ObjectDescriptionUtil {

	/**
	 * Returns the description to publish in the generated OpenAPI document,
	 * preferring English and falling back to the object definition's default
	 * language. The document is cached per company, with no locale in the cache
	 * key, so it cannot honor the locale the client asked for.
	 */
	public static String getDescription(ObjectDefinition objectDefinition) {
		return _getDescription(
			objectDefinition.getDescription(LocaleUtil.US),
			objectDefinition.getDescription(
				objectDefinition.getDefaultLanguageId()));
	}

	public static String getDescription(
		ObjectDefinition objectDefinition, ObjectField objectField) {

		return _getDescription(
			objectField.getDescription(LocaleUtil.US),
			objectField.getDescription(
				objectDefinition.getDefaultLanguageId()));
	}

	public static String getDescription(
		ObjectDefinition objectDefinition,
		ObjectRelationship objectRelationship) {

		return _getDescription(
			objectRelationship.getDescription(LocaleUtil.US),
			objectRelationship.getDescription(
				objectDefinition.getDefaultLanguageId()));
	}

	private static String _getDescription(
		String englishDescription, String defaultDescription) {

		if (Validator.isNotNull(englishDescription)) {
			return englishDescription;
		}

		return defaultDescription;
	}

}