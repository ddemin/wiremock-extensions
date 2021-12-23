package com.github.ddemin.wext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ddemin.wext.model.ResponseRule;
import com.github.ddemin.wext.util.ExWiremockUtils;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;

import java.util.Map;
import java.util.Set;

public class StatisticDelayedResponseTemplateTransformer extends ExtResponseTemplateTransformer {

    public static final String NAME = "delayed-response-template";

    public StatisticDelayedResponseTemplateTransformer(boolean global) {
        super(global);
    }

    public StatisticDelayedResponseTemplateTransformer(boolean global, String helperName, Helper helper) {
        super(global, helperName, helper);
    }

    public StatisticDelayedResponseTemplateTransformer(boolean global, Map<String, Helper> helpers) {
        super(global, helpers);
    }

    public StatisticDelayedResponseTemplateTransformer(
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
        // TODO Cache somehow
        final ResponseRule nextRule
                = new ObjectMapper().convertValue(parsedParameters.get("dynamic-delay"), ResponseRule.class);

        try {
            final long delay = ExWiremockUtils.getStatisticDelayForRule(nextRule);
            Thread.sleep(delay);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return super.transform(request, responseDefinition, files, parsedParameters);
    }

}
