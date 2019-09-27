/*
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.netflix.spinnaker.clouddriver.model.SecurityGroup
import com.netflix.spinnaker.clouddriver.model.SecurityGroupSummary
import com.netflix.spinnaker.clouddriver.model.securitygroups.Rule
import com.netflix.spinnaker.moniker.Moniker
import groovy.transform.Immutable

@Immutable
@JsonInclude(JsonInclude.Include.NON_EMPTY)
class HuaweiCloudSecurityGroup implements SecurityGroup {
  final String type
  final String cloudProvider

  final String id
  final String name
  final String region
  final String application
  final String accountName
  final String vpcId
  final Set<Rule> inboundRules
  final Set<Rule> outboundRules

  void setMoniker(Moniker _ignored) {}

  @Override
  SecurityGroupSummary getSummary() {
    new HuaweiCloudSecurityGroupSummary(name: this.name, id: this.id)
  }
}