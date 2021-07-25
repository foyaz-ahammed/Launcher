/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.assistant.SettingsUtility.overlay;

import android.content.Context;
import android.support.annotation.Keep;
import com.android.launcher3.assistant.SettingsUtility.experiment.ExperimentFeatureProvider;
import com.android.launcher3.assistant.SettingsUtility.instrumentation.MetricsFeatureProvider;
import com.android.launcher3.assistant.SettingsUtility.search.SearchFeatureProvider;
import com.android.launcher3.assistant.SettingsUtility.search.SearchFeatureProviderImpl;
import com.android.launcher3.assistant.SettingsUtility.suggestions.SuggestionFeatureProvider;

@Keep
public class FeatureFactoryImpl extends FeatureFactory {

    protected MetricsFeatureProvider mMetricsFeatureProvider;
    protected SuggestionFeatureProvider mSuggestionFeatureProvider;
    protected ExperimentFeatureProvider mExperimentFeatureProvider;
    protected SearchFeatureProvider mSearchFeatureProvider;

    @Override
    public MetricsFeatureProvider metricsFeatureProvider(Context context) {
        if (mMetricsFeatureProvider == null) {
            mMetricsFeatureProvider = new MetricsFeatureProvider(context.getApplicationContext());
        }
        return mMetricsFeatureProvider;
    }

    @Override
    public SuggestionFeatureProvider suggestionFeatureProvider() {
        if (mSuggestionFeatureProvider == null) {
            mSuggestionFeatureProvider = new SuggestionFeatureProvider();
        }
        return mSuggestionFeatureProvider;
    }

    @Override
    public ExperimentFeatureProvider experimentFeatureProvider() {
        if (mExperimentFeatureProvider == null) {
            mExperimentFeatureProvider = new ExperimentFeatureProvider();
        }
        return mExperimentFeatureProvider;
    }

    @Override
    public SearchFeatureProvider searchFeatureProvider() {
        if (mSearchFeatureProvider == null) {
            mSearchFeatureProvider = new SearchFeatureProviderImpl();
        }
        return mSearchFeatureProvider;
    }
}
