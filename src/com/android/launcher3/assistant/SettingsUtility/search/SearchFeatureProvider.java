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
 *
 */
package com.android.launcher3.assistant.SettingsUtility.search;

import android.content.Context;
import android.util.Pair;
import android.view.View;
import com.android.launcher3.assistant.SettingsUtility.search.indexing.DatabaseIndexingManager;
import com.android.launcher3.assistant.SettingsUtility.search.indexing.IndexingCallback;
import com.android.launcher3.assistant.SettingsUtility.search.query.SearchQueryTask;
import com.android.launcher3.assistant.SettingsUtility.search.sitemap.SiteMapManager;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

/**
 * FeatureProvider for Settings Search
 */
public interface SearchFeatureProvider {

    boolean DEBUG = false;

    /**
     * Returns a new loader to get settings search results.
     */
    SearchResultLoader getSearchResultLoader(Context context, String query);

    /**
     * Returns a list of {@link SearchQueryTask}, each responsible for searching a subsystem for
     * user query.
     */
    List<SearchQueryTask> getSearchQueryTasks(Context context, String query);

    /**
     * Returns the manager for indexing Settings data.
     */
    DatabaseIndexingManager getIndexingManager(Context context);

    /**
     * Returns the manager for looking up breadcrumbs.
     */
    SiteMapManager getSiteMapManager();

    /**
     * Updates the Settings indexes and calls {@link IndexingCallback#onIndexingFinished()} on
     * {@param callback} when indexing is complete.
     */
    void updateIndexAsync(Context context, IndexingCallback callback);

    /**
     * @returns true when indexing is complete.
     */
    boolean isIndexingComplete(Context context);

    /**
     * @return a {@link ExecutorService} to be shared between search tasks.
     */
    ExecutorService getExecutorService();

    /**
     * Initializes the feedback button in case it was dismissed.
     */
    void initFeedbackButton();
    /**
     * @return smart ranking timeout in milliseconds.
     */
    long smartSearchRankingTimeoutMs(Context context);

    /**
     * Prepare for search ranking predictions to avoid latency on the first prediction call.
     */
    void searchRankingWarmup(Context context);

    /**
     * Return a FutureTask to get a list of scores for search results.
     */
    FutureTask<List<Pair<String, Float>>> getRankerTask(Context context, String query);
}
