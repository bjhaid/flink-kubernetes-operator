/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.kubernetes.operator.kubeclient.decorators;

import org.apache.flink.client.deployment.ClusterSpecification;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.kubernetes.configuration.KubernetesConfigOptions;
import org.apache.flink.kubernetes.kubeclient.FlinkPod;
import org.apache.flink.kubernetes.operator.kubeclient.parameters.StandaloneKubernetesJobManagerParameters;
import org.apache.flink.kubernetes.operator.standalone.StandaloneKubernetesConfigOptionsInternal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/** @link CmdStandaloneJobManagerDecorator unit tests */
public class CmdStandaloneJobManagerDecoratorTest {

    private static final String MOCK_ENTRYPATH = "./docker-entrypath";

    private StandaloneKubernetesJobManagerParameters jmParameters;
    private Configuration configuration;
    private CmdStandaloneJobManagerDecorator decorator;

    @BeforeEach
    public void setup() {
        configuration = new Configuration();
        configuration.setString(KubernetesConfigOptions.KUBERNETES_ENTRY_PATH, MOCK_ENTRYPATH);
        jmParameters =
                new StandaloneKubernetesJobManagerParameters(
                        configuration,
                        new ClusterSpecification.ClusterSpecificationBuilder()
                                .createClusterSpecification());

        decorator = new CmdStandaloneJobManagerDecorator(jmParameters);
    }

    @Test
    public void testSessionCommandAdded() {
        configuration.set(
                StandaloneKubernetesConfigOptionsInternal.CLUSTER_MODE,
                StandaloneKubernetesConfigOptionsInternal.ClusterMode.SESSION);

        FlinkPod decoratedPod = decorator.decorateFlinkPod(new FlinkPod.Builder().build());
        assertThat(
                decoratedPod.getMainContainer().getCommand(), containsInAnyOrder(MOCK_ENTRYPATH));
        assertThat(
                decoratedPod.getMainContainer().getArgs(),
                containsInAnyOrder(CmdStandaloneJobManagerDecorator.JOBMANAGER_ENTRYPOINT_ARG));
    }

    @Test
    public void testApplicationCommandAdded() {
        configuration.set(
                StandaloneKubernetesConfigOptionsInternal.CLUSTER_MODE,
                StandaloneKubernetesConfigOptionsInternal.ClusterMode.APPLICATION);

        FlinkPod decoratedPod = decorator.decorateFlinkPod(new FlinkPod.Builder().build());
        assertThat(
                decoratedPod.getMainContainer().getCommand(), containsInAnyOrder(MOCK_ENTRYPATH));
        assertThat(
                decoratedPod.getMainContainer().getArgs(),
                containsInAnyOrder(CmdStandaloneJobManagerDecorator.APPLICATION_MODE_ARG));
    }
}
