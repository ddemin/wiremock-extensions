package com.github.ddemin.wext.model;

import com.github.tomakehurst.wiremock.extension.Parameters;

public class ResponseRule {

    private long delayMin;
    private long delay50;
    private long delay90;
    private long delay95;
    private long delayMax;
    private Parameters parameters;

    public ResponseRule() {
    }

    public ResponseRule(ResponseRule rule) {
        this(rule.delayMin, rule.delay50, rule.delay90, rule.delay95, rule.delayMax, rule.getParameters());
    }

    public ResponseRule(long min, long delay50, long delay90, long delay95, long max, Parameters parameters) {
        this.delayMin = min;
        this.delay50 = delay50;
        this.delay90 = delay90;
        this.delay95 = delay95;
        this.delayMax = max;
        this.parameters = parameters;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public long getDelayMin() {
        return delayMin;
    }

    public long getDelay50() {
        if (delay50 < getDelayMin()) {
            delay50 = delayMin;
        }
        return delay50;
    }

    public long getDelay90() {
        if (delay90 < getDelay50()) {
            delay90 = delay50;
        }
        return delay90;
    }

    public long getDelay95() {
        if (delay95 < getDelay90()) {
            delay95 = delay90;
        }
        return delay95;
    }

    public long getDelayMax() {
        if (delayMax < getDelay95()) {
            delayMax = delay95;
        }
        return delayMax;
    }

    public void setDelayMin(long delayMin) {
        this.delayMin = delayMin;
    }

    public void setDelay50(long delay50) {
        this.delay50 = delay50;
    }

    public void setDelay90(long delay90) {
        this.delay90 = delay90;
    }

    public void setDelay95(long delay95) {
        this.delay95 = delay95;
    }

    public void setDelayMax(long delayMax) {
        this.delayMax = delayMax;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }
}
