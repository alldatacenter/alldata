/*
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
 */

package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.TPROXY_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.BIND_PASSWORD;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.TPROXY_ALLOWED_GROUPS;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.TPROXY_AUTHENTICATION_ENABLED;
import static org.apache.ambari.server.configuration.ConfigurationPropertyType.PASSWORD;
import static org.apache.ambari.server.configuration.ConfigurationPropertyType.UNKNOWN;

import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.junit.Assert;
import org.junit.Test;

public class AmbariServerConfigurationUtilsTest {

  @Test
  public void testGetConfigurationKey() {
    Assert.assertSame(TPROXY_AUTHENTICATION_ENABLED,
        AmbariServerConfigurationUtils.getConfigurationKey(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory(), TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertSame(TPROXY_AUTHENTICATION_ENABLED,
        AmbariServerConfigurationUtils.getConfigurationKey(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory().getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key()));

    // Test Regex Key
    Assert.assertSame(TPROXY_ALLOWED_GROUPS,
        AmbariServerConfigurationUtils.getConfigurationKey(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), TPROXY_ALLOWED_GROUPS.key()));
    Assert.assertSame(TPROXY_ALLOWED_GROUPS,
        AmbariServerConfigurationUtils.getConfigurationKey(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "ambari.tproxy.proxyuser.knox.groups"));
    Assert.assertSame(TPROXY_ALLOWED_GROUPS,
        AmbariServerConfigurationUtils.getConfigurationKey(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "ambari.tproxy.proxyuser.not.knox.groups"));
    Assert.assertNull(AmbariServerConfigurationUtils.getConfigurationKey(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "invalid.tproxy.proxyuser.not.knox.groups"));

    Assert.assertNull(AmbariServerConfigurationUtils.getConfigurationKey((AmbariServerConfigurationCategory) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertNull(AmbariServerConfigurationUtils.getConfigurationKey((String) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertNull(AmbariServerConfigurationUtils.getConfigurationKey("invalid", TPROXY_AUTHENTICATION_ENABLED.key()));

    Assert.assertNull(AmbariServerConfigurationUtils.getConfigurationKey(TPROXY_CONFIGURATION.getCategoryName(), null));
    Assert.assertNull(AmbariServerConfigurationUtils.getConfigurationKey(TPROXY_CONFIGURATION.getCategoryName(), "invalid"));
  }

  @Test
  public void testGetConfigurationPropertyType() {
    Assert.assertSame(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType(),
        AmbariServerConfigurationUtils.getConfigurationPropertyType(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory(), TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertSame(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType(),
        AmbariServerConfigurationUtils.getConfigurationPropertyType(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory().getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key()));

    // Test Regex Key
    Assert.assertSame(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType(),
        AmbariServerConfigurationUtils.getConfigurationPropertyType(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), TPROXY_ALLOWED_GROUPS.key()));
    Assert.assertSame(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType(),
        AmbariServerConfigurationUtils.getConfigurationPropertyType(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "ambari.tproxy.proxyuser.knox.groups"));
    Assert.assertSame(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType(),
        AmbariServerConfigurationUtils.getConfigurationPropertyType(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "ambari.tproxy.proxyuser.not.knox.groups"));
    Assert.assertSame(UNKNOWN, AmbariServerConfigurationUtils.getConfigurationPropertyType(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "invalid.tproxy.proxyuser.not.knox.groups"));

    Assert.assertSame(UNKNOWN, AmbariServerConfigurationUtils.getConfigurationPropertyType((AmbariServerConfigurationCategory) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertSame(UNKNOWN, AmbariServerConfigurationUtils.getConfigurationPropertyType((String) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertSame(UNKNOWN, AmbariServerConfigurationUtils.getConfigurationPropertyType("invalid", TPROXY_AUTHENTICATION_ENABLED.key()));

    Assert.assertSame(UNKNOWN, AmbariServerConfigurationUtils.getConfigurationPropertyType(TPROXY_CONFIGURATION.getCategoryName(), null));
    Assert.assertSame(UNKNOWN, AmbariServerConfigurationUtils.getConfigurationPropertyType(TPROXY_CONFIGURATION.getCategoryName(), "invalid"));
  }

  @Test
  public void testGetConfigurationPropertyTypeName() {
    Assert.assertEquals(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType().name(),
        AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory(), TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertEquals(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType().name(),
        AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory().getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key()));

    // Test Regex Key
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType().name(),
        AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), TPROXY_ALLOWED_GROUPS.key()));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType().name(),
        AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "ambari.tproxy.proxyuser.knox.groups"));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType().name(),
        AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "ambari.tproxy.proxyuser.not.knox.groups"));
    Assert.assertEquals(UNKNOWN.name(), AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "invalid.tproxy.proxyuser.not.knox.groups"));

    Assert.assertEquals(UNKNOWN.name(), AmbariServerConfigurationUtils.getConfigurationPropertyTypeName((AmbariServerConfigurationCategory) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertEquals(UNKNOWN.name(), AmbariServerConfigurationUtils.getConfigurationPropertyTypeName((String) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertEquals(UNKNOWN.name(), AmbariServerConfigurationUtils.getConfigurationPropertyTypeName("invalid", TPROXY_AUTHENTICATION_ENABLED.key()));

    Assert.assertEquals(UNKNOWN.name(), AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_CONFIGURATION.getCategoryName(), null));
    Assert.assertEquals(UNKNOWN.name(), AmbariServerConfigurationUtils.getConfigurationPropertyTypeName(TPROXY_CONFIGURATION.getCategoryName(), "invalid"));
  }

  @Test
  public void isPassword() {
    Assert.assertEquals(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType() == PASSWORD,
        AmbariServerConfigurationUtils.isPassword(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory(), TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertEquals(TPROXY_AUTHENTICATION_ENABLED.getConfigurationPropertyType() == PASSWORD,
        AmbariServerConfigurationUtils.isPassword(TPROXY_AUTHENTICATION_ENABLED.getConfigurationCategory().getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key()));

    // Test Regex Key
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType() == PASSWORD,
        AmbariServerConfigurationUtils.isPassword(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), TPROXY_ALLOWED_GROUPS.key()));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType() == PASSWORD,
        AmbariServerConfigurationUtils.isPassword(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "ambari.tproxy.proxyuser.knox.groups"));
    Assert.assertEquals(TPROXY_ALLOWED_GROUPS.getConfigurationPropertyType() == PASSWORD,
        AmbariServerConfigurationUtils.isPassword(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "ambari.tproxy.proxyuser.not.knox.groups"));

    Assert.assertFalse(AmbariServerConfigurationUtils.isPassword(TPROXY_ALLOWED_GROUPS.getConfigurationCategory().getCategoryName(), "invalid.tproxy.proxyuser.not.knox.groups"));

    Assert.assertFalse(AmbariServerConfigurationUtils.isPassword((AmbariServerConfigurationCategory) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertFalse(AmbariServerConfigurationUtils.isPassword((String) null, TPROXY_AUTHENTICATION_ENABLED.key()));
    Assert.assertFalse(AmbariServerConfigurationUtils.isPassword("invalid", TPROXY_AUTHENTICATION_ENABLED.key()));

    Assert.assertFalse(AmbariServerConfigurationUtils.isPassword(TPROXY_CONFIGURATION.getCategoryName(), null));
    Assert.assertFalse(AmbariServerConfigurationUtils.isPassword(TPROXY_CONFIGURATION.getCategoryName(), "invalid"));

    // This is known to be a password
    Assert.assertTrue(AmbariServerConfigurationUtils.isPassword(BIND_PASSWORD));
  }
}