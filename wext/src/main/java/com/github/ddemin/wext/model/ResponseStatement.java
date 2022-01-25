package com.github.ddemin.wext.model;

import com.github.tomakehurst.wiremock.extension.Parameters;

public class ResponseStatement extends ResponseRule {

    private final int ruleNo;
    private final long startedTimestamp;

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

    public int getRuleNo() {
        return ruleNo;
    }

}
