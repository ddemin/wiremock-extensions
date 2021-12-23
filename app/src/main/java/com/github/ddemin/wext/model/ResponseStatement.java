package com.github.ddemin.wext.model;

import com.github.tomakehurst.wiremock.extension.Parameters;

public class ResponseStatement extends ResponseRule {

    private int ruleNo;
    private long startedTimestamp;

    public static ResponseStatement buildDefault() {
        return new ResponseStatement();
    }

    public ResponseStatement() {
        super(0, 0, 0, 0, 0, new Parameters());
        this.ruleNo = -1;
        this.startedTimestamp = System.currentTimeMillis();
    }

    public ResponseStatement(ResponseRule rule, int ruleNo) {
        super(rule);
        this.ruleNo = ruleNo;
        this.startedTimestamp = System.currentTimeMillis();
    }

    public long getStartedTimestamp() {
        return startedTimestamp;
    }

    public void setStartedTimestamp(long startedTimestamp) {
        this.startedTimestamp = startedTimestamp;
    }

    public boolean isDefault() {
        return this.ruleNo == -1;
    }

    public int getRuleNo() {
        return ruleNo;
    }

    public void setRuleNo(int ruleNo) {
        this.ruleNo = ruleNo;
    }

}
