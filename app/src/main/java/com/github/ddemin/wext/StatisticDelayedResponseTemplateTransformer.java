package com.github.ddemin.wext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ddemin.wext.model.ResponseRule;
import com.github.ddemin.wext.util.ExWiremockUtils;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StatisticDelayedResponseTemplateTransformer extends AbstractExtTemplateTransformer {

    public static final String NAME = "delayed-response-template";

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static Map<ResponseDefinition, ResponseRule> STORAGE_FOR_RULES = new ConcurrentHashMap<>();

    public StatisticDelayedResponseTemplateTransformer() {
        super(false);
    }

    @Override
    public String getName() {
        return StatisticDelayedResponseTemplateTransformer.NAME;
    }

    @Override
    public ResponseDefinition transform(
            Request request, ResponseDefinition responseDefinition, FileSource files, Parameters origParameters
    ) {
        final Parameters parsedParameters = uncheckedApplyTemplate(request, responseDefinition, files, origParameters);
        final ResponseRule rule = getAndCacheRule(responseDefinition, parsedParameters.get("dynamic-delay"));

        try {
            final long delay = ExWiremockUtils.chooseDelayForRule(rule);
            Thread.sleep(delay);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return responseDefinition;
    }

    private ResponseRule getAndCacheRule(ResponseDefinition responseDefinition, Object rawRule) {
        return STORAGE_FOR_RULES.computeIfAbsent(
                responseDefinition,
                k -> OBJECT_MAPPER.convertValue(rawRule, ResponseRule.class)
        );
    }

}
