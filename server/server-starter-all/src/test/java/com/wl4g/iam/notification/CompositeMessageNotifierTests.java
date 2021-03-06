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
package com.wl4g.iam.notification;

import com.wl4g.StandaloneIam;
import com.wl4g.infra.core.framework.operator.GenericOperatorAdapter;
import com.wl4g.infra.support.notification.GenericNotifyMessage;
import com.wl4g.infra.support.notification.MessageNotifier;
import com.wl4g.infra.support.notification.MessageNotifier.NotifierKind;

import static com.wl4g.infra.support.constant.SupportInfraConstant.CONF_PREFIX_INFRA_SUPPORT;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = StandaloneIam.class, properties = { CONF_PREFIX_INFRA_SUPPORT + ".vms.enable=true",
        CONF_PREFIX_INFRA_SUPPORT + ".vms.aliyun.regionId=cn-hangzhou",
        CONF_PREFIX_INFRA_SUPPORT + ".vms.aliyun.accessKeyId=LTAI4Fk9pjU7ezN2yVeiffYm",
        // Sensitive config, oneself setup
        CONF_PREFIX_INFRA_SUPPORT + ".vms.aliyun.accessKeySecret=${aliyun_vms_secret}",
        CONF_PREFIX_INFRA_SUPPORT + ".vms.aliyun.calledShowNumber=055162153866",
        CONF_PREFIX_INFRA_SUPPORT + ".vms.aliyun.templates.tts1=TTS_184820765",
        CONF_PREFIX_INFRA_SUPPORT + ".vms.aliyun.templates.tts2=TTS_184825642" })
@FixMethodOrder(MethodSorters.JVM)
public class CompositeMessageNotifierTests {

    @Autowired
    private GenericOperatorAdapter<NotifierKind, MessageNotifier> notifierAdapter;

    /**
     * 
     * [TTS_184820765]
     * 
     * <pre>
     * ????????????????????????????????????????????????${code}??????????????????????????????
     * </pre>
     */
    // @Test
    public void aliyunVmsCaptchaTest1() {
        System.out.println("Send starting...");
        GenericNotifyMessage msg = new GenericNotifyMessage("18007448807", "tts1");
        // Add placeholder parameters for a specific template
        msg.addParameter("code", "12345");
        notifierAdapter.forOperator(NotifierKind.AliyunVms).send(msg);
        System.out.println("Send end.");
    }

    /**
     * [TTS_184825642]
     * 
     * <pre>
     * ????????????????????????${product}????????????????????????????????????????????????${source}??????????????????${state}??????????????????${level}??????????????????${msg}????????????????????????????????????????????????????????????
     * </pre>
     */
    @Test
    public void aliyunVmsNotificationTest2() {
        System.out.println("Send starting...");
        GenericNotifyMessage msg = new GenericNotifyMessage("18007448807", "tts2");
        // Add placeholder parameters for a specific template
        msg.addParameter("product", "Devops Cloud");
        msg.addParameter("source", "????????????1");
        msg.addParameter("state", "?????????");
        msg.addParameter("level", "??????");
        msg.addParameter("msg", "?????????????????????");
        notifierAdapter.forOperator(NotifierKind.AliyunVms).send(msg);
        System.out.println("Send end.");
    }

}