/*
 * Copyright 2017 Huawei Technologies Co., Ltd
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
package io.servicecomb.config;

import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.Ordered;

import com.netflix.config.ConfigurationManager;

import io.servicecomb.config.archaius.sources.MicroserviceConfigLoader;
import mockit.Deencapsulation;

public class TestConfigurationSpringInitializer {
  @BeforeClass
  public static void classSetup() {
    ArchaiusUtils.resetConfig();
  }

  @AfterClass
  public static void classTeardown() {
    ArchaiusUtils.resetConfig();
  }

  @Test
  public void testAll() {
    ConfigurationSpringInitializer configurationSpringInitializer = new ConfigurationSpringInitializer();

    Assert.assertEquals(Ordered.LOWEST_PRECEDENCE / 2, configurationSpringInitializer.getOrder());
    Assert.assertEquals(true,
        Deencapsulation.getField(configurationSpringInitializer, "ignoreUnresolvablePlaceholders"));

    Object o = ConfigUtil.getProperty("zq");
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> listO = (List<Map<String, Object>>) o;
    Assert.assertEquals(3, listO.size());
    Assert.assertEquals(null, ConfigUtil.getProperty("notExist"));

    MicroserviceConfigLoader loader = ConfigUtil.getMicroserviceConfigLoader();
    Assert.assertNotNull(loader);

    Configuration instance = ConfigurationManager.getConfigInstance();
    ConfigUtil.installDynamicConfig();
    // must not reinstall
    Assert.assertEquals(instance, ConfigurationManager.getConfigInstance());
  }
}
