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

package org.apache.flink.kubernetes.operator.autoscaler;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.kubernetes.operator.autoscaler.metrics.EvaluatedScalingMetric;
import org.apache.flink.kubernetes.operator.autoscaler.metrics.ScalingMetric;
import org.apache.flink.metrics.Counter;
import org.apache.flink.metrics.MetricGroup;
import org.apache.flink.runtime.jobgraph.JobVertexID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static org.apache.flink.kubernetes.operator.autoscaler.metrics.ScalingMetric.PARALLELISM;
import static org.apache.flink.kubernetes.operator.autoscaler.metrics.ScalingMetric.RECOMMENDED_PARALLELISM;

/** Autoscaler metrics for observability. */
public class AutoscalerFlinkMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(AutoscalerFlinkMetrics.class);
    @VisibleForTesting static final String CURRENT = "Current";
    @VisibleForTesting static final String AVERAGE = "Average";
    @VisibleForTesting static final String JOB_VERTEX_ID = "jobVertexID";

    final Counter numScalings;

    final Counter numErrors;

    final Counter numBalanced;

    private final MetricGroup metricGroup;

    private final Set<JobVertexID> vertexMetrics = new HashSet<>();

    public AutoscalerFlinkMetrics(MetricGroup metricGroup) {
        this.numScalings = metricGroup.counter("scalings");
        this.numErrors = metricGroup.counter("errors");
        this.numBalanced = metricGroup.counter("balanced");
        this.metricGroup = metricGroup;
    }

    public void registerScalingMetrics(
            Supplier<Map<JobVertexID, Map<ScalingMetric, EvaluatedScalingMetric>>>
                    currentVertexMetrics) {
        currentVertexMetrics
                .get()
                .forEach(
                        (jobVertexID, evaluated) -> {
                            if (!vertexMetrics.add(jobVertexID)) {
                                return;
                            }
                            LOG.info("Registering scaling metrics for job vertex {}", jobVertexID);
                            var jobVertexMg =
                                    metricGroup.addGroup(JOB_VERTEX_ID, jobVertexID.toHexString());

                            evaluated.forEach(
                                    (sm, esm) -> {
                                        var smGroup = jobVertexMg.addGroup(sm.name());

                                        smGroup.gauge(
                                                CURRENT,
                                                () ->
                                                        Optional.ofNullable(
                                                                        currentVertexMetrics.get())
                                                                .map(m -> m.get(jobVertexID))
                                                                .map(metrics -> metrics.get(sm))
                                                                .map(
                                                                        EvaluatedScalingMetric
                                                                                ::getCurrent)
                                                                .orElse(Double.NaN));

                                        if (sm.isCalculateAverage()) {
                                            smGroup.gauge(
                                                    AVERAGE,
                                                    () ->
                                                            Optional.ofNullable(
                                                                            currentVertexMetrics
                                                                                    .get())
                                                                    .map(m -> m.get(jobVertexID))
                                                                    .map(metrics -> metrics.get(sm))
                                                                    .map(
                                                                            EvaluatedScalingMetric
                                                                                    ::getAverage)
                                                                    .orElse(Double.NaN));
                                        }
                                    });
                        });
    }

    @VisibleForTesting
    static void initRecommendedParallelism(
            Map<JobVertexID, Map<ScalingMetric, EvaluatedScalingMetric>> evaluatedMetrics) {
        evaluatedMetrics.forEach(
                (jobVertexID, evaluatedScalingMetricMap) ->
                        evaluatedScalingMetricMap.put(
                                RECOMMENDED_PARALLELISM,
                                evaluatedScalingMetricMap.get(PARALLELISM)));
    }

    @VisibleForTesting
    static void resetRecommendedParallelism(
            Map<JobVertexID, Map<ScalingMetric, EvaluatedScalingMetric>> evaluatedMetrics) {
        evaluatedMetrics.forEach(
                (jobVertexID, evaluatedScalingMetricMap) ->
                        evaluatedScalingMetricMap.put(RECOMMENDED_PARALLELISM, null));
    }
}
