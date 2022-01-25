package com.github.ddemin.wext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ddemin.wext.model.ResponseRule;
import com.github.ddemin.wext.model.ResponseStatement;
import com.github.ddemin.wext.util.ExWiremockUtils;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AsyncStateResponseTemplateTransformer extends AbstractExtTemplateTransformer {

    public static final String NAME = "async-state-response-template";

    private static final Map<ResponseDefinition, Map<String, ResponseStatement>> STORAGE_FOR_STATEMENTS = new ConcurrentHashMap<>();
    private static final Map<ResponseDefinition, List<ResponseRule>> STORAGE_FOR_RULES = new ConcurrentHashMap<>();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String KEY_FIELD = "dynamic-key";
    private static final String STATEMENTS_FIELD = "dynamic-statements";

    public AsyncStateResponseTemplateTransformer() {
        super(false);
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

        final String key = parsedParameters.getString(KEY_FIELD);
        if (key != null) {
            final List rules = parsedParameters.getList(STATEMENTS_FIELD);
            final Map<String, ResponseStatement> statementByKey = STORAGE_FOR_STATEMENTS.computeIfAbsent(
                    responseDefinition,
                    k -> new ConcurrentHashMap<>()
            );
            final ResponseStatement prevStatement = statementByKey.computeIfAbsent(
                    key,
                    k -> ResponseStatement.buildDefault()
            );

            final int prevRuleNo = prevStatement.getRuleNo();
            final int nextRuleNo = prevRuleNo + 1;

            if (nextRuleNo < rules.size()) {
                final List<ResponseRule> rulesList = getAndCacheRules(responseDefinition, rules);
                final ResponseRule nextRule = rulesList.get(nextRuleNo);
                final long delay = ExWiremockUtils.chooseDelayForRule(nextRule);
                final long nextRuleTs = prevStatement.getStartedTimestamp() + delay;

                if (nextRuleTs <= System.currentTimeMillis()) {
                    statementByKey.put(key, new ResponseStatement(nextRule, nextRuleNo));
                }
            }

            parseAndMergeParameters(
                    statementByKey.get(key).getParameters(),
                    request,
                    responseDefinition,
                    files,
                    parsedParameters
            );
        }

        return super.transform(request, responseDefinition, files, parsedParameters);
    }

    private List<ResponseRule> getAndCacheRules(ResponseDefinition responseDefinition, List rules) {
        final List<ResponseRule> rulesList = STORAGE_FOR_RULES.computeIfAbsent(
                responseDefinition,
                k -> {
                    final List<ResponseRule> parsedRulesList = new LinkedList<>();
                    rules.forEach(
                            rule -> parsedRulesList.add(OBJECT_MAPPER.convertValue(rule, ResponseRule.class))
                    );
                    return parsedRulesList;
                }
        );
        return rulesList;
    }

}
