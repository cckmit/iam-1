/*
 * Copyright 2017 ~ 2025 the original author or authors. <wanglsir@gmail.com, 983708408@qq.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wl4g.iam.handler;

import com.wl4g.component.common.web.rest.RespBase;
import com.wl4g.component.support.cache.jedis.ScanCursor;
import com.wl4g.iam.authc.LogoutAuthenticationToken;
import com.wl4g.iam.common.bean.ApplicationInfo;
import com.wl4g.iam.common.subject.IamPrincipal;
import com.wl4g.iam.common.subject.SimpleIamPrincipal;
import com.wl4g.iam.common.subject.IamPrincipal.Attributes;
import com.wl4g.iam.core.authc.IamAuthenticationTokenWrapper;
import com.wl4g.iam.core.authc.model.LoggedinResult;
import com.wl4g.iam.core.authc.model.LogoutResult;
import com.wl4g.iam.core.authc.model.SecondaryAuthcValidateResult;
import com.wl4g.iam.core.authc.model.SessionValidateModel;
import com.wl4g.iam.core.authc.model.TicketValidateRequest;
import com.wl4g.iam.core.authc.model.TicketValidateResult;
import com.wl4g.iam.core.cache.CacheKey;
import com.wl4g.iam.core.exception.IamException;
import com.wl4g.iam.core.exception.IllegalApplicationAccessException;
import com.wl4g.iam.core.exception.IllegalCallbackDomainException;
import com.wl4g.iam.core.exception.InvalidGrantTicketException;
import com.wl4g.iam.core.session.GrantCredentialsInfo;
import com.wl4g.iam.core.session.IamSession;
import com.wl4g.iam.core.session.GrantCredentialsInfo.GrantApp;
import com.wl4g.iam.core.session.IamSession.RelationAttrKey;
import com.wl4g.iam.core.session.mgt.IamSessionDAO;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.SessionException;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.wl4g.component.common.lang.Assert2.*;
import static com.wl4g.component.common.web.WebUtils2.getHttpRemoteAddr;
import static com.wl4g.component.common.web.WebUtils2.isEqualWithDomain;
import static com.wl4g.iam.common.constant.ServiceIAMConstants.*;
import static com.wl4g.iam.core.authc.model.SecondaryAuthcValidateResult.Status.ExpiredAuthorized;
import static com.wl4g.iam.core.utils.IamAuthenticatingUtils.*;
import static com.wl4g.iam.sns.handler.SecondaryAuthcSnsHandler.SECOND_AUTHC_CACHE;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.shiro.web.util.WebUtils.toHttp;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Default authentication handler implements
 *
 * @author Wangl.sir <983708408@qq.com>
 * @version v1.0
 * @date 2018年11月22日
 * @since
 */
public class CentralAuthenticatingHandler extends AbstractAuthenticatingHandler {

	/**
	 * IAM session DAO.
	 */
	@Autowired
	protected IamSessionDAO sessionDAO;

	@Override
	public void checkAuthenticateRedirectValidity(String appName, String redirectUrl) throws IllegalCallbackDomainException {
		// Check redirect URL(When source application is not empty)
		if (!isBlank(appName)) {
			if (isBlank(redirectUrl)) {
				throw new IllegalCallbackDomainException("Parameters redirectUrl and application cannot be null");
			}

			// Get application.
			ApplicationInfo app = configurer.getApplicationInfo(appName);
			if (Objects.isNull(app)) {
				throw new IllegalCallbackDomainException("Illegal redirect application URL parameters.");
			}
			state(!isAnyBlank(app.getAppName(), app.getExtranetBaseUri()),
					String.format("Invalid redirection domain configure, application[%s]", appName));
			log.debug("Check authentication requests application [{}]", app);

			// Check redirect URL are legitimate callback URI?(As long as there
			// is a match)
			String host = URI.create(redirectUrl).getHost();
			if (!(equalsAny(host, PERMISSIVE_HOSTS) || isEqualWithDomain(redirectUrl, app.getExtranetBaseUri())
					|| isEqualWithDomain(redirectUrl, app.getIntranetBaseUri()))) {
				throw new IllegalCallbackDomainException(format("Illegal redirectUrl [%s]", redirectUrl));
			}
		}
	}

	@Override
	public void assertApplicationAccessAuthorized(String principal, String appName) throws IllegalApplicationAccessException {
		hasText(principal, "'principal' must not be empty");
		hasText(appName, "'appName' must not be empty");
		if (!configurer.isApplicationAccessAuthorized(principal, appName)) {
			throw new IllegalApplicationAccessException(
					bundle.getMessage("GentralAuthenticationHandler.unaccessible", principal, appName));
		}
	}

	@Override
	public TicketValidateResult<IamPrincipal> validate(TicketValidateRequest param) {
		TicketValidateResult<IamPrincipal> assertion = new TicketValidateResult<>();
		String grantAppName = param.getApplication();
		hasTextOf(grantAppName, "grantAppName");

		// Get subject session of grantCredentials info.
		/*
		 * Synchronize with xx.xx.session.mgt.IamSessionManager#getSessionId
		 */
		Subject subject = getSubject();
		log.debug("Validating subject: {} by grantTicket: {}", subject, param.getTicket());

		// Assertion grantCredentials info.
		assertGrantingTicketValidity(subject, param);

		// Check access authorized from application.
		assertApplicationAccessAuthorized((String) subject.getPrincipal(), grantAppName);

		// Force clearance of last grant Ticket
		/*
		 * Synchronize with
		 * xx.xx.handler.impl.FastCasAuthenticationHandler#validate#loggedin
		 */
		cacheManager.getCache(CACHE_TICKET_S).remove(new CacheKey(param.getTicket()));
		log.debug("Clean older grantTicket: {}", param.getTicket());

		// --- Grant attributes setup. ---

		// Grant validated start date.
		long now = currentTimeMillis();
		assertion.setValidFromTime(now);

		/*
		 * x.client.realm.FastCasAuthorizingRealm#doGetAuthenticationInfo Grant
		 * term of validity(end date).
		 */
		assertion.setValidUntilTime(now + getSessionRemainingTime());

		// Renew grant credentials
		/**
		 * Synchronize with: </br>
		 * x.handler.impl.FastCasAuthenticationHandler#logout() </br>
		 * x.session.mgt.IamSessionManager#getSessionId
		 */
		String newGrantTicket = generateGrantTicket();

		// Principal info.
		/**
		 * {@link com.wl4g.devops.iam.client.realm.FastCasAuthorizingRealm#doAuthenticationInfo(AuthenticationToken)}
		 */
		assertion.setIamPrincipal(new SimpleIamPrincipal(getPrincipalInfo()).withStoredCredentials(newGrantTicket));
		log.info("New validated grantTicket: {}, sessionId: {}", newGrantTicket, getSessionId());

		// Principal info attributes.
		/**
		 * Grants roles and permissions attributes.
		 */
		Attributes attrs = assertion.getIamPrincipal().attributes();
		attrs.setSessionLang(getBindValue(KEY_LANG_NAME));
		attrs.setParentSessionId(valueOf(getSessionId()));

		// Sets re-generate childDataCipherKey(for grant app)
		String childDataCipherKey = null;
		if (config.getCipher().isEnableDataCipher()) {
			attrs.setDataCipher((childDataCipherKey = generateDataCipherKey()));
		}

		// Sets re-generate childAccessToken(for grant app)
		String childAccessTokenSignKey = null;
		if (config.getSession().isEnableAccessTokenValidity()) {
			String accessTokenSignKey = getBindValue(KEY_ACCESSTOKEN_SIGN_NAME);
			childAccessTokenSignKey = generateAccessTokenSignKey(param.getSessionId(), accessTokenSignKey);
			attrs.setAccessTokenSign(childAccessTokenSignKey);
		}

		// Sets authenticaing client host.
		IamAuthenticationTokenWrapper wrap = getBindValue(
				new RelationAttrKey(KEY_AUTHC_TOKEN, IamAuthenticationTokenWrapper.class));
		if (!isNull(wrap) && !isNull(wrap.getToken())) {
			attrs.setClientHost(wrap.getToken().getHost());
		}

		// Put grant credentials info.
		GrantApp grant = new GrantApp(newGrantTicket).setDataCipher(childDataCipherKey)
				.setAccessTokenSignKey(childAccessTokenSignKey);
		putGrantCredentials(getSession(false), grantAppName, grant);

		return assertion;
	}

	@Override
	public LoggedinResult loggedin(String grantAppname, Subject subject) {
		hasTextOf(grantAppname, "grantAppname");

		// Check authentication.
		if (nonNull(subject) && subject.isAuthenticated() && !isBlank((String) subject.getPrincipal())) {
			Session session = subject.getSession();

			// Generate granting ticket. Same: CAS/service-ticket
			String grantTicket = null;
			// If the ticket has been generated in the previous
			// moment.(currently?)
			GrantApp grant = getGrantCredentials(session).getGrantApp(grantAppname);
			if (!isNull(grant)) {
				grantTicket = grant.getGrantTicket();
			} else {
				// Init generate grantCredentials
				grantTicket = generateGrantTicket();
				log.info("New init grantTicket: {}, grantAppname: {}", grantTicket, grantAppname);
			}

			// Puts grantInfo session => applications
			putGrantCredentials(session, grantAppname, new GrantApp().setGrantTicket(grantTicket));

			return new LoggedinResult(grantTicket);
		}
		throw new AuthenticationException("Unauthenticated");
	}

	@Override
	public LogoutResult logout(boolean forced, String appName, HttpServletRequest request, HttpServletResponse response) {
		log.debug("Logout from: {}, forced: {}, sessionId: {}", appName, forced, getSessionId());
		Subject subject = getSubject();

		// From client signout
		coprocessor.preLogout(new LogoutAuthenticationToken(getPrincipal(false), getHttpRemoteAddr(request)), toHttp(request),
				toHttp(response));

		// Represents all logout mark.
		boolean logoutAllMark = true;

		// Gets session bind grantInfo
		GrantCredentialsInfo info = getGrantCredentials(subject.getSession());
		log.debug("Got grantInfo: {} with sessionId: {}", info, getSessionId(subject));

		if (!isNull(info) && info.hasEmpty()) {
			// Query applications by bind session names
			Set<String> appNames = info.getGrantApps().keySet();
			// Cleanup this(Solve the dead cycle).
			appNames.remove(config.getServiceName());

			List<ApplicationInfo> apps = configurer.findApplicationInfo(appNames.toArray(new String[] {}));
			if (!isEmpty(apps)) {
				// logout all
				logoutAllMark = handleLogoutSessionsAll(subject, info, apps);
			} else
				log.debug("Not found logout appInfo. appNames: {}", appNames);
		}

		if (forced || logoutAllMark) {
			// Logout all sessions.
			try {
				/**
				 * That's the subject Refer to
				 * {@link com.wl4g.devops.iam.session.mgt.IamServerSessionManager#getSessionId())
				 * try/catch added for #SHIRO-298:
				 */
				log.debug("Logouting... sessionId: {}", getSessionId(subject));
				subject.logout(); // After that, session is null
			} catch (SessionException e) {
				log.warn("Encountered session exception during logout. This can generally safely be ignored.", e);
			}
		}

		return isNotBlank(appName) ? new LogoutResult(appName) : new LogoutResult();
	}

	@Override
	public SecondaryAuthcValidateResult secondaryValidate(String secondAuthCode, String appName) {
		CacheKey ekey = new CacheKey(secondAuthCode, SecondaryAuthcValidateResult.class);
		try {
			/*
			 * Save authorized info to cache. See:
			 * xx.iam.sns.handler.SecondAuthcSnsHandler#afterCallbackSet()
			 */
			SecondaryAuthcValidateResult assertion = (SecondaryAuthcValidateResult) cacheManager.getIamCache(SECOND_AUTHC_CACHE)
					.get(ekey);
			// Check assertion expired
			if (assertion == null) {
				assertion = new SecondaryAuthcValidateResult(ExpiredAuthorized);
				assertion.setErrdesc("Authorization expires, please re-authorize.");
			}
			return assertion;
		} finally { // Release authentication code
			log.info("Remove release second authentication info. key[{}]", new String(ekey.getKey()));
			cacheManager.getIamCache(SECOND_AUTHC_CACHE).remove(ekey);
		}
	}

	@Override
	public SessionValidateModel sessionValidate(SessionValidateModel model) {
		hasTextOf(model.getApplication(), "grantAppname");

		ScanCursor<IamSession> cursor = sessionDAO.getAccessSessions(DEFAULT_SCAN_SIZE);
		while (cursor.hasNext()) {
			Session session = cursor.next();
			// GrantTicket of session.
			GrantCredentialsInfo info = getGrantCredentials(session);

			if (nonNull(info) && info.has(model.getApplication())) {
				String storedTicket = info.getGrantApps().get(model.getApplication()).getGrantTicket();
				// If exist grantTicket with application.
				if (!isBlank(storedTicket)) {
					model.getTickets().remove(storedTicket);
				}
			}
		}
		return model;
	}

	/**
	 * Assertion granting ticket validity </br>
	 *
	 * @param subject
	 * @param model
	 * @throws InvalidGrantTicketException
	 * @see {@link com.wl4g.iam.handler.CentralAuthenticatingHandler#loggedin}
	 */
	private void assertGrantingTicketValidity(Subject subject, TicketValidateRequest model) throws InvalidGrantTicketException {
		if (isBlank(model.getTicket())) {
			log.warn("Invalid grantTicket: {}, application: {}, sessionId: {}", model.getTicket(), model.getApplication(),
					getSessionId(subject));
			throw new InvalidGrantTicketException("Invalid granting ticket and is required");
		}

		// Get grant information
		GrantCredentialsInfo info = getGrantCredentials(subject.getSession());
		log.debug("Got grantTicketInfo: {}, sessionId:{}", info, getSessionId());

		// No grant ticket created or expired?
		if (isNull(info) || !info.has(model.getApplication())) {
			throw new InvalidGrantTicketException("Invalid granting ticket application");
		}

		// Validate grantTicket and storedTicket?
		String storedTicket = info.getGrantApp(model.getApplication()).getGrantTicket();
		if (!(model.getTicket().equals(storedTicket) && subject.isAuthenticated() && nonNull(subject.getPrincipal()))) {
			log.warn("Invalid grantTicket: {}, appName: {}, sessionId: {}", model.getTicket(), model.getApplication(),
					subject.getSession().getId());
			throw new InvalidGrantTicketException("Invalid granting ticket");
		}

	}

	/**
	 * Handle logout all
	 *
	 * @param subject
	 * @param info
	 * @param apps
	 * @return
	 */
	private boolean handleLogoutSessionsAll(Subject subject, GrantCredentialsInfo info, List<ApplicationInfo> apps) {
		boolean logoutAllMark = true; // Represents all logout mark.

		/*
		 * Notification all logged-in applications to logout
		 */
		for (ApplicationInfo app : apps) {
			hasText(app.getIntranetBaseUri(), "Application[%s] 'internalBaseUri' is required", app.getAppName());

			// Gets grantTicket by appName
			String grantTicket = info.getGrantApps().get(app.getAppName()).getGrantTicket();
			// Build logout URL
			String url = new StringBuffer(app.getIntranetBaseUri()).append(URI_C_BASE).append("/").append(URI_C_LOGOUT)
					.append("?").append(config.getParam().getGrantTicket()).append("=").append(grantTicket).toString();

			// Post to remote client logout
			try {
				RespBase<LogoutResult> resp = restTemplate
						.exchange(url, POST, null, new ParameterizedTypeReference<RespBase<LogoutResult>>() {
						}).getBody();
				if (RespBase.isSuccess(resp))
					log.info("Finished logout of principal: {}, appName: {}, url:{}", subject.getPrincipal(), app.getAppName(),
							url);
				else
					throw new IamException(resp != null ? resp.getMessage() : "No response");
			} catch (Exception e) {
				logoutAllMark = false;
				log.error(format("Remote client logout failure. principal: %s, appName: %s, url: %s", subject.getPrincipal(),
						app.getAppName(), url), e);
			}
		}

		return logoutAllMark;
	}

	/**
	 * Generate grantCredentials ticket.
	 *
	 * @return
	 */
	private String generateGrantTicket() {
		return "st" + randomAlphabetic(48, 64);
	}

	/**
	 * Puts grantCredentials to session. </br>
	 *
	 * @param session
	 *            Session
	 * @param grantAppname
	 *            granting application name
	 * @param grant
	 *            grant ticket
	 */
	private void putGrantCredentials(Session session, String grantAppname, GrantApp grant) {
		notNullOf(session, "session");
		hasTextOf(grantAppname, "grantAppname");
		notNullOf(grant, "grant");

		/**
		 * @See {@link CentralAuthenticationHandler#validate()}
		 */
		GrantCredentialsInfo info = getGrantCredentials(session);
		if (info.has(grantAppname)) {
			log.debug("Sets grantTicket of sessionId: {} grantAppname: {}", session.getId(), grantAppname);
		}
		// Updating grantTicket.
		session.setAttribute(new RelationAttrKey(KEY_GRANTCREDENTIALS), info.putGrant(grantAppname, grant));
		log.debug("Updated granting credentials to session. {}", info);

		// Sets grantTicket => sessionId.
		/**
		 * @see {@link com.wl4g.devops.iam.client.validation.FastCasTicketIamValidator#validate()}
		 * @see {@link com.wl4g.devops.iam.common.session.mgt.AbstractIamSessionManager#getSessionId()}
		 */
		long expireTime = getSessionRemainingTime(session); // Expiration time
		cacheManager.getIamCache(CACHE_TICKET_S).put(new CacheKey(grant.getGrantTicket(), expireTime), valueOf(session.getId()));
		log.debug("Sets grantTicket: '{}' of seesionId: '{}', expireTime: '{}'", grant, getSessionId(session), expireTime);
	}

	/**
	 * Gets bind session granting credentials.
	 *
	 * @param session
	 * @return
	 */
	public static GrantCredentialsInfo getGrantCredentials(Session session) {
		GrantCredentialsInfo info = (GrantCredentialsInfo) session
				.getAttribute(new RelationAttrKey(KEY_GRANTCREDENTIALS, GrantCredentialsInfo.class));
		return isNull(info) ? new GrantCredentialsInfo() : info;
	}

	/**
	 * Scan iteration batch size.
	 */
	final public static int DEFAULT_SCAN_SIZE = 100;

	/**
	 * Default gets {@link GrantCredentialsInfo} lock expirtion(ms)
	 */
	final public static long DEFAULT_LOCK_CREDENTIALS_EXPIRE = 5000L;

	/**
	 * IAM server grantTicket info of application.
	 */
	final public static String KEY_GRANTCREDENTIALS = "grantCredentials";

	/**
	 * Permissived whitelist hosts.
	 */
	final public static String[] PERMISSIVE_HOSTS = new String[] { "localhost", "127.0.0.1", "0:0:0:0:0:0:0:1" };

}