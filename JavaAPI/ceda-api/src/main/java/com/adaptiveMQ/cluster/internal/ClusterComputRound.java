/*
 *
 *  * Copyright 2003-2022 Beijing XinRong Meridian Limited.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * https://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.adaptiveMQ.cluster.internal;

import com.adaptiveMQ.cluster.ServiceInfo;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.List;

public class ClusterComputRound implements IClusterComput
{
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterComputRound.class);

    public ClusterComputRound()
    {
    }

    public ServiceInfo getComputService(List<ServiceInfo> inputList)
    {
        // 防止List为空时数组越界-先启动Client后注册Server时发生
        if (inputList.size() == 0) {
            return null;
        }

        ServiceInfo[] a = new ServiceInfo[inputList.size()];
        ServiceInfo[] a1 = inputList.toArray(a);
        ServiceInfo rsInfo = null;
        //int count=0;
        boolean bdeal = false;
        //int nStart=0;
        int i = 0;

        try {
            for (i = 0; i < a1.length; i++) {
                if (a1[i].bActive || a1[i].getClientNum() == 0) {
                    //count=a1[i].m_nComputCount;
                    rsInfo = a1[i];
                    bdeal = true;
                    a1[i].increaseClientNum();
                    break;
                }
            }
            if (!bdeal) {
                i = 0;
                rsInfo = a1[i];
                bdeal = true;
                a1[i].increaseClientNum();
                //m_clusterClient.setServiceInfor(a1[i]);
                //a1[i].m_nOnlineCount++;
            }
        }
        catch (Exception e) {
            logger.warn(e);
        }

        if (bdeal) {
            setActive(a1);
        }
        return rsInfo;
    }

    private void setActive(ServiceInfo[] sList)
    {
        boolean bfirst = true;
        double dload = 0;
        double dtmpLoad;
        int pos = 0;
        for (int i = 0; i < sList.length; i++) {
            sList[i].bActive = false;
            dtmpLoad = sList[i].getClientNum() / sList[i].getLBFactor();
            if (bfirst) {
                dload = dtmpLoad;
                pos = i;
                bfirst = false;
            }
            else {
                if (dtmpLoad < dload) {
                    dload = dtmpLoad;
                    pos = i;
                }
            }
        }

        sList[pos].bActive = true;
    }
}
