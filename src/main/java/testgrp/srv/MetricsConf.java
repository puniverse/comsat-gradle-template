package testgrp.srv;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;

/**
 * @author circlespainter
 */
public class MetricsConf {
    public static final MetricRegistry metrics = new MetricRegistry();

    static {
        JmxReporter.forRegistry(metrics).build().start(); // starts reporting via JMX
    }
}
