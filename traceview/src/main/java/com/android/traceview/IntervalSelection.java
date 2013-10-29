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

public class IntervalSelection {

	private final Action mAction;

	// times relative to logging start time
	private long mStart;
	private long mEnd;

	public IntervalSelection(Action action, long start, long end) {
		mStart = start;
		mEnd = end;
		mAction = action;
    }

	public static IntervalSelection highlight(long start, long end) {
		return new IntervalSelection(Action.Highlight, start, end);
    }

	public long getmStart() {
		return mStart;
	}

	public void setmStart(long mStart) {
		this.mStart = mStart;
	}

	public long getmEnd() {
		return mEnd;
	}

	public void setmEnd(long mEnd) {
		this.mEnd = mEnd;
	}


	public static enum Action {
        Highlight, Include, Exclude, Aggregate
    };
}
