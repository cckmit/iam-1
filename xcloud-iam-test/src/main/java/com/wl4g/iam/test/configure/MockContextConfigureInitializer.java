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
package com.wl4g.iam.test.configure;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static com.wl4g.components.common.collection.Collections2.safeMap;
import static com.wl4g.components.core.utils.AopUtils2.*;
import static com.wl4g.components.common.lang.Assert2.isTrue;
import static com.wl4g.components.common.lang.TypeConverts.*;
import static com.wl4g.components.common.log.SmartLoggerFactory.getLogger;
import static com.wl4g.components.common.serialize.JacksonUtils.toJSONString;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import com.wl4g.components.common.log.SmartLogger;
import com.wl4g.iam.client.annotation.EnableIamClient;
import com.wl4g.iam.client.config.IamClientProperties;
import com.wl4g.iam.common.config.ReplayProperties;
import com.wl4g.iam.common.config.XsrfProperties;
import com.wl4g.iam.common.subject.IamPrincipalInfo.OrganizationInfo;
import com.wl4g.iam.common.subject.IamPrincipalInfo.PrincipalOrganization;
import com.wl4g.iam.test.annotation.EnableIamMockAutoConfiguration;
import com.wl4g.iam.test.annotation.EnableIamMockAutoConfiguration.MockOrganization;

/**
 * {@link MockContextConfigureInitializer}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version 2020-08-10
 * @sine v1.0.0
 * @see
 */
public class MockContextConfigureInitializer implements InitializingBean {

	protected final SmartLogger log = getLogger(getClass());

	/** {@link ApplicationContext} */
	@Autowired
	protected ApplicationContext actx;

	/** {@link IamClientProperties} */
	@Autowired(required = false)
	protected IamClientProperties coreConfig;

	/** {@link ReplayProperties} */
	@Autowired(required = false)
	protected ReplayProperties replayConfig;

	/** {@link XsrfProperties} */
	@Autowired(required = false)
	protected XsrfProperties xsrfConfig;

	/** Mock config of {@link MockUserConfigWrapper} */
	protected MockUserConfigWrapper wrapper;

	public MockUserConfigWrapper getMockConfigWrapper() {
		return wrapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		applyDefaultMockContextProperties();
		parseMockConfiguration();
	}

	/**
	 * Parse mock configuration
	 */
	private void parseMockConfiguration() {
		Map<String, Object> beans = safeMap(actx.getBeansWithAnnotation(EnableIamMockAutoConfiguration.class));
		if (!(nonNull(beans) && beans.size() == 1)) {
			throw new Error(
					format("Shouldn't be here. please check config: @%s", EnableIamMockAutoConfiguration.class.getSimpleName()));
		}

		Object bootstrapBean = beans.entrySet().iterator().next().getValue();
		Class<?> bootClass = bootstrapBean.getClass();
		if (isAopProxy(bootstrapBean)) {
			bootClass = getTargetClass(bootstrapBean);
		}
		EnableIamMockAutoConfiguration anno = findAnnotation(bootClass, EnableIamMockAutoConfiguration.class);

		// Organizations
		PrincipalOrganization mockOrgan = new PrincipalOrganization();
		if (nonNull(anno.organizations())) {
			for (MockOrganization org : anno.organizations()) {
				String type = resolveMixValueIfNecessary(org.type());
				isTrue(isNumeric(type), "type: '%s' must be of numeric type", type);
				String organCode = resolveMixValueIfNecessary(org.organCode());
				String parent = resolveMixValueIfNecessary(org.parent());
				String name = resolveMixValueIfNecessary(org.name());
				String areaId = resolveMixValueIfNecessary(org.areaId());
				isTrue(isNumeric(areaId), "areaId: '%s' must be of numeric type", areaId);
				mockOrgan.getOrganizations()
						.add(new OrganizationInfo(organCode, parent, parseIntOrNull(type), name, parseIntOrNull(areaId)));
			}
		}

		// PrincipalId/principal
		String principalId = resolveMixValueIfNecessary(anno.principalId());
		String principal = resolveMixValueIfNecessary(anno.principal());
		// Roles/permissions.
		String roles = resolveMixValueIfNecessary(anno.roles());
		String permissions = resolveMixValueIfNecessary(anno.permissions());

		this.wrapper = new MockUserConfigWrapper(principalId, principal, roles, permissions, mockOrgan);
		log.info("Resolved mock configuration: {}", toJSONString(wrapper));

	}

	/**
	 * Apply sets mock context service properties.
	 */
	private void applyDefaultMockContextProperties() {
		// IAM server configuration
		if (nonNull(coreConfig)) {
			String thatPort = actx.getEnvironment().getRequiredProperty("server.port");
			String thatCtxPath = actx.getEnvironment().getRequiredProperty("server.servlet.context-path");
			coreConfig.setServerUri("http://localhost:".concat(thatPort).concat("/").concat(thatCtxPath));
		}

		// IAM replay configuration
		if (nonNull(replayConfig)) {
			replayConfig.getExcludeValidUriPatterns().add(URI_PATTERN_ALL);
		}

		// IAM xsrf configuration
		if (nonNull(xsrfConfig)) {
			xsrfConfig.getExcludeValidUriPatterns().add(URI_PATTERN_ALL);
		}
	}

	/**
	 * Resolve config value with mix. (if necessary) </br>
	 * </br>
	 * 
	 * for example:
	 * 
	 * <pre>
	 * <b>application.yml:</b>
	 * 	iam.mock.permissions: user_list,role_list,order_list,order_edit
	 * 
	 * <b>Bootstrap Class:</b>
	 * 
	 *	&#64;{@link EnableIamMockAutoConfiguration}(permissions="home,ALL,${iam.mock.permissions}", ...)
	 *	&#64;{@link EnableIamClient}
	 *	&#64;{@link SpringBootApplication}
	 *	public class IamExampleTests {
	 * 		public static void main(String[] args) throws Exception {
	 *			SpringApplication.run(Base.class, args);
	 * 		}
	 *	}
	 * 
	 * <b>Resolved permissions:</b>
	 * 	"home,ALL,user_list,role_list,order_list,order_edit"
	 * </pre>
	 * 
	 * @param text
	 * @return
	 */
	private String resolveMixValueIfNecessary(String text) {
		int sidx = text.indexOf("${");
		if (sidx >= 0) {
			int eidx = text.indexOf("}");
			isTrue(eidx > 0, "Illegal placeholder key '%s'", text);
			String prefixValue = text.substring(0, sidx);
			String stuffixValue = text.substring(eidx + 1);
			String placeholder = text.substring(sidx + 2, eidx);
			String resolvedValue = actx.getEnvironment().resolvePlaceholders("${".concat(placeholder).concat("}"));
			return prefixValue.concat(resolvedValue).concat(stuffixValue);
		}
		return text;
	}

	/**
	 * {@link MockUserConfigWrapper}
	 *
	 * @since
	 */
	public static class MockUserConfigWrapper {

		/** Mock principalId */
		final private String principalId;

		/** Mock principal */
		final private String principal;

		/** Mock roles */
		final private String roles;

		/** Mock permissions */
		final private String permissions;

		/** {@link PrincipalOrganization} */
		final private PrincipalOrganization organization;

		public MockUserConfigWrapper(String principalId, String principal, String roles, String permissions,
				PrincipalOrganization organization) {
			super();
			this.principalId = principalId;
			this.principal = principal;
			this.roles = roles;
			this.permissions = permissions;
			this.organization = organization;
		}

		public String getPrincipalId() {
			return principalId;
		}

		public String getPrincipal() {
			return principal;
		}

		public String getRoles() {
			return roles;
		}

		public String getPermissions() {
			return permissions;
		}

		public PrincipalOrganization getOrganization() {
			return organization;
		}

	}

	/** URI mapping any */
	public static final String URI_PATTERN_ALL = "/**";

}
