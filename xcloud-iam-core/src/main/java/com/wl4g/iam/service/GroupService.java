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

import com.wl4g.components.core.bean.iam.Group;
import com.wl4g.components.core.bean.iam.User;

/**
 * @author vjay
 * @date 2019-10-29 16:19:00
 */
public interface GroupService {

	List<Group> getGroupsTree();

	void save(Group group);

	void del(Long id);

	Group detail(Long id);

	Set<Group> getGroupsSet(User user);

	Group getParent(List<Group> groups, Long parentId);

}