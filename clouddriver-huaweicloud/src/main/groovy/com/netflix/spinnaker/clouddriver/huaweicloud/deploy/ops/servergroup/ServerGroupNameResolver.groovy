/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops.servergroup

import com.huawei.openstack4j.model.scaling.ScalingGroup
import com.netflix.frigga.Names
import com.netflix.spinnaker.clouddriver.helpers.AbstractServerGroupNameResolver
import com.netflix.spinnaker.clouddriver.huaweicloud.client.HuaweiCloudClient

class ServerGroupNameResolver extends AbstractServerGroupNameResolver {
  final String phase
  final String region
  final HuaweiCloudClient client

  ServerGroupNameResolver(HuaweiCloudClient client, String region, String phase) {
    this.client = client
    this.region = region
    this.phase = phase
  }

  @Override
  List<AbstractServerGroupNameResolver.TakenSlot> getTakenSlots(String clusterName) {
    def groups = client.getScalingGroups(region)
    if (!groups) {
      return []
    }

    groups.findResults { ScalingGroup group ->
      def names = Names.parseName(group.groupName)

      (names.cluster == clusterName) ?
        new AbstractServerGroupNameResolver.TakenSlot(
          serverGroupName: group.groupName,
          sequence: names.sequence,
          createdTime: group.createTime
        ) : null
    }
  }
}
