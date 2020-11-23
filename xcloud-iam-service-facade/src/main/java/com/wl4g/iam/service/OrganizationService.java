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
package com.wl4g.iam.service;

import java.util.List;
import java.util.Set;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.wl4g.iam.common.bean.Organization;
import com.wl4g.iam.common.bean.User;

/**
 * {@link OrganizationService}
 * 
 * @author Wangl.sir &lt;wanglsir@gmail.com, 983708408@qq.com&gt;
 * @author vjay
 * @date 2019-10-29
 * @sine v1.0
 * @see
 */
@FeignClient("organizationService")
public interface OrganizationService {

	@PostMapping("/save")
	void save(Organization org);

	@DeleteMapping("/del")
	void del(Long id);

	@GetMapping("/detail")
	Organization detail(Long id);

	@GetMapping("/getLoginOrganizationTree")
	List<Organization> getLoginOrganizationTree();

	@GetMapping("/getGroupsSet")
	Set<Organization> getUserOrganizations(User user);

	@GetMapping("/fillChildrenIds")
	void fillChildrenIds(Long parentId, Set<Long> set);

}