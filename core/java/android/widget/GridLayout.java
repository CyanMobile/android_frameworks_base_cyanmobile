/*
 * Copyright (C) 2011 The Android Open Source Project
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

package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R.styleable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * A layout that places its children in a rectangular <em>grid</em>.
 * <p>
 * The grid is composed of a set of infinitely thin lines that separate the
 * viewing area into <em>cells</em>. Throughout the API, grid lines are referenced
 * by grid <em>indices</em>. A grid with {@code N} columns
 * has {@code N + 1} grid indices that run from {@code 0}
 * through {@code N} inclusive. Regardless of how GridLayout is
 * configured, grid index {@code 0} is fixed to the leading edge of the
 * container and grid index {@code N} is fixed to its trailing edge
 * (after padding is taken into account).
 *
 * <h4>Row and Column Groups</h4>
 *
 * Children occupy one or more contiguous cells, as defined
 * by their {@link GridLayout.LayoutParams#rowGroup rowGroup} and
 * {@link GridLayout.LayoutParams#columnGroup columnGroup} layout parameters.
 * Each group specifies the set of rows or columns that are to be
 * occupied; and how children should be aligned within the resulting group of cells.
 * Although cells do not normally overlap in a GridLayout, GridLayout does
 * not prevent children being defined to occupy the same cell or group of cells.
 * In this case however, there is no guarantee that children will not themselves
 * overlap after the layout operation completes.
 *
 * <h4>Default Cell Assignment</h4>
 *
 * If a child does not specify the row and column indices of the cell it
 * wishes to occupy, GridLayout assigns cell locations automatically using its:
 * {@link GridLayout#setOrientation(int) orientation},
 * {@link GridLayout#setRowCount(int) rowCount} and
 * {@link GridLayout#setColumnCount(int) columnCount} properties.
 *
 * <h4>Space</h4>
 *
 * Space between children may be specified either by using instances of the
 * dedicated {@link Space} view or by setting the
 *
 * {@link ViewGroup.MarginLayoutParams#leftMargin leftMargin},
 * {@link ViewGroup.MarginLayoutParams#topMargin topMargin},
 * {@link ViewGroup.MarginLayoutParams#rightMargin rightMargin} and
 * {@link ViewGroup.MarginLayoutParams#bottomMargin bottomMargin}
 *
 * layout parameters. When the
 * {@link GridLayout#setUseDefaultMargins(boolean) useDefaultMargins}
 * property is set, default margins around children are automatically
 * allocated based on the child's visual characteristics. Each of the
 * margins so defined may be independently overridden by an assignment
 * to the appropriate layout parameter.
 *
 * <h4>Excess Space Distribution</h4>
 *
 * Like {@link LinearLayout}, a child's ability to stretch is controlled
 * using <em>weights</em>, which are specified using the
 * {@link GridLayout.LayoutParams#widthSpec widthSpec} and
 * {@link GridLayout.LayoutParams#heightSpec heightSpec} layout parameters.
 * <p>
 * <p>
 * See {@link GridLayout.LayoutParams} for a full description of the
 * layout parameters used by GridLayout.
 *
 * @attr ref android.R.styleable#GridLayout_orientation
 * @attr ref android.R.styleable#GridLayout_rowCount
 * @attr ref android.R.styleable#GridLayout_columnCount
 * @attr ref android.R.styleable#GridLayout_useDefaultMargins
 * @attr ref android.R.styleable#GridLayout_rowOrderPreserved
 * @attr ref android.R.styleable#GridLayout_columnOrderPreserved
 */
public class GridLayout extends ViewGroup {

    // Public constants

    /**
     * The horizontal orientation.
     */
    public static final int HORIZONTAL = LinearLayout.HORIZONTAL;

    /**
     * The vertical orientation.
     */
    public static final int VERTICAL = LinearLayout.VERTICAL;

    /**
     * The constant used to indicate that a value is undefined.
     * Fields can use this value to indicate that their values
     * have not yet been set. Similarly, methods can return this value
     * to indicate that there is no suitable value that the implementation
     * can return.
     * The value used for the constant (currently {@link Integer#MIN_VALUE}) is
     * intended to avoid confusion between valid values whose sign may not be known.
     */
    public static final int UNDEFINED = Integer.MIN_VALUE;

    /**
     * This constant is an {@link #setAlignmentMode(int) alignmentMode}.
     * When the {@code alignmentMode} is set to {@link #ALIGN_BOUNDS}, alignment
     * is made between the edges of each component's raw
     * view boundary: i.e. the area delimited by the component's:
     * {@link android.view.View#getTop() top},
     * {@link android.view.View#getLeft() left},
     * {@link android.view.View#getBottom() bottom} and
     * {@link android.view.View#getRight() right} properties.
     * <p>
     * For example, when {@code GridLayout} is in {@link #ALIGN_BOUNDS} mode,
     * children that belong to a row group that uses {@link #TOP} alignment will
     * all return the same value when their {@link android.view.View#getTop()}
     * method is called.
     *
     * @see #setAlignmentMode(int)
     */
    public static final int ALIGN_BOUNDS = 0;

    /**
     * This constant is an {@link #setAlignmentMode(int) alignmentMode}.
     * When the {@code alignmentMode} is set to {@link #ALIGN_MARGINS},
     * the bounds of each view are extended outwards, according
     * to their margins, before the edges of the resulting rectangle are aligned.
     * <p>
     * For example, when {@code GridLayout} is in {@link #ALIGN_MARGINS} mode,
     * the quantity {@code top - layoutParams.topMargin} is the same for all children that
     * belong to a row group that uses {@link #TOP} alignment.
     *
     * @see #setAlignmentMode(int)
     */
    public static final int ALIGN_MARGINS = 1;

    // Misc constants

    private static final String TAG = GridLayout.class.getName();
    private static final boolean DEBUG = false;
    private static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
    private static final int PRF = 1;

    // Defaults

    private static final int DEFAULT_ORIENTATION = HORIZONTAL;
    private static final int DEFAULT_COUNT = UNDEFINED;
    private static final boolean DEFAULT_USE_DEFAULT_MARGINS = false;
    private static final boolean DEFAULT_ORDER_PRESERVED = false;
    private static final int DEFAULT_ALIGNMENT_MODE = ALIGN_MARGINS;
    // todo remove this
    private static final int DEFAULT_CONTAINER_MARGIN = 20;
    private static final int MAX_SIZE = 100000;

    // TypedArray indices

    private static final int ORIENTATION = styleable.GridLayout_orientation;
    private static final int ROW_COUNT = styleable.GridLayout_rowCount;
    private static final int COLUMN_COUNT = styleable.GridLayout_columnCount;
    private static final int USE_DEFAULT_MARGINS = styleable.GridLayout_useDefaultMargins;
    private static final int ALIGNMENT_MODE = styleable.GridLayout_alignmentMode;
    private static final int ROW_ORDER_PRESERVED = styleable.GridLayout_rowOrderPreserved;
    private static final int COLUMN_ORDER_PRESERVED = styleable.GridLayout_columnOrderPreserved;

    // Instance variables

    private final Axis mHorizontalAxis = new Axis(true);
    private final Axis mVerticalAxis = new Axis(false);
    private boolean mLayoutParamsValid = false;
    private int mOrientation = DEFAULT_ORIENTATION;
    private boolean mUseDefaultMargins = DEFAULT_USE_DEFAULT_MARGINS;
    private int mAlignmentMode = DEFAULT_ALIGNMENT_MODE;
    private int mDefaultGravity = Gravity.NO_GRAVITY;

    // Constructors

    /**
     * {@inheritDoc}
     */
    public GridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (DEBUG) {
            setWillNotDraw(false);
        }
        TypedArray a = context.obtainStyledAttributes(attrs, styleable.GridLayout);
        try {
            setRowCount(a.getInt(ROW_COUNT, DEFAULT_COUNT));
            setColumnCount(a.getInt(COLUMN_COUNT, DEFAULT_COUNT));
            mOrientation = a.getInt(ORIENTATION, DEFAULT_ORIENTATION);
            mUseDefaultMargins = a.getBoolean(USE_DEFAULT_MARGINS, DEFAULT_USE_DEFAULT_MARGINS);
            mAlignmentMode = a.getInt(ALIGNMENT_MODE, DEFAULT_ALIGNMENT_MODE);
            setRowOrderPreserved(a.getBoolean(ROW_ORDER_PRESERVED, DEFAULT_ORDER_PRESERVED));
            setColumnOrderPreserved(a.getBoolean(COLUMN_ORDER_PRESERVED, DEFAULT_ORDER_PRESERVED));
        } finally {
            a.recycle();
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * {@inheritDoc}
     */
    public GridLayout(Context context) {
        this(context, null);
    }

    // Implementation

    /**
     * Returns the current orientation.
     *
     * @return either {@link #HORIZONTAL} or {@link #VERTICAL}
     *
     * @see #setOrientation(int)
     *
     * @attr ref android.R.styleable#GridLayout_orientation
     */
    public int getOrientation() {
        return mOrientation;
    }

    /**
     * The orientation property does not affect layout. Orientation is used
     * only to generate default row/column indices when they are not specified
     * by a component's layout parameters.
     * <p>
     * The default value of this property is {@link #HORIZONTAL}.
     *
     * @param orientation either {@link #HORIZONTAL} or {@link #VERTICAL}
     *
     * @see #getOrientation()
     *
     * @attr ref android.R.styleable#GridLayout_orientation
     */
    public void setOrientation(int orientation) {
        if (mOrientation != orientation) {
            mOrientation = orientation;
            requestLayout();
        }
    }

    /**
     * Returns the current number of rows. This is either the last value that was set
     * with {@link #setRowCount(int)} or, if no such value was set, the maximum
     * value of each the upper bounds defined in {@link LayoutParams#rowGroup}.
     *
     * @return the current number of rows
     *
     * @see #setRowCount(int)
     * @see LayoutParams#rowGroup
     *
     * @attr ref android.R.styleable#GridLayout_rowCount
     */
    public int getRowCount() {
        return mVerticalAxis.getCount();
    }

    /**
     * The rowCount property does not affect layout. RowCount is used
     * only to generate default row/column indices when they are not specified
     * by a component's layout parameters.
     *
     * @param rowCount the number of rows
     *
     * @see #getRowCount()
     * @see LayoutParams#rowGroup
     *
     * @attr ref android.R.styleable#GridLayout_rowCount
     */
    public void setRowCount(int rowCount) {
        mVerticalAxis.setCount(rowCount);
    }

    /**
     * Returns the current number of columns. This is either the last value that was set
     * with {@link #setColumnCount(int)} or, if no such value was set, the maximum
     * value of each the upper bounds defined in {@link LayoutParams#columnGroup}.
     *
     * @return the current number of columns
     *
     * @see #setColumnCount(int)
     * @see LayoutParams#columnGroup
     *
     * @attr ref android.R.styleable#GridLayout_columnCount
     */
    public int getColumnCount() {
        return mHorizontalAxis.getCount();
    }

    /**
     * The columnCount property does not affect layout. ColumnCount is used
     * only to generate default column/column indices when they are not specified
     * by a component's layout parameters.
     *
     * @param columnCount the number of columns.
     *
     * @see #getColumnCount()
     * @see LayoutParams#columnGroup
     *
     * @attr ref android.R.styleable#GridLayout_columnCount
     */
    public void setColumnCount(int columnCount) {
        mHorizontalAxis.setCount(columnCount);
    }

    /**
     * Returns whether or not this GridLayout will allocate default margins when no
     * corresponding layout parameters are defined.
     *
     * @return {@code true} if default margins should be allocated
     *
     * @see #setUseDefaultMargins(boolean)
     *
     * @attr ref android.R.styleable#GridLayout_useDefaultMargins
     */
    public boolean getUseDefaultMargins() {
        return mUseDefaultMargins;
    }

    /**
     * When {@code true}, GridLayout allocates default margins around children
     * based on the child's visual characteristics. Each of the
     * margins so defined may be independently overridden by an assignment
     * to the appropriate layout parameter.
     * <p>
     * When {@code false}, the default value of all margins is zero.
     * <p>
     * When setting to {@code true}, consider setting the value of the
     * {@link #setAlignmentMode(int) alignmentMode}
     * property to {@link #ALIGN_BOUNDS}.
     * <p>
     * The default value of this property is {@code false}.
     *
     * @param useDefaultMargins use {@code true} to make GridLayout allocate default margins
     *
     * @see #getUseDefaultMargins()
     * @see #setAlignmentMode(int)
     *
     * @see MarginLayoutParams#leftMargin
     * @see MarginLayoutParams#topMargin
     * @see MarginLayoutParams#rightMargin
     * @see MarginLayoutParams#bottomMargin
     *
     * @attr ref android.R.styleable#GridLayout_useDefaultMargins
     */
    public void setUseDefaultMargins(boolean useDefaultMargins) {
        mUseDefaultMargins = useDefaultMargins;
        requestLayout();
    }

    /**
     * Returns the alignment mode.
     *
     * @return the alignment mode; either {@link #ALIGN_BOUNDS} or {@link #ALIGN_MARGINS}
     *
     * @see #ALIGN_BOUNDS
     * @see #ALIGN_MARGINS
     *
     * @see #setAlignmentMode(int)
     *
     * @attr ref android.R.styleable#GridLayout_alignmentMode
     */
    public int getAlignmentMode() {
        return mAlignmentMode;
    }

    /**
     * Sets the alignment mode to be used for all of the alignments between the
     * children of this container.
     * <p>
     * The default value of this property is {@link #ALIGN_MARGINS}.
     *
     * @param alignmentMode either {@link #ALIGN_BOUNDS} or {@link #ALIGN_MARGINS}
     *
     * @see #ALIGN_BOUNDS
     * @see #ALIGN_MARGINS
     *
     * @see #getAlignmentMode()
     *
     * @attr ref android.R.styleable#GridLayout_alignmentMode
     */
    public void setAlignmentMode(int alignmentMode) {
        mAlignmentMode = alignmentMode;
        requestLayout();
    }

    /**
     * Returns whether or not row boundaries are ordered by their grid indices.
     *
     * @return {@code true} if row boundaries must appear in the order of their indices,
     *         {@code false} otherwise
     *
     * @see #setRowOrderPreserved(boolean)
     *
     * @attr ref android.R.styleable#GridLayout_rowOrderPreserved
     */
    public boolean isRowOrderPreserved() {
        return mVerticalAxis.isOrderPreserved();
    }

    /**
     * When this property is {@code false}, the default state, GridLayout
     * is at liberty to choose an order that better suits the heights of its children.
     <p>
     * When this property is {@code true}, GridLayout is forced to place the row boundaries
     * so that their associated grid indices are in ascending order in the view.
     * <p>
     * GridLayout implements this specification by creating ordering constraints between
     * the variables that represent the locations of the row boundaries.
     *
     * When this property is {@code true}, constraints are added for each pair of consecutive
     * indices: i.e. between row boundaries: {@code [0..1], [1..2], [2..3],...} etc.
     *
     * When the property is {@code false}, the ordering constraints are placed
     * only between boundaries that separate opposing edges of the layout's children.
     * <p>
     * The default value of this property is {@code false}.

     * @param rowOrderPreserved {@code true} to force GridLayout to respect the order
     *        of row boundaries
     *
     * @see #isRowOrderPreserved()
     *
     * @attr ref android.R.styleable#GridLayout_rowOrderPreserved
     */
    public void setRowOrderPreserved(boolean rowOrderPreserved) {
        mVerticalAxis.setOrderPreserved(rowOrderPreserved);
        invalidateStructure();
        requestLayout();
    }

    /**
     * Returns whether or not column boundaries are ordered by their grid indices.
     *
     * @return {@code true} if column boundaries must appear in the order of their indices,
     *         {@code false} otherwise
     *
     * @see #setColumnOrderPreserved(boolean)
     *
     * @attr ref android.R.styleable#GridLayout_columnOrderPreserved
     */
    public boolean isColumnOrderPreserved() {
        return mHorizontalAxis.isOrderPreserved();
    }

    /**
     * When this property is {@code false}, the default state, GridLayout
     * is at liberty to choose an order that better suits the widths of its children.
     <p>
     * When this property is {@code true}, GridLayout is forced to place the column boundaries
     * so that their associated grid indices are in ascending order in the view.
     * <p>
     * GridLayout implements this specification by creating ordering constraints between
     * the variables that represent the locations of the column boundaries.
     *
     * When this property is {@code true}, constraints are added for each pair of consecutive
     * indices: i.e. between column boundaries: {@code [0..1], [1..2], [2..3],...} etc.
     *
     * When the property is {@code false}, the ordering constraints are placed
     * only between boundaries that separate opposing edges of the layout's children.
     * <p>
     * The default value of this property is {@code false}.
     *
     * @param columnOrderPreserved use {@code true} to force GridLayout to respect the order
     *        of column boundaries.
     *
     * @see #isColumnOrderPreserved()
     *
     * @attr ref android.R.styleable#GridLayout_columnOrderPreserved
     */
    public void setColumnOrderPreserved(boolean columnOrderPreserved) {
        mHorizontalAxis.setOrderPreserved(columnOrderPreserved);
        invalidateStructure();
        requestLayout();
    }

    private static int max2(int[] a, int valueIfEmpty) {
        int result = valueIfEmpty;
        for (int i = 0, N = a.length; i < N; i++) {
            result = Math.max(result, a[i]);
        }
        return result;
    }

    private static <T> T[] append(T[] a, T[] b) {
        T[] result = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private int getDefaultMargin(View c, boolean horizontal, boolean leading) {
        // In the absence of any other information, calculate a default gap such
        // that, in a grid of identical components, the heights and the vertical
        // gaps are in the proportion of the golden ratio.
        // To effect this with equal margins at each edge, set each of the
        // four margin values to half this amount.
        return (int) (c.getMeasuredHeight() / GOLDEN_RATIO / 2);
    }

    private int getDefaultMargin(View c, boolean isAtEdge, boolean horizontal, boolean leading) {
        // todo remove DEFAULT_CONTAINER_MARGIN. Use padding? Seek advice on Themes/Styles, etc.
        return isAtEdge ? DEFAULT_CONTAINER_MARGIN : getDefaultMargin(c, horizontal, leading);
    }

    private int getDefaultMarginValue(View c, LayoutParams p, boolean horizontal, boolean leading) {
        if (!mUseDefaultMargins) {
            return 0;
        }
        Group group = horizontal ? p.columnGroup : p.rowGroup;
        Axis axis = horizontal ? mHorizontalAxis : mVerticalAxis;
        Interval span = group.span;
        boolean isAtEdge = leading ? (span.min == 0) : (span.max == axis.getCount());

        return getDefaultMargin(c, isAtEdge, horizontal, leading);
    }

    private int getMargin(View view, boolean horizontal, boolean leading) {
        LayoutParams lp = getLayoutParams(view);
        int margin = horizontal ?
                (leading ? lp.leftMargin : lp.rightMargin) :
                (leading ? lp.topMargin : lp.bottomMargin);
        return margin == UNDEFINED ? getDefaultMarginValue(view, lp, horizontal, leading) : margin;
    }

    private int getTotalMargin(View child, boolean horizontal) {
        return getMargin(child, horizontal, true) + getMargin(child, horizontal, false);
    }

    private static int valueIfDefined(int value, int defaultValue) {
        return (value != UNDEFINED) ? value : defaultValue;
    }

    // install default indices for cells that don't define them
    private void validateLayoutParams() {
        new Object() {
            public int maxSize = 0;

            private int valueIfDefined2(int value, int defaultValue) {
                if (value != UNDEFINED) {
                    maxSize = 0;
                    return value;
                } else {
                    return defaultValue;
                }
            }

            {
                final boolean horizontal = (mOrientation == HORIZONTAL);
                final int axis = horizontal ? mHorizontalAxis.count : mVerticalAxis.count;
                final int count = valueIfDefined(axis, Integer.MAX_VALUE);

                int row = 0;
                int col = 0;
                for (int i = 0, N = getChildCount(); i < N; i++) {
                    View c = getChildAt(i);
                    if (isGone(c)) continue;
                    LayoutParams lp = getLayoutParams1(c);

                    final Group colGroup = lp.columnGroup;
                    final Interval cols = colGroup.span;
                    final int colSpan = cols.size();

                    final Group rowGroup = lp.rowGroup;
                    final Interval rows = rowGroup.span;
                    final int rowSpan = rows.size();

                    if (horizontal) {
                        row = valueIfDefined2(rows.min, row);

                        int newCol = valueIfDefined(cols.min, (col + colSpan > count) ? 0 : col);
                        if (newCol < col) {
                            row += maxSize;
                            maxSize = 0;
                        }
                        col = newCol;
                        maxSize = max(maxSize, rowSpan);
                    } else {
                        col = valueIfDefined2(cols.min, col);

                        int newRow = valueIfDefined(rows.min, (row + rowSpan > count) ? 0 : row);
                        if (newRow < row) {
                            col += maxSize;
                            maxSize = 0;
                        }
                        row = newRow;
                        maxSize = max(maxSize, colSpan);
                    }

                    lp.setColumnGroupSpan(new Interval(col, col + colSpan));
                    lp.setRowGroupSpan(new Interval(row, row + rowSpan));

                    if (horizontal) {
                        col = col + colSpan;
                    } else {
                        row = row + rowSpan;
                    }
                }
            }
        };
        invalidateStructure();
    }

    private void invalidateStructure() {
        mLayoutParamsValid = false;
        mHorizontalAxis.invalidateStructure();
        mVerticalAxis.invalidateStructure();
        // This can end up being done twice. But better that than not at all.
        invalidateValues();
    }

    private void invalidateValues() {
        // Need null check because requestLayout() is called in View's initializer,
        // before we are set up.
        if (mHorizontalAxis != null && mVerticalAxis != null) {
            mHorizontalAxis.invalidateValues();
            mVerticalAxis.invalidateValues();
        }
    }

    private LayoutParams getLayoutParams1(View c) {
        return (LayoutParams) c.getLayoutParams();
    }

    private LayoutParams getLayoutParams(View c) {
        if (!mLayoutParamsValid) {
            validateLayoutParams();
            mLayoutParamsValid = true;
        }
        return getLayoutParams1(c);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs, mDefaultGravity);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    // Draw grid

    private void drawLine(Canvas graphics, int x1, int y1, int x2, int y2, Paint paint) {
        int dx = getPaddingLeft();
        int dy = getPaddingTop();
        graphics.drawLine(dx + x1, dy + y1, dx + x2, dy + y2, paint);
    }

    private void drawRectangle(Canvas graphics, int x1, int y1, int x2, int y2, Paint paint) {
        x2 = x2 - 1;
        y2 = y2 - 1;
        graphics.drawLine(x1, y1, x1, y2, paint);
        graphics.drawLine(x1, y1, x2, y1, paint);
        graphics.drawLine(x1, y2, x2, y2, paint);
        graphics.drawLine(x2, y1, x2, y2, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (DEBUG) {
            int height = getHeight() - getPaddingTop() - getPaddingBottom();
            int width = getWidth() - getPaddingLeft() - getPaddingRight();

            Paint paint = new Paint();
            paint.setColor(Color.argb(50, 255, 255, 255));

            int[] xs = mHorizontalAxis.locations;
            if (xs != null) {
                for (int i = 0, length = xs.length; i < length; i++) {
                    int x = xs[i];
                    drawLine(canvas, x, 0, x, height - 1, paint);
                }
            }

            int[] ys = mVerticalAxis.locations;
            if (ys != null) {
                for (int i = 0, length = ys.length; i < length; i++) {
                    int y = ys[i];
                    drawLine(canvas, 0, y, width - 1, y, paint);
                }
            }

            // Draw bounds
            paint.setColor(Color.BLUE);
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                drawRectangle(canvas,
                        c.getLeft(),
                        c.getTop(),
                        c.getRight(),
                        c.getBottom(), paint);
            }

            // Draw margins
            paint.setColor(Color.YELLOW);
            for (int i = 0; i < getChildCount(); i++) {
                View c = getChildAt(i);
                drawRectangle(canvas,
                        c.getLeft() - getMargin(c, true, true),
                        c.getTop() - getMargin(c, false, true),
                        c.getRight() + getMargin(c, true, false),
                        c.getBottom() + getMargin(c, false, false), paint);
            }
        }
    }

    // Add/remove

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        invalidateStructure();
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        invalidateStructure();
    }

    @Override
    public void removeViewInLayout(View view) {
        super.removeViewInLayout(view);
        invalidateStructure();
    }

    @Override
    public void removeViewsInLayout(int start, int count) {
        super.removeViewsInLayout(start, count);
        invalidateStructure();
    }

    @Override
    public void removeViewAt(int index) {
        super.removeViewAt(index);
        invalidateStructure();
    }

    // Measurement

    private boolean isGone(View c) {
        return c.getVisibility() == View.GONE;
    }

    private void measureChildWithMargins(View child, int widthMeasureSpec, int heightMeasureSpec) {
        LayoutParams lp = getLayoutParams(child);
        int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                mPaddingLeft + mPaddingRight + getTotalMargin(child, true), lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                mPaddingTop + mPaddingBottom + getTotalMargin(child, false), lp.height);
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    private void measureChildrenWithMargins(int widthMeasureSpec, int heightMeasureSpec) {
        for (int i = 0, N = getChildCount(); i < N; i++) {
            View c = getChildAt(i);
            if (isGone(c)) continue;
            measureChildWithMargins(c, widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        measureChildrenWithMargins(widthSpec, heightSpec);

        int width = getPaddingLeft() + mHorizontalAxis.getMeasure(widthSpec) + getPaddingRight();
        int height = getPaddingTop() + mVerticalAxis.getMeasure(heightSpec) + getPaddingBottom();

        int measuredWidth = Math.max(width, getSuggestedMinimumWidth());
        int measuredHeight = Math.max(height, getSuggestedMinimumHeight());

        setMeasuredDimension(
                resolveSizeAndState(measuredWidth, widthSpec, 0),
                resolveSizeAndState(measuredHeight, heightSpec, 0));
    }

    private int protect(int alignment) {
        return (alignment == UNDEFINED) ? 0 : alignment;
    }

    private int getMeasurement(View c, boolean horizontal) {
        return horizontal ? c.getMeasuredWidth() : c.getMeasuredHeight();
    }

    private int getMeasurementIncludingMargin(View c, boolean horizontal) {
        int result = getMeasurement(c, horizontal);
        if (mAlignmentMode == ALIGN_MARGINS) {
            return result + getTotalMargin(c, horizontal);
        }
        return result;
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        invalidateValues();
    }

    // Layout container

    /**
     * {@inheritDoc}
     */
    /*
     The layout operation is implemented by delegating the heavy lifting to the
     to the mHorizontalAxis and mVerticalAxis instances of the internal Axis class.
     Together they compute the locations of the vertical and horizontal lines of
     the grid (respectively!).

     This method is then left with the simpler task of applying margins, gravity
     and sizing to each child view and then placing it in its cell.
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int targetWidth = right - left;
        int targetHeight = bottom - top;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        mHorizontalAxis.layout(targetWidth - paddingLeft - paddingRight);
        mVerticalAxis.layout(targetHeight - paddingTop - paddingBottom);

        for (int i = 0, N = getChildCount(); i < N; i++) {
            View c = getChildAt(i);
            if (isGone(c)) continue;
            LayoutParams lp = getLayoutParams(c);
            Group columnGroup = lp.columnGroup;
            Group rowGroup = lp.rowGroup;

            Interval colSpan = columnGroup.span;
            Interval rowSpan = rowGroup.span;

            int x1 = mHorizontalAxis.getLocationIncludingMargin(true, colSpan.min);
            int y1 = mVerticalAxis.getLocationIncludingMargin(true, rowSpan.min);

            int x2 = mHorizontalAxis.getLocationIncludingMargin(false, colSpan.max);
            int y2 = mVerticalAxis.getLocationIncludingMargin(false, rowSpan.max);

            int cellWidth = x2 - x1;
            int cellHeight = y2 - y1;

            int pWidth = getMeasurement(c, true);
            int pHeight = getMeasurement(c, false);

            Alignment hAlign = columnGroup.alignment;
            Alignment vAlign = rowGroup.alignment;

            int dx, dy;

            Bounds colBounds = mHorizontalAxis.getGroupBounds().getValue(i);
            Bounds rowBounds = mVerticalAxis.getGroupBounds().getValue(i);

            // Gravity offsets: the location of the alignment group relative to its cell group.
            int c2ax = protect(hAlign.getAlignmentValue(null, cellWidth - colBounds.size(true)));
            int c2ay = protect(vAlign.getAlignmentValue(null, cellHeight - rowBounds.size(true)));

            if (mAlignmentMode == ALIGN_MARGINS) {
                int leftMargin = getMargin(c, true, true);
                int topMargin = getMargin(c, false, true);
                int rightMargin = getMargin(c, true, false);
                int bottomMargin = getMargin(c, false, false);

                // Same calculation as getMeasurementIncludingMargin()
                int mWidth = leftMargin + pWidth + rightMargin;
                int mHeight = topMargin + pHeight + bottomMargin;

                // Alignment offsets: the location of the view relative to its alignment group.
                int a2vx = colBounds.getOffset(c, hAlign, mWidth);
                int a2vy = rowBounds.getOffset(c, vAlign, mHeight);

                dx = c2ax + a2vx + leftMargin;
                dy = c2ay + a2vy + topMargin;

                cellWidth -= leftMargin + rightMargin;
                cellHeight -= topMargin + bottomMargin;
            } else {
                // Alignment offsets: the location of the view relative to its alignment group.
                int a2vx = colBounds.getOffset(c, hAlign, pWidth);
                int a2vy = rowBounds.getOffset(c, vAlign, pHeight);

                dx = c2ax + a2vx;
                dy = c2ay + a2vy;
            }

            int type = PRF;
            int width = hAlign.getSizeInCell(c, pWidth, cellWidth, type);
            int height = vAlign.getSizeInCell(c, pHeight, cellHeight, type);

            int cx = paddingLeft + x1 + dx;
            int cy = paddingTop + y1 + dy;
            c.layout(cx, cy, cx + width, cy + height);
        }
    }

    // Inner classes

    /*
     This internal class houses the algorithm for computing the locations of grid lines;
     along either the horizontal or vertical axis. A GridLayout uses two instances of this class -
     distinguished by the "horizontal" flag which is true for the horizontal axis and false
     for the vertical one.
     */
    private class Axis {
        private static final int MIN_VALUE = -1000000;

        private static final int NEW = 0;
        private static final int PENDING = 1;
        private static final int COMPLETE = 2;

        public final boolean horizontal;

        public int count = UNDEFINED;
        public boolean countValid = false;
        public boolean countWasExplicitySet = false;

        PackedMap<Group, Bounds> groupBounds;
        public boolean groupBoundsValid = false;

        PackedMap<Interval, MutableInt> forwardLinks;
        public boolean forwardLinksValid = false;

        PackedMap<Interval, MutableInt> backwardLinks;
        public boolean backwardLinksValid = false;

        public int[] leadingMargins;
        public boolean leadingMarginsValid = false;

        public int[] trailingMargins;
        public boolean trailingMarginsValid = false;

        public Arc[] arcs;
        public boolean arcsValid = false;

        public int[] locations;
        public boolean locationsValid = false;

        private boolean mOrderPreserved = DEFAULT_ORDER_PRESERVED;

        private MutableInt parentMin = new MutableInt(0);
        private MutableInt parentMax = new MutableInt(-MAX_SIZE);

        private Axis(boolean horizontal) {
            this.horizontal = horizontal;
        }

        private int maxIndex() {
            // note the number Integer.MIN_VALUE + 1 comes up in undefined cells
            int count = -1;
            for (int i = 0, N = getChildCount(); i < N; i++) {
                View c = getChildAt(i);
                if (isGone(c)) continue;
                LayoutParams params = getLayoutParams(c);
                Group g = horizontal ? params.columnGroup : params.rowGroup;
                count = max(count, g.span.min);
                count = max(count, g.span.max);
            }
            return count == -1 ? UNDEFINED : count;
        }

        public int getCount() {
            if (!countValid) {
                count = max(0, maxIndex()); // if there are no cells, the count is zero
                countValid = true;
            }
            return count;
        }

        public void setCount(int count) {
            this.count = count;
            this.countWasExplicitySet = count != UNDEFINED;
        }

        public boolean isOrderPreserved() {
            return mOrderPreserved;
        }

        public void setOrderPreserved(boolean orderPreserved) {
            mOrderPreserved = orderPreserved;
            invalidateStructure();
        }

        private PackedMap<Group, Bounds> createGroupBounds() {
            Assoc<Group, Bounds> assoc = Assoc.of(Group.class, Bounds.class);
            for (int i = 0, N = getChildCount(); i < N; i++) {
                View c = getChildAt(i);
                if (isGone(c)) {
                    assoc.put(Group.GONE, Bounds.GONE);
                } else {
                    LayoutParams lp = getLayoutParams(c);
                    Group group = horizontal ? lp.columnGroup : lp.rowGroup;
                    Bounds bounds = group.alignment.getBounds();
                    assoc.put(group, bounds);
                }
            }
            return assoc.pack();
        }

        private void computeGroupBounds() {
            Bounds[] values = groupBounds.values;
            for (int i = 0; i < values.length; i++) {
                values[i].reset();
            }
            for (int i = 0, N = getChildCount(); i < N; i++) {
                View c = getChildAt(i);
                if (isGone(c)) continue;
                LayoutParams lp = getLayoutParams(c);
                Group g = horizontal ? lp.columnGroup : lp.rowGroup;
                groupBounds.getValue(i).include(c, g, GridLayout.this, this, lp);
            }
        }

        private PackedMap<Group, Bounds> getGroupBounds() {
            if (groupBounds == null) {
                groupBounds = createGroupBounds();
            }
            if (!groupBoundsValid) {
                computeGroupBounds();
                groupBoundsValid = true;
            }
            return groupBounds;
        }

        // Add values computed by alignment - taking the max of all alignments in each span
        private PackedMap<Interval, MutableInt> createLinks(boolean min) {
            Assoc<Interval, MutableInt> result = Assoc.of(Interval.class, MutableInt.class);
            Group[] keys = getGroupBounds().keys;
            for (int i = 0, N = keys.length; i < N; i++) {
                Interval span = min ? keys[i].span : keys[i].span.inverse();
                result.put(span, new MutableInt());
            }
            return result.pack();
        }

        private void computeLinks(PackedMap<Interval, MutableInt> links, boolean min) {
            MutableInt[] spans = links.values;
            for (int i = 0; i < spans.length; i++) {
                spans[i].reset();
            }

            // use getter to trigger a re-evaluation
            Bounds[] bounds = getGroupBounds().values;
            for (int i = 0; i < bounds.length; i++) {
                int size = bounds[i].size(min);
                int value = min ? size : -size;
                MutableInt valueHolder = links.getValue(i);
                valueHolder.value = max(valueHolder.value, value);
            }
        }

        private PackedMap<Interval, MutableInt> getForwardLinks() {
            if (forwardLinks == null) {
                forwardLinks = createLinks(true);
            }
            if (!forwardLinksValid) {
                computeLinks(forwardLinks, true);
                forwardLinksValid = true;
            }
            return forwardLinks;
        }

        private PackedMap<Interval, MutableInt> getBackwardLinks() {
            if (backwardLinks == null) {
                backwardLinks = createLinks(false);
            }
            if (!backwardLinksValid) {
                computeLinks(backwardLinks, false);
                backwardLinksValid = true;
            }
            return backwardLinks;
        }

        private void include(List<Arc> arcs, Interval key, MutableInt size,
                boolean ignoreIfAlreadyPresent) {
            /*
            Remove self referential links.
            These appear:
                . as parental constraints when GridLayout has no children
                . when components have been marked as GONE
            */
            if (key.size() == 0) {
                return;
            }
            // this bit below should really be computed outside here -
            // its just to stop default (row/col > 0) constraints obliterating valid entries
            if (ignoreIfAlreadyPresent) {
                for (Arc arc : arcs) {
                    Interval span = arc.span;
                    if (span.equals(key)) {
                        return;
                    }
                }
            }
            arcs.add(new Arc(key, size));
        }

        private void include(List<Arc> arcs, Interval key, MutableInt size) {
            include(arcs, key, size, true);
        }

        // Group arcs by their first vertex, returning an array of arrays.
        // This is linear in the number of arcs.
        private Arc[][] groupArcsByFirstVertex(Arc[] arcs) {
            int N = getCount() + 1; // the number of vertices
            Arc[][] result = new Arc[N][];
            int[] sizes = new int[N];
            for (Arc arc : arcs) {
                sizes[arc.span.min]++;
           }
            for (int i = 0; i < sizes.length; i++) {
                result[i] = new Arc[sizes[i]];
            }
            // reuse the sizes array to hold the current last elements as we insert each arc
            Arrays.fill(sizes, 0);
            for (Arc arc : arcs) {
                int i = arc.span.min;
                result[i][sizes[i]++] = arc;
            }

            return result;
        }

        private Arc[] topologicalSort(final Arc[] arcs) {
            return new Object() {
                Arc[] result = new Arc[arcs.length];
                int cursor = result.length - 1;
                Arc[][] arcsByVertex = groupArcsByFirstVertex(arcs);
                int[] visited = new int[getCount() + 1];

                void walk(int loc) {
                    switch (visited[loc]) {
                        case NEW: {
                            visited[loc] = PENDING;
                            for (Arc arc : arcsByVertex[loc]) {
                                walk(arc.span.max);
                                result[cursor--] = arc;
                            }
                            visited[loc] = COMPLETE;
                            break;
                        }
                        case PENDING: {
                            assert false;
                            break;
                        }
                        case COMPLETE: {
                            break;
                        }
                    }
                }

                Arc[] sort() {
                    for (int loc = 0, N = arcsByVertex.length; loc < N; loc++) {
                        walk(loc);
                    }
                    assert cursor == -1;
                    return result;
                }
            }.sort();
        }

        private Arc[] topologicalSort(List<Arc> arcs) {
            return topologicalSort(arcs.toArray(new Arc[arcs.size()]));
        }

        private boolean[] findUsed(Collection<Arc> arcs) {
            boolean[] result = new boolean[getCount()];
            for (Arc arc : arcs) {
                Interval span = arc.span;
                int min = min(span.min, span.max);
                int max = max(span.min, span.max);
                for (int i = min; i < max; i++) {
                    result[i] = true;
                }
            }
            return result;
        }

        // todo unify with findUsed above. Both routines analyze which rows/columns are empty.
        private Collection<Interval> getSpacers() {
            List<Interval> result = new ArrayList<Interval>();
            int V = getCount() + 1;
            int[] leadingEdgeCount = new int[V];
            int[] trailingEdgeCount = new int[V];
            for (int i = 0, N = getChildCount(); i < N; i++) {
                View c = getChildAt(i);
                if (isGone(c)) continue;
                LayoutParams lp = getLayoutParams(c);
                Group g = horizontal ? lp.columnGroup : lp.rowGroup;
                Interval span = g.span;
                leadingEdgeCount[span.min]++;
                trailingEdgeCount[span.max]++;
            }

            int lastTrailingEdge = 0;

            // treat the parent's edges like peer edges of the opposite type
            trailingEdgeCount[0] = 1;
            leadingEdgeCount[V - 1] = 1;

            for (int i = 0; i < V; i++) {
                if (trailingEdgeCount[i] > 0) {
                    lastTrailingEdge = i;
                    continue; // if this is also a leading edge, don't add a space of length zero
                }
                if (leadingEdgeCount[i] > 0) {
                    result.add(new Interval(lastTrailingEdge, i));
                }
            }
            return result;
        }

        private void addComponentSizes(List<Arc> result, PackedMap<Interval, MutableInt> links) {
            for (int i = 0; i < links.keys.length; i++) {
                Interval key = links.keys[i];
                include(result, key, links.values[i], false);
            }
        }

        private Arc[] createArcs() {
            List<Arc> mins = new ArrayList<Arc>();
            List<Arc> maxs = new ArrayList<Arc>();

            // Add the minimum values from the components.
            addComponentSizes(mins, getForwardLinks());
            // Add the maximum values from the components.
            addComponentSizes(maxs, getBackwardLinks());

            // Find redundant rows/cols and glue them together with 0-length arcs to link the tree
            boolean[] used = findUsed(mins);
            for (int i = 0; i < getCount(); i++) {
                if (!used[i]) {
                    Interval span = new Interval(i, i + 1);
                    include(mins, span, new MutableInt(0));
                    include(maxs, span.inverse(), new MutableInt(0));
                }
            }

            // Add ordering constraints to prevent row/col sizes from going negative
            if (mOrderPreserved) {
                // Add a constraint for every row/col
                for (int i = 0; i < getCount(); i++) {
                    if (used[i]) {
                        include(mins, new Interval(i, i + 1), new MutableInt(0));
                    }
                }
            } else {
                // Add a constraint for each row/col that separates opposing component edges
                for (Interval gap : getSpacers()) {
                    include(mins, gap, new MutableInt(0));
                }
            }

            // Add the container constraints. Use the version of include that allows
            // duplicate entries in case a child spans the entire grid.
            int N = getCount();
            include(mins, new Interval(0, N), parentMin, false);
            include(maxs, new Interval(N, 0), parentMax, false);

            // Sort
            Arc[] sMins = topologicalSort(mins);
            Arc[] sMaxs = topologicalSort(maxs);

            return append(sMins, sMaxs);
        }

        private void computeArcs() {
            // getting the links validates the values that are shared by the arc list
            getForwardLinks();
            getBackwardLinks();
        }

        public Arc[] getArcs() {
            if (arcs == null) {
                arcs = createArcs();
            }
            if (!arcsValid) {
                computeArcs();
                arcsValid = true;
            }
            return arcs;
        }

        private boolean relax(int[] locations, Arc entry) {
            if (!entry.valid) {
                return false;
            }
            Interval span = entry.span;
            int u = span.min;
            int v = span.max;
            int value = entry.value.value;
            int candidate = locations[u] + value;
            if (candidate > locations[v]) {
                locations[v] = candidate;
                return true;
            }
            return false;
        }

        /*
        Bellman-Ford variant - modified to reduce typical running time from O(N^2) to O(N)

        GridLayout converts its requirements into a system of linear constraints of the
        form:

        x[i] - x[j] < a[k]

        Where the x[i] are variables and the a[k] are constants.

        For example, if the variables were instead labeled x, y, z we might have:

            x - y < 17
            y - z < 23
            z - x < 42

        This is a special case of the Linear Programming problem that is, in turn,
        equivalent to the single-source shortest paths problem on a digraph, for
        which the O(n^2) Bellman-Ford algorithm the most commonly used general solution.

        Other algorithms are faster in the case where no arcs have negative weights
        but allowing negative weights turns out to be the same as accommodating maximum
        size requirements as well as minimum ones.

        Bellman-Ford works by iteratively 'relaxing' constraints over all nodes (an O(N)
        process) and performing this step N times. Proof of correctness hinges on the
        fact that there can be no negative weight chains of length > N - unless a
        'negative weight loop' exists. The algorithm catches this case in a final
        checking phase that reports failure.

        By topologically sorting the nodes and checking this condition at each step
        typical layout problems complete after the first iteration and the algorithm
        completes in O(N) steps with very low constants.
        */
        private void solve(Arc[] arcs, int[] locations) {
            String axis = horizontal ? "horizontal" : "vertical";
            int N = getCount() + 1; // The number of vertices is the number of columns/rows + 1.

            boolean changed = false;
            // We take one extra pass over traditional Bellman-Ford (and omit their final step)
            for (int i = 0; i < N; i++) {
                changed = false;
                for (int j = 0, length = arcs.length; j < length; j++) {
                    changed |= relax(locations, arcs[j]);
                }
                if (!changed) {
                    if (DEBUG) {
                        Log.d(TAG, axis + " iteration completed in " + (1 + i) + " steps of " + N);
                    }
                    return;
                }
            }

            Log.d(TAG, "The " + axis + " constraints contained a contradiction. Resolving... ");
            Log.d(TAG, Arrays.toString(arcs));

            boolean[] culprits = new boolean[arcs.length];
            for (int i = 0; i < N; i++) {
                for (int j = 0, length = arcs.length; j < length; j++) {
                    culprits[j] |= relax(locations, arcs[j]);
                }
            }
            for (int i = 0; i < culprits.length; i++) {
                if (culprits[i]) {
                    Arc arc = arcs[i];
                    // Only remove max values, min values alone cannot be inconsistent
                    if (arc.span.min < arc.span.max) {
                        continue;
                    }
                    Log.d(TAG, "Removing: " + arc);
                    arc.valid = false;
                    break;
                }
            }
            solve1(arcs, locations);
        }

        private void solve1(Arc[] arcs, int[] a) {
            Arrays.fill(a, MIN_VALUE);
            a[0] = 0;
            solve(arcs, a);
        }

        private void computeMargins(boolean leading) {
            int[] margins = leading ? leadingMargins : trailingMargins;
            for (int i = 0, N = getChildCount(); i < N; i++) {
                View c = getChildAt(i);
                if (isGone(c)) continue;
                LayoutParams lp = getLayoutParams(c);
                Group g = horizontal ? lp.columnGroup : lp.rowGroup;
                Interval span = g.span;
                int index = leading ? span.min : span.max;
                margins[index] = max(margins[index], getMargin(c, horizontal, leading));
            }
        }

        private int[] getLeadingMargins() {
            if (leadingMargins == null) {
                leadingMargins = new int[getCount() + 1];
            }
            if (!leadingMarginsValid) {
                computeMargins(true);
                leadingMarginsValid = true;
            }
            return leadingMargins;
        }

        private int[] getTrailingMargins() {
            if (trailingMargins == null) {
                trailingMargins = new int[getCount() + 1];
            }
            if (!trailingMarginsValid) {
                computeMargins(false);
                trailingMarginsValid = true;
            }
            return trailingMargins;
        }

        private void addMargins() {
            int[] leadingMargins = getLeadingMargins();
            int[] trailingMargins = getTrailingMargins();

            int delta = 0;
            for (int i = 0, N = getCount(); i < N; i++) {
                int margins = leadingMargins[i] + trailingMargins[i + 1];
                delta += margins;
                locations[i + 1] += delta;
            }
        }

        private int getLocationIncludingMargin(boolean leading, int index) {
            int location = locations[index];
            int margin;
            if (mAlignmentMode != ALIGN_MARGINS) {
                margin = (leading ? leadingMargins : trailingMargins)[index];
            } else {
                margin = 0;
            }
            return leading ? (location + margin) : (location - margin);
        }

        private void computeLocations(int[] a) {
            solve1(getArcs(), a);
            if (mAlignmentMode != ALIGN_MARGINS) {
                addMargins();
            }
        }

        private int[] getLocations() {
            if (locations == null) {
                int N = getCount() + 1;
                locations = new int[N];
            }
            if (!locationsValid) {
                computeLocations(locations);
                locationsValid = true;
            }
            return locations;
        }

        // External entry points

        private int size(int[] locations) {
            return max2(locations, 0) - locations[0];
        }

        private void setParentConstraints(int min, int max) {
            parentMin.value = min;
            parentMax.value = -max;
            locationsValid = false;
        }

        private int getMeasure(int min, int max) {
            setParentConstraints(min, max);
            return size(getLocations());
        }

        private int getMeasure(int measureSpec) {
            int mode = MeasureSpec.getMode(measureSpec);
            int size = MeasureSpec.getSize(measureSpec);
            switch (mode) {
                case MeasureSpec.UNSPECIFIED: {
                     return getMeasure(0, MAX_SIZE);
                }
                case MeasureSpec.EXACTLY: {
                    return getMeasure(size, size);
                }
                case MeasureSpec.AT_MOST: {
                    return getMeasure(0, size);
                }
                default: {
                    assert false;
                    return 0;
                }
            }
        }

        private void layout(int size) {
            setParentConstraints(size, size);
            getLocations();
        }

        private void invalidateStructure() {
            countValid = false;

            groupBounds = null;
            forwardLinks = null;
            backwardLinks = null;

            leadingMargins = null;
            trailingMargins = null;
            arcs = null;

            locations = null;

            invalidateValues();
        }

        private void invalidateValues() {
            groupBoundsValid = false;
            forwardLinksValid = false;
            backwardLinksValid = false;

            leadingMarginsValid = false;
            trailingMarginsValid = false;
            arcsValid = false;

            locationsValid = false;
        }
    }

    /**
     * Layout information associated with each of the children of a GridLayout.
     * <p>
     * GridLayout supports both row and column spanning and arbitrary forms of alignment within
     * each cell group. The fundamental parameters associated with each cell group are
     * gathered into their vertical and horizontal components and stored
     * in the {@link #rowGroup} and {@link #columnGroup} layout parameters.
     * {@link Group Groups} are immutable structures and may be shared between the layout
     * parameters of different children.
     * <p>
     * The row and column groups contain the leading and trailing indices along each axis
     * and together specify the four grid indices that delimit the cells of this cell group.
     * <p>
     * The {@link Group#alignment alignment} fields of the row and column groups together specify
     * both aspects of alignment within the cell group. It is also possible to specify a child's
     * alignment within its cell group by using the {@link GridLayout.LayoutParams#setGravity(int)}
     * method.
     * <p>
     * See {@link GridLayout} for a description of the conventions used by GridLayout
     * in reference to grid indices.
     *
     * <h4>Default values</h4>
     *
     * <ul>
     *     <li>{@link #width} = {@link #WRAP_CONTENT}</li>
     *     <li>{@link #height} = {@link #WRAP_CONTENT}</li>
     *     <li>{@link #topMargin} = 0 when
     *          {@link GridLayout#setUseDefaultMargins(boolean) useDefaultMargins} is
     *          {@code false}; otherwise {@link #UNDEFINED}, to
     *          indicate that a default value should be computed on demand. </li>
     *     <li>{@link #leftMargin} = 0 when
     *          {@link GridLayout#setUseDefaultMargins(boolean) useDefaultMargins} is
     *          {@code false}; otherwise {@link #UNDEFINED}, to
     *          indicate that a default value should be computed on demand. </li>
     *     <li>{@link #bottomMargin} = 0 when
     *          {@link GridLayout#setUseDefaultMargins(boolean) useDefaultMargins} is
     *          {@code false}; otherwise {@link #UNDEFINED}, to
     *          indicate that a default value should be computed on demand. </li>
     *     <li>{@link #rightMargin} = 0 when
     *          {@link GridLayout#setUseDefaultMargins(boolean) useDefaultMargins} is
     *          {@code false}; otherwise {@link #UNDEFINED}, to
     *          indicate that a default value should be computed on demand. </li>
     *     <li>{@link #rowGroup}{@code .span} = {@code [0, 1]} </li>
     *     <li>{@link #rowGroup}{@code .alignment} = {@link #BASELINE} </li>
     *     <li>{@link #columnGroup}{@code .span} = {@code [0, 1]} </li>
     *     <li>{@link #columnGroup}{@code .alignment} = {@link #LEFT} </li>
     *     <li>{@link #widthSpec} = {@link #FIXED} </li>
     *     <li>{@link #heightSpec} = {@link #FIXED} </li>
     * </ul>
     *
     * @attr ref android.R.styleable#GridLayout_Layout_layout_row
     * @attr ref android.R.styleable#GridLayout_Layout_layout_rowSpan
     * @attr ref android.R.styleable#GridLayout_Layout_layout_heightSpec
     * @attr ref android.R.styleable#GridLayout_Layout_layout_column
     * @attr ref android.R.styleable#GridLayout_Layout_layout_columnSpan
     * @attr ref android.R.styleable#GridLayout_Layout_layout_widthSpec
     * @attr ref android.R.styleable#GridLayout_Layout_layout_gravity
     */
    public static class LayoutParams extends MarginLayoutParams {

        // Default values

        private static final int DEFAULT_WIDTH = WRAP_CONTENT;
        private static final int DEFAULT_HEIGHT = WRAP_CONTENT;
        private static final int DEFAULT_MARGIN = UNDEFINED;
        private static final int DEFAULT_ROW = UNDEFINED;
        private static final int DEFAULT_COLUMN = UNDEFINED;
        private static final Interval DEFAULT_SPAN = new Interval(UNDEFINED, UNDEFINED + 1);
        private static final int DEFAULT_SPAN_SIZE = DEFAULT_SPAN.size();
        private static final Alignment DEFAULT_COLUMN_ALIGNMENT = LEFT;
        private static final Alignment DEFAULT_ROW_ALIGNMENT = BASELINE;
        private static final Group DEFAULT_COLUMN_GROUP =
                new Group(DEFAULT_SPAN, DEFAULT_COLUMN_ALIGNMENT);
        private static final Group DEFAULT_ROW_GROUP =
                new Group(DEFAULT_SPAN, DEFAULT_ROW_ALIGNMENT);
        private static final Spec DEFAULT_SPEC = FIXED;
        private static final int DEFAULT_SPEC_INDEX = 0;

        // Misc

        private static final Rect CONTAINER_BOUNDS = new Rect(0, 0, 2, 2);
        private static final Alignment[] COLUMN_ALIGNMENTS = { LEFT, CENTER, RIGHT };
        private static final Alignment[] ROW_ALIGNMENTS = { TOP, CENTER, BOTTOM };
        private static final Spec[] SPECS = { FIXED, CAN_SHRINK, CAN_STRETCH };

        // TypedArray indices

        private static final int MARGIN = styleable.ViewGroup_MarginLayout_layout_margin;
        private static final int LEFT_MARGIN = styleable.ViewGroup_MarginLayout_layout_marginLeft;
        private static final int TOP_MARGIN = styleable.ViewGroup_MarginLayout_layout_marginTop;
        private static final int RIGHT_MARGIN = styleable.ViewGroup_MarginLayout_layout_marginRight;
        private static final int BOTTOM_MARGIN =
                styleable.ViewGroup_MarginLayout_layout_marginBottom;

        private static final int COLUMN = styleable.GridLayout_Layout_layout_column;
        private static final int COLUMN_SPAN = styleable.GridLayout_Layout_layout_columnSpan;
        private static final int WIDTH_SPEC = styleable.GridLayout_Layout_layout_widthSpec;
        private static final int ROW = styleable.GridLayout_Layout_layout_row;
        private static final int ROW_SPAN = styleable.GridLayout_Layout_layout_rowSpan;
        private static final int HEIGHT_SPEC = styleable.GridLayout_Layout_layout_heightSpec;
        private static final int GRAVITY = styleable.GridLayout_Layout_layout_gravity;

        // Instance variables

        /**
         * The group that specifies the vertical characteristics of the cell group
         * described by these layout parameters.
         */
        public Group rowGroup;
        /**
         * The group that specifies the horizontal characteristics of the cell group
         * described by these layout parameters.
         */
        public Group columnGroup;
        /**
         * The proportional space that should be taken by the associated column group
         * during excess space distribution.
         */
        public Spec widthSpec;
        /**
         * The proportional space that should be taken by the associated row group
         * during excess space distribution.
         */
        public Spec heightSpec;

        // Constructors

        private LayoutParams(
                int width, int height,
                int left, int top, int right, int bottom,
                Group rowGroup, Group columnGroup,
                Spec widthSpec, Spec heightSpec) {
            super(width, height);
            setMargins(left, top, right, bottom);
            this.rowGroup = rowGroup;
            this.columnGroup = columnGroup;
            this.heightSpec = heightSpec;
            this.widthSpec = widthSpec;
        }

        /**
         * Constructs a new LayoutParams instance for this <code>rowGroup</code>
         * and <code>columnGroup</code>. All other fields are initialized with
         * default values as defined in {@link LayoutParams}.
         *
         * @param rowGroup    the rowGroup
         * @param columnGroup the columnGroup
         */
        public LayoutParams(Group rowGroup, Group columnGroup) {
            this(DEFAULT_WIDTH, DEFAULT_HEIGHT,
                    DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN, DEFAULT_MARGIN,
                    rowGroup, columnGroup, DEFAULT_SPEC, DEFAULT_SPEC);
        }

        /**
         * Constructs a new LayoutParams with default values as defined in {@link LayoutParams}.
         */
        public LayoutParams() {
            this(DEFAULT_ROW_GROUP, DEFAULT_COLUMN_GROUP);
        }

        // Copying constructors

        /**
         * {@inheritDoc}
         */
        public LayoutParams(ViewGroup.LayoutParams params) {
            super(params);
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(MarginLayoutParams params) {
            super(params);
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(LayoutParams that) {
            super(that);
            this.columnGroup = that.columnGroup;
            this.rowGroup = that.rowGroup;
            this.widthSpec = that.widthSpec;
            this.heightSpec = that.heightSpec;
        }

        // AttributeSet constructors

        private LayoutParams(Context context, AttributeSet attrs, int defaultGravity) {
            super(context, attrs);
            reInitSuper(context, attrs);
            init(context, attrs, defaultGravity);
        }

        /**
         * {@inheritDoc}
         *
         * Values not defined in the attribute set take the default values
         * defined in {@link LayoutParams}.
         */
        public LayoutParams(Context context, AttributeSet attrs) {
            this(context, attrs, Gravity.NO_GRAVITY);
        }

        // Implementation

        private static boolean definesVertical(int gravity) {
            return gravity > 0 && (gravity & Gravity.VERTICAL_GRAVITY_MASK) != 0;
        }

        private static boolean definesHorizontal(int gravity) {
            return gravity > 0 && (gravity & Gravity.HORIZONTAL_GRAVITY_MASK) != 0;
        }

        private static <T> T getAlignment(T[] alignments, T fill, int min, int max,
                boolean isUndefined, T defaultValue) {
            if (isUndefined) {
                return defaultValue;
            }
            return min != max ? fill : alignments[min];
        }

        // Reinitialise the margins using a different default policy than MarginLayoutParams.
        // Here we use the value UNDEFINED (as distinct from zero) to represent the undefined state
        // so that a layout manager default can be accessed post set up. We need this as, at the
        // point of installation, we do not know how many rows/cols there are and therefore
        // which elements are positioned next to the container's trailing edges. We need to
        // know this as margins around the container's boundary should have different
        // defaults to those between peers.

        // This method could be parametrized and moved into MarginLayout.
        private void reInitSuper(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, styleable.ViewGroup_MarginLayout);
            try {
                int margin = a.getDimensionPixelSize(MARGIN, DEFAULT_MARGIN);

                this.leftMargin = a.getDimensionPixelSize(LEFT_MARGIN, margin);
                this.topMargin = a.getDimensionPixelSize(TOP_MARGIN, margin);
                this.rightMargin = a.getDimensionPixelSize(RIGHT_MARGIN, margin);
                this.bottomMargin = a.getDimensionPixelSize(BOTTOM_MARGIN, margin);
            } finally {
                a.recycle();
            }
        }

        // Gravity. For conversion from the static the integers defined in the Gravity class,
        // use Gravity.apply() to apply gravity to a view of zero size and see where it ends up.
        private static Alignment getColumnAlignment(int gravity, int width) {
            Rect r = new Rect(0, 0, 0, 0);
            Gravity.apply(gravity, 0, 0, CONTAINER_BOUNDS, r);

            boolean fill = (width == MATCH_PARENT);
            Alignment defaultAlignment = fill ? FILL : DEFAULT_COLUMN_ALIGNMENT;
            return getAlignment(COLUMN_ALIGNMENTS, FILL, r.left, r.right,
                    !definesHorizontal(gravity), defaultAlignment);
        }

        private static Alignment getRowAlignment(int gravity, int height) {
            Rect r = new Rect(0, 0, 0, 0);
            Gravity.apply(gravity, 0, 0, CONTAINER_BOUNDS, r);

            boolean fill = (height == MATCH_PARENT);
            Alignment defaultAlignment = fill ? FILL : DEFAULT_ROW_ALIGNMENT;
            return getAlignment(ROW_ALIGNMENTS, FILL, r.top, r.bottom,
                    !definesVertical(gravity), defaultAlignment);
        }

        private void init(Context context, AttributeSet attrs, int defaultGravity) {
            TypedArray a = context.obtainStyledAttributes(attrs, styleable.GridLayout_Layout);
            try {
                int gravity = a.getInt(GRAVITY, defaultGravity);

                int column = a.getInt(COLUMN, DEFAULT_COLUMN);
                int columnSpan = a.getInt(COLUMN_SPAN, DEFAULT_SPAN_SIZE);
                Interval hSpan = new Interval(column, column + columnSpan);
                this.columnGroup = new Group(hSpan, getColumnAlignment(gravity, width));
                this.widthSpec = SPECS[a.getInt(WIDTH_SPEC, DEFAULT_SPEC_INDEX)];

                int row = a.getInt(ROW, DEFAULT_ROW);
                int rowSpan = a.getInt(ROW_SPAN, DEFAULT_SPAN_SIZE);
                Interval vSpan = new Interval(row, row + rowSpan);
                this.rowGroup = new Group(vSpan, getRowAlignment(gravity, height));
                this.heightSpec = SPECS[a.getInt(HEIGHT_SPEC, DEFAULT_SPEC_INDEX)];
            } finally {
                a.recycle();
            }
        }

        /**
         * Describes how the child views are positioned. Default is {@code LEFT | BASELINE}.
         * See {@link android.view.Gravity}.
         *
         * @param gravity the new gravity value
         *
         * @attr ref android.R.styleable#GridLayout_Layout_layout_gravity
         */
        public void setGravity(int gravity) {
            columnGroup = columnGroup.copyWriteAlignment(getColumnAlignment(gravity, width));
            rowGroup = rowGroup.copyWriteAlignment(getRowAlignment(gravity, height));
        }

        @Override
        protected void setBaseAttributes(TypedArray attributes, int widthAttr, int heightAttr) {
            this.width = attributes.getLayoutDimension(widthAttr, DEFAULT_WIDTH);
            this.height = attributes.getLayoutDimension(heightAttr, DEFAULT_HEIGHT);
        }

        private void setRowGroupSpan(Interval span) {
            rowGroup = rowGroup.copyWriteSpan(span);
        }

        private void setColumnGroupSpan(Interval span) {
            columnGroup = columnGroup.copyWriteSpan(span);
        }
    }

    /*
    In place of a HashMap from span to Int, use an array of key/value pairs - stored in Arcs.
    Add the mutables completesCycle flag to avoid creating another hash table for detecting cycles.
     */
    private static class Arc {
        public final Interval span;
        public final MutableInt value;
        public boolean valid = true;

        public Arc(Interval span, MutableInt value) {
            this.span = span;
            this.value = value;
        }

        @Override
        public String toString() {
            return span + " " + (!valid ? "+>" : "->") + " " + value;
        }
    }

    // A mutable Integer - used to avoid heap allocation during the layout operation

    private static class MutableInt {
        public int value;

        private MutableInt() {
            reset();
        }

        private MutableInt(int value) {
            this.value = value;
        }

        private void reset() {
            value = Integer.MIN_VALUE;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    private static class Assoc<K, V> extends ArrayList<Pair<K, V>> {
        private final Class<K> keyType;
        private final Class<V> valueType;

        private Assoc(Class<K> keyType, Class<V> valueType) {
            this.keyType = keyType;
            this.valueType = valueType;
        }

        private static <K, V> Assoc<K, V> of(Class<K> keyType, Class<V> valueType) {
            return new Assoc<K, V>(keyType, valueType);
        }

        public void put(K key, V value) {
            add(Pair.create(key, value));
        }

        @SuppressWarnings(value = "unchecked")
        public PackedMap<K, V> pack() {
            int N = size();
            K[] keys = (K[]) Array.newInstance(keyType, N);
            V[] values = (V[]) Array.newInstance(valueType, N);
            for (int i = 0; i < N; i++) {
                keys[i] = get(i).first;
                values[i] = get(i).second;
            }
            return new PackedMap<K, V>(keys, values);
        }
    }

    /*
    This data structure is used in place of a Map where we have an index that refers to the order
    in which each key/value pairs were added to the map. In this case we store keys and values
    in arrays of a length that is equal to the number of unique keys. We also maintain an
    array of indexes from insertion order to the compacted arrays of keys and values.

    Note that behavior differs from that of a LinkedHashMap in that repeated entries
    *do* get added multiples times. So the length of index is equals to the number of
    items added.

    This is useful in the GridLayout class where we can rely on the order of children not
    changing during layout - to use integer-based lookup for our internal structures
    rather than using (and storing) an implementation of Map<Key, ?>.
     */
    @SuppressWarnings(value = "unchecked")
    private static class PackedMap<K, V> {
        public final int[] index;
        public final K[] keys;
        public final V[] values;

        private PackedMap(K[] keys, V[] values) {
            this.index = createIndex(keys);

            this.keys = compact(keys, index);
            this.values = compact(values, index);
        }

        private K getKey(int i) {
            return keys[index[i]];
        }

        private V getValue(int i) {
            return values[index[i]];
        }

        private static <K> int[] createIndex(K[] keys) {
            int size = keys.length;
            int[] result = new int[size];

            Map<K, Integer> keyToIndex = new HashMap<K, Integer>();
            for (int i = 0; i < size; i++) {
                K key = keys[i];
                Integer index = keyToIndex.get(key);
                if (index == null) {
                    index = keyToIndex.size();
                    keyToIndex.put(key, index);
                }
                result[i] = index;
            }
            return result;
        }

        /*
        Create a compact array of keys or values using the supplied index.
         */
        private static <K> K[] compact(K[] a, int[] index) {
            int size = a.length;
            Class<?> componentType = a.getClass().getComponentType();
            K[] result = (K[]) Array.newInstance(componentType, max2(index, -1) + 1);

            // this overwrite duplicates, retaining the last equivalent entry
            for (int i = 0; i < size; i++) {
                result[index[i]] = a[i];
            }
            return result;
        }
    }

    /*
    For each Group (with a given alignment) we need to store the amount of space required
    before the alignment point and the amount of space required after it. One side of this
    calculation is always 0 for LEADING and TRAILING alignments but we don't make use of this.
    For CENTER and BASELINE alignments both sides are needed and in the BASELINE case no
    simple optimisations are possible.

    The general algorithm therefore is to create a Map (actually a PackedMap) from
    Group to Bounds and to loop through all Views in the group taking the maximum
    of the values for each View.
    */
    private static class Bounds {
        private static final Bounds GONE = new Bounds();

        public int before;
        public int after;
        public boolean canStretch;

        private Bounds() {
            reset();
        }

        protected void reset() {
            before = Integer.MIN_VALUE;
            after = Integer.MIN_VALUE;
            canStretch = false;
        }

        protected void include(int before, int after) {
            this.before = max(this.before, before);
            this.after = max(this.after, after);
        }

        protected int size(boolean min) {
            if (!min && canStretch) {
                return MAX_SIZE;
            }
            return before + after;
        }

        protected int getOffset(View c, Alignment alignment, int size) {
            return before - alignment.getAlignmentValue(c, size);
        }

        protected void include(View c, Group g, GridLayout gridLayout, Axis axis, LayoutParams lp) {
            Spec spec = axis.horizontal ? lp.widthSpec : lp.heightSpec;
            if (spec == CAN_STRETCH) {
                canStretch = true;
            }
            int size = gridLayout.getMeasurementIncludingMargin(c, axis.horizontal);
            // todo test this works correctly when the returned value is UNDEFINED
            int before = g.alignment.getAlignmentValue(c, size);
            include(before, size - before);
        }

        @Override
        public String toString() {
            return "Bounds{" +
                    "before=" + before +
                    ", after=" + after +
                    '}';
        }
    }

    /**
     * An Interval represents a contiguous range of values that lie between
     * the interval's {@link #min} and {@link #max} values.
     * <p>
     * Intervals are immutable so may be passed as values and used as keys in hash tables.
     * It is not necessary to have multiple instances of Intervals which have the same
     * {@link #min} and {@link #max} values.
     * <p>
     * Intervals are often written as {@code [min, max]} and represent the set of values
     * {@code x} such that {@code min <= x < max}.
     */
    static class Interval {
        private static final Interval GONE = new Interval(UNDEFINED, UNDEFINED);

        /**
         * The minimum value.
         */
        public final int min;

        /**
         * The maximum value.
         */
        public final int max;

        /**
         * Construct a new Interval, {@code interval}, where:
         * <ul>
         *     <li> {@code interval.min = min} </li>
         *     <li> {@code interval.max = max} </li>
         * </ul>
         *
         * @param min the minimum value.
         * @param max the maximum value.
         */
        public Interval(int min, int max) {
            this.min = min;
            this.max = max;
        }

        private int size() {
            return max - min;
        }

        private Interval inverse() {
            return new Interval(max, min);
        }

        /**
         * Returns {@code true} if the {@link #getClass class},
         * {@link #min} and {@link #max} properties of this Interval and the
         * supplied parameter are pairwise equal; {@code false} otherwise.
         *
         * @param that the object to compare this interval with
         *
         * @return {@code true} if the specified object is equal to this
         *         {@code Interval}, {@code false} otherwise.
         */
        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null || getClass() != that.getClass()) {
                return false;
            }

            Interval interval = (Interval) that;

            if (max != interval.max) {
                return false;
            }
            if (min != interval.min) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = min;
            result = 31 * result + max;
            return result;
        }

        @Override
        public String toString() {
            return "[" + min + ", " + max + "]";
        }
    }

    /**
     * A group specifies either the horizontal or vertical characteristics of a group of
     * cells.
     * <p>
     * Groups are immutable and so may be shared between views with the same
     * {@code span} and {@code alignment}.
     */
    public static class Group {
        private static final Group GONE = new Group(Interval.GONE, Alignment.GONE);

        /**
         * The grid indices of the leading and trailing edges of this cell group for the
         * appropriate axis.
         * <p>
         * See {@link GridLayout} for a description of the conventions used by GridLayout
         * for grid indices.
         */
        final Interval span;
        /**
         * Specifies how cells should be aligned in this group.
         * For row groups, this specifies the vertical alignment.
         * For column groups, this specifies the horizontal alignment.
         */
        public final Alignment alignment;

        /**
         * Construct a new Group, {@code group}, where:
         * <ul>
         *     <li> {@code group.span = span} </li>
         *     <li> {@code group.alignment = alignment} </li>
         * </ul>
         *
         * @param span      the span
         * @param alignment the alignment
         */
        Group(Interval span, Alignment alignment) {
            this.span = span;
            this.alignment = alignment;
        }

        /**
         * Construct a new Group, {@code group}, where:
         * <ul>
         *     <li> {@code group.span = [start, start + size]} </li>
         *     <li> {@code group.alignment = alignment} </li>
         * </ul>
         *
         * @param start     the start
         * @param size      the size
         * @param alignment the alignment
         */
        public Group(int start, int size, Alignment alignment) {
            this(new Interval(start, start + size), alignment);
        }

        /**
         * Construct a new Group, {@code group}, where:
         * <ul>
         *     <li> {@code group.span = [start, start + 1]} </li>
         *     <li> {@code group.alignment = alignment} </li>
         * </ul>
         *
         * @param start     the start index
         * @param alignment the alignment
         */
        public Group(int start, Alignment alignment) {
            this(start, 1, alignment);
        }

        private Group copyWriteSpan(Interval span) {
            return new Group(span, alignment);
        }

        private Group copyWriteAlignment(Alignment alignment) {
            return new Group(span, alignment);
        }

        /**
         * Returns {@code true} if the {@link #getClass class}, {@link #alignment} and {@code span}
         * properties of this Group and the supplied parameter are pairwise equal,
         * {@code false} otherwise.
         *
         * @param that the object to compare this group with
         *
         * @return {@code true} if the specified object is equal to this
         *         {@code Group}; {@code false} otherwise
         */
        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null || getClass() != that.getClass()) {
                return false;
            }

            Group group = (Group) that;

            if (!alignment.equals(group.alignment)) {
                return false;
            }
            if (!span.equals(group.span)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = span.hashCode();
            result = 31 * result + alignment.hashCode();
            return result;
        }
    }

    /**
     * Alignments specify where a view should be placed within a cell group and
     * what size it should be.
     * <p>
     * The {@link LayoutParams} class contains a {@link LayoutParams#rowGroup rowGroup}
     * and a {@link LayoutParams#columnGroup columnGroup} each of which contains an
     * {@link Group#alignment alignment}. Overall placement of the view in the cell
     * group is specified by the two alignments which act along each axis independently.
     * <p>
     *  The GridLayout class defines the most common alignments used in general layout:
     * {@link #TOP}, {@link #LEFT}, {@link #BOTTOM}, {@link #RIGHT}, {@link #CENTER}, {@link
     * #BASELINE} and {@link #FILL}.
     */
    /*
     * An Alignment implementation must define {@link #getAlignmentValue(View, int, int)},
     * to return the appropriate value for the type of alignment being defined.
     * The enclosing algorithms position the children
     * so that the locations defined by the alignment values
     * are the same for all of the views in a group.
     * <p>
     */
    public static abstract class Alignment {
        private static final Alignment GONE = new Alignment() {
            public int getAlignmentValue(View view, int viewSize) {
                assert false;
                return 0;
            }
        };

        Alignment() {
        }

        /**
         * Returns an alignment value. In the case of vertical alignments the value
         * returned should indicate the distance from the top of the view to the
         * alignment location.
         * For horizontal alignments measurement is made from the left edge of the component.
         *
         * @param view              the view to which this alignment should be applied
         * @param viewSize          the measured size of the view
         * @return the alignment value
         */
        abstract int getAlignmentValue(View view, int viewSize);

        /**
         * Returns the size of the view specified by this alignment.
         * In the case of vertical alignments this method should return a height; for
         * horizontal alignments this method should return the width.
         * <p>
         * The default implementation returns {@code viewSize}.
         *
         * @param view              the view to which this alignment should be applied
         * @param viewSize          the measured size of the view
         * @param cellSize          the size of the cell into which this view will be placed
         * @param measurementType   This parameter is currently unused as GridLayout only supports
         *                          one type of measurement: {@link View#measure(int, int)}.
         *
         * @return the aligned size
         */
        int getSizeInCell(View view, int viewSize, int cellSize, int measurementType) {
            return viewSize;
        }

        Bounds getBounds() {
            return new Bounds();
        }
    }

    private static final Alignment LEADING = new Alignment() {
        public int getAlignmentValue(View view, int viewSize) {
            return 0;
        }

    };

    private static final Alignment TRAILING = new Alignment() {
        public int getAlignmentValue(View view, int viewSize) {
            return viewSize;
        }
    };

    /**
     * Indicates that a view should be aligned with the <em>top</em>
     * edges of the other views in its cell group.
     */
    public static final Alignment TOP = LEADING;

    /**
     * Indicates that a view should be aligned with the <em>bottom</em>
     * edges of the other views in its cell group.
     */
    public static final Alignment BOTTOM = TRAILING;

    /**
     * Indicates that a view should be aligned with the <em>right</em>
     * edges of the other views in its cell group.
     */
    public static final Alignment RIGHT = TRAILING;

    /**
     * Indicates that a view should be aligned with the <em>left</em>
     * edges of the other views in its cell group.
     */
    public static final Alignment LEFT = LEADING;

    /**
     * Indicates that a view should be <em>centered</em> with the other views in its cell group.
     * This constant may be used in both {@link LayoutParams#rowGroup rowGroups} and {@link
     * LayoutParams#columnGroup columnGroups}.
     */
    public static final Alignment CENTER = new Alignment() {
        public int getAlignmentValue(View view, int viewSize) {
            return viewSize >> 1;
        }
    };

    /**
     * Indicates that a view should be aligned with the <em>baselines</em>
     * of the other views in its cell group.
     * This constant may only be used as an alignment in {@link LayoutParams#rowGroup rowGroups}.
     *
     * @see View#getBaseline()
     */
    public static final Alignment BASELINE = new Alignment() {
        public int getAlignmentValue(View view, int viewSize) {
            if (view == null) {
                return UNDEFINED;
            }
            int baseline = view.getBaseline();
            return (baseline == -1) ? UNDEFINED : baseline;
        }

        @Override
        public Bounds getBounds() {
            return new Bounds() {
                /*
                In a baseline aligned row in which some components define a baseline
                and some don't, we need a third variable to properly account for all
                the sizes. This tracks the maximum size of all the components -
                including those that don't define a baseline.
                */
                private int size;

                @Override
                protected void reset() {
                    super.reset();
                    size = Integer.MIN_VALUE;
                }

                @Override
                protected void include(int before, int after) {
                    super.include(before, after);
                    size = max(size, before + after);
                }

                @Override
                protected int size(boolean min) {
                    return max(super.size(min), size);
                }

                @Override
                protected int getOffset(View c, Alignment alignment, int size) {
                    return max(0, super.getOffset(c, alignment, size));
                }
            };
        }
    };

    /**
     * Indicates that a view should expanded to fit the boundaries of its cell group.
     * This constant may be used in both {@link LayoutParams#rowGroup rowGroups} and
     * {@link LayoutParams#columnGroup columnGroups}.
     */
    public static final Alignment FILL = new Alignment() {
        public int getAlignmentValue(View view, int viewSize) {
            return UNDEFINED;
        }

        @Override
        public int getSizeInCell(View view, int viewSize, int cellSize, int measurementType) {
            return cellSize;
        }
    };

    /**
     * Spec's tell GridLayout how to derive minimum and maximum size values for a
     * component. Specifications are made with respect to a child's 'measured size'.
     * A child's measured size is, in turn, controlled by its height and width
     * layout parameters which either specify a size or, in the case of
     * WRAP_CONTENT, defer to the computed size of the component.
     */
    public static abstract class Spec {
    }

    /**
     * Indicates that a view requests precisely the size specified by its layout parameters.
     *
     * @see Spec
     */
    public static final Spec FIXED = new Spec() {
    };

    /**
     * Indicates that a view's size should lie between its minimum and the size specified by
     * its layout parameters.
     *
     * @see Spec
     */
    public static final Spec CAN_SHRINK = new Spec() {
    };

    /**
     * Indicates that a view's size should be greater than or equal to the size specified by
     * its layout parameters.
     *
     * @see Spec
     */
    public static final Spec CAN_STRETCH = new Spec() {
    };

}
