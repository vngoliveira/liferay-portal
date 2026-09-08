/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.captcha.rest.internal.resource.v1_0;

import com.liferay.captcha.configuration.CaptchaConfiguration;
import com.liferay.captcha.rest.dto.v1_0.Captcha;
import com.liferay.captcha.rest.resource.v1_0.CaptchaResource;
import com.liferay.captcha.simplecaptcha.SimpleCaptchaImpl;
import com.liferay.captcha.util.CaptchaUtil;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.captcha.CaptchaTextException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.security.SecureRandomUtil;
import com.liferay.portal.kernel.util.Base64;
import com.liferay.portal.kernel.util.Time;
import com.liferay.portal.servlet.filters.secure.NonceUtil;

import jakarta.ws.rs.ForbiddenException;

import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;

import java.security.GeneralSecurityException;
import java.security.Key;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

/**
 * @author Loc Pham
 */
@Component(
	properties = "OSGI-INF/liferay/rest/v1_0/captcha.properties",
	scope = ServiceScope.PROTOTYPE, service = CaptchaResource.class
)
public class CaptchaResourceImpl extends BaseCaptchaResourceImpl {

	@Override
	public Captcha getCaptchaChallenge() throws Exception {
		_checkCaptchaConfiguration();

		com.liferay.portal.kernel.captcha.Captcha kernelCaptcha =
			CaptchaUtil.getCaptcha();

		try (ByteArrayOutputStream byteArrayOutputStream =
				new ByteArrayOutputStream()) {

			String expectedAnswer = kernelCaptcha.serveImage(
				byteArrayOutputStream);

			return new Captcha() {
				{
					setImage(
						() -> {
							String data = Base64.encode(
								byteArrayOutputStream.toByteArray());

							return "data:image/png;base64," + data;
						});
					setToken(
						() -> _encryptToken(
							contextCompany.getKeyObj(),
							JSONUtil.put(
								"answer", expectedAnswer
							).put(
								"expiryTime",
								System.currentTimeMillis() + (Time.MINUTE * 5)
							).put(
								"nonce",
								NonceUtil.generate(
									contextCompany.getCompanyId(),
									contextHttpServletRequest.getRemoteAddr())
							).toString()));
				}
			};
		}
	}

	@Override
	public void postCaptchaResponse(Captcha captcha) throws Exception {
		_checkCaptchaConfiguration();

		JSONObject jsonObject = null;

		try {
			jsonObject = _jsonFactory.createJSONObject(
				_decryptToken(contextCompany.getKeyObj(), captcha.getToken()));
		}
		catch (Exception exception) {
			throw new IllegalArgumentException(exception);
		}

		if (!jsonObject.has("answer") || !jsonObject.has("expiryTime") ||
			!NonceUtil.verify(jsonObject.getString("nonce"))) {

			throw new IllegalArgumentException("Token: " + captcha.getToken());
		}

		long expiryTime = jsonObject.getLong("expiryTime");

		if (expiryTime < System.currentTimeMillis()) {
			throw new CaptchaTextException("Captcha is expired");
		}

		if (!StringUtil.equalsIgnoreCase(
				jsonObject.getString("answer"), captcha.getAnswer())) {

			throw new CaptchaTextException("Answer is invalid");
		}
	}

	private void _checkCaptchaConfiguration() throws Exception {
		CaptchaConfiguration captchaConfiguration =
			_configurationProvider.getCompanyConfiguration(
				CaptchaConfiguration.class, contextCompany.getCompanyId());

		if (!StringUtil.equalsIgnoreCase(
				captchaConfiguration.captchaEngine(),
				SimpleCaptchaImpl.class.getName())) {

			throw new ForbiddenException(
				"Captcha engine is not configured to use SimpleCaptcha");
		}
	}

	private String _decryptToken(Key key, String token)
		throws GeneralSecurityException {

		byte[] encryptedBytes = Base64.decode(token);

		byte[] cipherBytes = Arrays.copyOfRange(
			encryptedBytes, _GCM_INITIALIZATION_VECTOR_LENGTH,
			encryptedBytes.length);

		byte[] initializationVector = Arrays.copyOfRange(
			encryptedBytes, 0, _GCM_INITIALIZATION_VECTOR_LENGTH);

		Cipher cipher = Cipher.getInstance(_AES_GCM_NOPADDING);

		cipher.init(
			Cipher.DECRYPT_MODE, key,
			new GCMParameterSpec(_GCM_TAG_LENGTH_BITS, initializationVector));

		return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
	}

	private String _encryptToken(Key key, String plainText)
		throws GeneralSecurityException {

		byte[] initializationVector =
			new byte[_GCM_INITIALIZATION_VECTOR_LENGTH];

		for (int i = 0; i < initializationVector.length; i++) {
			initializationVector[i] = SecureRandomUtil.nextByte();
		}

		Cipher cipher = Cipher.getInstance(_AES_GCM_NOPADDING);

		cipher.init(
			Cipher.ENCRYPT_MODE, key,
			new GCMParameterSpec(_GCM_TAG_LENGTH_BITS, initializationVector));

		byte[] cipherBytes = cipher.doFinal(
			plainText.getBytes(StandardCharsets.UTF_8));

		byte[] encryptedBytes =
			new byte[initializationVector.length + cipherBytes.length];

		System.arraycopy(
			initializationVector, 0, encryptedBytes, 0,
			initializationVector.length);
		System.arraycopy(
			cipherBytes, 0, encryptedBytes, initializationVector.length,
			cipherBytes.length);

		return Base64.encode(encryptedBytes);
	}

	private static final String _AES_GCM_NOPADDING = "AES/GCM/NoPadding";

	private static final int _GCM_INITIALIZATION_VECTOR_LENGTH = 12;

	private static final int _GCM_TAG_LENGTH_BITS = 128;

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private JSONFactory _jsonFactory;

}