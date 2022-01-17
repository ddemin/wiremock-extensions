/*
 * Copyright (C) 2011 Thomas Akehurst
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.ddemin.wext;

import com.github.tomakehurst.wiremock.common.Exceptions;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.StubLifecycleListener;
import com.github.tomakehurst.wiremock.extension.responsetemplating.HandlebarsOptimizedTemplate;
import com.github.tomakehurst.wiremock.extension.responsetemplating.RequestTemplateModel;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.extension.responsetemplating.SystemKeyAuthoriser;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.SystemValueHelper;
import com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.WireMockHelpers;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import wiremock.com.github.jknack.handlebars.Handlebars;
import wiremock.com.github.jknack.handlebars.Helper;
import wiremock.com.github.jknack.handlebars.helper.AssignHelper;
import wiremock.com.github.jknack.handlebars.helper.ConditionalHelpers;
import wiremock.com.github.jknack.handlebars.helper.NumberHelper;
import wiremock.com.github.jknack.handlebars.helper.StringHelpers;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public abstract class AbstractExtTemplateTransformer extends ResponseTemplateTransformer implements StubLifecycleListener {

    private final Handlebars handlebars;
    private final Cache<Object, HandlebarsOptimizedTemplate> cache;
    private final Long maxCacheEntries;

    public abstract String getName();

    public AbstractExtTemplateTransformer(boolean global) {
        this(global, Collections.emptyMap());
    }

    public AbstractExtTemplateTransformer(boolean global, String helperName, Helper helper) {
        this(global, ImmutableMap.of(helperName, helper));
    }

    public AbstractExtTemplateTransformer(boolean global, Map<String, Helper> helpers) {
        this(global, new Handlebars(), helpers, null, null);
    }

    public AbstractExtTemplateTransformer(boolean global, Handlebars handlebars, Map<String, Helper> helpers, Long maxCacheEntries, Set<String> permittedSystemKeys) {
        super(global, handlebars, helpers, maxCacheEntries, permittedSystemKeys);

        this.handlebars = handlebars;

        for (StringHelpers helper: StringHelpers.values()) {
            if (!helper.name().equals("now")) {
                this.handlebars.registerHelper(helper.name(), helper);
            }
        }

        for (NumberHelper helper: NumberHelper.values()) {
            this.handlebars.registerHelper(helper.name(), helper);
        }

        for (ConditionalHelpers helper: ConditionalHelpers.values()) {
            this.handlebars.registerHelper(helper.name(), helper);
        }

        this.handlebars.registerHelper(AssignHelper.NAME, new AssignHelper());

        //Add all available wiremock helpers
        for (WireMockHelpers helper: WireMockHelpers.values()) {
            this.handlebars.registerHelper(helper.name(), helper);
        }

        this.handlebars.registerHelper("systemValue", new SystemValueHelper(new SystemKeyAuthoriser(permittedSystemKeys)));

        for (Map.Entry<String, Helper> entry: helpers.entrySet()) {
            this.handlebars.registerHelper(entry.getKey(), entry.getValue());
        }

        this.maxCacheEntries = maxCacheEntries;
        CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
        if (maxCacheEntries != null) {
            cacheBuilder.maximumSize(maxCacheEntries);
        }
        cache = cacheBuilder.build();
    }


    @Override
    public void beforeStubCreated(StubMapping stub) {}

    @Override
    public void afterStubCreated(StubMapping stub) {}

    @Override
    public void beforeStubEdited(StubMapping oldStub, StubMapping newStub) {}

    @Override
    public void afterStubEdited(StubMapping oldStub, StubMapping newStub) {}

    @Override
    public void beforeStubRemoved(StubMapping stub) {}

    @Override
    public void afterStubRemoved(StubMapping stub) {
        cache.invalidateAll();
    }

    @Override
    public void beforeStubsReset() {}

    @Override
    public void afterStubsReset() {
        cache.invalidateAll();
    }

    protected Parameters uncheckedApplyTemplate(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        final ImmutableMap<String, Object> modelForParams = ImmutableMap.<String, Object>builder()
                .put("request", RequestTemplateModel.from(request))
                .putAll(addExtraModelElements(request, responseDefinition, files, parameters))
                .build();

        String parametersAsString = Json.write(parameters);
        HandlebarsOptimizedTemplate parametersTemplate = getTemplateObj(Json.write(parameters), parametersAsString);
        String newParametersAsString = uncheckedApplyTemplate(parametersTemplate, modelForParams);

        Parameters newParameters = Parameters.from(Json.read(newParametersAsString, Map.class));
        return newParameters;
    }

    /**
     * Override this to add extra elements to the template model
     */
    protected Map<String, Object> addExtraModelElements(Request request, ResponseDefinition responseDefinition, FileSource files, Parameters parameters) {
        return Collections.emptyMap();
    }

    protected void parseAndMergeParameters(
            Parameters newRawParameters,
            Request request,
            ResponseDefinition responseDefinition,
            FileSource files,
            Parameters parametersToMerge
    ) {
        if (newRawParameters != null) {
            final Parameters newParsedParameters
                    = uncheckedApplyTemplate(request, responseDefinition, files, newRawParameters);
            parametersToMerge.putAll(newParsedParameters);
        }
    }

    private String uncheckedApplyTemplate(HandlebarsOptimizedTemplate template, Object context) {
        try {
            return template.apply(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HandlebarsOptimizedTemplate getTemplateObj(final Object key, final String content) {
        if (maxCacheEntries != null && maxCacheEntries < 1) {
            return new HandlebarsOptimizedTemplate(handlebars, content);
        }

        try {
            return cache.get(key, new Callable<HandlebarsOptimizedTemplate>() {
                @Override
                public HandlebarsOptimizedTemplate call() {
                    return new HandlebarsOptimizedTemplate(handlebars, content);
                }
            });
        } catch (ExecutionException e) {
            return Exceptions.throwUnchecked(e, HandlebarsOptimizedTemplate.class);
        }
    }

}
