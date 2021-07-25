/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.launcher3.pageindicators;

/**
 * 여러개의 페지가 있을때 페지구분을 보여주는 PageIndicator Interface
 *
 * @see WorkspacePageIndicator
 * @see PageIndicatorDots
 */
public interface PageIndicator {

    /**
     * Scroll진행
     * @param currentScroll scroll 현재 위치
     * @param totalScroll 전체 scroll 길이
     */
    void setScroll(int currentScroll, int totalScroll);

    /**
     * 페지선택
     * @param activePage 선택할 페지
     */
    void setActiveMarker(int activePage);

    /**
     * 페지개수 설정
     * @param numMarkers 페지개수
     */
    void setMarkersCount(int numMarkers);
}
