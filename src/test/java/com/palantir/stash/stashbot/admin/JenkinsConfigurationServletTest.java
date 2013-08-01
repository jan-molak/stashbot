//   Copyright 2013 Palantir Technologies
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
package com.palantir.stash.stashbot.admin;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.soy.renderer.SoyTemplateRenderer;
import com.atlassian.stash.repository.Repository;
import com.google.common.collect.ImmutableList;
import com.palantir.stash.stashbot.config.ConfigurationPersistenceManager;
import com.palantir.stash.stashbot.config.JenkinsServerConfiguration;
import com.palantir.stash.stashbot.managers.PluginUserManager;

public class JenkinsConfigurationServletTest {

    @Mock
    private ConfigurationPersistenceManager cpm;
    @Mock
    private WebResourceManager webResourceManager;
    @Mock
    private SoyTemplateRenderer soyTemplateRenderer;

    @Mock
    private HttpServletRequest req;
    @Mock
    private HttpServletResponse res;
    @Mock
    private PrintWriter writer;
    @Mock
    private JenkinsServerConfiguration jsc;
    @Mock
    private JenkinsServerConfiguration jsc2;
    @Mock
    private PluginUserManager pum;

    private JenkinsConfigurationServlet jcs;

    private static final String JN = "default";
    private static final String JURL = "JenkinsURL";
    private static final String JU = "JenkisnUsername";
    private static final String JP = "JenkinsPassword";
    private static final String SU = "StashUsername";
    private static final String SP = "StashPassword";

    private static final String REQUEST_URI = "http://someuri.example.com/blah";

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);

        Mockito.when(res.getWriter()).thenReturn(writer);
        Mockito.when(req.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URI));
        Mockito.when(req.getPathInfo()).thenReturn("");

        Mockito.when(cpm.getDefaultJenkinsServerConfiguration()).thenReturn(jsc);
        Mockito.when(cpm.getJenkinsServerConfiguration(JN)).thenReturn(jsc);
        Mockito.when(cpm.getJenkinsServerConfiguration(JN + "2")).thenReturn(jsc2);
        Mockito.when(cpm.getAllJenkinsServerConfigurations()).thenReturn(ImmutableList.of(jsc));

        Mockito.when(jsc.getName()).thenReturn(JN);
        Mockito.when(jsc.getUrl()).thenReturn(JURL);
        Mockito.when(jsc.getUsername()).thenReturn(JU);
        Mockito.when(jsc.getPassword()).thenReturn(JP);
        Mockito.when(jsc.getStashUsername()).thenReturn(SU);
        Mockito.when(jsc.getStashPassword()).thenReturn(SP);

        Mockito.when(jsc2.getName()).thenReturn(JN + "2");
        Mockito.when(jsc2.getUrl()).thenReturn(JURL + "2");
        Mockito.when(jsc2.getUsername()).thenReturn(JU + "2");
        Mockito.when(jsc2.getPassword()).thenReturn(JP + "2");
        Mockito.when(jsc2.getStashUsername()).thenReturn(SU + "2");
        Mockito.when(jsc2.getStashPassword()).thenReturn(SP + "2");

        jcs = new JenkinsConfigurationServlet(soyTemplateRenderer, webResourceManager, cpm, pum);
    }

    @Test
    public void getTest() throws Exception {

        jcs.doGet(req, res);

        Mockito.verify(res).setContentType("text/html;charset=UTF-8");
        Mockito.verify(webResourceManager).requireResourcesForContext("plugin.page.stashbot");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Class<Map<String, Object>> cls = (Class<Map<String, Object>>) (Class) Map.class;
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(cls);

        Mockito.verify(soyTemplateRenderer).render(Mockito.eq(writer),
            Mockito.eq("com.palantir.stash.stashbot:stashbotConfigurationResources"),
            Mockito.eq("plugin.page.stashbot.jenkinsConfigurationPanel"), mapCaptor.capture());

        Mockito.verify(pum, Mockito.never()).addUserToRepoForReading(Mockito.any(String.class),
            Mockito.any(Repository.class));
        Mockito.verify(pum, Mockito.never()).createStashbotUser(Mockito.any(JenkinsServerConfiguration.class));

        Map<String, Object> map = mapCaptor.getValue();

        @SuppressWarnings("unchecked")
        List<JenkinsServerConfiguration> jscs = (List<JenkinsServerConfiguration>) map.get("jenkinsConfigs");
        Assert.assertEquals(jsc, jscs.get(0));
    }

    @Test
    public void postTest() throws Exception {

        // test that things are actually updated
        Mockito.when(req.getParameter("name")).thenReturn(JN + "2");
        Mockito.when(req.getParameter("url")).thenReturn(JURL + "2");
        Mockito.when(req.getParameter("username")).thenReturn(JU + "2");
        Mockito.when(req.getParameter("password")).thenReturn(JP + "2");
        Mockito.when(req.getParameter("stashUsername")).thenReturn(SU + "2");
        Mockito.when(req.getParameter("stashPassword")).thenReturn(SP + "2");

        Mockito.when(cpm.getDefaultJenkinsServerConfiguration()).thenReturn(jsc2);
        Mockito.when(cpm.getJenkinsServerConfiguration(JN + "2")).thenReturn(jsc2);

        jcs.doPost(req, res);

        // Verify it persists
        Mockito.verify(cpm).setJenkinsServerConfiguration(JN + "2", JURL + "2", JU + "2", JP + "2", SU + "2", SP + "2");

        // doGet() is then called, so this is the same as getTest()...
        Mockito.verify(res).setContentType("text/html;charset=UTF-8");
        Mockito.verify(webResourceManager).requireResourcesForContext("plugin.page.stashbot");

        @SuppressWarnings({ "unchecked", "rawtypes" })
        Class<Map<String, Object>> cls = (Class<Map<String, Object>>) (Class) Map.class;
        ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(cls);

        Mockito.verify(soyTemplateRenderer).render(Mockito.eq(writer),
            Mockito.eq("com.palantir.stash.stashbot:stashbotConfigurationResources"),
            Mockito.eq("plugin.page.stashbot.jenkinsConfigurationPanel"), mapCaptor.capture());

        Mockito.verify(pum, Mockito.atLeastOnce()).createStashbotUser(Mockito.any(JenkinsServerConfiguration.class));

        Map<String, Object> map = mapCaptor.getValue();

        // Except the details are now changed
        @SuppressWarnings("unchecked")
        List<JenkinsServerConfiguration> jscs = (List<JenkinsServerConfiguration>) map.get("jenkinsConfigs");
        Assert.assertEquals(jsc, jscs.get(0));
    }
}