package com.github.ddemin.wext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ddemin.wext.model.ResponseRule;
import com.github.ddemin.wext.model.ResponseStatement;
import com.github.ddemin.wext.util.ExWiremockUtils;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncStateResponseTemplateTransformer extends ExtResponseTemplateTransformer {

    public static final String NAME = "async-state-response-template";

    private static final Map<String, ResponseStatement> TS_BY_KEY = new ConcurrentHashMap<>();

    public AsyncStateResponseTemplateTransformer(boolean global) {
        super(global);
    }

    public AsyncStateResponseTemplateTransformer(boolean global, String helperName, Helper helper) {
        super(global, helperName, helper);
    }

    public AsyncStateResponseTemplateTransformer(boolean global, Map<String, Helper> helpers) {
        super(global, helpers);
    }

    public AsyncStateResponseTemplateTransformer(
            boolean global,
            Handlebars handlebars,
            Map<String, Helper> helpers,
            Long maxCacheEntries,
            Set<String> permittedSystemKeys
    ) {
        super(global, handlebars, helpers, maxCacheEntries, permittedSystemKeys);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ResponseDefinition transform(
            Request request, ResponseDefinition responseDefinition, FileSource files, Parameters origParameters
    ) {
        final Parameters parsedParameters = uncheckedApplyTemplate(request, responseDefinition, files, origParameters);

        String key = parsedParameters.getString("dynamic-key");
        if (key != null) {
            // TODO Cache somehow
            List rules = parsedParameters.getList("dynamic-statements");

            ResponseStatement prevStatement = TS_BY_KEY.computeIfAbsent(key, k -> ResponseStatement.buildDefault());
            final int prevRuleNo = prevStatement.getRuleNo();
            final int nextRuleNo = prevRuleNo + 1;
            if (nextRuleNo < rules.size()) {
                // TODO Cache somehow
                final ResponseRule nextRule = new ObjectMapper().convertValue(rules.get(nextRuleNo), ResponseRule.class);
                final long delay = ExWiremockUtils.getStatisticDelayForRule(nextRule);
                final long nextRuleTs = prevStatement.getStartedTimestamp() + delay;

                if (nextRuleTs <= System.currentTimeMillis()) {
                    TS_BY_KEY.put(key, new ResponseStatement(nextRule, nextRuleNo));
                }
            }

            parseAndMergeParameters(
                    TS_BY_KEY.get(key).getParameters(),
                    request,
                    responseDefinition,
                    files,
                    parsedParameters
            );
        }

        return super.transform(request, responseDefinition, files, parsedParameters);
    }

}
