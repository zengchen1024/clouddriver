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
 *  See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.huaweicloud.deploy.ops

class AsyncWait {

  static Boolean asyncWait(long timeout, Closure<AsyncWaitStatus> refresh) {
    // delay 3s to query status
    sleep(3000)

    Boolean isLastTime = false
    Integer unkownTimes = 0
    Integer wait = 0
    long end = System.currentTimeSeconds() + timeout

    while(!isLastTime) {
      if ((timeout != -1) && (System.currentTimeSeconds() > end)) {
        isLastTime = true
      }

      switch(refresh.call()) {
        case AsyncWaitStatus.SUCCESS:
          return true

        case AsyncWaitStatus.FAILED:
          return false

        case AsyncWaitStatus.UNKNOWN:
          unkownTimes += 1
          if (unkownTimes > 10) {
            return false
          }
      }

      if (!isLastTime) {
        wait *= 2
        if (wait < 1) {
          wait = 1
        } else if (wait > 10) {
          wait = 10
        }

        sleep(wait * 1000)
      }
    }

    return false
  }

  static enum AsyncWaitStatus {
    SUCCESS,
    FAILED,
    PENDING,
    UNKNOWN
  }
}
