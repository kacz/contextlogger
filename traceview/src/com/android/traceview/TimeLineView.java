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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Pattern;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;

import cz.cuni.kacz.contextlogger.LogType;

public class TimeLineView extends Composite implements Observer {

    private final HashMap<String, RowData> mRowByName;
    private RowData[] mRows;
    private Segment[] mSegments;
    private final HashMap<Integer, String> mThreadLabels;
    private final Timescale mTimescale;
    private final Surface mSurface;
    private final RowLabels mLabels;
    private final SashForm mSashForm;
    private int mScrollOffsetY;

	// >>>>>added
	private final HashMap<String, LogRowData> mLogRowByName;
	private LogRowData[] mLogRows;
	final SashForm mLogSashForm;
	private final LogRowLabels mLogLabels;
	private final LogSurface mLogSurface;
	private int mLogScrollOffsetY;

	private static final int logRowHeight = 30;
	private static final int logRowYMargin = 12;
	private static final int logRowYMarginHalf = logRowYMargin / 2;
	private static final int logRowLabelYMargin = 22;
	private static final int logRowLabelYMarginHalf = logRowLabelYMargin / 2;
	private static final int logRowYSpace = logRowHeight + logRowYMargin;

	private final long mStartDiff;
	// <<<<<<added

    public static final int PixelsPerTick = 50;
    private final TickScaler mScaleInfo = new TickScaler(0, 0, 0, PixelsPerTick);
    private static final int LeftMargin = 10; // blank space on left
    private static final int RightMargin = 60; // blank space on right

    private final Color mColorBlack;
    private final Color mColorGray;
    private final Color mColorDarkGray;
	private final Color mColorRed;
	private final Color mColorGreen;
    private final Color mColorForeground;
    private final Color mColorRowBack;
	private final Color mColorLogRowBack;
    private final Color mColorZoomSelection;
	private final Color mBigSashColor;
    private final FontRegistry mFontRegistry;

    /** vertical height of drawn blocks in each row */
    private static final int rowHeight = 20;

    /** the blank space between rows */
    private static final int rowYMargin = 12;
    private static final int rowYMarginHalf = rowYMargin / 2;

    /** total vertical space for row */
    private static final int rowYSpace = rowHeight + rowYMargin;
    private static final int majorTickLength = 8;
    private static final int minorTickLength = 4;
    private static final int timeLineOffsetY = 58;
    private static final int tickToFontSpacing = 2;

    /** start of first row */
    private static final int topMargin = 90;
    private int mMouseRow = -1;
    private int mNumRows;
    private int mStartRow;
    private int mEndRow;
    private final TraceUnits mUnits;
    private final String mClockSource;
    private final boolean mHaveCpuTime;
    private final boolean mHaveRealTime;
    private final int mSmallFontWidth;
    private final int mSmallFontHeight;
    private final SelectionController mSelectionController;
    private MethodData mHighlightMethodData;
    private Call mHighlightCall;
    private static final int MinInclusiveRange = 3;

	// <<<<<<<<added
	private int mMouseLogRow = -1;
	private int mNumLogRows;
	private int mStartLogRow;
	private int mEndLogRow;
	// >>>>>>>>added

    /** Setting the fonts looks good on Linux but bad on Macs */
    private final boolean mSetFonts = false;

    public static interface Block {
        public String getName();
        public MethodData getMethodData();
        public long getStartTime();
        public long getEndTime();
        public Color getColor();
        public double addWeight(int x, int y, double weight);
        public void clearWeight();
        public long getExclusiveCpuTime();
        public long getInclusiveCpuTime();
        public long getExclusiveRealTime();
        public long getInclusiveRealTime();
        public boolean isContextSwitch();
        public boolean isIgnoredBlock();
        public Block getParentBlock();
    }

	public static interface LogRow extends Row {
		public LogType getType();
	}

    public static interface Row {
        public int getId();
        public String getName();
    }

    public static class Record {
        Row row;
        Block block;

        public Record(Row row, Block block) {
            this.row = row;
            this.block = block;
        }
    }

	public static class LogRecord {
		ContextLogData row;

		public LogRecord(ContextLogData row) {
			this.row = row;
		}
	}

	public TimeLineView(Composite parent, TraceReader reader,
			ContextLogReader logReader,
            SelectionController selectionController) {
        super(parent, SWT.NONE);
        mRowByName = new HashMap<String, RowData>();
		mLogRowByName = new HashMap<String, LogRowData>();
        this.mSelectionController = selectionController;
        selectionController.addObserver(this);
        mUnits = reader.getTraceUnits();
        mClockSource = reader.getClockSource();
        mHaveCpuTime = reader.haveCpuTime();
        mHaveRealTime = reader.haveRealTime();
        mThreadLabels = reader.getThreadLabels();

        Display display = getDisplay();
        mColorGray = display.getSystemColor(SWT.COLOR_GRAY);
        mColorDarkGray = display.getSystemColor(SWT.COLOR_DARK_GRAY);
        mColorBlack = display.getSystemColor(SWT.COLOR_BLACK);
		mColorGreen = display.getSystemColor(SWT.COLOR_GREEN);
		mColorRed = display.getSystemColor(SWT.COLOR_RED);
        // mColorBackground = display.getSystemColor(SWT.COLOR_WHITE);
        mColorForeground = display.getSystemColor(SWT.COLOR_BLACK);
        mColorRowBack = new Color(display, 240, 240, 255);
		mColorLogRowBack = new Color(display, 215, 215, 255);
        mColorZoomSelection = new Color(display, 230, 230, 230);
		mBigSashColor = display.getSystemColor(SWT.COLOR_DARK_GRAY);

        mFontRegistry = new FontRegistry(display);
        mFontRegistry.put("small",  //$NON-NLS-1$
                new FontData[] { new FontData("Arial", 8, SWT.NORMAL) });  //$NON-NLS-1$
        mFontRegistry.put("courier8",  //$NON-NLS-1$
                new FontData[] { new FontData("Courier New", 8, SWT.BOLD) });  //$NON-NLS-1$
        mFontRegistry.put("medium",  //$NON-NLS-1$
                new FontData[] { new FontData("Courier New", 10, SWT.NORMAL) });  //$NON-NLS-1$

        Image image = new Image(display, new Rectangle(100, 100, 100, 100));
        GC gc = new GC(image);
        if (mSetFonts) {
            gc.setFont(mFontRegistry.get("small"));  //$NON-NLS-1$
        }
        mSmallFontWidth = gc.getFontMetrics().getAverageCharWidth();
        mSmallFontHeight = gc.getFontMetrics().getHeight();

        image.dispose();
        gc.dispose();

        setLayout(new FillLayout());

        SashForm mBigSashForm = new SashForm(this, SWT.VERTICAL);
        
        // Create a sash form for holding two canvas views, one for the
        // thread labels and one for the thread timeline.
        mSashForm = new SashForm(mBigSashForm, SWT.HORIZONTAL);
        mSashForm.setBackground(mColorGray);
        mSashForm.SASH_WIDTH = 3;

        // Create a composite for the left side of the sash
        Composite composite = new Composite(mSashForm, SWT.NONE);
        GridLayout layout = new GridLayout(1, true /* make columns equal width */);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 1;
        composite.setLayout(layout);

        // Create a blank corner space in the upper left corner
        BlankCorner corner = new BlankCorner(composite);
        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = topMargin;
        corner.setLayoutData(gridData);

        // Add the thread labels below the blank corner.
        mLabels = new RowLabels(composite);
        gridData = new GridData(GridData.FILL_BOTH);
        mLabels.setLayoutData(gridData);

        // Create another composite for the right side of the sash
        composite = new Composite(mSashForm, SWT.NONE);
        layout = new GridLayout(1, true /* make columns equal width */);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 1;
        composite.setLayout(layout);

        mTimescale = new Timescale(composite);
        gridData = new GridData(GridData.FILL_HORIZONTAL);
        gridData.heightHint = topMargin;
        mTimescale.setLayoutData(gridData);

        mSurface = new Surface(composite);
        gridData = new GridData(GridData.FILL_BOTH);
        mSurface.setLayoutData(gridData);
        mSashForm.setWeights(new int[] { 1, 5 });
        
		// >>>>>>>>>>>>>add

		mLogSashForm = new SashForm(mBigSashForm, SWT.HORIZONTAL);
		mLogSashForm.setBackground(mColorGray);
		mLogSashForm.setSashWidth(3);
        
        Composite logLabelsComposite = new Composite(mLogSashForm, SWT.NONE);
		layout = new GridLayout(1, true /* make columns equal width */);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 1;
		logLabelsComposite.setLayout(layout);
		// logLabelsComposite.setBackground(display.getSystemColor(SWT.COLOR_CYAN));
		mLogLabels = new LogRowLabels(logLabelsComposite);
        gridData = new GridData(GridData.FILL_BOTH);
        mLogLabels.setLayoutData(gridData);
        
		// final ScrollBar vLogBar = mLogLabels.getVerticalBar();
		// vLogBar.addListener(SWT.Selection, new Listener() {
		// @Override
		// public void handleEvent(Event e) {
		// mLogScrollOffsetY = vLogBar.getSelection();
		// Point dim = mSurface.getSize();
		// int newScrollOffsetY = computeVisibleLogRows(dim.y);
		// if (newScrollOffsetY != mLogScrollOffsetY) {
		// mLogScrollOffsetY = newScrollOffsetY;
		// vLogBar.setSelection(newScrollOffsetY);
		// }
		// mLogLabels.redraw();
		// }
		// });

        Composite logDataComposite = new Composite(mLogSashForm, SWT.NONE);
		layout = new GridLayout(1, true /* make columns equal width */);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 1;
		logDataComposite.setLayout(layout);
        logDataComposite.setBackground(display.getSystemColor(SWT.COLOR_RED));
		mLogSurface = new LogSurface(logDataComposite);// LogRowLabels(logDataComposite);
        gridData = new GridData(GridData.FILL_BOTH);
        mLogSurface.setLayoutData(gridData);
        mLogSashForm.setWeights(new int[] {1,5});
        
        mBigSashForm.setWeights(new int[] {5,2});
		mBigSashForm.setSashWidth(3);
		mBigSashForm.setBackground(mBigSashColor);
        
		// logLabelsComposite.addControlListener(new ControlListener() {
		//
		// @Override
		// public void controlResized(ControlEvent arg0) {
		// mSashForm.setWeights(mLogSashForm.getWeights());
		//
		// }
		//
		// @Override
		// public void controlMoved(ControlEvent arg0) {
		// // TODO Auto-generated method stub
		//
		// }
		// });

		mLogLabels.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent me) {
				mLogLabels.mouseMove(me);
			}
		});

		// listeners to synchronize the two horizontal slashes
		mLogLabels.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent arg0) {
				mSashForm.setWeights(mLogSashForm.getWeights());
				mLogSashForm.setSashWidth(3);
				mSashForm.setSashWidth(3);
				// test
				Point dim = mLogLabels.getSize();
				computeVisibleLogRows(dim.y);

			}

			@Override
			public void controlMoved(ControlEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
        

        corner.addControlListener(new ControlListener() {
			
			@Override
			public void controlResized(ControlEvent arg0) {
				mLogSashForm.setWeights(mSashForm.getWeights());
				mLogSashForm.setSashWidth(3);
				mSashForm.setSashWidth(3);
			}
			
			@Override
			public void controlMoved(ControlEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});

		final ScrollBar vLogBar = mLogSurface.getVerticalBar();
		vLogBar.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				mLogScrollOffsetY = vLogBar.getSelection();
				Point dim = mLogSurface.getSize();
				int newLogScrollOffsetY = computeVisibleLogRows(dim.y);
				if (newLogScrollOffsetY != mLogScrollOffsetY) {
					mLogScrollOffsetY = newLogScrollOffsetY;
					vLogBar.setSelection(newLogScrollOffsetY);
				}
				mLogLabels.redraw();
				mLogSurface.redraw();
			}
		});

		mLogSurface.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent me) {
				mLogSurface.mouseMove(me);
			}
		});

		mLogSurface.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent me) {
				mLogSurface.mouseUp(me);
			}

			@Override
			public void mouseDown(MouseEvent me) {
				mLogSurface.mouseDown(me);
			}

			@Override
			public void mouseDoubleClick(MouseEvent me) {
				// mLogSurface.mouseDoubleClick(me);
			}
		});

		mLogSurface.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event e) {
				Point dim = mLogSurface.getSize();

				// If we don't need the scroll bar then don't display it.
				if (dim.y >= mNumLogRows * logRowYSpace) {
					vLogBar.setVisible(false);
				} else {
					vLogBar.setVisible(true);
				}
				int newScrollOffsetY = computeVisibleLogRows(dim.y);
				if (newScrollOffsetY != mLogScrollOffsetY) {
					mLogScrollOffsetY = newScrollOffsetY;
					vLogBar.setSelection(newScrollOffsetY);
				}

				int spaceNeeded = mNumLogRows * logRowYSpace;
				vLogBar.setMaximum(spaceNeeded);
				vLogBar.setThumb(dim.y);

				mLogLabels.redraw();
				mLogSurface.redraw();
			}
		});

		mLogSurface.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent me) {
				mSurface.mouseScrolled(me);
			}
		});

		final ScrollBar hLogBar = mLogSurface.getHorizontalBar();
		hLogBar.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event e) {
				mSurface.setScaleFromHorizontalScrollBar(hLogBar.getSelection());
				mSurface.redraw();
				mLogSurface.redraw();
			}
		});

		// <<<<<<<<<Added

        final ScrollBar vBar = mSurface.getVerticalBar();
        vBar.addListener(SWT.Selection, new Listener() {
           @Override
        public void handleEvent(Event e) {
               mScrollOffsetY = vBar.getSelection();
               Point dim = mSurface.getSize();
               int newScrollOffsetY = computeVisibleRows(dim.y);
               if (newScrollOffsetY != mScrollOffsetY) {
                   mScrollOffsetY = newScrollOffsetY;
                   vBar.setSelection(newScrollOffsetY);
               }
               mLabels.redraw();
               mSurface.redraw();
           }
        });

        final ScrollBar hBar = mSurface.getHorizontalBar();
        hBar.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event e) {
                mSurface.setScaleFromHorizontalScrollBar(hBar.getSelection());
                mSurface.redraw();
				mLogSurface.redraw();
            }
        });

		// hBar on Surface is hidden, if LogSurface is present
		if (mLogSurface != null) {
			hBar.setVisible(false);
		}

        mSurface.addListener(SWT.Resize, new Listener() {
            @Override
            public void handleEvent(Event e) {
                Point dim = mSurface.getSize();

                // If we don't need the scroll bar then don't display it.
                if (dim.y >= mNumRows * rowYSpace) {
                    vBar.setVisible(false);
                } else {
                    vBar.setVisible(true);
                }
                int newScrollOffsetY = computeVisibleRows(dim.y);
                if (newScrollOffsetY != mScrollOffsetY) {
                    mScrollOffsetY = newScrollOffsetY;
                    vBar.setSelection(newScrollOffsetY);
                }

                int spaceNeeded = mNumRows * rowYSpace;
                vBar.setMaximum(spaceNeeded);
                vBar.setThumb(dim.y);

                mLabels.redraw();
                mSurface.redraw();
            }
        });

        mSurface.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent me) {
                mSurface.mouseUp(me);
            }

            @Override
            public void mouseDown(MouseEvent me) {
                mSurface.mouseDown(me);
            }

            @Override
            public void mouseDoubleClick(MouseEvent me) {
                mSurface.mouseDoubleClick(me);
            }
        });

        mSurface.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent me) {
                mSurface.mouseMove(me);
            }
        });

        mSurface.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseScrolled(MouseEvent me) {
                mSurface.mouseScrolled(me);
            }
        });

        mTimescale.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent me) {
                mTimescale.mouseUp(me);
            }

            @Override
            public void mouseDown(MouseEvent me) {
                mTimescale.mouseDown(me);
            }

            @Override
            public void mouseDoubleClick(MouseEvent me) {
                mTimescale.mouseDoubleClick(me);
            }
        });

        mTimescale.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent me) {
                mTimescale.mouseMove(me);
            }
        });

        mLabels.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent me) {
                mLabels.mouseMove(me);
            }
        });

		// debug
		System.out.println("trace start: " + reader.getStartTime());
		System.out.println("log start: " + logReader.getStartTime());
		mStartDiff = reader.getStartTime() - logReader.getStartTime();
		System.out.println("diff: " + mStartDiff);
        setData(reader.getThreadTimeRecords());
		setLogData(logReader.getLogRecords());
    }

    @Override
    public void update(Observable objservable, Object arg) {
        // Ignore updates from myself
        if (arg == "TimeLineView") {
			return;
		}
        // System.out.printf("timeline update from %s\n", arg);
        boolean foundHighlight = false;
        ArrayList<Selection> selections;
        selections = mSelectionController.getSelections();
        for (Selection selection : selections) {
            Selection.Action action = selection.getAction();
            if (action != Selection.Action.Highlight) {
				continue;
			}
            String name = selection.getName();
            // System.out.printf(" timeline highlight %s from %s\n", name, arg);
            if (name == "MethodData") {  //$NON-NLS-1$
                foundHighlight = true;
                mHighlightMethodData = (MethodData) selection.getValue();
                // System.out.printf(" method %s\n",
                // highlightMethodData.getName());
                mHighlightCall = null;
                startHighlighting();
            } else if (name == "Call") {  //$NON-NLS-1$
                foundHighlight = true;
                mHighlightCall = (Call) selection.getValue();
                // System.out.printf(" call %s\n", highlightCall.getName());
                mHighlightMethodData = null;
                startHighlighting();
            }
        }
        if (foundHighlight == false) {
			mSurface.clearHighlights();
		}
    }

	public void setLogData(ArrayList<LogRecord> logRecords) {
		if (logRecords == null) {
			logRecords = new ArrayList<LogRecord>();
		}

		for (LogRecord rec : logRecords) {
			ContextLogData row = rec.row;

			String rowName = row.getName();
			LogRowData rd = mLogRowByName.get(rowName);
			if (rd == null) {
				rd = new LogRowData(row);
				mLogRowByName.put(rowName, rd);
			}
			
		}

		// Sort the log rows into increasing id value
		Collection<LogRowData> rv = mLogRowByName.values();
		mLogRows = rv.toArray(new LogRowData[rv.size()]);

		Arrays.sort(mLogRows, new Comparator<LogRowData>() {
			@Override
			public int compare(LogRowData rd1, LogRowData rd2) {
				return rd1.mId - rd2.mId;
			}
		});

		mNumLogRows = mLogRows.length;

		// Assign ranks to the sorted rows
		// for (int ii = 0; ii < mLogRows.length; ++ii) {
		// mLogRows[ii].mRank = ii;
		// }
	}

    public void setData(ArrayList<Record> records) {
        if (records == null) {
			records = new ArrayList<Record>();
		}

        if (false) {
            System.out.println("TimelineView() list of records:");  //$NON-NLS-1$
            for (Record r : records) {
                System.out.printf("row '%s' block '%s' [%d, %d]\n", r.row  //$NON-NLS-1$
                        .getName(), r.block.getName(), r.block.getStartTime(),
                        r.block.getEndTime());
                if (r.block.getStartTime() > r.block.getEndTime()) {
                    System.err.printf("Error: block startTime > endTime\n");  //$NON-NLS-1$
                    System.exit(1);
                }
            }
        }

        // Sort the records into increasing start time, and decreasing end time
        Collections.sort(records, new Comparator<Record>() {
            @Override
            public int compare(Record r1, Record r2) {
                long start1 = r1.block.getStartTime();
                long start2 = r2.block.getStartTime();
                if (start1 > start2) {
					return 1;
				}
                if (start1 < start2) {
					return -1;
				}

                // The start times are the same, so compare the end times
                long end1 = r1.block.getEndTime();
                long end2 = r2.block.getEndTime();
                if (end1 > end2) {
					return -1;
				}
                if (end1 < end2) {
					return 1;
				}

                return 0;
            }
        });

        ArrayList<Segment> segmentList = new ArrayList<Segment>();

        // The records are sorted into increasing start time,
        // so the minimum start time is the start time of the first record.
        double minVal = 0;
        if (records.size() > 0) {
			minVal = records.get(0).block.getStartTime();
		}

        // Sum the time spent in each row and block, and
        // keep track of the maximum end time.
        double maxVal = 0;
        for (Record rec : records) {
            Row row = rec.row;
            Block block = rec.block;
            if (block.isIgnoredBlock()) {
                continue;
            }

            String rowName = row.getName();
            RowData rd = mRowByName.get(rowName);
            if (rd == null) {
                rd = new RowData(row);
                mRowByName.put(rowName, rd);
            }
            long blockStartTime = block.getStartTime();
            long blockEndTime = block.getEndTime();
            if (blockEndTime > rd.mEndTime) {
                long start = Math.max(blockStartTime, rd.mEndTime);
                rd.mElapsed += blockEndTime - start;
                rd.mEndTime = blockEndTime;
            }
            if (blockEndTime > maxVal) {
				maxVal = blockEndTime;
			}

            // Keep track of nested blocks by using a stack (for each row).
            // Create a Segment object for each visible part of a block.
            Block top = rd.top();
            if (top == null) {
                rd.push(block);
                continue;
            }

            long topStartTime = top.getStartTime();
            long topEndTime = top.getEndTime();
            if (topEndTime >= blockStartTime) {
                // Add this segment if it has a non-zero elapsed time.
                if (topStartTime < blockStartTime) {
                    Segment segment = new Segment(rd, top, topStartTime,
                            blockStartTime);
                    segmentList.add(segment);
                }

                // If this block starts where the previous (top) block ends,
                // then pop off the top block.
                if (topEndTime == blockStartTime) {
					rd.pop();
				}
                rd.push(block);
            } else {
                // We may have to pop several frames here.
                popFrames(rd, top, blockStartTime, segmentList);
                rd.push(block);
            }
        }

        // Clean up the stack of each row
        for (RowData rd : mRowByName.values()) {
            Block top = rd.top();
            popFrames(rd, top, Integer.MAX_VALUE, segmentList);
        }

        mSurface.setRange(minVal, maxVal);
        mSurface.setLimitRange(minVal, maxVal);

        // Sort the rows into decreasing elapsed time
        Collection<RowData> rv = mRowByName.values();
        mRows = rv.toArray(new RowData[rv.size()]);
        Arrays.sort(mRows, new Comparator<RowData>() {
            @Override
            public int compare(RowData rd1, RowData rd2) {
                return (int) (rd2.mElapsed - rd1.mElapsed);
            }
        });

        // Assign ranks to the sorted rows
        for (int ii = 0; ii < mRows.length; ++ii) {
            mRows[ii].mRank = ii;
        }

        // Compute the number of rows with data
        mNumRows = 0;
        for (int ii = 0; ii < mRows.length; ++ii) {
            if (mRows[ii].mElapsed == 0) {
				break;
			}
            mNumRows += 1;
        }

        // Sort the blocks into increasing rows, and within rows into
        // increasing start values.
        mSegments = segmentList.toArray(new Segment[segmentList.size()]);
        Arrays.sort(mSegments, new Comparator<Segment>() {
            @Override
            public int compare(Segment bd1, Segment bd2) {
                RowData rd1 = bd1.mRowData;
                RowData rd2 = bd2.mRowData;
                int diff = rd1.mRank - rd2.mRank;
                if (diff == 0) {
                    long timeDiff = bd1.mStartTime - bd2.mStartTime;
                    if (timeDiff == 0) {
						timeDiff = bd1.mEndTime - bd2.mEndTime;
					}
                    return (int) timeDiff;
                }
                return diff;
            }
        });

        if (false) {
            for (Segment segment : mSegments) {
                System.out.printf("seg '%s' [%6d, %6d] %s\n",
                        segment.mRowData.mName, segment.mStartTime,
                        segment.mEndTime, segment.mBlock.getName());
                if (segment.mStartTime > segment.mEndTime) {
                    System.err.printf("Error: segment startTime > endTime\n");
                    System.exit(1);
                }
            }
        }
    }

    private static void popFrames(RowData rd, Block top, long startTime,
            ArrayList<Segment> segmentList) {
        long topEndTime = top.getEndTime();
        long lastEndTime = top.getStartTime();
        while (topEndTime <= startTime) {
            if (topEndTime > lastEndTime) {
                Segment segment = new Segment(rd, top, lastEndTime, topEndTime);
                segmentList.add(segment);
                lastEndTime = topEndTime;
            }
            rd.pop();
            top = rd.top();
            if (top == null) {
				return;
			}
            topEndTime = top.getEndTime();
        }

        // If we get here, then topEndTime > startTime
        if (lastEndTime < startTime) {
            Segment bd = new Segment(rd, top, lastEndTime, startTime);
            segmentList.add(bd);
        }
    }

    private class RowLabels extends Canvas {

        /** The space between the row label and the sash line */
        private static final int labelMarginX = 2;

        public RowLabels(Composite parent) {
            super(parent, SWT.NO_BACKGROUND);
            addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent pe) {
                    draw(pe.display, pe.gc);
                }
            });
        }

        private void mouseMove(MouseEvent me) {
            int rownum = (me.y + mScrollOffsetY) / rowYSpace;
            if (mMouseRow != rownum) {
                mMouseRow = rownum;
                redraw();
                mSurface.redraw();
            }
        }

        private void draw(Display display, GC gc) {
            if (mSegments.length == 0) {
                // gc.setBackground(colorBackground);
                // gc.fillRectangle(getBounds());
                return;
            }
            Point dim = getSize();

            // Create an image for double-buffering
            Image image = new Image(display, getBounds());

            // Set up the off-screen gc
            GC gcImage = new GC(image);
            if (mSetFonts)
			 {
				gcImage.setFont(mFontRegistry.get("medium"));  //$NON-NLS-1$
			}

            if (mNumRows > 2) {
                // Draw the row background stripes
                gcImage.setBackground(mColorRowBack);
                for (int ii = 1; ii < mNumRows; ii += 2) {
                    RowData rd = mRows[ii];
                    int y1 = rd.mRank * rowYSpace - mScrollOffsetY;
                    gcImage.fillRectangle(0, y1, dim.x, rowYSpace);
                }
            }

            // Draw the row labels
            int offsetY = rowYMarginHalf - mScrollOffsetY;
            for (int ii = mStartRow; ii <= mEndRow; ++ii) {
                RowData rd = mRows[ii];
                int y1 = rd.mRank * rowYSpace + offsetY;
                Point extent = gcImage.stringExtent(rd.mName);
                int x1 = dim.x - extent.x - labelMarginX;
                gcImage.drawString(rd.mName, x1, y1, true);
            }

            // Draw a highlight box on the row where the mouse is.
            if (mMouseRow >= mStartRow && mMouseRow <= mEndRow) {
                gcImage.setForeground(mColorGray);
                int y1 = mMouseRow * rowYSpace - mScrollOffsetY;
                gcImage.drawRectangle(0, y1, dim.x, rowYSpace);
            }

            // Draw the off-screen buffer to the screen
            gc.drawImage(image, 0, 0);

            // Clean up
            image.dispose();
            gcImage.dispose();
        }
    }

	private class LogRowLabels extends Canvas {

		/** The space between the row label and the sash line */
		private static final int labelMarginX = 2;

		public LogRowLabels(Composite parent) {
			super(parent, SWT.NO_BACKGROUND);
			addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent pe) {
					draw(pe.display, pe.gc);
				}
			});
		}

		private void mouseMove(MouseEvent me) {
			int rownum = (me.y + mLogScrollOffsetY) / logRowYSpace;
			if (mMouseLogRow != rownum) {
				mMouseLogRow = rownum;
				redraw();
				mLogSurface.redraw();
			}
		}

		private void draw(Display display, GC gc) {
			// if (mSegments.length == 0) {
			// // gc.setBackground(colorBackground);
			// // gc.fillRectangle(getBounds());
			// return;
			// }
			Point dim = getSize();

			// Create an image for double-buffering
			Image image = new Image(display, getBounds());

			// Set up the off-screen gc
			GC gcImage = new GC(image);
			if (mSetFonts) {
				gcImage.setFont(mFontRegistry.get("medium")); //$NON-NLS-1$
			}

			if (mNumLogRows > 2) {
				// Draw the row background stripes
				gcImage.setBackground(mColorLogRowBack);
				for (int ii = 1; ii < mNumLogRows; ii += 2) {
					LogRowData rd = mLogRows[ii];
					// test
					int y1 = (ii - 1) * logRowYSpace - mLogScrollOffsetY;
					// int y1 = rd.mRank * rowYSpace - mLogScrollOffsetY;
					gcImage.fillRectangle(0, y1, dim.x, logRowYSpace);
				}
			}

			// Draw the row labels
			int offsetY = logRowLabelYMarginHalf - mLogScrollOffsetY;
			for (int ii = mStartLogRow; ii <= mEndLogRow; ++ii) {
				LogRowData rd = mLogRows[ii];
				// test
				int y1 = ii * logRowYSpace + offsetY;
				String label = "[" + rd.mId + "] " + rd.mName + " ("
						+ rd.mType.name() + ")";
				// int y1 = rd.mRank * rowYSpace + offsetY;
				Point extent = gcImage.stringExtent(label);
				int x1 = dim.x - extent.x - labelMarginX;
				gcImage.drawString(label, x1, y1, true);
			}

			// Draw a highlight box on the row where the mouse is.
			if (mMouseLogRow >= mStartLogRow && mMouseLogRow <= mEndLogRow) {
				gcImage.setForeground(mColorGray);
				int y1 = mMouseLogRow * logRowYSpace - mLogScrollOffsetY;
				gcImage.drawRectangle(0, y1, dim.x, logRowYSpace);
			}

			// Draw the off-screen buffer to the screen
			gc.drawImage(image, 0, 0);

			// Clean up
			image.dispose();
			gcImage.dispose();
		}
	}

    private class BlankCorner extends Canvas {
        public BlankCorner(Composite parent) {
            //super(parent, SWT.NO_BACKGROUND);
            super(parent, SWT.NONE);
            addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent pe) {
                    draw(pe.display, pe.gc);
                }
            });
        }

        private void draw(Display display, GC gc) {
            // Create a blank image and draw it to the canvas
            Image image = new Image(display, getBounds());
            gc.drawImage(image, 0, 0);

            // Clean up
            image.dispose();
        }
    }

    private class Timescale extends Canvas {
        private final Point mMouse = new Point(LeftMargin, 0);
        private final Cursor mZoomCursor;
        private String mMethodName = null;
        private Color mMethodColor = null;
        private String mDetails;
        private final int mMethodStartY;
        private final int mDetailsStartY;
        private int mMarkStartX;
        private int mMarkEndX;

        /** The space between the colored block and the method name */
        private static final int METHOD_BLOCK_MARGIN = 10;

        public Timescale(Composite parent) {
            //super(parent, SWT.NO_BACKGROUND);
            super(parent, SWT.NONE);
            Display display = getDisplay();
            mZoomCursor = new Cursor(display, SWT.CURSOR_SIZEWE);
            setCursor(mZoomCursor);
            mMethodStartY = mSmallFontHeight + 1;
            mDetailsStartY = mMethodStartY + mSmallFontHeight + 1;
            addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent pe) {
                    draw(pe.display, pe.gc);
                }
            });
        }

        public void setVbarPosition(int x) {
            mMouse.x = x;
        }

        public void setMarkStart(int x) {
            mMarkStartX = x;
        }

        public void setMarkEnd(int x) {
            mMarkEndX = x;
        }

        public void setMethodName(String name) {
            mMethodName = name;
        }

        public void setMethodColor(Color color) {
            mMethodColor = color;
        }

        public void setDetails(String details) {
            mDetails = details;
        }

        private void mouseMove(MouseEvent me) {
            me.y = -1;
            mSurface.mouseMove(me);
			mLogSurface.mouseMove(me);
        }

        private void mouseDown(MouseEvent me) {
            mSurface.startScaling(me.x);
            mSurface.redraw();
        }

        private void mouseUp(MouseEvent me) {
            mSurface.stopScaling(me.x);
        }

        private void mouseDoubleClick(MouseEvent me) {
            mSurface.resetScale();
            mSurface.redraw();
        }

        private void draw(Display display, GC gc) {
            Point dim = getSize();

            // Create an image for double-buffering
            Image image = new Image(display, getBounds());

            // Set up the off-screen gc
            GC gcImage = new GC(image);
            if (mSetFonts)
			 {
				gcImage.setFont(mFontRegistry.get("medium"));  //$NON-NLS-1$
			}

            if (mSurface.drawingSelection()) {
                drawSelection(display, gcImage);
            }

            drawTicks(display, gcImage);

            // Draw the vertical bar where the mouse is
            gcImage.setForeground(mColorDarkGray);
            gcImage.drawLine(mMouse.x, timeLineOffsetY, mMouse.x, dim.y);

            // Draw the current millseconds
            drawTickLegend(display, gcImage);

            // Draw the method name and color, if needed
            drawMethod(display, gcImage);

            // Draw the details, if needed
            drawDetails(display, gcImage);

            // Draw the off-screen buffer to the screen
            gc.drawImage(image, 0, 0);

            // Clean up
            image.dispose();
            gcImage.dispose();
        }

        private void drawSelection(Display display, GC gc) {
            Point dim = getSize();
            gc.setForeground(mColorGray);
            gc.drawLine(mMarkStartX, timeLineOffsetY, mMarkStartX, dim.y);
            gc.setBackground(mColorZoomSelection);
            int x, width;
            if (mMarkStartX < mMarkEndX) {
                x = mMarkStartX;
                width = mMarkEndX - mMarkStartX;
            } else {
                x = mMarkEndX;
                width = mMarkStartX - mMarkEndX;
            }
            if (width > 1) {
                gc.fillRectangle(x, timeLineOffsetY, width, dim.y);
            }
        }

        private void drawTickLegend(Display display, GC gc) {
            int mouseX = mMouse.x - LeftMargin;
            double mouseXval = mScaleInfo.pixelToValue(mouseX);
            String info = mUnits.labelledString(mouseXval);
            gc.setForeground(mColorForeground);
            gc.drawString(info, LeftMargin + 2, 1, true);

            // Display the maximum data value
            double maxVal = mScaleInfo.getMaxVal();
            info = mUnits.labelledString(maxVal);
            if (mClockSource != null) {
                info = String.format(" max %s (%s)", info, mClockSource);  //$NON-NLS-1$
            } else {
                info = String.format(" max %s ", info);  //$NON-NLS-1$
            }
            Point extent = gc.stringExtent(info);
            Point dim = getSize();
            int x1 = dim.x - RightMargin - extent.x;
            gc.drawString(info, x1, 1, true);
        }

        private void drawMethod(Display display, GC gc) {
            if (mMethodName == null) {
                return;
            }

            int x1 = LeftMargin;
            int y1 = mMethodStartY;
            gc.setBackground(mMethodColor);
            int width = 2 * mSmallFontWidth;
            gc.fillRectangle(x1, y1, width, mSmallFontHeight);
            x1 += width + METHOD_BLOCK_MARGIN;
            gc.drawString(mMethodName, x1, y1, true);
        }

        private void drawDetails(Display display, GC gc) {
            if (mDetails == null) {
                return;
            }

            int x1 = LeftMargin + 2 * mSmallFontWidth + METHOD_BLOCK_MARGIN;
            int y1 = mDetailsStartY;
            gc.drawString(mDetails, x1, y1, true);
        }

        private void drawTicks(Display display, GC gc) {
            Point dim = getSize();
            int y2 = majorTickLength + timeLineOffsetY;
            int y3 = minorTickLength + timeLineOffsetY;
            int y4 = y2 + tickToFontSpacing;
            gc.setForeground(mColorForeground);
            gc.drawLine(LeftMargin, timeLineOffsetY, dim.x - RightMargin,
                    timeLineOffsetY);
            double minVal = mScaleInfo.getMinVal();
            double maxVal = mScaleInfo.getMaxVal();
            double minMajorTick = mScaleInfo.getMinMajorTick();
            double tickIncrement = mScaleInfo.getTickIncrement();
            double minorTickIncrement = tickIncrement / 5;
            double pixelsPerRange = mScaleInfo.getPixelsPerRange();

            // Draw the initial minor ticks, if any
            if (minVal < minMajorTick) {
                gc.setForeground(mColorGray);
                double xMinor = minMajorTick;
                for (int ii = 1; ii <= 4; ++ii) {
                    xMinor -= minorTickIncrement;
                    if (xMinor < minVal) {
						break;
					}
                    int x1 = LeftMargin
                            + (int) (0.5 + (xMinor - minVal) * pixelsPerRange);
                    gc.drawLine(x1, timeLineOffsetY, x1, y3);
                }
            }

            if (tickIncrement <= 10) {
                // TODO avoid rendering the loop when tickIncrement is invalid. It can be zero
                // or too small.
                // System.out.println(String.format("Timescale.drawTicks error: tickIncrement=%1f", tickIncrement));
                return;
            }
            for (double x = minMajorTick; x <= maxVal; x += tickIncrement) {
                int x1 = LeftMargin
                        + (int) (0.5 + (x - minVal) * pixelsPerRange);

                // Draw a major tick
                gc.setForeground(mColorForeground);
                gc.drawLine(x1, timeLineOffsetY, x1, y2);
                if (x > maxVal) {
					break;
				}

                // Draw the tick text
                String tickString = mUnits.valueOf(x);
                gc.drawString(tickString, x1, y4, true);

                // Draw 4 minor ticks between major ticks
                gc.setForeground(mColorGray);
                double xMinor = x;
                for (int ii = 1; ii <= 4; ii++) {
                    xMinor += minorTickIncrement;
                    if (xMinor > maxVal) {
						break;
					}
                    x1 = LeftMargin
                            + (int) (0.5 + (xMinor - minVal) * pixelsPerRange);
                    gc.drawLine(x1, timeLineOffsetY, x1, y3);
                }
            }
        }
    }

    private static enum GraphicsState {
        Normal, Marking, Scaling, Animating, Scrolling
    };

	private class LogSurface extends Canvas {

		private final GraphicsState mGraphicsState;
		private int mCachedStartRow;
		private int mCachedEndRow;
		private double mCachedMinVal;
		private double mCachedMaxVal;
		private double mLimitMinVal;
		private double mLimitMaxVal;
		private final Point mMouse = new Point(LeftMargin, 0);
		private int mMouseMarkStartX;
		private int mMouseMarkEndX;

		private final Cursor mNormalCursor;
		private final Cursor mIncreasingCursor;
		private final Cursor mDecreasingCursor;

		// The minimum margin on each side of the zoom window, in pixels.
		private static final int MinZoomPixelMargin = 10;

		public LogSurface(Composite parent) {
			super(parent, SWT.NO_BACKGROUND | SWT.V_SCROLL | SWT.H_SCROLL);
			mGraphicsState = GraphicsState.Normal;

			Display display = getDisplay();

			mNormalCursor = new Cursor(display, SWT.CURSOR_CROSS);
			mIncreasingCursor = new Cursor(display, SWT.CURSOR_SIZEE);
			mDecreasingCursor = new Cursor(display, SWT.CURSOR_SIZEW);

			addPaintListener(new PaintListener() {
				@Override
				public void paintControl(PaintEvent pe) {
					draw(pe.display, pe.gc);
				}
			});

		}

		private void updateHorizontalScrollBar() {
			double minVal = mScaleInfo.getMinVal();
			double maxVal = mScaleInfo.getMaxVal();
			double visibleRange = maxVal - minVal;
			double fullRange = mSurface.mLimitMaxVal - mSurface.mLimitMinVal;

			ScrollBar hBar = getHorizontalBar();
			if (fullRange > visibleRange) {
				hBar.setVisible(true);
				hBar.setMinimum(0);
				hBar.setMaximum((int) Math.ceil(fullRange));
				hBar.setThumb((int) Math.ceil(visibleRange));
				hBar.setSelection((int) Math.floor(minVal
						- mSurface.mLimitMinVal));
			} else {
				hBar.setVisible(false);
			}
		}

		private boolean drawingSelection() {
			return mSurface.mGraphicsState == GraphicsState.Marking
					|| mSurface.mGraphicsState == GraphicsState.Animating;
		}

		private void drawSelection(Display display, GC gc) {
			Point dim = getSize();
			gc.setForeground(mColorGray);
			gc.drawLine(mSurface.mMouseMarkStartX, 0,
					mSurface.mMouseMarkStartX, dim.y);
			gc.setBackground(mColorZoomSelection);
			int width;
			int mouseX = (mSurface.mGraphicsState == GraphicsState.Animating) ? mSurface.mMouseMarkEndX
					: mMouse.x;
			int x;
			if (mSurface.mMouseMarkStartX < mouseX) {
				x = mSurface.mMouseMarkStartX;
				width = mouseX - mSurface.mMouseMarkStartX;
			} else {
				x = mouseX;
				width = mSurface.mMouseMarkStartX - mouseX;
			}
			gc.fillRectangle(x, 0, width, dim.y);
		}

		private void updateGraphs() {
			for (int ii = mStartLogRow; ii <= mEndLogRow; ++ii) {
				LogRowData rd = mLogRows[ii];

				try {
					switch (rd.mType) {
					case INT:
					{
						rd.mGraphData.clear();
						Long startKey = rd.mRow.getIntDataMap().floorKey(
										Math.round(mScaleInfo.getMinVal())
												+ mStartDiff);
						Long endKey = rd.mRow.getIntDataMap().ceilingKey(
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff);
						if (startKey == null) {
							startKey = Math.round(mScaleInfo.getMinVal())
									+ mStartDiff;
						}
						if (endKey == null) {
							endKey = Math.round(mScaleInfo.getMaxVal())
									+ mStartDiff;
						}

						for (Map.Entry<Long, Integer> e : rd.mRow
								.getIntDataMap().subMap(startKey, endKey + 1)
								.entrySet()) {
							int x = mScaleInfo.valueToPixel(e.getKey()
									- mStartDiff);

							int length = (int) Math
									.round((e.getValue() - rd.mRow
											.getMinValue())
											/ rd.mRow.getMaxDiff()
											* logRowHeight);
							rd.mGraphData.put(x + LeftMargin, length);
						}
						break;
					}
					case LONG:
					{
						rd.mGraphData.clear();
						Long startKey = rd.mRow.getLongDataMap()
								.floorKey(
										Math.round(mScaleInfo.getMinVal())
												+ mStartDiff);
						Long endKey = rd.mRow.getLongDataMap()
								.ceilingKey(
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff);
						if (startKey == null) {
							startKey = Math.round(mScaleInfo.getMinVal())
									+ mStartDiff;
						}
						if (endKey == null) {
							endKey = Math.round(mScaleInfo.getMaxVal())
									+ mStartDiff;
						}

						for (Map.Entry<Long, Long> e : rd.mRow.getLongDataMap()
								.subMap(startKey, endKey + 1).entrySet()) {
							int x = mScaleInfo.valueToPixel(e.getKey()
									- mStartDiff);

							int length = (int) Math
									.round((e.getValue() - rd.mRow
											.getMinValue())
											/ (rd.mRow.getMaxValue() - rd.mRow
													.getMinValue())
											* logRowHeight);
							rd.mGraphData.put(x + LeftMargin, length);
						}
						break;
					}
					case FLOAT:
					{
						rd.mGraphData.clear();
						Long startKey = rd.mRow.getFloatDataMap().floorKey(Math.round(mScaleInfo.getMinVal())+ mStartDiff);
						Long endKey = rd.mRow.getFloatDataMap()
								.higherKey(
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff);
						if (startKey == null) {
							startKey = Math.round(mScaleInfo.getMinVal())
									+ mStartDiff;
						}
						if (endKey == null) {
							endKey = Math.round(mScaleInfo.getMaxVal())
									+ mStartDiff;
						}
						// System.out.println("endKey: " + (endKey -
						// mStartDiff));

						for (Map.Entry<Long, Float> e : rd.mRow
								.getFloatDataMap().subMap(startKey, endKey + 1)
								.entrySet()) {
							int x = mScaleInfo.valueToPixel(e.getKey()
									- mStartDiff);

							int length = (int) Math
									.round((e.getValue() - rd.mRow
											.getMinValue())
											/ (rd.mRow.getMaxValue() - rd.mRow
													.getMinValue())
											* logRowHeight);
							// System.out.println("key: "
							// + (e.getKey() - mStartDiff) + " x: " + x
							// + " length: " + length);
							rd.mGraphData.put(x + LeftMargin, length);
							// System.out.println("x " + (x + LeftMargin)
							// + " len " + length);
						}
						if (rd.mGraphData.size() > 1) {
							int firstKey = rd.mGraphData.firstKey();
							int secKey = rd.mGraphData.higherKey(firstKey);
							int firstPix = mScaleInfo.valueToPixel(mScaleInfo
									.getMinVal()) + LeftMargin;
							int firstVal = rd.mGraphData.get(firstKey);
							int secVal = rd.mGraphData.get(secKey);
							if (firstKey < firstPix) {
								Entry<Integer, Integer> firstEntry = rd.mGraphData
										.firstEntry();
								Entry<Integer, Integer> secEntry = rd.mGraphData
										.higherEntry(firstEntry.getKey());
								int newVal = firstVal
										+ ((firstPix - firstKey)
												/ (secKey - firstKey) * (secVal - firstVal));
								rd.mGraphData.remove(firstKey);
								rd.mGraphData.put(firstPix, newVal);
							}

							int lastKey = rd.mGraphData.lastKey();
							int prevKey = rd.mGraphData.lowerKey(lastKey);
							int lastPix = mScaleInfo.valueToPixel(mScaleInfo
									.getMaxVal()) + LeftMargin;
							int lastVal = rd.mGraphData.get(lastKey);
							int prevVal = rd.mGraphData.get(prevKey);
							if (lastKey > lastPix) {
								Entry<Integer, Integer> lastEntry = rd.mGraphData
										.lastEntry();
								Entry<Integer, Integer> prevEntry = rd.mGraphData
										.lowerEntry(lastEntry.getKey());
								int newVal = lastVal
										+ ((lastPix - lastKey)
												/ (prevKey - lastKey) * (prevVal - lastVal));
								rd.mGraphData.remove(lastKey);
								rd.mGraphData.put(lastPix, newVal);
							}
							// System.out.println("firstKey:" + firstKey);
							// System.out.println("secKey:" + secKey);
							// System.out.println("firstpix:" + firstPix);
							//
							// System.out.println("lastKey:" + lastKey);
							// System.out.println("prevKey:" + prevKey);
							// System.out.println("lastpix:" + lastPix);
						}
						// for (Entry<Integer, Integer> e : rd.mGraphData
						// .entrySet()) {
						// System.out.println("x " + e.getKey() + " len "
						// + e.getValue());
						// }
						// System.out.println("=========");
						break;
					}
					case DOUBLE:
					{
						rd.mGraphData.clear();
						Long startKey = rd.mRow.getDoubleDataMap().floorKey(Math.round(mScaleInfo.getMinVal())+ mStartDiff);
						Long endKey = rd.mRow.getDoubleDataMap()
								.higherKey(
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff);
						if (startKey == null) {
							startKey = Math.round(mScaleInfo.getMinVal())
									+ mStartDiff;
						}
						if (endKey == null) {
							endKey = Math.round(mScaleInfo.getMaxVal())
									+ mStartDiff;
						}

						for (Map.Entry<Long, Double> e : rd.mRow
								.getDoubleDataMap()
								.subMap(startKey, endKey + 1).entrySet()) {
							int x = mScaleInfo.valueToPixel(e.getKey()
									- mStartDiff);

							int length = (int) Math
									.round((e.getValue() - rd.mRow
											.getMinValue())
											/ (rd.mRow.getMaxValue() - rd.mRow
													.getMinValue())
											* logRowHeight);
							rd.mGraphData.put(x + LeftMargin, length);
						}
						if (rd.mGraphData.size() > 1) {
							int firstKey = rd.mGraphData.firstKey();
							int secKey = rd.mGraphData.higherKey(firstKey);
							int firstPix = mScaleInfo.valueToPixel(mScaleInfo
									.getMinVal()) + LeftMargin;
							int firstVal = rd.mGraphData.get(firstKey);
							int secVal = rd.mGraphData.get(secKey);
							if (firstKey < firstPix) {
								Entry<Integer, Integer> firstEntry = rd.mGraphData
										.firstEntry();
								Entry<Integer, Integer> secEntry = rd.mGraphData
										.higherEntry(firstEntry.getKey());
								int newVal = firstVal
										+ ((firstPix - firstKey)
												/ (secKey - firstKey) * (secVal - firstVal));
								rd.mGraphData.remove(firstKey);
								rd.mGraphData.put(firstPix, newVal);
							}

							int lastKey = rd.mGraphData.lastKey();
							int prevKey = rd.mGraphData.lowerKey(lastKey);
							int lastPix = mScaleInfo.valueToPixel(mScaleInfo
									.getMaxVal()) + LeftMargin;
							int lastVal = rd.mGraphData.get(lastKey);
							int prevVal = rd.mGraphData.get(prevKey);
							if (lastKey > lastPix) {
								Entry<Integer, Integer> lastEntry = rd.mGraphData
										.lastEntry();
								Entry<Integer, Integer> prevEntry = rd.mGraphData
										.lowerEntry(lastEntry.getKey());
								int newVal = lastVal
										+ ((lastPix - lastKey)
												/ (prevKey - lastKey) * (prevVal - lastVal));
								rd.mGraphData.remove(lastKey);
								rd.mGraphData.put(lastPix, newVal);
							}

						}
						break;
					}
					case STRING:
					{
						rd.mGraphData.clear();
						Long startKey = rd.mRow.getStringDataMap()
								.floorKey(
										Math.round(mScaleInfo.getMinVal())
												+ mStartDiff);
						Long endKey = rd.mRow.getStringDataMap()
								.ceilingKey(
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff);
						if (startKey == null) {
							startKey = Math.round(mScaleInfo.getMinVal())
									+ mStartDiff;
						}
						if (endKey == null) {
							endKey = Math.round(mScaleInfo.getMaxVal())
									+ mStartDiff;
						}

						for (Map.Entry<Long, String> e : rd.mRow
								.getStringDataMap()
								.subMap(startKey, endKey + 1)
								.entrySet()) {
							int x = mScaleInfo.valueToPixel(e.getKey()
									- mStartDiff);
							rd.mGraphData.put(x + LeftMargin, 0);
						}

						int firstKey = rd.mGraphData.firstKey();
						int firstPix = mScaleInfo.valueToPixel(mScaleInfo
								.getMinVal()) + LeftMargin;

						if (firstKey < firstPix) {
							rd.mGraphData.remove(firstKey);
							rd.mGraphData.put(firstPix, 0);
						}
						int lastKey = rd.mGraphData.lastKey();
						int lastPix = mScaleInfo.valueToPixel(mScaleInfo
								.getMaxVal()) + LeftMargin;

						if (lastKey > lastPix) {
							rd.mGraphData.remove(lastKey);
							rd.mGraphData.put(lastPix, 0);
						}

						System.out.println("firstKey:" + firstKey);
						System.out.println("firstpix:" + firstPix);

						System.out.println("lastKey:" + lastKey);
						System.out.println("lastpix:" + lastPix);
						for (Entry<Integer, Integer> e : rd.mGraphData
								.entrySet()) {
							System.out.println("x " + e.getKey() + " len "
									+ e.getValue());
						}
						break;
					}
					}
				} catch (NoSuchElementException e) {

				}

			}
		}

		private void draw(Display display, GC gc) {
			// if (mSegments.length == 0) {
			// // gc.setBackground(colorBackground);
			// // gc.fillRectangle(getBounds());
			// return;
			// }

			// Create an image for double-buffering
			Image image = new Image(display, getBounds());

			// Set up the off-screen gc
			GC gcImage = new GC(image);
			if (mSetFonts) {
				gcImage.setFont(mFontRegistry.get("small")); //$NON-NLS-1$
			}

			// Draw the background
			// gcImage.setBackground(colorBackground);
			// gcImage.fillRectangle(image.getBounds());

			/*
			 * if (mGraphicsState == GraphicsState.Scaling) { double diff =
			 * mMouse.x - mMouseMarkStartX;to if (diff > 0) { double newMinVal =
			 * mScaleMinVal - diff / mScalePixelsPerRange; if (newMinVal <
			 * mLimitMinVal) { newMinVal = mLimitMinVal; }
			 * mScaleInfo.setMinVal(newMinVal); //
			 * System.out.printf("diff %f scaleMin %f newMin %f\n", // diff,
			 * scaleMinVal, newMinVal); } else if (diff < 0) { double newMaxVal
			 * = mScaleMaxVal - diff / mScalePixelsPerRange; if (newMaxVal >
			 * mLimitMaxVal) { newMaxVal = mLimitMaxVal; }
			 * mScaleInfo.setMaxVal(newMaxVal); //
			 * System.out.printf("diff %f scaleMax %f newMax %f\n", // diff,
			 * scaleMaxVal, newMaxVal); } }
			 */
			// Recompute the ticks and strips only if the size has changed,
			// or we scrolled so that a new row is visible.
			Point dim = getSize();

			if (mStartLogRow != mCachedStartRow || mEndLogRow != mCachedEndRow
					|| mScaleInfo.getMinVal() != mCachedMinVal
					|| mScaleInfo.getMaxVal() != mCachedMaxVal) {
				mCachedStartRow = mStartLogRow;
				mCachedEndRow = mEndLogRow;
				int xdim = dim.x - Surface.TotalXMargin;
				mScaleInfo.setNumPixels(xdim);
				boolean forceEndPoints = (mSurface.mGraphicsState == GraphicsState.Scaling
						|| mSurface.mGraphicsState == GraphicsState.Animating || mSurface.mGraphicsState == GraphicsState.Scrolling);
				mScaleInfo.computeTicks(forceEndPoints);
				mCachedMinVal = mScaleInfo.getMinVal();
				mCachedMaxVal = mScaleInfo.getMaxVal();
				if (mLimitMinVal > mScaleInfo.getMinVal()) {
					mLimitMinVal = mScaleInfo.getMinVal();
				}
				if (mLimitMaxVal < mScaleInfo.getMaxVal()) {
					mLimitMaxVal = mScaleInfo.getMaxVal();
				}

				// Compute the strips
				updateGraphs();

				// Update the horizontal scrollbar.
				updateHorizontalScrollBar();
			}

			if (mNumLogRows > 2) {
				// Draw the row background stripes
				gcImage.setBackground(mColorLogRowBack);
				for (int ii = 1; ii < mNumLogRows; ii += 2) {
					LogRowData rd = mLogRows[ii];
					int y1 = (ii - 1) * logRowYSpace - mLogScrollOffsetY;
					gcImage.fillRectangle(0, y1, dim.x, logRowYSpace);
				}
			}

			if (drawingSelection()) {
				drawSelection(display, gcImage);
			}
			/*
			 * String blockName = null; Color blockColor = null; String
			 * blockDetails = null;
			 */
			/*
			 * if (mDebug) { double pixelsPerRange =
			 * mScaleInfo.getPixelsPerRange(); System.out
			 * .printf("dim.x %d pixels %d minVal %f, maxVal %f ppr %f rpp %f\n"
			 * , dim.x, dim.x - TotalXMargin, mScaleInfo.getMinVal(),
			 * mScaleInfo.getMaxVal(), pixelsPerRange, 1.0 / pixelsPerRange); }
			 */
			/*
			 * // Draw the strips Block selectBlock = null; for (Strip strip :
			 * mStripList) { if (strip.mColor == null) { //
			 * System.out.printf("strip.color is null\n"); continue; }
			 * gcImage.setBackground(strip.mColor);
			 * gcImage.fillRectangle(strip.mX, strip.mY - mScrollOffsetY,
			 * strip.mWidth, strip.mHeight); if (mMouseRow ==
			 * strip.mRowData.mRank) { if (mMouse.x >= strip.mX && mMouse.x <
			 * strip.mX + strip.mWidth) { Block block = strip.mSegment.mBlock;
			 * blockName = block.getName(); blockColor = strip.mColor; if
			 * (mHaveCpuTime) { if (mHaveRealTime) { blockDetails =
			 * String.format( "excl cpu %s, incl cpu %s, " +
			 * "excl real %s, incl real %s", mUnits.labelledString(block
			 * .getExclusiveCpuTime()), mUnits.labelledString(block
			 * .getInclusiveCpuTime()), mUnits.labelledString(block
			 * .getExclusiveRealTime()), mUnits.labelledString(block
			 * .getInclusiveRealTime())); } else { blockDetails = String
			 * .format("excl cpu %s, incl cpu %s", mUnits.labelledString(block
			 * .getExclusiveCpuTime()), mUnits.labelledString(block
			 * .getInclusiveCpuTime())); } } else { blockDetails =
			 * String.format( "excl real %s, incl real %s", mUnits
			 * .labelledString(block .getExclusiveRealTime()),
			 * mUnits.labelledString(block .getInclusiveRealTime())); } } if
			 * (mMouseSelect.x >= strip.mX && mMouseSelect.x < strip.mX +
			 * strip.mWidth) { selectBlock = strip.mSegment.mBlock; } } }
			 * mMouseSelect.x = 0; mMouseSelect.y = 0;
			 */

			// Draw a highlight box on the row where the mouse is.
			// Except don't draw the box if we are animating the
			// highlighing of a call or method because the inclusive
			// highlight bar passes through the highlight box and
			// causes an annoying flashing artifact.
			if (mMouseLogRow >= 0 && mMouseLogRow < mNumLogRows) {
				gcImage.setForeground(mColorGray);
				int y1 = mMouseLogRow * logRowYSpace - mLogScrollOffsetY;
				gcImage.drawLine(0, y1, dim.x, y1);
				gcImage.drawLine(0, y1 + logRowYSpace, dim.x, y1 + logRowYSpace);
			}

			for (int ii = mStartLogRow; ii <= mEndLogRow; ++ii) {
				LogRowData rd = mLogRows[ii];

				int y1 = ii * logRowYSpace - mLogScrollOffsetY;

				long sizeAll = 0;
				long size = 0;
				long first = 0;
				long last = 0;

				try {
				switch(rd.mType) {
				case INT:
 {
						// debug
					sizeAll = rd.mRow.getIntDataMap().size();
					size = rd.mRow
							.getIntDataMap()
								.subMap(Math.round(mScaleInfo.getMinVal())
										+ mStartDiff,
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff).size();
						first = rd.mRow.getIntDataMap().firstKey() - mStartDiff;
						last = rd.mRow.getIntDataMap().lastKey() - mStartDiff;
						// -- debug

						gcImage.setForeground(mColorBlack);

						int lastX = -1;
						int lastY = -1;

						for (Map.Entry<Integer, Integer> e : rd.mGraphData
								.entrySet()) {
							if (lastX != -1 || lastY != -1) {
								gcImage.drawLine(lastX, y1 + logRowHeight
										+ logRowYMarginHalf - lastY,
										e.getKey(), y1 + logRowHeight
												+ logRowYMarginHalf
										- lastY);
								gcImage.drawLine(e.getKey(), y1 + logRowHeight
										+ logRowYMarginHalf - lastY,
										e.getKey(),
										y1 + logRowHeight + logRowYMarginHalf
										- e.getValue());
							} else {
								gcImage.drawLine(e.getKey(), y1 + logRowHeight
										+ logRowYMarginHalf,
										e.getKey(),
										y1 + logRowHeight + logRowYMarginHalf
												- e.getValue());
						}
							lastX = e.getKey();
							lastY = e.getValue();
						}

						break;
					}
				case LONG:
 {
						// debug
					sizeAll = rd.mRow.getLongDataMap().size();
					size = rd.mRow
							.getLongDataMap()
								.subMap(Math.round(mScaleInfo.getMinVal())
										+ mStartDiff,
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff).size();
						first = rd.mRow.getLongDataMap().firstKey()
								- mStartDiff;
						last = rd.mRow.getLongDataMap().lastKey() - mStartDiff;
						// -- debug

						gcImage.setForeground(mColorBlack);

						int lastX = -1;
						int lastY = -1;

						for (Map.Entry<Integer, Integer> e : rd.mGraphData
								.entrySet()) {
							if (lastX != -1 || lastY != -1) {
								gcImage.drawLine(lastX, y1 + logRowHeight
										+ logRowYMarginHalf - lastY,
										e.getKey(), y1 + logRowHeight
												+ logRowYMarginHalf
										- lastY);
								gcImage.drawLine(e.getKey(), y1 + logRowHeight
										+ logRowYMarginHalf - lastY,
										e.getKey(),
										y1 + logRowHeight + logRowYMarginHalf
										- e.getValue());
							} else {
								gcImage.drawLine(e.getKey(), y1 + logRowHeight
										+ logRowYMarginHalf,
										e.getKey(),
										y1 + logRowHeight + logRowYMarginHalf
												- e.getValue());
							}
							lastX = e.getKey();
							lastY = e.getValue();
						}

						break;
					}
				case FLOAT:
 {
						// debug
					sizeAll = rd.mRow.getFloatDataMap().size();
						size = rd.mGraphData.size();
						first = rd.mRow.getFloatDataMap().firstKey()
								- mStartDiff;
						last = rd.mRow.getFloatDataMap().lastKey() - mStartDiff;
						// -- debug

						int[] poly = { 0,
								y1 + logRowHeight + logRowYMarginHalf, 0,
								y1 + logRowHeight + logRowYMarginHalf, -1, -1,
								-1, -1 };

						Pattern p = new Pattern(display, 0f, y1, 0f, y1
								+ logRowYSpace, mColorRed, mColorGreen);
						gcImage.setBackgroundPattern(p);
						// gcImage.setForegroundPattern(p);
						for (Map.Entry<Integer, Integer> e : rd.mGraphData
								.entrySet()) {
							poly[0] = poly[6];
							poly[1] = poly[7];
							poly[2] = poly[4];
							poly[3] = poly[5];
							poly[4] = e.getKey();
							poly[5] = y1 + logRowHeight + logRowYMarginHalf;
							poly[6] = e.getKey();
							poly[7] = y1 + logRowHeight + logRowYMarginHalf
									- e.getValue();
							if (poly[1] != -1)
							 {
								gcImage.fillPolygon(poly);
								// gcImage.drawLine(poly[0], poly[1], poly[6],
								// poly[7]);
							}
						}
					break;
					}
				case DOUBLE:
 {
						// debug
					sizeAll = rd.mRow.getDoubleDataMap().size();
					size = rd.mRow
							.getDoubleDataMap()
								.subMap(Math.round(mScaleInfo.getMinVal())
										+ mStartDiff,
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff).size();
						first = rd.mRow.getDoubleDataMap().firstKey()
								- mStartDiff;
						last = rd.mRow.getDoubleDataMap().lastKey()
								- mStartDiff;
						// -- debug

						int[] poly = { 0,
								y1 + logRowHeight + logRowYMarginHalf, 0,
								y1 + logRowHeight + logRowYMarginHalf, -1, -1,
								-1, -1 };

						Pattern p = new Pattern(display, 0f, y1
								+ logRowYMarginHalf, 0f, y1 + logRowHeight
								+ logRowYMarginHalf, mColorRed, mColorGreen);
						gcImage.setBackgroundPattern(p);
						for (Map.Entry<Integer, Integer> e : rd.mGraphData
								.entrySet()) {
							poly[0] = poly[6];
							poly[1] = poly[7];
							poly[2] = poly[4];
							poly[3] = poly[5];
							poly[4] = e.getKey();
							poly[5] = y1 + logRowHeight + logRowYMarginHalf;
							poly[6] = e.getKey();
							poly[7] = y1 + logRowHeight + logRowYMarginHalf
									- e.getValue();
							if (poly[7] != -1)
							 {
								gcImage.fillPolygon(poly);
							}
						}
					break;
					}
				case STRING:
						// debug
					sizeAll = rd.mRow.getStringDataMap().size();
					size = rd.mRow
							.getStringDataMap()
								.subMap(Math.round(mScaleInfo.getMinVal())
										+ mStartDiff,
										Math.round(mScaleInfo.getMaxVal())
												+ mStartDiff).size();
						first = rd.mRow.getStringDataMap().firstKey()
								- mStartDiff;
						last = rd.mRow.getStringDataMap().lastKey()
								- mStartDiff;
						// -- debug

						int lastX = -1;
						int lastY = -1;

						gcImage.setBackground(mColorGray);
						gcImage.setForeground(mColorDarkGray);
						for (Map.Entry<Integer, Integer> e : rd.mGraphData
								.entrySet()) {
							int x = e.getKey();
							if (lastY != -1) {
								gcImage.fillGradientRectangle(lastX, y1
										+ logRowYMarginHalf, x - lastX,
										logRowHeight, false);
							}
							lastX = x;
							lastY = e.getValue();
						}
						if (lastY != -1) {
							gcImage.fillGradientRectangle(
									lastX,
									y1 + logRowYMarginHalf,
									mScaleInfo.valueToPixel(mScaleInfo
											.getMaxVal()) + LeftMargin - lastX,
									logRowHeight, false);
						}

						break;
				}
				} catch (NoSuchElementException e) {

				}
				
				String label = "[" + mScaleInfo.getMinVal() + " - "
						+ mScaleInfo.getMaxVal() + "] " + size + " " + sizeAll
						+ " " + first + " " + last + " " + " ("
						+ rd.mType.name() + ")";

				// int y1 = rd.mRank * rowYSpace + offsetY;
				Point extent = gcImage.stringExtent(label);
				int x1 = dim.x - extent.x;// - labelMarginX;
				gcImage.setForeground(mColorBlack);
				gcImage.drawString(label, x1, y1, true);
			}

			/*
			 * if (selectBlock != null) { ArrayList<Selection> selections = new
			 * ArrayList<Selection>(); // Get the row label RowData rd =
			 * mRows[mMouseRow]; selections.add(Selection.highlight("Thread",
			 * rd.mName)); //$NON-NLS-1$
			 * selections.add(Selection.highlight("Call", selectBlock));
			 * //$NON-NLS-1$
			 * 
			 * int mouseX = mMouse.x - LeftMargin; double mouseXval =
			 * mScaleInfo.pixelToValue(mouseX);
			 * selections.add(Selection.highlight("Time", mouseXval));
			 * //$NON-NLS-1$
			 * 
			 * mSelectionController.change(selections, "TimeLineView");
			 * //$NON-NLS-1$ mHighlightMethodData = null; mHighlightCall =
			 * (Call) selectBlock; startHighlighting(); }
			 */
			// Draw a vertical line where the mouse is.
			gcImage.setForeground(mColorDarkGray);
			int lineEnd = Math.min(dim.y, mNumLogRows * logRowYSpace);
			gcImage.drawLine(mMouse.x, 0, mMouse.x, lineEnd);

			if (mMouseLogRow != -1) {
				LogRowData ld = mLogRows[mMouseLogRow];
				String logDetails = null;
				long timeStamp = (long) mScaleInfo.pixelToValue(mMouse.x
						- LeftMargin)
						+ mStartDiff;
				switch (ld.mType) {
				case INT:
					Entry<Long, Integer> intEntry = ld.mRow.getIntDataMap()
							.floorEntry(timeStamp);
					if (intEntry != null) {
						logDetails = intEntry.getValue().toString();
					}
					break;
				case LONG:
					Entry<Long, Long> longEntry = ld.mRow.getLongDataMap()
							.floorEntry(timeStamp);
					if (longEntry != null) {
						logDetails = longEntry.getValue().toString();
					}
					break;
				case FLOAT:
					Entry<Long, Float> ceilingFloatEntry = ld.mRow
							.getFloatDataMap()
							.ceilingEntry(timeStamp);
					Entry<Long, Float> floorFloatEntry = ld.mRow
							.getFloatDataMap()
							.floorEntry(timeStamp);
					if (ceilingFloatEntry != null && floorFloatEntry != null) {
						float floatValue = floorFloatEntry.getValue()
								+ (float) (timeStamp - floorFloatEntry
										.getKey())
								/ (float) (ceilingFloatEntry.getKey() - floorFloatEntry
										.getKey())
								* (ceilingFloatEntry.getValue() - floorFloatEntry
										.getValue());
						logDetails = String.valueOf(floatValue);
						// debug
						// System.out.println("q "
						// + (timeStamp - floorFloatEntry.getKey())
						// + " / "
						// + (ceilingFloatEntry.getKey() - floorFloatEntry
						// .getKey()));
					}


					break;
				case DOUBLE:
					Entry<Long, Double> ceilingDoubleEntry = ld.mRow
							.getDoubleDataMap().ceilingEntry(timeStamp);
					Entry<Long, Double> floorDoubleEntry = ld.mRow
							.getDoubleDataMap()
							.floorEntry(timeStamp);
					if (ceilingDoubleEntry != null && floorDoubleEntry != null) {
						double doubleValue = floorDoubleEntry.getValue()
								+ (timeStamp - floorDoubleEntry.getKey())
								/ (ceilingDoubleEntry.getKey() - floorDoubleEntry
										.getKey())
								* ceilingDoubleEntry.getValue();
						logDetails = String.valueOf(doubleValue);
					}

					break;
				case STRING:
					Entry<Long, String> stringEntry = ld.mRow
							.getStringDataMap().floorEntry(timeStamp);
					if (stringEntry != null) {
						logDetails = stringEntry.getValue();
					}
					break;
				}

				mTimescale.setMethodName(ld.mName);
				mTimescale.setMethodColor(mColorBlack);
				mTimescale.setDetails(logDetails);
			} else {
				// mTimescale.setMethodName(null);
				// mTimescale.setMethodColor(null);
				// mTimescale.setDetails(null);
			}
			mTimescale.redraw();

			// Draw the off-screen buffer to the screen
			gc.drawImage(image, 0, 0);

			// Clean up
			image.dispose();
			gcImage.dispose();
		}

		private void mouseMove(MouseEvent me) {
			if (false) {
				if (mHighlightMethodData != null) {
					mHighlightMethodData = null;
					// Force a recomputation of the strip colors
					mCachedEndRow = -1;
				}
			}
			Point dim = mLogSurface.getSize();
			int x = me.x;
			if (x < LeftMargin) {
				x = LeftMargin;
			}
			if (x > dim.x - RightMargin) {
				x = dim.x - RightMargin;
			}
			mMouse.x = x;
			mMouse.y = me.y;
			mTimescale.setVbarPosition(x);
			if (mSurface.mGraphicsState == GraphicsState.Marking) {
				mTimescale.setMarkEnd(x);
			}

			/*
			 * if (mGraphicsState == GraphicsState.Normal) { // Set the cursor
			 * to the normal state. mLogSurface.setCursor(mNormalCursor); } else
			 * if (mGraphicsState == GraphicsState.Marking) { // Make the cursor
			 * point in the direction of the sweep if (mMouse.x >=
			 * mMouseMarkStartX) { mLogSurface.setCursor(mIncreasingCursor); }
			 * else { mLogSurface.setCursor(mDecreasingCursor); } }
			 */int rownum = (mMouse.y + mLogScrollOffsetY) / logRowYSpace;
			if (me.y < 0 || me.y >= dim.y) {
				rownum = -1;
			}
			if (mMouseLogRow != rownum) {
				mMouseLogRow = rownum;
				mLogLabels.redraw();
			}
			if (me.y != -1) {
				me.y = -1;
				mSurface.mouseMove(me);
			}
			redraw();
		}

		private void mouseDown(MouseEvent me) {
			mSurface.mouseDown(me);
			// Point dim = mLogSurface.getSize();
			// int x = me.x;
			// if (x < LeftMargin) {
			// x = LeftMargin;
			// }
			// if (x > dim.x - RightMargin) {
			// x = dim.x - RightMargin;
			// }
			// mSurface.mMouseMarkStartX = x;
			// mSurface.mGraphicsState = GraphicsState.Marking;
			// // mLogSurface.setCursor(mIncreasingCursor);
			// mTimescale.setMarkStart(mSurface.mMouseMarkStartX);
			// mTimescale.setMarkEnd(mSurface.mMouseMarkStartX);
			redraw();
		}

		private void mouseUp(MouseEvent me) {
			mLogSurface.setCursor(mSurface.mNormalCursor);
			mSurface.mouseUp(me);
			// if (mSurface.mGraphicsState != GraphicsState.Marking) {
			// mSurface.mGraphicsState = GraphicsState.Normal;
			// return;
			// }
			// mSurface.mGraphicsState = GraphicsState.Animating;
			// Point dim = mLogSurface.getSize();
			//
			// // If the user released the mouse outside the drawing area then
			// // cancel the zoom.
			// if (me.y <= 0 || me.y >= dim.y) {
			// mSurface.mGraphicsState = GraphicsState.Normal;
			// redraw();
			// return;
			// }
			//
			// int x = me.x;
			// if (x < LeftMargin) {
			// x = LeftMargin;
			// }
			// if (x > dim.x - RightMargin) {
			// x = dim.x - RightMargin;
			// }
			// mSurface.mMouseMarkEndX = x;
			//
			// // If the user clicked and released the mouse at the same point
			// // (+/- a pixel or two) then cancel the zoom (but select the
			// // method).
			// int dist = mSurface.mMouseMarkEndX - mSurface.mMouseMarkStartX;
			// if (dist < 0) {
			// dist = -dist;
			// }
			// // if (dist <= 2) {
			// // mGraphicsState = GraphicsState.Normal;
			// //
			// // // Select the method underneath the mouse
			// // mMouseSelect.x = mMouseMarkStartX;
			// // mMouseSelect.y = me.y;
			// // redraw();
			// // return;
			// // }
			//
			// // Make mouseEndX be the higher end point
			// if (mSurface.mMouseMarkEndX < mSurface.mMouseMarkStartX) {
			// int temp = mSurface.mMouseMarkEndX;
			// mSurface.mMouseMarkEndX = mSurface.mMouseMarkStartX;
			// mSurface.mMouseMarkStartX = temp;
			// }
			//
			// // If the zoom area is the whole window (or nearly the whole
			// // window) then cancel the zoom.
			// if (mSurface.mMouseMarkStartX <= LeftMargin
			// + mSurface.MinZoomPixelMargin
			// && mSurface.mMouseMarkEndX >= dim.x - RightMargin
			// - mSurface.MinZoomPixelMargin) {
			// mSurface.mGraphicsState = GraphicsState.Normal;
			// redraw();
			// return;
			// }
			//
			// // Compute some variables needed for zooming.
			// // It's probably easiest to explain by an example. There
			// // are two scales (or dimensions) involved: one for the pixels
			// // and one for the values (microseconds). To keep the example
			// // simple, suppose we have pixels in the range [0,16] and
			// // values in the range [100, 260], and suppose the user
			// // selects a zoom window from pixel 4 to pixel 8.
			// //
			// // usec: 100 140 180 260
			// // |-------|ZZZZZZZ|---------------|
			// // pixel: 0 4 8 16
			// //
			// // I've drawn the pixels starting at zero for simplicity, but
			// // in fact the drawable area is offset from the left margin
			// // by the value of "LeftMargin".
			// //
			// // The "pixels-per-range" (ppr) in this case is 0.1 (a tenth of
			// // a pixel per usec). What we want is to redraw the screen in
			// // several steps, each time increasing the zoom window until the
			// // zoom window fills the screen. For simplicity, assume that
			// // we want to zoom in four equal steps. Then the snapshots
			// // of the screen at each step would look something like this:
			// //
			// // usec: 100 140 180 260
			// // |-------|ZZZZZZZ|---------------|
			// // pixel: 0 4 8 16
			// //
			// // usec: ? 140 180 ?
			// // |-----|ZZZZZZZZZZZZZ|-----------|
			// // pixel: 0 3 10 16
			// //
			// // usec: ? 140 180 ?
			// // |---|ZZZZZZZZZZZZZZZZZZZ|-------|
			// // pixel: 0 2 12 16
			// //
			// // usec: ?140 180 ?
			// // |-|ZZZZZZZZZZZZZZZZZZZZZZZZZ|---|
			// // pixel: 0 1 14 16
			// //
			// // usec: 140 180
			// // |ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ|
			// // pixel: 0 16
			// //
			// // The problem is how to compute the endpoints (denoted by ?)
			// // for each step. This is a little tricky. We first need to
			// // compute the "fixed point": this is the point in the selection
			// // that doesn't move left or right. Then we can recompute the
			// // "ppr" (pixels per range) at each step and then find the
			// // endpoints. The computation of the end points is done
			// // in animateZoom(). This method computes the fixed point
			// // and some other variables needed in animateZoom().
			//
			// double minVal = mScaleInfo.getMinVal();
			// double maxVal = mScaleInfo.getMaxVal();
			// double ppr = mScaleInfo.getPixelsPerRange();
			// mSurface.mZoomMin = minVal
			// + ((mSurface.mMouseMarkStartX - LeftMargin) / ppr);
			// mSurface.mZoomMax = minVal
			// + ((mSurface.mMouseMarkEndX - LeftMargin) / ppr);
			//
			// // Clamp the min and max values to the actual data min and max
			// if (mSurface.mZoomMin < mSurface.mMinDataVal) {
			// mSurface.mZoomMin = mSurface.mMinDataVal;
			// }
			// if (mSurface.mZoomMax > mSurface.mMaxDataVal) {
			// mSurface.mZoomMax = mSurface.mMaxDataVal;
			// }
			//
			// // Snap the min and max points to the grid determined by the
			// // TickScaler
			// // before we zoom.
			// int xdim = dim.x - TotalXMargin;
			// TickScaler scaler = new TickScaler(mZoomMin, mZoomMax, xdim,
			// PixelsPerTick);
			// scaler.computeTicks(false);
			// mZoomMin = scaler.getMinVal();
			// mZoomMax = scaler.getMaxVal();
			//
			// // Also snap the mouse points (in pixel space) to be consistent
			// with
			// // zoomMin and zoomMax (in value space).
			// mMouseMarkStartX = (int) ((mZoomMin - minVal) * ppr +
			// LeftMargin);
			// mMouseMarkEndX = (int) ((mZoomMax - minVal) * ppr + LeftMargin);
			// mTimescale.setMarkStart(mMouseMarkStartX);
			// mTimescale.setMarkEnd(mMouseMarkEndX);
			//
			// // Compute the mouse selection end point distances
			// mMouseEndDistance = dim.x - RightMargin - mMouseMarkEndX;
			// mMouseStartDistance = mMouseMarkStartX - LeftMargin;
			// mZoomMouseStart = mMouseMarkStartX;
			// mZoomMouseEnd = mMouseMarkEndX;
			// mZoomStep = 0;
			//
			// // Compute the fixed point in both value space and pixel space.
			// mMin2ZoomMin = mZoomMin - minVal;
			// mZoomMax2Max = maxVal - mZoomMax;
			// mZoomFixed = mZoomMin + (mZoomMax - mZoomMin) * mMin2ZoomMin
			// / (mMin2ZoomMin + mZoomMax2Max);
			// mZoomFixedPixel = (mZoomFixed - minVal) * ppr + LeftMargin;
			// mFixedPixelStartDistance = mZoomFixedPixel - LeftMargin;
			// mFixedPixelEndDistance = dim.x - RightMargin - mZoomFixedPixel;
			//
			// mZoomMin2Fixed = mZoomFixed - mZoomMin;
			// mFixed2ZoomMax = mZoomMax - mZoomFixed;
			//
			// getDisplay().timerExec(ZOOM_TIMER_INTERVAL, mZoomAnimator);
			redraw();
			update();
		}
	}

    private class Surface extends Canvas {

        public Surface(Composite parent) {
            super(parent, SWT.NO_BACKGROUND | SWT.V_SCROLL | SWT.H_SCROLL);
            Display display = getDisplay();
            mNormalCursor = new Cursor(display, SWT.CURSOR_CROSS);
            mIncreasingCursor = new Cursor(display, SWT.CURSOR_SIZEE);
            mDecreasingCursor = new Cursor(display, SWT.CURSOR_SIZEW);

            initZoomFractionsWithExp();

            addPaintListener(new PaintListener() {
                @Override
                public void paintControl(PaintEvent pe) {
                    draw(pe.display, pe.gc);
                }
            });

            mZoomAnimator = new Runnable() {
                @Override
                public void run() {
                    animateZoom();
                }
            };

            mHighlightAnimator = new Runnable() {
                @Override
                public void run() {
                    animateHighlight();
                }
            };
        }

        private void initZoomFractionsWithExp() {
            mZoomFractions = new double[ZOOM_STEPS];
            int next = 0;
            for (int ii = 0; ii < ZOOM_STEPS / 2; ++ii, ++next) {
                mZoomFractions[next] = (double) (1 << ii)
                        / (double) (1 << (ZOOM_STEPS / 2));
                // System.out.printf("%d %f\n", next, zoomFractions[next]);
            }
            for (int ii = 2; ii < 2 + ZOOM_STEPS / 2; ++ii, ++next) {
                mZoomFractions[next] = (double) ((1 << ii) - 1)
                        / (double) (1 << ii);
                // System.out.printf("%d %f\n", next, zoomFractions[next]);
            }
        }

        @SuppressWarnings("unused")
        private void initZoomFractionsWithSinWave() {
            mZoomFractions = new double[ZOOM_STEPS];
            for (int ii = 0; ii < ZOOM_STEPS; ++ii) {
                double offset = Math.PI * ii / ZOOM_STEPS;
                mZoomFractions[ii] = (Math.sin((1.5 * Math.PI + offset)) + 1.0) / 2.0;
                // System.out.printf("%d %f\n", ii, zoomFractions[ii]);
            }
        }

        public void setRange(double minVal, double maxVal) {
            mMinDataVal = minVal;
            mMaxDataVal = maxVal;
            mScaleInfo.setMinVal(minVal);
            mScaleInfo.setMaxVal(maxVal);
        }

        public void setLimitRange(double minVal, double maxVal) {
            mLimitMinVal = minVal;
            mLimitMaxVal = maxVal;
        }

        public void resetScale() {
            mScaleInfo.setMinVal(mLimitMinVal);
            mScaleInfo.setMaxVal(mLimitMaxVal);
        }

        public void setScaleFromHorizontalScrollBar(int selection) {
            double minVal = mScaleInfo.getMinVal();
            double maxVal = mScaleInfo.getMaxVal();
            double visibleRange = maxVal - minVal;

            minVal = mLimitMinVal + selection;
            maxVal = minVal + visibleRange;
            if (maxVal > mLimitMaxVal) {
                maxVal = mLimitMaxVal;
                minVal = maxVal - visibleRange;
            }
            mScaleInfo.setMinVal(minVal);
            mScaleInfo.setMaxVal(maxVal);

            mGraphicsState = GraphicsState.Scrolling;
        }

        private void updateHorizontalScrollBar() {
            double minVal = mScaleInfo.getMinVal();
            double maxVal = mScaleInfo.getMaxVal();
            double visibleRange = maxVal - minVal;
            double fullRange = mLimitMaxVal - mLimitMinVal;

            ScrollBar hBar = getHorizontalBar();
            if (fullRange > visibleRange) {
                hBar.setVisible(true);
                hBar.setMinimum(0);
                hBar.setMaximum((int)Math.ceil(fullRange));
                hBar.setThumb((int)Math.ceil(visibleRange));
                hBar.setSelection((int)Math.floor(minVal - mLimitMinVal));
            } else {
                hBar.setVisible(false);
            }
        }

        private void draw(Display display, GC gc) {
            if (mSegments.length == 0) {
                // gc.setBackground(colorBackground);
                // gc.fillRectangle(getBounds());
                return;
            }

            // Create an image for double-buffering
            Image image = new Image(display, getBounds());

            // Set up the off-screen gc
            GC gcImage = new GC(image);
            if (mSetFonts)
			 {
				gcImage.setFont(mFontRegistry.get("small"));  //$NON-NLS-1$
			}

            // Draw the background
            // gcImage.setBackground(colorBackground);
            // gcImage.fillRectangle(image.getBounds());

            if (mGraphicsState == GraphicsState.Scaling) {
                double diff = mMouse.x - mMouseMarkStartX;
                if (diff > 0) {
                    double newMinVal = mScaleMinVal - diff / mScalePixelsPerRange;
                    if (newMinVal < mLimitMinVal) {
						newMinVal = mLimitMinVal;
					}
                    mScaleInfo.setMinVal(newMinVal);
                    // System.out.printf("diff %f scaleMin %f newMin %f\n",
                    // diff, scaleMinVal, newMinVal);
                } else if (diff < 0) {
                    double newMaxVal = mScaleMaxVal - diff / mScalePixelsPerRange;
                    if (newMaxVal > mLimitMaxVal) {
						newMaxVal = mLimitMaxVal;
					}
                    mScaleInfo.setMaxVal(newMaxVal);
                    // System.out.printf("diff %f scaleMax %f newMax %f\n",
                    // diff, scaleMaxVal, newMaxVal);
                }
            }

            // Recompute the ticks and strips only if the size has changed,
            // or we scrolled so that a new row is visible.
            Point dim = getSize();
            if (mStartRow != mCachedStartRow || mEndRow != mCachedEndRow
                    || mScaleInfo.getMinVal() != mCachedMinVal
                    || mScaleInfo.getMaxVal() != mCachedMaxVal) {
                mCachedStartRow = mStartRow;
                mCachedEndRow = mEndRow;
                int xdim = dim.x - TotalXMargin;
                mScaleInfo.setNumPixels(xdim);
                boolean forceEndPoints = (mGraphicsState == GraphicsState.Scaling
                        || mGraphicsState == GraphicsState.Animating
                        || mGraphicsState == GraphicsState.Scrolling);
                mScaleInfo.computeTicks(forceEndPoints);
                mCachedMinVal = mScaleInfo.getMinVal();
                mCachedMaxVal = mScaleInfo.getMaxVal();
                if (mLimitMinVal > mScaleInfo.getMinVal()) {
					mLimitMinVal = mScaleInfo.getMinVal();
				}
                if (mLimitMaxVal < mScaleInfo.getMaxVal()) {
					mLimitMaxVal = mScaleInfo.getMaxVal();
				}

                // Compute the strips
                computeStrips();

                // Update the horizontal scrollbar.
				if (mLogSurface == null) {
					updateHorizontalScrollBar();
				}
            }

            if (mNumRows > 2) {
                // Draw the row background stripes
                gcImage.setBackground(mColorRowBack);
                for (int ii = 1; ii < mNumRows; ii += 2) {
                    RowData rd = mRows[ii];
                    int y1 = rd.mRank * rowYSpace - mScrollOffsetY;
                    gcImage.fillRectangle(0, y1, dim.x, rowYSpace);
                }
            }

            if (drawingSelection()) {
                drawSelection(display, gcImage);
            }

            String blockName = null;
            Color blockColor = null;
            String blockDetails = null;

            if (mDebug) {
                double pixelsPerRange = mScaleInfo.getPixelsPerRange();
                System.out
                        .printf(
                                "dim.x %d pixels %d minVal %f, maxVal %f ppr %f rpp %f\n",
                                dim.x, dim.x - TotalXMargin, mScaleInfo
                                        .getMinVal(), mScaleInfo.getMaxVal(),
                                pixelsPerRange, 1.0 / pixelsPerRange);
            }

            // Draw the strips
            Block selectBlock = null;
            for (Strip strip : mStripList) {
                if (strip.mColor == null) {
                    // System.out.printf("strip.color is null\n");
                    continue;
                }
                gcImage.setBackground(strip.mColor);
                gcImage.fillRectangle(strip.mX, strip.mY - mScrollOffsetY, strip.mWidth,
                        strip.mHeight);
                if (mMouseRow == strip.mRowData.mRank) {
                    if (mMouse.x >= strip.mX
                            && mMouse.x < strip.mX + strip.mWidth) {
                        Block block = strip.mSegment.mBlock;
                        blockName = block.getName();
                        blockColor = strip.mColor;
                        if (mHaveCpuTime) {
                            if (mHaveRealTime) {
                                blockDetails = String.format(
                                        "excl cpu %s, incl cpu %s, "
                                        + "excl real %s, incl real %s",
                                        mUnits.labelledString(block.getExclusiveCpuTime()),
                                        mUnits.labelledString(block.getInclusiveCpuTime()),
                                        mUnits.labelledString(block.getExclusiveRealTime()),
                                        mUnits.labelledString(block.getInclusiveRealTime()));
                            } else {
                                blockDetails = String.format(
                                        "excl cpu %s, incl cpu %s",
                                        mUnits.labelledString(block.getExclusiveCpuTime()),
                                        mUnits.labelledString(block.getInclusiveCpuTime()));
                            }
                        } else {
                            blockDetails = String.format(
                                    "excl real %s, incl real %s",
                                    mUnits.labelledString(block.getExclusiveRealTime()),
                                    mUnits.labelledString(block.getInclusiveRealTime()));
                        }
                    }
                    if (mMouseSelect.x >= strip.mX
                            && mMouseSelect.x < strip.mX + strip.mWidth) {
                        selectBlock = strip.mSegment.mBlock;
                    }
                }
            }
            mMouseSelect.x = 0;
            mMouseSelect.y = 0;

            if (selectBlock != null) {
                ArrayList<Selection> selections = new ArrayList<Selection>();
                // Get the row label
                RowData rd = mRows[mMouseRow];
                selections.add(Selection.highlight("Thread", rd.mName));  //$NON-NLS-1$
                selections.add(Selection.highlight("Call", selectBlock));  //$NON-NLS-1$

                int mouseX = mMouse.x - LeftMargin;
                double mouseXval = mScaleInfo.pixelToValue(mouseX);
                selections.add(Selection.highlight("Time", mouseXval));  //$NON-NLS-1$

                mSelectionController.change(selections, "TimeLineView");  //$NON-NLS-1$
                mHighlightMethodData = null;
                mHighlightCall = (Call) selectBlock;
                startHighlighting();
            }

            // Draw a highlight box on the row where the mouse is.
            // Except don't draw the box if we are animating the
            // highlighing of a call or method because the inclusive
            // highlight bar passes through the highlight box and
            // causes an annoying flashing artifact.
            if (mMouseRow >= 0 && mMouseRow < mNumRows && mHighlightStep == 0) {
                gcImage.setForeground(mColorGray);
                int y1 = mMouseRow * rowYSpace - mScrollOffsetY;
                gcImage.drawLine(0, y1, dim.x, y1);
                gcImage.drawLine(0, y1 + rowYSpace, dim.x, y1 + rowYSpace);
            }

            // Highlight a selected method, if any
            drawHighlights(gcImage, dim);

            // Draw a vertical line where the mouse is.
            gcImage.setForeground(mColorDarkGray);
			int lineEnd = dim.y;// Math.min(dim.y, mNumRows * rowYSpace);
            gcImage.drawLine(mMouse.x, 0, mMouse.x, lineEnd);

            if (blockName != null) {
                mTimescale.setMethodName(blockName);
                mTimescale.setMethodColor(blockColor);
                mTimescale.setDetails(blockDetails);
                mShowHighlightName = false;
            } else if (mShowHighlightName) {
                // Draw the highlighted method name
                MethodData md = mHighlightMethodData;
                if (md == null && mHighlightCall != null) {
					md = mHighlightCall.getMethodData();
				}
                if (md == null)
				 {
					System.out.printf("null highlight?\n");  //$NON-NLS-1$
				}
                if (md != null) {
                    mTimescale.setMethodName(md.getProfileName());
                    mTimescale.setMethodColor(md.getColor());
                    mTimescale.setDetails(null);
                }
            } else {
                mTimescale.setMethodName(null);
                mTimescale.setMethodColor(null);
                mTimescale.setDetails(null);
            }
            mTimescale.redraw();

            // Draw the off-screen buffer to the screen
            gc.drawImage(image, 0, 0);

            // Clean up
            image.dispose();
            gcImage.dispose();
        }

        private void drawHighlights(GC gc, Point dim) {
            int height = mHighlightHeight;
            if (height <= 0) {
				return;
			}
            for (Range range : mHighlightExclusive) {
                gc.setBackground(range.mColor);
                int xStart = range.mXdim.x;
                int width = range.mXdim.y;
                gc.fillRectangle(xStart, range.mY - height - mScrollOffsetY, width, height);
            }

            // Draw the inclusive lines a bit shorter
            height -= 1;
            if (height <= 0) {
				height = 1;
			}

            // Highlight the inclusive ranges
            gc.setForeground(mColorDarkGray);
            gc.setBackground(mColorDarkGray);
            for (Range range : mHighlightInclusive) {
                int x1 = range.mXdim.x;
                int x2 = range.mXdim.y;
                boolean drawLeftEnd = false;
                boolean drawRightEnd = false;
                if (x1 >= LeftMargin) {
					drawLeftEnd = true;
				} else {
					x1 = LeftMargin;
				}
                if (x2 >= LeftMargin) {
					drawRightEnd = true;
				} else {
					x2 = dim.x - RightMargin;
				}
                int y1 = range.mY + rowHeight + 2 - mScrollOffsetY;

                // If the range is very narrow, then just draw a small
                // rectangle.
                if (x2 - x1 < MinInclusiveRange) {
                    int width = x2 - x1;
                    if (width < 2) {
						width = 2;
					}
                    gc.fillRectangle(x1, y1, width, height);
                    continue;
                }
                if (drawLeftEnd) {
                    if (drawRightEnd) {
                        // Draw both ends
                        int[] points = { x1, y1, x1, y1 + height, x2,
                                y1 + height, x2, y1 };
                        gc.drawPolyline(points);
                    } else {
                        // Draw the left end
                        int[] points = { x1, y1, x1, y1 + height, x2,
                                y1 + height };
                        gc.drawPolyline(points);
                    }
                } else {
                    if (drawRightEnd) {
                        // Draw the right end
                        int[] points = { x1, y1 + height, x2, y1 + height, x2,
                                y1 };
                        gc.drawPolyline(points);
                    } else {
                        // Draw neither end, just the line
                        int[] points = { x1, y1 + height, x2, y1 + height };
                        gc.drawPolyline(points);
                    }
                }

                // Draw the arrowheads, if necessary
                if (drawLeftEnd == false) {
                    int[] points = { x1 + 7, y1 + height - 4, x1, y1 + height,
                            x1 + 7, y1 + height + 4 };
                    gc.fillPolygon(points);
                }
                if (drawRightEnd == false) {
                    int[] points = { x2 - 7, y1 + height - 4, x2, y1 + height,
                            x2 - 7, y1 + height + 4 };
                    gc.fillPolygon(points);
                }
            }
        }

        private boolean drawingSelection() {
            return mGraphicsState == GraphicsState.Marking
                    || mGraphicsState == GraphicsState.Animating;
        }

        private void drawSelection(Display display, GC gc) {
            Point dim = getSize();
            gc.setForeground(mColorGray);
            gc.drawLine(mMouseMarkStartX, 0, mMouseMarkStartX, dim.y);
            gc.setBackground(mColorZoomSelection);
            int width;
            int mouseX = (mGraphicsState == GraphicsState.Animating) ? mMouseMarkEndX : mMouse.x;
            int x;
            if (mMouseMarkStartX < mouseX) {
                x = mMouseMarkStartX;
                width = mouseX - mMouseMarkStartX;
            } else {
                x = mouseX;
                width = mMouseMarkStartX - mouseX;
            }
            gc.fillRectangle(x, 0, width, dim.y);
        }

        private void computeStrips() {
            double minVal = mScaleInfo.getMinVal();
            double maxVal = mScaleInfo.getMaxVal();

            // Allocate space for the pixel data
            Pixel[] pixels = new Pixel[mNumRows];
            for (int ii = 0; ii < mNumRows; ++ii) {
				pixels[ii] = new Pixel();
			}

            // Clear the per-block pixel data
            for (int ii = 0; ii < mSegments.length; ++ii) {
                mSegments[ii].mBlock.clearWeight();
            }

            mStripList.clear();
            mHighlightExclusive.clear();
            mHighlightInclusive.clear();
            MethodData callMethod = null;
            long callStart = 0;
            long callEnd = -1;
            RowData callRowData = null;
            int prevMethodStart = -1;
            int prevMethodEnd = -1;
            int prevCallStart = -1;
            int prevCallEnd = -1;
            if (mHighlightCall != null) {
                int callPixelStart = -1;
                int callPixelEnd = -1;
                callStart = mHighlightCall.getStartTime();
                callEnd = mHighlightCall.getEndTime();
                callMethod = mHighlightCall.getMethodData();
                if (callStart >= minVal) {
					callPixelStart = mScaleInfo.valueToPixel(callStart);
				}
                if (callEnd <= maxVal) {
					callPixelEnd = mScaleInfo.valueToPixel(callEnd);
				}
                // System.out.printf("callStart,End %d,%d minVal,maxVal %f,%f
                // callPixelStart,End %d,%d\n",
                // callStart, callEnd, minVal, maxVal, callPixelStart,
                // callPixelEnd);
                int threadId = mHighlightCall.getThreadId();
                String threadName = mThreadLabels.get(threadId);
                callRowData = mRowByName.get(threadName);
                int y1 = callRowData.mRank * rowYSpace + rowYMarginHalf;
                Color color = callMethod.getColor();
                mHighlightInclusive.add(new Range(callPixelStart + LeftMargin,
                        callPixelEnd + LeftMargin, y1, color));
            }
            for (Segment segment : mSegments) {
                if (segment.mEndTime <= minVal) {
					continue;
				}
                if (segment.mStartTime >= maxVal) {
					continue;
				}

                Block block = segment.mBlock;

                // Skip over blocks that were not assigned a color, including the
                // top level block and others that have zero inclusive time.
                Color color = block.getColor();
                if (color == null) {
					continue;
				}

                double recordStart = Math.max(segment.mStartTime, minVal);
                double recordEnd = Math.min(segment.mEndTime, maxVal);
                if (recordStart == recordEnd) {
					continue;
				}
                int pixelStart = mScaleInfo.valueToPixel(recordStart);
                int pixelEnd = mScaleInfo.valueToPixel(recordEnd);
                int width = pixelEnd - pixelStart;
                boolean isContextSwitch = segment.mIsContextSwitch;

                RowData rd = segment.mRowData;
                MethodData md = block.getMethodData();

                // We will add the scroll offset later when we draw the strips
                int y1 = rd.mRank * rowYSpace + rowYMarginHalf;

                // If we can't display any more rows, then quit
                if (rd.mRank > mEndRow) {
					break;
				}

                // System.out.printf("segment %s val: [%.1f, %.1f] frac [%f, %f]
                // pixel: [%d, %d] pix.start %d weight %.2f %s\n",
                // block.getName(), recordStart, recordEnd,
                // scaleInfo.valueToPixelFraction(recordStart),
                // scaleInfo.valueToPixelFraction(recordEnd),
                // pixelStart, pixelEnd, pixels[rd.rank].start,
                // pixels[rd.rank].maxWeight,
                // pixels[rd.rank].segment != null
                // ? pixels[rd.rank].segment.block.getName()
                // : "null");

                if (mHighlightMethodData != null) {
                    if (mHighlightMethodData == md) {
                        if (prevMethodStart != pixelStart || prevMethodEnd != pixelEnd) {
                            prevMethodStart = pixelStart;
                            prevMethodEnd = pixelEnd;
                            int rangeWidth = width;
                            if (rangeWidth == 0) {
								rangeWidth = 1;
							}
                            mHighlightExclusive.add(new Range(pixelStart
                                    + LeftMargin, rangeWidth, y1, color));
                            callStart = block.getStartTime();
                            int callPixelStart = -1;
                            if (callStart >= minVal) {
								callPixelStart = mScaleInfo.valueToPixel(callStart);
							}
                            int callPixelEnd = -1;
                            callEnd = block.getEndTime();
                            if (callEnd <= maxVal) {
								callPixelEnd = mScaleInfo.valueToPixel(callEnd);
							}
                            if (prevCallStart != callPixelStart || prevCallEnd != callPixelEnd) {
                                prevCallStart = callPixelStart;
                                prevCallEnd = callPixelEnd;
                                mHighlightInclusive.add(new Range(
                                        callPixelStart + LeftMargin,
                                        callPixelEnd + LeftMargin, y1, color));
                            }
                        }
                    } else if (mFadeColors) {
                        color = md.getFadedColor();
                    }
                } else if (mHighlightCall != null) {
                    if (segment.mStartTime >= callStart
                            && segment.mEndTime <= callEnd && callMethod == md
                            && callRowData == rd) {
                        if (prevMethodStart != pixelStart || prevMethodEnd != pixelEnd) {
                            prevMethodStart = pixelStart;
                            prevMethodEnd = pixelEnd;
                            int rangeWidth = width;
                            if (rangeWidth == 0) {
								rangeWidth = 1;
							}
                            mHighlightExclusive.add(new Range(pixelStart
                                    + LeftMargin, rangeWidth, y1, color));
                        }
                    } else if (mFadeColors) {
                        color = md.getFadedColor();
                    }
                }

                // Cases:
                // 1. This segment starts on a different pixel than the
                // previous segment started on. In this case, emit
                // the pixel strip, if any, and:
                // A. If the width is 0, then add this segment's
                // weight to the Pixel.
                // B. If the width > 0, then emit a strip for this
                // segment (no partial Pixel data).
                //
                // 2. Otherwise (the new segment starts on the same
                // pixel as the previous segment): add its "weight"
                // to the current pixel, and:
                // A. If the new segment has width 1,
                // then emit the pixel strip and then
                // add the segment's weight to the pixel.
                // B. If the new segment has width > 1,
                // then emit the pixel strip, and emit the rest
                // of the strip for this segment (no partial Pixel
                // data).

                Pixel pix = pixels[rd.mRank];
                if (pix.mStart != pixelStart) {
                    if (pix.mSegment != null) {
                        // Emit the pixel strip. This also clears the pixel.
                        emitPixelStrip(rd, y1, pix);
                    }

                    if (width == 0) {
                        // Compute the "weight" of this segment for the first
                        // pixel. For a pixel N, the "weight" of a segment is
                        // how much of the region [N - 0.5, N + 0.5] is covered
                        // by the segment.
                        double weight = computeWeight(recordStart, recordEnd,
                                isContextSwitch, pixelStart);
                        weight = block.addWeight(pixelStart, rd.mRank, weight);
                        if (weight > pix.mMaxWeight) {
                            pix.setFields(pixelStart, weight, segment, color,
                                    rd);
                        }
                    } else {
                        int x1 = pixelStart + LeftMargin;
                        Strip strip = new Strip(
                                x1, isContextSwitch ? y1 + rowHeight - 1 : y1,
                                width, isContextSwitch ? 1 : rowHeight,
                                rd, segment, color);
                        mStripList.add(strip);
                    }
                } else {
                    double weight = computeWeight(recordStart, recordEnd,
                            isContextSwitch, pixelStart);
                    weight = block.addWeight(pixelStart, rd.mRank, weight);
                    if (weight > pix.mMaxWeight) {
                        pix.setFields(pixelStart, weight, segment, color, rd);
                    }
                    if (width == 1) {
                        // Emit the pixel strip. This also clears the pixel.
                        emitPixelStrip(rd, y1, pix);

                        // Compute the weight for the next pixel
                        pixelStart += 1;
                        weight = computeWeight(recordStart, recordEnd,
                                isContextSwitch, pixelStart);
                        weight = block.addWeight(pixelStart, rd.mRank, weight);
                        pix.setFields(pixelStart, weight, segment, color, rd);
                    } else if (width > 1) {
                        // Emit the pixel strip. This also clears the pixel.
                        emitPixelStrip(rd, y1, pix);

                        // Emit a strip for the rest of the segment.
                        pixelStart += 1;
                        width -= 1;
                        int x1 = pixelStart + LeftMargin;
                        Strip strip = new Strip(
                                x1, isContextSwitch ? y1 + rowHeight - 1 : y1,
                                width, isContextSwitch ? 1 : rowHeight,
                                rd,segment, color);
                        mStripList.add(strip);
                    }
                }
            }

            // Emit the last pixels of each row, if any
            for (int ii = 0; ii < mNumRows; ++ii) {
                Pixel pix = pixels[ii];
                if (pix.mSegment != null) {
                    RowData rd = pix.mRowData;
                    int y1 = rd.mRank * rowYSpace + rowYMarginHalf;
                    // Emit the pixel strip. This also clears the pixel.
                    emitPixelStrip(rd, y1, pix);
                }
            }

            if (false) {
                System.out.printf("computeStrips()\n");
                for (Strip strip : mStripList) {
                    System.out.printf("%3d, %3d width %3d height %d %s\n",
                            strip.mX, strip.mY, strip.mWidth, strip.mHeight,
                            strip.mSegment.mBlock.getName());
                }
            }
        }

        private double computeWeight(double start, double end,
                boolean isContextSwitch, int pixel) {
            if (isContextSwitch) {
                return 0;
            }
            double pixelStartFraction = mScaleInfo.valueToPixelFraction(start);
            double pixelEndFraction = mScaleInfo.valueToPixelFraction(end);
            double leftEndPoint = Math.max(pixelStartFraction, pixel - 0.5);
            double rightEndPoint = Math.min(pixelEndFraction, pixel + 0.5);
            double weight = rightEndPoint - leftEndPoint;
            return weight;
        }

        private void emitPixelStrip(RowData rd, int y, Pixel pixel) {
            Strip strip;

            if (pixel.mSegment == null) {
				return;
			}

            int x = pixel.mStart + LeftMargin;
            // Compute the percentage of the row height proportional to
            // the weight of this pixel. But don't let the proportion
            // exceed 3/4 of the row height so that we can easily see
            // if a given time range includes more than one method.
            int height = (int) (pixel.mMaxWeight * rowHeight * 0.75);
            if (height < mMinStripHeight) {
				height = mMinStripHeight;
			}
            int remainder = rowHeight - height;
            if (remainder > 0) {
                strip = new Strip(x, y, 1, remainder, rd, pixel.mSegment,
                        mFadeColors ? mColorGray : mColorBlack);
                mStripList.add(strip);
                // System.out.printf("emitPixel (%d, %d) height %d black\n",
                // x, y, remainder);
            }
            strip = new Strip(x, y + remainder, 1, height, rd, pixel.mSegment,
                    pixel.mColor);
            mStripList.add(strip);
            // System.out.printf("emitPixel (%d, %d) height %d %s\n",
            // x, y + remainder, height, pixel.segment.block.getName());
            pixel.mSegment = null;
            pixel.mMaxWeight = 0.0;
        }

        private void mouseMove(MouseEvent me) {
            if (false) {
                if (mHighlightMethodData != null) {
                    mHighlightMethodData = null;
                    // Force a recomputation of the strip colors
                    mCachedEndRow = -1;
                }
            }
            Point dim = mSurface.getSize();
            int x = me.x;
            if (x < LeftMargin) {
				x = LeftMargin;
			}
            if (x > dim.x - RightMargin) {
				x = dim.x - RightMargin;
			}
            mMouse.x = x;
            mMouse.y = me.y;
            mTimescale.setVbarPosition(x);
            if (mGraphicsState == GraphicsState.Marking) {
                mTimescale.setMarkEnd(x);
            }

            if (mGraphicsState == GraphicsState.Normal) {
                // Set the cursor to the normal state.
                mSurface.setCursor(mNormalCursor);
            } else if (mGraphicsState == GraphicsState.Marking) {
                // Make the cursor point in the direction of the sweep
                if (mMouse.x >= mMouseMarkStartX) {
					mSurface.setCursor(mIncreasingCursor);
				} else {
					mSurface.setCursor(mDecreasingCursor);
				}
            }
            int rownum = (mMouse.y + mScrollOffsetY) / rowYSpace;
            if (me.y < 0 || me.y >= dim.y) {
                rownum = -1;
            }
            if (mMouseRow != rownum) {
                mMouseRow = rownum;
                mLabels.redraw();
            }
			if (me.y != -1) {
				me.y = -1;
				mLogSurface.mouseMove(me);
			}
            redraw();
        }

        private void mouseDown(MouseEvent me) {
            Point dim = mSurface.getSize();
            int x = me.x;
            if (x < LeftMargin) {
				x = LeftMargin;
			}
            if (x > dim.x - RightMargin) {
				x = dim.x - RightMargin;
			}
            mMouseMarkStartX = x;
            mGraphicsState = GraphicsState.Marking;
            mSurface.setCursor(mIncreasingCursor);
            mTimescale.setMarkStart(mMouseMarkStartX);
            mTimescale.setMarkEnd(mMouseMarkStartX);
            redraw();
        }

        private void mouseUp(MouseEvent me) {
            mSurface.setCursor(mNormalCursor);
            if (mGraphicsState != GraphicsState.Marking) {
                mGraphicsState = GraphicsState.Normal;
                return;
            }
            mGraphicsState = GraphicsState.Animating;
            Point dim = mSurface.getSize();

            // If the user released the mouse outside the drawing area then
            // cancel the zoom.
            if (me.y <= 0 || me.y >= dim.y) {
                mGraphicsState = GraphicsState.Normal;
                redraw();
                return;
            }

            int x = me.x;
            if (x < LeftMargin) {
				x = LeftMargin;
			}
            if (x > dim.x - RightMargin) {
				x = dim.x - RightMargin;
			}
            mMouseMarkEndX = x;

            // If the user clicked and released the mouse at the same point
            // (+/- a pixel or two) then cancel the zoom (but select the
            // method).
            int dist = mMouseMarkEndX - mMouseMarkStartX;
            if (dist < 0) {
				dist = -dist;
			}
            if (dist <= 2) {
                mGraphicsState = GraphicsState.Normal;

                // Select the method underneath the mouse
                mMouseSelect.x = mMouseMarkStartX;
                mMouseSelect.y = me.y;
                redraw();
                return;
            }

            // Make mouseEndX be the higher end point
            if (mMouseMarkEndX < mMouseMarkStartX) {
                int temp = mMouseMarkEndX;
                mMouseMarkEndX = mMouseMarkStartX;
                mMouseMarkStartX = temp;
            }

            // If the zoom area is the whole window (or nearly the whole
            // window) then cancel the zoom.
            if (mMouseMarkStartX <= LeftMargin + MinZoomPixelMargin
                    && mMouseMarkEndX >= dim.x - RightMargin - MinZoomPixelMargin) {
                mGraphicsState = GraphicsState.Normal;
                redraw();
                return;
            }

            // Compute some variables needed for zooming.
            // It's probably easiest to explain by an example. There
            // are two scales (or dimensions) involved: one for the pixels
            // and one for the values (microseconds). To keep the example
            // simple, suppose we have pixels in the range [0,16] and
            // values in the range [100, 260], and suppose the user
            // selects a zoom window from pixel 4 to pixel 8.
            //
            // usec: 100 140 180 260
            // |-------|ZZZZZZZ|---------------|
            // pixel: 0 4 8 16
            //
            // I've drawn the pixels starting at zero for simplicity, but
            // in fact the drawable area is offset from the left margin
            // by the value of "LeftMargin".
            //
            // The "pixels-per-range" (ppr) in this case is 0.1 (a tenth of
            // a pixel per usec). What we want is to redraw the screen in
            // several steps, each time increasing the zoom window until the
            // zoom window fills the screen. For simplicity, assume that
            // we want to zoom in four equal steps. Then the snapshots
            // of the screen at each step would look something like this:
            //
            // usec: 100 140 180 260
            // |-------|ZZZZZZZ|---------------|
            // pixel: 0 4 8 16
            //
            // usec: ? 140 180 ?
            // |-----|ZZZZZZZZZZZZZ|-----------|
            // pixel: 0 3 10 16
            //
            // usec: ? 140 180 ?
            // |---|ZZZZZZZZZZZZZZZZZZZ|-------|
            // pixel: 0 2 12 16
            //
            // usec: ?140 180 ?
            // |-|ZZZZZZZZZZZZZZZZZZZZZZZZZ|---|
            // pixel: 0 1 14 16
            //
            // usec: 140 180
            // |ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ|
            // pixel: 0 16
            //
            // The problem is how to compute the endpoints (denoted by ?)
            // for each step. This is a little tricky. We first need to
            // compute the "fixed point": this is the point in the selection
            // that doesn't move left or right. Then we can recompute the
            // "ppr" (pixels per range) at each step and then find the
            // endpoints. The computation of the end points is done
            // in animateZoom(). This method computes the fixed point
            // and some other variables needed in animateZoom().

            double minVal = mScaleInfo.getMinVal();
            double maxVal = mScaleInfo.getMaxVal();
            double ppr = mScaleInfo.getPixelsPerRange();
            mZoomMin = minVal + ((mMouseMarkStartX - LeftMargin) / ppr);
            mZoomMax = minVal + ((mMouseMarkEndX - LeftMargin) / ppr);

            // Clamp the min and max values to the actual data min and max
            if (mZoomMin < mMinDataVal) {
				mZoomMin = mMinDataVal;
			}
            if (mZoomMax > mMaxDataVal) {
				mZoomMax = mMaxDataVal;
			}

            // Snap the min and max points to the grid determined by the
            // TickScaler
            // before we zoom.
            int xdim = dim.x - TotalXMargin;
            TickScaler scaler = new TickScaler(mZoomMin, mZoomMax, xdim,
                    PixelsPerTick);
            scaler.computeTicks(false);
            mZoomMin = scaler.getMinVal();
            mZoomMax = scaler.getMaxVal();

            // Also snap the mouse points (in pixel space) to be consistent with
            // zoomMin and zoomMax (in value space).
            mMouseMarkStartX = (int) ((mZoomMin - minVal) * ppr + LeftMargin);
            mMouseMarkEndX = (int) ((mZoomMax - minVal) * ppr + LeftMargin);
            mTimescale.setMarkStart(mMouseMarkStartX);
            mTimescale.setMarkEnd(mMouseMarkEndX);

            // Compute the mouse selection end point distances
            mMouseEndDistance = dim.x - RightMargin - mMouseMarkEndX;
            mMouseStartDistance = mMouseMarkStartX - LeftMargin;
            mZoomMouseStart = mMouseMarkStartX;
            mZoomMouseEnd = mMouseMarkEndX;
            mZoomStep = 0;

            // Compute the fixed point in both value space and pixel space.
            mMin2ZoomMin = mZoomMin - minVal;
            mZoomMax2Max = maxVal - mZoomMax;
            mZoomFixed = mZoomMin + (mZoomMax - mZoomMin) * mMin2ZoomMin
                    / (mMin2ZoomMin + mZoomMax2Max);
            mZoomFixedPixel = (mZoomFixed - minVal) * ppr + LeftMargin;
            mFixedPixelStartDistance = mZoomFixedPixel - LeftMargin;
            mFixedPixelEndDistance = dim.x - RightMargin - mZoomFixedPixel;

            mZoomMin2Fixed = mZoomFixed - mZoomMin;
            mFixed2ZoomMax = mZoomMax - mZoomFixed;

            getDisplay().timerExec(ZOOM_TIMER_INTERVAL, mZoomAnimator);
            redraw();
            update();
        }

        private void mouseScrolled(MouseEvent me) {
            mGraphicsState = GraphicsState.Scrolling;
            double tMin = mScaleInfo.getMinVal();
            double tMax = mScaleInfo.getMaxVal();
            double zoomFactor = 2;
            double tMinRef = mLimitMinVal;
            double tMaxRef = mLimitMaxVal;
            double t; // the fixed point
            double tMinNew;
            double tMaxNew;
            if (me.count > 0) {
                // we zoom in
                Point dim = mSurface.getSize();
                int x = me.x;
                if (x < LeftMargin) {
					x = LeftMargin;
				}
                if (x > dim.x - RightMargin) {
					x = dim.x - RightMargin;
				}
                double ppr = mScaleInfo.getPixelsPerRange();
                t = tMin + ((x - LeftMargin) / ppr);
                tMinNew = Math.max(tMinRef, t - (t - tMin) / zoomFactor);
                tMaxNew = Math.min(tMaxRef, t + (tMax - t) / zoomFactor);
            } else {
                // we zoom out
                double factor = (tMax - tMin) / (tMaxRef - tMinRef);
                if (factor < 1) {
                    t = (factor * tMinRef - tMin) / (factor - 1);
                    tMinNew = Math.max(tMinRef, t - zoomFactor * (t - tMin));
                    tMaxNew = Math.min(tMaxRef, t + zoomFactor * (tMax - t));
                } else {
                    return;
                }
            }
            mScaleInfo.setMinVal(tMinNew);
            mScaleInfo.setMaxVal(tMaxNew);
            mSurface.redraw();
			mLogSurface.redraw();
        }

        // No defined behavior yet for double-click.
        private void mouseDoubleClick(MouseEvent me) {
        }

        public void startScaling(int mouseX) {
            Point dim = mSurface.getSize();
            int x = mouseX;
            if (x < LeftMargin) {
				x = LeftMargin;
			}
            if (x > dim.x - RightMargin) {
				x = dim.x - RightMargin;
			}
            mMouseMarkStartX = x;
            mGraphicsState = GraphicsState.Scaling;
            mScalePixelsPerRange = mScaleInfo.getPixelsPerRange();
            mScaleMinVal = mScaleInfo.getMinVal();
            mScaleMaxVal = mScaleInfo.getMaxVal();
        }

        public void stopScaling(int mouseX) {
            mGraphicsState = GraphicsState.Normal;
        }

        private void animateHighlight() {
            mHighlightStep += 1;
            if (mHighlightStep >= HIGHLIGHT_STEPS) {
                mFadeColors = false;
                mHighlightStep = 0;
                // Force a recomputation of the strip colors
                mCachedEndRow = -1;
            } else {
                mFadeColors = true;
                mShowHighlightName = true;
                mHighlightHeight = highlightHeights[mHighlightStep];
                getDisplay().timerExec(HIGHLIGHT_TIMER_INTERVAL, mHighlightAnimator);
            }
            redraw();
        }

        private void clearHighlights() {
            // System.out.printf("clearHighlights()\n");
            mShowHighlightName = false;
            mHighlightHeight = 0;
            mHighlightMethodData = null;
            mHighlightCall = null;
            mFadeColors = false;
            mHighlightStep = 0;
            // Force a recomputation of the strip colors
            mCachedEndRow = -1;
            redraw();
        }

        private void animateZoom() {
            mZoomStep += 1;
            if (mZoomStep > ZOOM_STEPS) {
                mGraphicsState = GraphicsState.Normal;
                // Force a normal recomputation
                mCachedMinVal = mScaleInfo.getMinVal() + 1;
            } else if (mZoomStep == ZOOM_STEPS) {
                mScaleInfo.setMinVal(mZoomMin);
                mScaleInfo.setMaxVal(mZoomMax);
                mMouseMarkStartX = LeftMargin;
                Point dim = getSize();
                mMouseMarkEndX = dim.x - RightMargin;
                mTimescale.setMarkStart(mMouseMarkStartX);
                mTimescale.setMarkEnd(mMouseMarkEndX);
                getDisplay().timerExec(ZOOM_TIMER_INTERVAL, mZoomAnimator);
            } else {
                // Zoom in slowly at first, then speed up, then slow down.
                // The zoom fractions are precomputed to save time.
                double fraction = mZoomFractions[mZoomStep];
                mMouseMarkStartX = (int) (mZoomMouseStart - fraction * mMouseStartDistance);
                mMouseMarkEndX = (int) (mZoomMouseEnd + fraction * mMouseEndDistance);
                mTimescale.setMarkStart(mMouseMarkStartX);
                mTimescale.setMarkEnd(mMouseMarkEndX);

                // Compute the new pixels-per-range. Avoid division by zero.
                double ppr;
                if (mZoomMin2Fixed >= mFixed2ZoomMax) {
					ppr = (mZoomFixedPixel - mMouseMarkStartX) / mZoomMin2Fixed;
				} else {
					ppr = (mMouseMarkEndX - mZoomFixedPixel) / mFixed2ZoomMax;
				}
                double newMin = mZoomFixed - mFixedPixelStartDistance / ppr;
                double newMax = mZoomFixed + mFixedPixelEndDistance / ppr;
                mScaleInfo.setMinVal(newMin);
                mScaleInfo.setMaxVal(newMax);

                getDisplay().timerExec(ZOOM_TIMER_INTERVAL, mZoomAnimator);
            }
            redraw();
			if (mLogSurface != null) {
				mLogSurface.redraw();
			}
        }

        private static final int TotalXMargin = LeftMargin + RightMargin;
        private static final int yMargin = 1; // blank space on top
        // The minimum margin on each side of the zoom window, in pixels.
        private static final int MinZoomPixelMargin = 10;
        private GraphicsState mGraphicsState = GraphicsState.Normal;
        private final Point mMouse = new Point(LeftMargin, 0);
        private int mMouseMarkStartX;
        private int mMouseMarkEndX;
        private final boolean mDebug = false;
        private final ArrayList<Strip> mStripList = new ArrayList<Strip>();
        private final ArrayList<Range> mHighlightExclusive = new ArrayList<Range>();
        private final ArrayList<Range> mHighlightInclusive = new ArrayList<Range>();
        private final int mMinStripHeight = 2;
        private double mCachedMinVal;
        private double mCachedMaxVal;
        private int mCachedStartRow;
        private int mCachedEndRow;
        private double mScalePixelsPerRange;
        private double mScaleMinVal;
        private double mScaleMaxVal;
        private double mLimitMinVal;
        private double mLimitMaxVal;
        private double mMinDataVal;
        private double mMaxDataVal;
        private final Cursor mNormalCursor;
        private final Cursor mIncreasingCursor;
        private final Cursor mDecreasingCursor;
        private static final int ZOOM_TIMER_INTERVAL = 10;
        private static final int HIGHLIGHT_TIMER_INTERVAL = 50;
        private static final int ZOOM_STEPS = 8; // must be even
        private int mHighlightHeight = 4;
        private final int[] highlightHeights = { 0, 2, 4, 5, 6, 5, 4, 2, 4, 5,
                6 };
        private final int HIGHLIGHT_STEPS = highlightHeights.length;
        private boolean mFadeColors;
        private boolean mShowHighlightName;
        private double[] mZoomFractions;
        private int mZoomStep;
        private int mZoomMouseStart;
        private int mZoomMouseEnd;
        private int mMouseStartDistance;
        private int mMouseEndDistance;
        private final Point mMouseSelect = new Point(0, 0);
        private double mZoomFixed;
        private double mZoomFixedPixel;
        private double mFixedPixelStartDistance;
        private double mFixedPixelEndDistance;
        private double mZoomMin2Fixed;
        private double mMin2ZoomMin;
        private double mFixed2ZoomMax;
        private double mZoomMax2Max;
        private double mZoomMin;
        private double mZoomMax;
        private final Runnable mZoomAnimator;
        private final Runnable mHighlightAnimator;
        private int mHighlightStep;
    }

    private int computeVisibleRows(int ydim) {
        // If we resize, then move the bottom row down.  Don't allow the scroll
        // to waste space at the bottom.
        int offsetY = mScrollOffsetY;
        int spaceNeeded = mNumRows * rowYSpace;
        if (offsetY + ydim > spaceNeeded) {
            offsetY = spaceNeeded - ydim;
            if (offsetY < 0) {
                offsetY = 0;
            }
        }
        mStartRow = offsetY / rowYSpace;
        mEndRow = (offsetY + ydim) / rowYSpace;
        if (mEndRow >= mNumRows) {
            mEndRow = mNumRows - 1;
        }

        return offsetY;
    }

	private int computeVisibleLogRows(int ydim) {
		// If we resize, then move the bottom row down. Don't allow the scroll
		// to waste space at the bottom.
		int offsetY = mLogScrollOffsetY;
		int spaceNeeded = mNumLogRows * logRowYSpace;
		if (offsetY + ydim > spaceNeeded) {
			offsetY = spaceNeeded - ydim;
			if (offsetY < 0) {
				offsetY = 0;
			}
		}
		mStartLogRow = offsetY / logRowYSpace;
		mEndLogRow = (offsetY + ydim) / logRowYSpace;
		if (mEndLogRow >= mNumLogRows) {
			mEndLogRow = mNumLogRows - 1;
		}

		return offsetY;
	}

    private void startHighlighting() {
        // System.out.printf("startHighlighting()\n");
        mSurface.mHighlightStep = 0;
        mSurface.mFadeColors = true;
        // Force a recomputation of the color strips
        mSurface.mCachedEndRow = -1;
        getDisplay().timerExec(0, mSurface.mHighlightAnimator);
    }

	// <<<<<<<<<<<ADDED
	private static class LogRowData {
		LogRowData(ContextLogData row) {
			mName = row.getName();
			mId = row.getId();
			mType = row.getType();
			mRow = row;
			mGraphData = new TreeMap<Integer, Integer>();
		}

		private final String mName;
		private final int mId;
		private final LogType mType;
		private final ContextLogData mRow;
		private final TreeMap<Integer, Integer> mGraphData;
	}

	// >>>>>>>>>>>>Till here

    private static class RowData {
        RowData(Row row) {
            mName = row.getName();
            mStack = new ArrayList<Block>();
        }

        public void push(Block block) {
            mStack.add(block);
        }

        public Block top() {
            if (mStack.size() == 0) {
				return null;
			}
            return mStack.get(mStack.size() - 1);
        }

        public void pop() {
            if (mStack.size() == 0) {
				return;
			}
            mStack.remove(mStack.size() - 1);
        }

        private final String mName;
        private int mRank;
        private long mElapsed;
        private long mEndTime;
        private final ArrayList<Block> mStack;
    }

    private static class Segment {
        Segment(RowData rowData, Block block, long startTime, long endTime) {
            mRowData = rowData;
            if (block.isContextSwitch()) {
                mBlock = block.getParentBlock();
                mIsContextSwitch = true;
            } else {
                mBlock = block;
            }
            mStartTime = startTime;
            mEndTime = endTime;
        }

        private final RowData mRowData;
        private Block mBlock;
        private final long mStartTime;
        private final long mEndTime;
        private boolean mIsContextSwitch;
    }

    private static class Strip {
        Strip(int x, int y, int width, int height, RowData rowData,
                Segment segment, Color color) {
            mX = x;
            mY = y;
            mWidth = width;
            mHeight = height;
            mRowData = rowData;
            mSegment = segment;
            mColor = color;
        }

        int mX;
        int mY;
        int mWidth;
        int mHeight;
        RowData mRowData;
        Segment mSegment;
        Color mColor;
    }

    private static class Pixel {
        public void setFields(int start, double weight, Segment segment,
                Color color, RowData rowData) {
            mStart = start;
            mMaxWeight = weight;
            mSegment = segment;
            mColor = color;
            mRowData = rowData;
        }

        int mStart = -2; // some value that won't match another pixel
        double mMaxWeight;
        Segment mSegment;
        Color mColor; // we need the color here because it may be faded
        RowData mRowData;
    }

    private static class Range {
        Range(int xStart, int width, int y, Color color) {
            mXdim.x = xStart;
            mXdim.y = width;
            mY = y;
            mColor = color;
        }

        Point mXdim = new Point(0, 0);
        int mY;
        Color mColor;
    }
}
