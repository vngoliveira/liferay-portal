/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.workflow.kaleo.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.module.configuration.ConfigurationException;
import com.liferay.portal.kernel.security.auth.PrincipalThreadLocal;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.kernel.security.permission.PermissionThreadLocal;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DataGuard;
import com.liferay.portal.kernel.test.util.RandomTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.test.util.TestPropsValues;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.test.rule.Inject;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.test.rule.PermissionCheckerMethodTestRule;
import com.liferay.portal.workflow.configuration.WorkflowDefinitionConfiguration;
import com.liferay.portal.workflow.kaleo.model.KaleoDefinition;
import com.liferay.portal.workflow.kaleo.service.KaleoDefinitionService;

import java.io.IOException;
import java.io.InputStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Nathaly Gomes
 */
@DataGuard(scope = DataGuard.Scope.METHOD)
@RunWith(Arquillian.class)
public class KaleoDefinitionServiceImplTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			PermissionCheckerMethodTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_omniAdminUser = TestPropsValues.getUser();
		_originalName = PrincipalThreadLocal.getName();
		_originalPermissionChecker =
			PermissionThreadLocal.getPermissionChecker();
		_serviceContext = ServiceContextTestUtil.getServiceContext();
	}

	@After
	public void tearDown() {
		PermissionThreadLocal.setPermissionChecker(_originalPermissionChecker);
		PrincipalThreadLocal.setName(_originalName);
	}

	@Test
	public void testAddKaleoDefinitionAsOmniAdminUser() throws Exception {
		_setCompanyAdministratorCanPublish();

		Assert.assertNotNull(_testAddKaleoDefinition(_omniAdminUser));

		_deleteCompanyAdministratorCanPublish();

		Assert.assertNotNull(_testAddKaleoDefinition(_omniAdminUser));
	}

	@Test
	public void testUpdateKaleoDefinitionAsOmniAdminUser()
		throws IOException, PortalException {

		_setCompanyAdministratorCanPublish();

		Assert.assertNotNull(_testUpdateKaleoDefinition(_omniAdminUser));

		_deleteCompanyAdministratorCanPublish();

		Assert.assertNotNull(_testUpdateKaleoDefinition(_omniAdminUser));
	}

	private void _deleteCompanyAdministratorCanPublish()
		throws ConfigurationException {

		_configurationProvider.deleteSystemConfiguration(
			WorkflowDefinitionConfiguration.class);
	}

	private String _read(String name) throws IOException {
		ClassLoader classLoader =
			BaseKaleoLocalServiceTestCase.class.getClassLoader();

		try (InputStream inputStream = classLoader.getResourceAsStream(
				"com/liferay/portal/workflow/kaleo/dependencies/" + name)) {

			return StringUtil.read(inputStream);
		}
	}

	private void _setCompanyAdministratorCanPublish()
		throws ConfigurationException {

		_configurationProvider.saveSystemConfiguration(
			WorkflowDefinitionConfiguration.class,
			HashMapDictionaryBuilder.<String, Object>put(
				"companyAdministratorCanPublish", true
			).build());
	}

	private void _setUser(User user) {
		PermissionThreadLocal.setPermissionChecker(
			PermissionCheckerFactoryUtil.create(user));
		PrincipalThreadLocal.setName(user.getUserId());
	}

	private KaleoDefinition _testAddKaleoDefinition(User user)
		throws Exception {

		_setUser(user);

		return _kaleoDefinitionService.addKaleoDefinition(
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			RandomTestUtil.randomString(),
			_read("legal-marketing-workflow-definition.xml"), "company", 1,
			_serviceContext);
	}

	private KaleoDefinition _testUpdateKaleoDefinition(User user)
		throws IOException, PortalException {

		_setUser(_omniAdminUser);

		KaleoDefinition kaleoDefinition =
			_kaleoDefinitionService.addKaleoDefinition(
				RandomTestUtil.randomString(), RandomTestUtil.randomString(),
				RandomTestUtil.randomString(),
				_read("legal-marketing-workflow-definition.xml"), "company", 1,
				_serviceContext);

		_setUser(user);

		return _kaleoDefinitionService.updateKaleoDefinition(
			kaleoDefinition.getKaleoDefinitionId(),
			RandomTestUtil.randomString(), RandomTestUtil.randomString(),
			kaleoDefinition.getContent(), _serviceContext);
	}

	@Inject
	private ConfigurationProvider _configurationProvider;

	@Inject
	private KaleoDefinitionService _kaleoDefinitionService;

	private User _omniAdminUser;
	private String _originalName;
	private PermissionChecker _originalPermissionChecker;
	private ServiceContext _serviceContext;

}