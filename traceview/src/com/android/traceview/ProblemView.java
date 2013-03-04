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
import java.util.Observable;
import java.util.Observer;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.viewers.StructuredSelection;
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

import com.android.traceview.TimeLineView.Record;

public class ProblemView extends Composite implements Observer {

    private final SelectionController mSelectionController;
    private final ProfileProvider mProfileProvider;
    private MethodData mCurrentHighlightedMethod;

	private MethodHandler mMethodHandler;

	private final Map<Integer, ContextLogData> mLogMap;
	private final ArrayList<Record> mTraceRecords;

	private ArrayList<Long> mTimestamps;
	private ArrayList<Long> mInsideTimestamps;

	private final ArrayList<IntervalSelection> mIntervals;

	private final Color mColorNoMatch;
	private final Color mColorMatch;

	private final SimpleContentProposalProvider mScp;

	private final MethodData BRBMethodData;

    public interface MethodHandler {
        void handleMethod(MethodData method);
    }

	public ProblemView(Composite parent, TraceReader reader,
			ContextLogReader logReader,
            SelectionController selectController) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout(2, true));
        this.mSelectionController = selectController;
        mSelectionController.addObserver(this);

		Display display = getDisplay();

		mProfileProvider = reader.getProfileProvider();

		mLogMap = logReader.getLogMap();

		mTraceRecords = reader.getThreadTimeRecords();

		BRBMethodData = null;
		mIntervals = new ArrayList<IntervalSelection>();
		mTimestamps = new ArrayList<Long>();
		mInsideTimestamps = new ArrayList<Long>();

		// Add container holding the problem definition form
		Composite formComposite = new Composite(this, SWT.NONE);
		formComposite.setLayout(new FillLayout(SWT.VERTICAL));
		formComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Add a label for the problem definer
		Label problemLabel = new Label(formComposite, SWT.NONE);
		problemLabel.setText("Problem:");

		// Add radio button for BRB option
		final Button BRBButton = new Button(formComposite, SWT.RADIO);
		BRBButton.setText("BigRedButton");

		// Add radio button for own problem definition option
		final Button problemDefButton = new Button(formComposite, SWT.RADIO);
		problemDefButton.setText("Function call + constraint");

		Composite composite = new Composite(formComposite, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		// composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Add a text box for searching for method names
		Label callLabel = new Label(composite, SWT.NONE);
		callLabel.setText("Function:");

		// Add a text box for searching for method names
		final Text mProblemBox = new Text(composite, SWT.BORDER);
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

		final Combo logTypeCombo = new Combo(mConstraintComposite,
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

		final Combo intCombo = new Combo(mIntComposite, SWT.DROP_DOWN
				| SWT.BORDER | SWT.READ_ONLY);
		intCombo.add("<");
		intCombo.add("<=");
		intCombo.add("=");
		intCombo.add(">=");
		intCombo.add(">");

		final Text mIntValueBox = new Text(mIntComposite, SWT.BORDER);

		// Composite holding controls for float type logs
		final Composite mFloatComposite = new Composite(
				constraintHolderComposite, SWT.NONE);
		mFloatComposite.setLayout(new FillLayout());

		final Combo floatCombo = new Combo(mFloatComposite, SWT.DROP_DOWN
				| SWT.BORDER | SWT.READ_ONLY);
		floatCombo.add("<");
		floatCombo.add(">");

		final Text mFloatValueBox = new Text(mFloatComposite, SWT.BORDER);

		// Composite holding controls for float type logs
		final Composite mStringComposite = new Composite(
				constraintHolderComposite, SWT.NONE);
		mStringComposite.setLayout(new FillLayout());

		final Combo stringCombo = new Combo(mStringComposite, SWT.DROP_DOWN
				| SWT.BORDER | SWT.READ_ONLY);
		stringCombo.add("=");
		stringCombo.add("<>");

		final Combo stringValueCombo = new Combo(mStringComposite,
				SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);

		mIntComposite.setLayoutData(hide);
		mFloatComposite.setLayoutData(hide);
		mStringComposite.setLayoutData(hide);

		// Add result text field to the right side

		Text mResultBox = new Text(this, SWT.MULTI | SWT.BORDER);
		mResultBox.setLayoutData(new GridData(GridData.FILL_BOTH));
		mResultBox.setEditable(false);

		logTypeCombo.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub

				int index = logTypeCombo.getSelectionIndex();
				System.out.println(index);
				ContextLogData cld = mLogMap.get(index);
				if (cld == null) {
					System.out.println("null");
					return;
				}
				switch (cld.getType()) {
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
					intCombo.setEnabled(true);
					floatCombo.setEnabled(true);
					stringCombo.setEnabled(true);

					mProblemBox.setText("");
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
					intCombo.setEnabled(false);
					floatCombo.setEnabled(false);
					stringCombo.setEnabled(false);

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


	private void updateStatistics() {
		if (!mIntervals.isEmpty()) {
			// computeInsideTimestamps();
		} else {
			mInsideTimestamps = mTimestamps;
		}
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
        selectMethod(md);
    }

    private void findNextName(String query) {
        MethodData md = mProfileProvider.findNextMatchingName(query);
        selectMethod(md);
    }

    private void selectMethod(MethodData md) {
        if (md == null) {
			// mSearchBox.setBackground(mColorNoMatch);
            return;
        }
		// mSearchBox.setBackground(mColorMatch);
        highlightMethod(md, false);
    }

    @Override
    public void update(Observable objservable, Object arg) {
        // Ignore updates from myself
		if (arg == "ProfileView" || arg == "ProblemView") {
			return;
		}
        // System.out.printf("profileview update from %s\n", arg);
        ArrayList<Selection> selections;
        selections = mSelectionController.getSelections();
        for (Selection selection : selections) {
            Selection.Action action = selection.getAction();
            if (action != Selection.Action.Highlight) {
				continue;
			}
            String name = selection.getName();
            if (name == "MethodData") {
                MethodData md = (MethodData) selection.getValue();
                highlightMethod(md, true);
                return;
            }
            if (name == "Call") {
                Call call = (Call) selection.getValue();
                MethodData md = call.getMethodData();
                highlightMethod(md, true);
                return;
            }
        }
    }

    private void highlightMethod(MethodData md, boolean clearSearch) {
        if (md == null) {
			return;
		}
        // Avoid an infinite recursion
        if (md == mCurrentHighlightedMethod) {
			return;
		}
        if (clearSearch) {
			// mSearchBox.setText("");
			// mSearchBox.setBackground(mColorMatch);
        }
        mCurrentHighlightedMethod = md;
		// mTreeViewer.collapseAll();
        // Expand this node and its children
        expandNode(md);
        StructuredSelection sel = new StructuredSelection(md);
		// mTreeViewer.setSelection(sel, true);
		// Tree tree = mTreeViewer.getTree();
		// TreeItem[] items = tree.getSelection();
		// if (items.length != 0) {
		// tree.setTopItem(items[0]);
		// // workaround a Mac bug by adding showItem().
		// tree.showItem(items[0]);
		// }
    }

    private void expandNode(MethodData md) {
        ProfileNode[] nodes = md.getProfileNodes();
		// mTreeViewer.setExpandedState(md, true);
		// // Also expand the "Parents" and "Children" nodes.
		// if (nodes != null) {
		// for (ProfileNode node : nodes) {
		// if (node.isRecursive() == false) {
		// mTreeViewer.setExpandedState(node, true);
		// }
		// }
		// }
    }
}
