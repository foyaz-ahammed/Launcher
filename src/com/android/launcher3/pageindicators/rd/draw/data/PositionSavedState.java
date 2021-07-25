/*
 *     Copyright (C) 2020 Lawnchair Team.
 *
 *     This file is part of Lawnchair Launcher.
 *
 *     Lawnchair Launcher is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Lawnchair Launcher is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Lawnchair Launcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.android.launcher3.pageindicators.rd.draw.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

public class PositionSavedState extends View.BaseSavedState {

    private int selectedPosition;
    private int selectingPosition;
    private int lastSelectedPosition;

    public PositionSavedState(Parcelable superState) {
        super(superState);
    }

    private PositionSavedState(Parcel in) {
        super(in);
        this.selectedPosition = in.readInt();
        this.selectingPosition = in.readInt();
        this.lastSelectedPosition = in.readInt();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    public int getSelectingPosition() {
        return selectingPosition;
    }

    public void setSelectingPosition(int selectingPosition) {
        this.selectingPosition = selectingPosition;
    }

    public int getLastSelectedPosition() {
        return lastSelectedPosition;
    }

    public void setLastSelectedPosition(int lastSelectedPosition) {
        this.lastSelectedPosition = lastSelectedPosition;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);
        out.writeInt(this.selectedPosition);
        out.writeInt(this.selectingPosition);
        out.writeInt(this.lastSelectedPosition);
    }

    public static final Creator<PositionSavedState> CREATOR = new Creator<PositionSavedState>() {
        public PositionSavedState createFromParcel(Parcel in) {
            return new PositionSavedState(in);
        }

        public PositionSavedState[] newArray(int size) {
            return new PositionSavedState[size];
        }
    };
}
