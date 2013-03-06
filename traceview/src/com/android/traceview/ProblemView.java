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
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.android.traceview.IntervalSelection.Action;
import com.android.traceview.TimeLineView.Record;

public class ProblemView extends Composite {

    private final SelectionController mSelectionController;
    private final ProfileProvider mProfileProvider;
    private MethodData mCurrentHighlightedMethod;

	private MethodHandler mMethodHandler;

	private final Map<Integer, ContextLogData> mLogMap;
	private final ArrayList<Record> mTraceRecords;

	private List<Long> mTimestamps;
	private List<Long> mInsideTimestamps;

	private List<IntervalSelection> mIntervals;

	private ContextLogData mSelectedLog;

	private final Color mColorNoMatch;
	private final Color mColorMatch;

	private final SimpleContentProposalProvider mScp;

	private final MethodData BRBMethodData;

	private static long mStartDiff;

	// UI elements
	Text mResultBox;

	final Combo stringRelationCombo;
	final Combo stringValueCombo;

	final Button BRBButton;
	final Button problemDefButton;

	final Text mProblemBox;

	final Combo logTypeCombo;

	final Combo intRelationCombo;
	final Text mIntValueBox;
	
	final Combo floatRelationCombo;
	final Text mFloatValueBox;

    public interface MethodHandler {
        void handleMethod(MethodData method);
    }

	public ProblemView(Composite parent, TraceReader reader,
			ContextLogReader logReader,
            SelectionController selectController) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout(2, true));
        this.mSelectionController = selectController;

		Display display = getDisplay();

		mProfileProvider = reader.getProfileProvider();

		mLogMap = logReader.getLogMap();

		mTraceRecords = reader.getThreadTimeRecords();

		BRBMethodData = null;
		mSelectedLog = null;
		mIntervals = new ArrayList<IntervalSelection>();
		mTimestamps = new ArrayList<Long>();
		mInsideTimestamps = new ArrayList<Long>();

		mStartDiff = reader.getStartTime() - logReader.getStartTime();

		// Add container holding the problem definition form
		Composite formComposite = new Composite(this, SWT.NONE);
		formComposite.setLayout(new FillLayout(SWT.VERTICAL));
		formComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Add a label for the problem definer
		Label problemLabel = new Label(formComposite, SWT.NONE);
		problemLabel.setText("Problem:");

		// Add radio button for BRB option
		BRBButton = new Button(formComposite, SWT.RADIO);
		BRBButton.setText("BigRedButton");

		// Add radio button for own problem definition option
		problemDefButton = new Button(formComposite, SWT.RADIO);
		problemDefButton.setText("Function call + constraint");

		Composite composite = new Composite(formComposite, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		// composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Add a text box for searching for method names
		Label callLabel = new Label(composite, SWT.NONE);
		callLabel.setText("Function:");

		// Add a text box for searching for method names
		mProblemBox = new Text(composite, SWT.BORDER);
		mProblemBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		mProblemBox.setEnabled(false);

		mColorNoMatch = new Color(display, 255, 200, 200);
		mColorMatch = mProblemBox.getBackground();

		ContentProposalAdapter adapter = null;
		mScp = new SimpleContentProposalProvider(new String[0]);

		adapter = new ContentProposalAdapter(mProblemBox,
				new TextContentAdapter(), mScp, null, null);
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);

		final Composite constraintHolderComposite = new Composite(
				formComposite,
				SWT.NONE);
		constraintHolderComposite.setLayout(new FormLayout());

		// composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));


		Composite mConstraintComposite = new Composite(
				constraintHolderComposite, SWT.NONE);
		mConstraintComposite.setLayout(new FillLayout());

		Label constraintLabel = new Label(mConstraintComposite, SWT.NONE);
		constraintLabel.setText("Constraint:");

		logTypeCombo = new Combo(mConstraintComposite,
				SWT.DROP_DOWN
				| SWT.BORDER
				| SWT.READ_ONLY);
		for (Entry<Integer, ContextLogData> ld : mLogMap.entrySet()) {
			logTypeCombo.add(ld.getValue().getName(), ld.getKey());
			System.out.println(ld.getValue().getName() + " " + ld.getKey());
		}
		logTypeCombo.setEnabled(false);

		final FormData hide = new FormData();
		hide.top = new FormAttachment(0);
		hide.bottom = new FormAttachment(0);
		hide.left = new FormAttachment(0);
		hide.right = new FormAttachment(0);

		final FormData show = new FormData();
		show.top = new FormAttachment(0);
		show.bottom = new FormAttachment(100, -4);
		show.left = new FormAttachment(mConstraintComposite, 0, 4);
		show.right = new FormAttachment(100);

		// Composite holding controls for int type logs
		final Composite mIntComposite = new Composite(
				constraintHolderComposite, SWT.NONE);
		mIntComposite.setLayout(new FillLayout());

		intRelationCombo = new Combo(mIntComposite, SWT.DROP_DOWN
				| SWT.BORDER | SWT.READ_ONLY);
		intRelationCombo.add("<");
		intRelationCombo.add("<=");
		intRelationCombo.add("=");
		intRelationCombo.add(">=");
		intRelationCombo.add(">");

		mIntValueBox = new Text(mIntComposite, SWT.BORDER);

		// Composite holding controls for float type logs
		final Composite mFloatComposite = new Composite(
				constraintHolderComposite, SWT.NONE);
		mFloatComposite.setLayout(new FillLayout());

		floatRelationCombo = new Combo(mFloatComposite, SWT.DROP_DOWN
				| SWT.BORDER | SWT.READ_ONLY);
		floatRelationCombo.add("<");
		floatRelationCombo.add(">");

		mFloatValueBox = new Text(mFloatComposite, SWT.BORDER);

		// Composite holding controls for float type logs
		final Composite mStringComposite = new Composite(
				constraintHolderComposite, SWT.NONE);
		mStringComposite.setLayout(new FillLayout());

		stringRelationCombo = new Combo(mStringComposite, SWT.DROP_DOWN
				| SWT.BORDER | SWT.READ_ONLY);
		stringRelationCombo.add("=");
		stringRelationCombo.add("<>");
		stringValueCombo = new Combo(mStringComposite,
				SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);

		mIntComposite.setLayoutData(hide);
		mFloatComposite.setLayoutData(hide);
		mStringComposite.setLayoutData(hide);

		// Add result text field to the right side

		mResultBox = new Text(this, SWT.MULTI | SWT.BORDER);
		mResultBox.setLayoutData(new GridData(GridData.FILL_BOTH));
		mResultBox.setEditable(false);

		logTypeCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

				int index = logTypeCombo.getSelectionIndex();
				System.out.println(index);
				mSelectedLog = mLogMap.get(index);
				if (mSelectedLog == null) {
					System.out.println("null");
					return;
				}
				switch (mSelectedLog.getType()) {
				case INT:
				case LONG:
					System.out.println("i");
					mIntComposite.setLayoutData(show);
					mFloatComposite.setLayoutData(hide);
					mStringComposite.setLayoutData(hide);
					mIntValueBox.setText("");
					break;
				case FLOAT:
				case DOUBLE:
					System.out.println("f");
					mIntComposite.setLayoutData(hide);
					mFloatComposite.setLayoutData(show);
					mStringComposite.setLayoutData(hide);
					mFloatValueBox.setText("");
					break;
				case STRING:
					System.out.println("s");
					stringValueCombo.removeAll();
					for (String s : mSelectedLog.getStringDataMap().values()) {
						stringValueCombo.add(s);
					}
					mIntComposite.setLayoutData(hide);
					mFloatComposite.setLayoutData(hide);
					mStringComposite.setLayoutData(show);
					break;
				}
				constraintHolderComposite.pack();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
		
		intRelationCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				try {
					String relationStr = intRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					updateIntervals(rel, Long.valueOf(mIntValueBox.getText()));

				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals, "ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				}
			}

		});

		mIntValueBox.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {
				try {
					String relationStr = intRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					updateIntervals(rel, Long.valueOf(mIntValueBox.getText()));
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				}
			}
		});

		floatRelationCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				try {
					String relationStr = floatRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					updateIntervals(rel,
							Double.valueOf(mFloatValueBox.getText()));
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				}
			}
			
		});
		
		mFloatValueBox.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent arg0) {
				try {
					String relationStr = floatRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					updateIntervals(rel,
							Double.valueOf(mFloatValueBox.getText()));
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				}
			}
		});

		stringRelationCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				try {
					String relationStr = stringRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					updateIntervals(rel, stringValueCombo.getText());
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				}
			}
		});

		stringValueCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				try {
					String relationStr = stringRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					updateIntervals(rel, stringValueCombo.getText());
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo");
					mIntervals.clear();
					mSelectionController.changeIntervals(mIntervals,
							"ProblemView");
				}
			}
		});

		problemDefButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (problemDefButton.getSelection()) {
					mProblemBox.setEnabled(true);
					logTypeCombo.setEnabled(true);
					mIntValueBox.setEnabled(true);
					mFloatValueBox.setEnabled(true);
					stringValueCombo.setEnabled(true);
					intRelationCombo.setEnabled(true);
					floatRelationCombo.setEnabled(true);
					stringRelationCombo.setEnabled(true);

					mProblemBox.setText("");

					mIntervals.clear();
					updateTimeStamps(null);
				}
			}
		});

		BRBButton.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				if (BRBButton.getSelection()) {
					mProblemBox.setText("");
					mProblemBox.setEnabled(false);
					logTypeCombo.setEnabled(false);
					mIntValueBox.setEnabled(false);
					mFloatValueBox.setEnabled(false);
					stringValueCombo.setEnabled(false);
					intRelationCombo.setEnabled(false);
					floatRelationCombo.setEnabled(false);
					stringRelationCombo.setEnabled(false);

					mIntervals.clear();
					updateTimeStamps(BRBMethodData);

				}
			}
		});

		mProblemBox.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent ev) {
				String query = mProblemBox.getText();
				if (query.length() < 4) {
					mScp.setProposals(new String[0]);
					mProblemBox.setBackground(mColorMatch);
					return;
				}
				MethodData[] matchingMD = findAllName(query);

				if (matchingMD.length == 0) {
					mProblemBox.setBackground(mColorNoMatch);
				} else {
					mProblemBox.setBackground(mColorMatch);
				}
				List<String> proposals = new ArrayList<String>();

				for (MethodData md : matchingMD) {
					StringBuilder proposal = new StringBuilder(md.getName());

					proposals.add(proposal.toString());
				}
				// TODO: get matching call names
				mScp.setProposals(proposals.toArray(new String[0]));


				if (matchingMD.length == 1) {
					updateTimeStamps(matchingMD[0]);
				}
			}
		});

    }

	private void updateTimeStamps(MethodData md) {
		ArrayList<Long> timeStamps = new ArrayList<Long>();
		if (md != null) {
			for (Record rec : mTraceRecords) {
				if (rec.block.getMethodData() == md) {
					timeStamps.add(rec.block.getStartTime());
				}
			}
		}
		mSelectionController.changeTimestamps(timeStamps, "ProblemView");
		mTimestamps = timeStamps;
	}

	private void updateIntervals(Relation rel, long constraint) {
		ArrayList<IntervalSelection> intervals = new ArrayList<IntervalSelection> (); 
		if (mSelectedLog == null) {
			mIntervals = intervals;
		}
		boolean inside = false;
		long begining = -1;
		long end;
		NavigableMap<Long, ? extends Number> logMap;
		switch (mSelectedLog.getType()) {
		case INT:
			logMap = mSelectedLog.getIntDataMap();
			break;
		case LONG:
			logMap = mSelectedLog.getLongDataMap();
			break;
		default:
			return;
		}
		for(Entry<Long,? extends Number> e : logMap.entrySet()) {
			Long val = e.getValue().longValue();
			// start of an interval
			if (inside == false && eval(val, rel, constraint) == true) {
				begining = e.getKey();
				inside = true;
				continue;
			}
			// we are inside of an interval
			if (inside == true && eval(val, rel, constraint) == true) {
				continue;
			}
			// we leave the interval
			if (inside == true && eval(val, rel, constraint) == false) {
				end = e.getKey();
				inside = false;
				intervals.add(new IntervalSelection(Action.Highlight,begining,end));
				begining = -1;
				continue;
			}
			if (inside == false && eval(val, rel, constraint) == false) {
				continue;
			}

		}
		
		if (begining != -1) {
			intervals.add(new IntervalSelection(Action.Highlight, begining,
					Long.MAX_VALUE));
		}

		mIntervals = intervals;
		// debug
		// for (IntervalSelection i : intervals) {
		// System.out.println(i.getmStart() + " - " + i.getmEnd());
		// }
		mSelectionController.changeIntervals(intervals, "ProblemView");
	}

	private void updateIntervals(Relation rel, double constraint) {
		ArrayList<IntervalSelection> intervals = new ArrayList<IntervalSelection>();
		if (mSelectedLog == null) {
			mIntervals = intervals;
		}

		boolean inside = false;
		long begining = -1;
		long end;
		long lastKey = -1;
		double lastValue = -1;

		NavigableMap<Long, ? extends Number> logMap;
		switch (mSelectedLog.getType()) {
		case FLOAT:
			logMap = mSelectedLog.getFloatDataMap();
			break;
		case DOUBLE:
			logMap = mSelectedLog.getDoubleDataMap();
			break;
		default:
			return;
		}

		for (Entry<Long, ? extends Number> e : logMap.entrySet()) {
			double val = e.getValue().doubleValue();
			// start of an interval
			if (inside == false && eval(val, rel, constraint) == true) {
				if (lastKey == -1) {
					begining = e.getKey();
				} else {
					begining = (long) (Math.abs(constraint - lastValue)
							/ Math.abs(val - lastValue)
							* Math.abs(e.getKey() - lastKey) + lastKey);
				}

				inside = true;
				lastKey = e.getKey();
				lastValue = val;
				continue;
			}
			// we are inside of an interval
			if (inside == true && eval(val, rel, constraint) == true) {
				lastKey = e.getKey();
				lastValue = val;
				continue;
			}
			// we leave the interval
			if (inside == true && eval(val, rel, constraint) == false) {
				end = (long) (Math.abs(constraint - lastValue)
						/ Math.abs(val - lastValue)
						* Math.abs(e.getKey() - lastKey) + lastKey);
				inside = false;
				intervals.add(new IntervalSelection(Action.Highlight, begining,
						end));
				begining = -1;

				lastKey = e.getKey();
				lastValue = val;
				continue;
			}
			if (inside == false && eval(val, rel, constraint) == false) {
				lastKey = e.getKey();
				lastValue = val;
				continue;
			}

		}

		if (begining != -1) {
			intervals.add(new IntervalSelection(Action.Highlight, begining,
					Long.MAX_VALUE));
		}

		mIntervals = intervals;
		// debug
		// for (IntervalSelection i : intervals) {
		// System.out.println(i.getmStart() + " - " + i.getmEnd());
		// }
		mSelectionController.changeIntervals(intervals, "ProblemView");
	}

	private void updateIntervals(Relation rel, String constraint) {
		ArrayList<IntervalSelection> intervals = new ArrayList<IntervalSelection>();
		if (mSelectedLog == null) {
			mIntervals = intervals;
		}

		boolean inside = false;
		long begining = -1;
		long end;
		boolean equals = (rel == Relation.EQ);

		NavigableMap<Long, String> logMap = mSelectedLog.getStringDataMap();
		for (Entry<Long, String> e : logMap.entrySet()) {
			if (inside == false && e.getValue().equals(constraint) == equals) {
				begining = e.getKey();
				inside = true;
				continue;
			}
			if (inside == true && e.getValue().equals(constraint) == equals) {
				continue;
			}
			if (inside == true && e.getValue().equals(constraint) != equals) {
				end = e.getKey();
				inside = false;
				intervals.add(new IntervalSelection(Action.Highlight, begining,
						end));
				begining = -1;
				continue;
			}
		}
		if (begining != -1) {
			intervals.add(new IntervalSelection(Action.Highlight, begining,
					Long.MAX_VALUE));
		}

		mIntervals = intervals;
		// debug
		// for (IntervalSelection i : intervals) {
		// System.out.println(i.getmStart() + " - " + i.getmEnd());
		// }
		mSelectionController.changeIntervals(intervals, "ProblemView");
	}


	private void updateStatistics() {
		if (!mIntervals.isEmpty()) {
			mInsideTimestamps = computeInsideTimestamps(mTimestamps, mIntervals);
		} else {
			mInsideTimestamps = mTimestamps;
		}
	}

	public static List<Long> computeInsideTimestamps(List<Long> timestamps,
			List<IntervalSelection> intervals) {
		List<Long> insideTimestamps = new ArrayList<Long>();

		int numTs = timestamps.size();
		int numInt = intervals.size();
		int iTs = 0;
		int iInt = 0;
		while (iTs < numTs && iInt < numInt) {
			long intBegin = intervals.get(iInt).getmStart();
			long intEnd = intervals.get(iInt).getmEnd();
			long ts = timestamps.get(iTs) + mStartDiff;
			if (ts >= intBegin && ts <= intEnd) {
				insideTimestamps.add(timestamps.get(iTs));
				iTs++;
			}
			if (ts < intBegin) {
				iTs++;
			}
			if (ts > intEnd) {
				iInt++;
			}
		}
		return insideTimestamps;
	}
    public void setMethodHandler(MethodHandler handler) {
        mMethodHandler = handler;
    }

	private MethodData[] findAllName(String query) {
    	List<MethodData> mds = new ArrayList<MethodData>(); 
    	
		MethodData md = mProfileProvider.findMatchingName(query);

		while (md != null) {
			mds.add(md);
			md = mProfileProvider.findNextMatchingName(query);
		}
    	
		return mds.toArray(new MethodData[0]);
    }

    private void findName(String query) {
        MethodData md = mProfileProvider.findMatchingName(query);
    }

    private void findNextName(String query) {
        MethodData md = mProfileProvider.findNextMatchingName(query);
    }

	enum Relation {
		LT, LTE, EQ, GTE, GT, NE;
		public static Relation value(String v) throws IllegalArgumentException {
			if (">".equals(v)) {
				return GT;
			}
			if (">=".equals(v)) {
				return GTE;
			}
			if ("<".equals(v)) {
				return LT;
			}
			if ("<=".equals(v)) {
				return LTE;
			}
			if ("<>".equals(v)) {
				return NE;
			}
			if ("=".equals(v)) {
				return EQ;
			}
			throw new IllegalArgumentException(v);
		}
	}

	private boolean eval(long a, Relation rel, long b) {
		switch (rel) {
		case EQ:
			return a == b;
		case GT:
			return a > b;
		case GTE:
			return a >= b;
		case LT:
			return a < b;
		case LTE:
			return a <= b;
		case NE:
			return a != b;
		default:
			return false;
		}
	}

	private boolean eval(double a, Relation rel, double b) {
		switch (rel) {
		case EQ:
			return a == b;
		case GT:
			return a > b;
		case GTE:
			return a >= b;
		case LT:
			return a < b;
		case LTE:
			return a <= b;
		case NE:
			return a != b;
		default:
			return false;
		}
	}
}
