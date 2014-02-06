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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

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
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.android.traceview.IntervalSelection.Action;
import com.android.traceview.ProblemConstraint.Relation;
import com.android.traceview.TimeLineView.Record;

import cz.cuni.kacz.contextlogger.LogType;

public class ProblemView extends Composite {
	
	private final CombinedReader mCombinedReader;

    private final SelectionController mSelectionController;
    private MethodData mCurrentHighlightedMethod;

	private MethodHandler mMethodHandler;

	private final Map<Integer, ContextLogData> mLogMap;
	private final ArrayList<Record> mTraceRecords;

	private final Color mColorNoMatch;
	private final Color mColorMatch;

	private final SimpleContentProposalProvider mScp;

	private Problem mProblem;
	private ProblemDefinition mProblemDefinition;
	
	private List<Problem> mProblems = new LinkedList<Problem>();
	private List<CombinedReader> mReaders = new ArrayList<CombinedReader>();
	private Map<CombinedReader,Problem> mReaderToProblemMap = new HashMap<CombinedReader, Problem>();
	
	// UI elements
	
    private final Shell parentShell;
    
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
	
	final org.eclipse.swt.widgets.List openedReadersList;

	protected ContextLogData mSelectedLog1;
	
	ProblemConstraint mProblemConstraint;
	String mFunctionName;
	String mLogname;

    public interface MethodHandler {
        void handleMethod(MethodData method);
    }

	public ProblemView(Composite parent, TraceReader reader,
			ContextLogReader logReader,
            SelectionController selectController, Shell shell) {
		super(parent, SWT.NONE);
		parentShell = shell;
		setLayout(new GridLayout(2, true));
        this.mSelectionController = selectController;

        mCombinedReader = new CombinedReader(reader, logReader);
        mReaders.add(mCombinedReader);
        
		Display display = getDisplay();

		mLogMap = logReader.getLogMap();

		mTraceRecords = reader.getThreadTimeRecords();

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
		RowLayout constraintLayout = new RowLayout();
		constraintLayout.pack = true;
		constraintLayout.type = SWT.HORIZONTAL;
		mConstraintComposite.setLayout(constraintLayout);

		Label constraintLabel = new Label(mConstraintComposite, SWT.NONE);
		constraintLabel.setText("Constraint:");

		logTypeCombo = new Combo(mConstraintComposite,
				SWT.DROP_DOWN
				| SWT.BORDER
				| SWT.READ_ONLY);
		for (Entry<Integer, ContextLogData> ld : mLogMap.entrySet()) {
			logTypeCombo.add(ld.getValue().getName(), ld.getKey());
			//System.out.println(ld.getValue().getName() + " " + ld.getKey());
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

		mResultBox = new Text(this, SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		mResultBox.setLayoutData(new GridData(GridData.FILL_BOTH));
		mResultBox.setEditable(false);

		final Button addProblems = new Button(formComposite, SWT.PUSH);
		addProblems.setText("Add more input");
		addProblems.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				System.out.println("OK");
				FileDialog fd = new FileDialog(parentShell, SWT.OPEN);
				fd.setText("Open context log files");
//				fd.setFilterPath("./");
				fd.setFilterExtensions( new String[] {"*.clog"} );
				String selected = fd.open();
				if(selected != null) {
					addReaders(fd.getFilterPath(),fd.getFileNames());
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				System.out.println("KO");
			}
		});
		
		openedReadersList = new org.eclipse.swt.widgets.List(this,SWT.SINGLE);
		openedReadersList.add("main");
		
		logTypeCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				int index = logTypeCombo.getSelectionIndex();
				System.out.println(index);
				mSelectedLog1 = mLogMap.get(index);
				if (mSelectedLog1 == null) {
					mLogname = null;
					System.out.println("null");
					return;
				}
				mLogname = mSelectedLog1.getName();
				switch (mSelectedLog1.getType()) {
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
					for (String s : mSelectedLog1.getStringDataMap().values()) {
						stringValueCombo.add(s);
					}
					mIntComposite.setLayoutData(hide);
					mFloatComposite.setLayoutData(hide);
					mStringComposite.setLayoutData(show);
					break;
				}
				updateProblems();
				constraintHolderComposite.pack();
				updateStatistics();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {

			}
		});
		
		intRelationCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {

				try {
					String relationStr = intRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					mProblemConstraint = new ProblemConstraint(rel, LogType.LONG, Long.valueOf(mIntValueBox.getText()));
					updateProblems();
//					updateIntervals(rel, Long.valueOf(mIntValueBox.getText()));
					updateStatistics();
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo1");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals, "ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo1");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
				}
			}

		});

		mIntValueBox.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {
				try {
					String relationStr = intRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					mProblemConstraint = new ProblemConstraint(rel, LogType.LONG, Long.valueOf(mIntValueBox.getText()));
					updateProblems();
//					updateIntervals(rel, Long.valueOf(mIntValueBox.getText()));
					updateStatistics();
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo2");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo2");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
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
					mProblemConstraint = new ProblemConstraint(rel, LogType.FLOAT, Double.valueOf(mFloatValueBox.getText()));
					updateProblems();
//					updateIntervals(rel,
//							Double.valueOf(mFloatValueBox.getText()));
					updateStatistics();
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo3");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo3");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
				}
			}
			
		});
		
		mFloatValueBox.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent arg0) {
				try {
					String relationStr = floatRelationCombo.getText();
					Relation rel = Relation.value(relationStr);
					mProblemConstraint = new ProblemConstraint(rel, LogType.FLOAT, Double.valueOf(mFloatValueBox.getText()));
					updateProblems();
//					updateIntervals(rel,
//							Double.valueOf(mFloatValueBox.getText()));
					updateStatistics();
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo4");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo4");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
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
					mProblemConstraint = new ProblemConstraint(rel, LogType.STRING, stringValueCombo.getText());
					updateProblems();
//					updateIntervals(rel, stringValueCombo.getText());
					updateStatistics();
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo5");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo5");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
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
					mProblemConstraint = new ProblemConstraint(rel, LogType.STRING, stringValueCombo.getText());
					updateProblems();
//					updateIntervals(rel, stringValueCombo.getText());
					updateStatistics();
				} catch (NumberFormatException e) {
					// boo
					System.out.println("boo6");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
				} catch (IllegalArgumentException e) {
					// foo
					System.out.println("foo6");
					mProblemConstraint = null;
					updateProblems();
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
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
					mProblemConstraint = null;
					mFunctionName = null;
					
					mProblemBox.setEnabled(true);
					logTypeCombo.setEnabled(true);
					mIntValueBox.setEnabled(true);
					mFloatValueBox.setEnabled(true);
					stringValueCombo.setEnabled(true);
					intRelationCombo.setEnabled(true);
					floatRelationCombo.setEnabled(true);
					stringRelationCombo.setEnabled(true);

					mProblemBox.setText("");
					
					updateProblems();
//					mIntervals.clear();
//					updateTimeStamps(null);
					updateStatistics();
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
					mFunctionName = ProblemDefinition.BRB_FUNCTION_NAME;
//					mProblemBox.setText("");
					mProblemBox.setEnabled(false);
					logTypeCombo.setEnabled(false);
					mIntValueBox.setEnabled(false);
					mFloatValueBox.setEnabled(false);
					stringValueCombo.setEnabled(false);
					intRelationCombo.setEnabled(false);
					floatRelationCombo.setEnabled(false);
					stringRelationCombo.setEnabled(false);

					mProblemConstraint = null;
					
//					mIntervals.clear();
//					mSelectionController.changeIntervals(mIntervals,
//							"ProblemView");
//					updateTimeStamps(BRBMethodData);
					updateProblems();
					updateStatistics();
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
					
					//TODO: make empty problem
					mFunctionName = null;
					updateProblems();
					
//					updateTimeStamps(null);
					return;
				}
				MethodData[] matchingMD = mCombinedReader.findAllMethodDataByName(query);

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
					mFunctionName = matchingMD[0].getName();
					updateProblems();
//					updateTimeStamps(matchingMD[0]);
					updateStatistics();
				}
			}
		});

    }

	protected void addReaders(String path, String[] fileNames) {
		for(String fileName : fileNames) {
			if(fileName.endsWith(".clog")) {
				try{
					CombinedReader r = new CombinedReader(path + File.separator + fileName.replace(".clog", ""));
					mReaders.add(r);
					if(this.mProblemDefinition != null) {
						Problem p = new Problem(mProblemDefinition, r);
						mProblems.add(p);
						mReaderToProblemMap.put(r, p);
					}
					openedReadersList.add(fileName);
				} catch (IllegalArgumentException e) {
					// file not exist, do nothing, continue with the next one
				}
			}
		}
	}

	protected void updateProblems() {
		
		ProblemDefinition olDef = this.mProblemDefinition;
		
		if(ProblemDefinition.BRB_FUNCTION_NAME.equals(this.mFunctionName)) {
			this.mProblemDefinition = new BRBProblemDefinition();
		} else if(this.mFunctionName == null 
					&& ( this.mLogname == null
					     || this.mProblemConstraint == null)) {
			this.mProblemDefinition = null;
		} else {
			this.mProblemDefinition = new DefaultProblemDefinition(mLogname, mProblemConstraint, mFunctionName);
		}
		
		for(Problem p: mProblems) {
			p.setProblemDefinition(mProblemDefinition);
		}
		for(CombinedReader r: mReaders) {
			if(!mReaderToProblemMap.containsKey(r)) {
				Problem p = new Problem(mProblemDefinition, r);
				mProblems.add(p);
				mReaderToProblemMap.put(r, p);
			}
		}
		
		if(this.mProblem == null) {
			this.mProblem = mReaderToProblemMap.get(this.mCombinedReader);
		}
		
		mSelectionController.changeIntervals(mProblem.getIntervals(), "ProblemView");
		mSelectionController.changeTimestamps(mProblem.getTimestamps(), "ProblemView");
		mSelectionController.changeInsideTimestamps(mProblem.getInsideTimestamps(), "ProblemView");		
	}

	private void updateStatistics() {
		
		mResultBox.setText("");
		
		List<ProblemOccurence> occurrences = new LinkedList<ProblemOccurence>();
		
		for(Problem p: mProblems) {
			List<ProblemOccurence> po = p.getOccurences();
			occurrences.addAll(po);
		}
		
		Map<String, ContextLogData> logMap = mProblem.getLogMapByName();
		
		for(Entry<String, ContextLogData> entry: logMap.entrySet()) {
			String name = entry.getKey();
			LogType type = entry.getValue().getType();
			
			mResultBox.append(name + ":\n");
			
			switch(type) {
			case INT:
			case LONG: {
				List<Long> values = new ArrayList<Long>();
				for(ProblemOccurence po : occurrences) {
					Object o = po.getContextValues().get(name);
					if(o == null) {
						continue;
					}
					if(!(o instanceof Integer) && !(o instanceof Long)) {
						throw new IllegalStateException();
					}
					
					long val = ((Number)o).longValue();
					values.add(val);
				}
				mResultBox.append(ProblemStatistics.computeLongStats(values));
				break;
			}
			case FLOAT:
			case DOUBLE: {
				List<Double> values = new ArrayList<Double>();
				for(ProblemOccurence po : occurrences) {
					Object o = po.getContextValues().get(name);
					if(o == null) {
						continue;
					}
					if(!(o instanceof Float) && !(o instanceof Double)) {
						throw new IllegalStateException();
					}
					
					double val = ((Number)o).doubleValue();
					values.add(val);
				}
				mResultBox.append(ProblemStatistics.computeDoubleStats(values));
				break;
			}
			case STRING: {
				List<String> values = new ArrayList<String>();
				for(ProblemOccurence po : occurrences) {
					Object o = po.getContextValues().get(name);
					if(o == null) {
						continue;
					}
					if(!(o instanceof String)) {
						throw new IllegalStateException();
					}
					
					values.add((String)o);
				}
				mResultBox.append(ProblemStatistics.computeStringStats(values));
				break;
			}
			}
		}
	}
		
    public void setMethodHandler(MethodHandler handler) {
        mMethodHandler = handler;
    }

	
}
