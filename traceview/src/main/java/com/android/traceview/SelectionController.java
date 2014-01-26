/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.traceview;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

public class SelectionController extends Observable {

	private ArrayList<Selection> mSelections;
	private List<IntervalSelection> mIntervals;
	private List<Long> mTimestamps;
	private List<Long> mInsideTimestamps;

    public void change(ArrayList<Selection> selections, Object arg) {
        this.mSelections = selections;
        setChanged();
        notifyObservers(arg);
    }

	public void changeIntervals(List<IntervalSelection> intervals,
			Object arg) {
		this.mIntervals = intervals;
		setChanged();
		notifyObservers(arg);
	}

	public void changeTimestamps(List<Long> timestamps, Object arg) {
		this.mTimestamps = timestamps;
		setChanged();
		notifyObservers(arg);
	}
	
	public void changeInsideTimestamps(List<Long> insideTimestamps, Object arg) {
		this.mInsideTimestamps = insideTimestamps;
		setChanged();
		notifyObservers(arg);
	}

	public ArrayList<Selection> getSelections() {
        return mSelections;
    }

	public List<IntervalSelection> getIntervals() {
		return mIntervals;
	}

	public List<Long> getTimestamps() {
		return mTimestamps;
	}
	
	public List<Long> getInsideTimestamps() {
		return mInsideTimestamps;
	}
}
