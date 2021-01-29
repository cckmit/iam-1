/*
 * Copyright (C) 2017 ~ 2025 the original author or authors.
 * <Wanglsir@gmail.com, 983708408@qq.com> Technology CO.LTD.
 * All rights reserved.
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
 *
 * Reference to website: http://wl4g.com
 */
package com.wl4g.iam.common.utils;

import com.wl4g.iam.common.subject.IamPrincipal;
import com.wl4g.iam.common.subject.SimpleIamPrincipal;

import static com.wl4g.iam.common.constant.RpcContextIAMConstants.*;

import com.wl4g.component.common.bridge.IamSecurityHolderBridgeUtils;
import com.wl4g.component.common.bridge.RpcContextHolderBridgeUtils;

/**
 * {@link RpcContextSecurityUtils}
 *
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @version v1.0 2021-01-05
 * @sine v1.0
 * @see {@link com.wl4g.iam.web.interceptor.IamContextAutoConfiguration}
 */
public abstract class RpcContextSecurityUtils extends RpcContextHolderBridgeUtils {

	public static IamPrincipal currentIamPrincipal() {
		if (hasRpcContextHolderClass()) { // Distributed-mode?
			return (IamPrincipal) invokeGetRef(CURRENT_IAM_PRINCIPAL, SimpleIamPrincipal.class);
		}
		// Standalone mode
		return (IamPrincipal) IamSecurityHolderBridgeUtils.invokeGetPrincipalInfo();
	}

	public static String currentIamPrincipalId() {
		if (hasRpcContextHolderClass()) { // Distributed-mode?
			return (String) invokeGet(CURRENT_IAM_PRINCIPAL_ID, String.class);
		}
		// Standalone mode
		return currentIamPrincipal().getPrincipalId();
	}

	public static String currentIamPrincipalName() {
		if (hasRpcContextHolderClass()) { // Distributed-mode?
			return (String) invokeGet(CURRENT_IAM_PRINCIPAL_USER, String.class);
		}
		// Standalone mode
		return currentIamPrincipal().getPrincipal();
	}

}
