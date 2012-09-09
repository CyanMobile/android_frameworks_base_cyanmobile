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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.R.styleable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.UNSPECIFIED;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * A layout that places its children in a rectangular <em>grid</em>.
 * <p>
 * The grid is composed of a set of infinitely thin lines that separate the
 * viewing area into <em>cells</em>. Throughout the API, grid lines are referenced
 * by grid <em>indices</em>. A grid with <code>N</code> columns
 * has <code>N + 1</code> grid indices that run from <code>0</code>
 * through <code>N</code> inclusive. Regardless of how GridLayout is
 * configured, grid index <code>0</code> is fixed to the leading edge of the
 * container and grid index <code>N</code> is fixed to its trailing edge
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
 * If no child specifies the row and column indices of the cell it
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
 * {@link GridLayout.LayoutParams#rowWeight rowWeight} and
 * {@link GridLayout.LayoutParams#columnWeight columnWeight} layout parameters.
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

    // Misc constants

    private static final String TAG = GridLayout.class.getName();
    private static final boolean DEBUG = false;
    private static final Paint GRID_PAINT = new Paint();
    private static final double GOLDEN_RATIO = (1 + Math.sqrt(5)) / 2;
    private static final int MIN = 0;
    private static final int PRF = 1;
    private static final int MAX = 2;

    // Defaults

    private static final int DEFAULT_ORIENTATION = HORIZONTAL;
    private static final int DEFAULT_COUNT = UNDEFINED;
    private static final boolean DEFAULT_USE_DEFAULT_MARGINS = false;
    private static final boolean DEFAULT_ORDER_PRESERVED = false;
    private static final boolean DEFAULT_MARGINS_INCLUDED = true;
    // todo remove this
    private static final int DEFAULT_CONTAINER_MARGIN = 20;

    // TypedArray indices

    private static final int ORIENTATION = styleable.GridLayout_orientation;
    private static final int ROW_COUNT = styleable.GridLayout_rowCount;
    private static final int COLUMN_COUNT = styleable.GridLayout_columnCount;
    private static final int USE_DEFAULT_MARGINS = styleable.GridLayout_useDefaultMargins;
    private static final int MARGINS_INCLUDED = styleable.GridLayout_marginsIncludedInAlignment;
    private static final int ROW_ORDER_PRESERVED = styleable.GridLayout_rowOrderPreserved;
    private static final int COLUMN_ORDER_PRESERVED = styleable.GridLayout_columnOrderPreserved;

    // Static initialization

    static {
        GRID_PAINT.setColor(Color.argb(50, 255, 255, 255));
    }

    // Instance variables

    private final Axis mHorizontalAxis = new Axis(true);
    private final Axis mVerticalAxis = new Axis(false);
    private boolean mLayoutParamsValid = false;
    private int mOrientation = DEFAULT_ORIENTATION;
    private boolean mUseDefaultMargins = DEFAULT_USE_DEFAULT_MARGINS;
    private boolean mMarginsIncludedInAlignment = DEFAULT_MARGINS_INCLUDED;
    private int mDefaultGravity = Gravity.NO_GRAVITY;

    /* package */ boolean accommodateBothMinAndMax = false;

    // Constructors

    /**
     * {@inheritDoc}
     */
    public GridLayout(Context context) {
        super(context);
        if (DEBUG) {
            setWillNotDraw(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    public GridLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        processAttributes(context, attrs);
    }

    /**
     * {@inheritDoc}
     */
    public GridLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        processAttributes(context, attrs);
    }

    private void processAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, styleable.GridLayout);
        try {
            setRowCount(a.getInteger(ROW_COUNT, DEFAULT_COUNT));
            setColumnCount(a.getInteger(COLUMN_COUNT, DEFAULT_COUNT));
            mOrientation = a.getInteger(ORIENTATION, DEFAULT_ORIENTATION);
            mUseDefaultMargins = a.getBoolean(USE_DEFAULT_MARGINS, DEFAULT_USE_DEFAULT_MARGINS);
            mMarginsIncludedInAlignment = a.getBoolean(MARGINS_INCLUDED, DEFAULT_MARGINS_INCLUDED);
            setRowOrderPreserved(a.getBoolean(ROW_ORDER_PRESERVED, DEFAULT_ORDER_PRESERVED));
            setColumnOrderPreserved(a.getBoolean(COLUMN_ORDER_PRESERVED, DEFAULT_ORDER_PRESERVED));
        } finally {
            a.recycle();
        }
    }

    // Implementation

    /**
     * Returns the current orientation.
     *
     * @return either {@link #HORIZONTAL} or {@link #VERTICAL}. The default
     * is {@link #HORIZONTAL}.
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
     *
     * @param orientation the orientation, either {@link #HORIZONTAL} or {@link #VERTICAL}.
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
     * @param rowCount the number of rows.
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
     * @return true if default margins should be allocated.
     *
     * @see #setUseDefaultMargins(boolean)
     *
     * @attr ref android.R.styleable#GridLayout_useDefaultMargins
     */
    public boolean getUseDefaultMargins() {
        return mUseDefaultMargins;
    }

    /**
     * When true, GridLayout allocates default margins around children
     * based on the child's visual characteristics. Each of the
     * margins so defined may be independently overridden by an assignment
     * to the appropriate layout parameter.
     * <p>
     * When false, the default value of all margins is zero.
     * <p>
     * When setting to true, consider setting the value of the
     * {@link #setMarginsIncludedInAlignment(boolean) marginsIncludedInAlignment}
     * property to false.
     *
     * @param useDefaultMargins use true to make GridLayout allocate default margins
     *
     * @see #getUseDefaultMargins()
     * @see #setMarginsIncludedInAlignment(boolean)
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
     * Returns whether GridLayout aligns the edges of the view or the edges
     * of the larger rectangle created by extending the view by its associated
     * margins.
     *
     * @see #setMarginsIncludedInAlignment(boolean)
     *
     * @return true if alignment is between edges including margins.
     *
     * @attr ref android.R.styleable#GridLayout_marginsIncludedInAlignment
     */
    public boolean getMarginsIncludedInAlignment() {
        return mMarginsIncludedInAlignment;
    }

    /**
     * When true, the bounds of a view are extended outwards according to its
     * margins before the edges of the resulting rectangle are aligned.
     * When false, alignment occurs between the bounds of the view - i.e.
     * {@link #LEFT} alignment means align the left edges of the view.
     *
     * @param marginsIncludedInAlignment true if alignment is between edges including margins.
     *
     * @see #getMarginsIncludedInAlignment()
     *
     * @attr ref android.R.styleable#GridLayout_marginsIncludedInAlignment
     */
    public void setMarginsIncludedInAlignment(boolean marginsIncludedInAlignment) {
        mMarginsIncludedInAlignment = marginsIncludedInAlignment;
        requestLayout();
    }

    /**
     * Returns whether or not row boundaries are ordered by their grid indices.
     *
     * @return true if row boundaries must appear in the order of their indices, false otherwise.
     *         The default is false.
     *
     * @see #setRowOrderPreserved(boolean)
     *
     * @attr ref android.R.styleable#GridLayout_rowOrderPreserved
     */
    public boolean isRowOrderPreserved() {
        return mVerticalAxis.isOrderPreserved();
    }

    /**
     * When this property is <code>false</code>, the default state, GridLayout
     * is at liberty to choose an order that better suits the heights of its children.
     <p>
     * When this property is <code>true</code>, GridLayout is forced to place the row boundaries
     * so that their associated grid indices are in ascending order in the view.
     * <p>
     * GridLayout implements this specification by creating ordering constraints between
     * the variables that represent the locations of the row boundaries.
     *
     * When this property is <code>true</code>, constraints are added for each pair of consecutive
     * indices: i.e. between row boundaries: <code>[0..1], [1..2], [3..4],...</code> etc.
     *
     * When the property is <code>false</code>, the ordering constraints are placed
     * only between boundaries that separate opposing edges of the layout's children.
     *
     * @param rowOrderPreserved use true to force GridLayout to respect the order
     *        of row boundaries.
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
     * @return true if column boundaries must appear in the order of their indices, false otherwise.
     *         The default is false.
     *
     * @see #setColumnOrderPreserved(boolean)
     *
     * @attr ref android.R.styleable#GridLayout_columnOrderPreserved
     */
    public boolean isColumnOrderPreserved() {
        return mHorizontalAxis.isOrderPreserved();
    }

    /**
     * When this property is <code>false</code>, the default state, GridLayout
     * is at liberty to choose an order that better suits the widths of its children.
     <p>
     * When this property is <code>true</code>, GridLayout is forced to place the column boundaries
     * so that their associated grid indices are in ascending order in the view.
     * <p>
     * GridLayout implements this specification by creating ordering constraints between
     * the variables that represent the locations of the column boundaries.
     *
     * When this property is <code>true</code>, constraints are added for each pair of consecutive
     * indices: i.e. between column boundaries: <code>[0..1], [1..2], [3..4],...</code> etc.
     *
     * When the property is <code>false</code>, the ordering constraints are placed
     * only between boundaries that separate opposing edges of the layout's children.
     *
     * @param columnOrderPreserved use true to force GridLayout to respect the order
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

    private static int sum(float[] a) {
        int result = 0;
        for (int i = 0, length = a.length; i < length; i++) {
            result += a[i];
        }
        return result;
    }

    private int getDefaultMargin(View c, boolean leading, boolean horizontal) {
        // In the absence of any other information, calculate a default gap such
        // that, in a grid of identical components, the heights and the vertical
        // gaps are in the proportion of the golden ratio.
        // To effect this with equal margins at each edge, set each of the
        // four margin values to half this amount.
        return (int) (c.getMeasuredHeight() / GOLDEN_RATIO / 2);
    }

    private int getDefaultMargin(View c, boolean isAtEdge, boolean leading, boolean horizontal) {
        // todo remove DEFAULT_CONTAINER_MARGIN. Use padding? Seek advice on Themes/Styles, etc.
        return isAtEdge ? DEFAULT_CONTAINER_MARGIN : getDefaultMargin(c, leading, horizontal);
    }

    private int getDefaultMarginValue(View c, LayoutParams p, boolean leading, boolean horizontal) {
        if (!mUseDefaultMargins) {
            return 0;
        }
        Group group = horizontal ? p.columnGroup : p.rowGroup;
        Axis axis = horizontal ? mHorizontalAxis : mVerticalAxis;
        Interval span = group.span;
        boolean isAtEdge = leading ? (span.min == 0) : (span.max == axis.getCount());

        return getDefaultMargin(c, isAtEdge, leading, horizontal);
    }

    private int getMargin(View view, boolean leading, boolean horizontal) {
        LayoutParams lp = getLayoutParams(view);
        int margin = horizontal ?
                (leading ? lp.leftMargin : lp.rightMargin) :
                (leading ? lp.topMargin : lp.bottomMargin);
        return margin == UNDEFINED ? getDefaultMarginValue(view, lp, leading, horizontal) : margin;
    }

    private static boolean isUndefined(Interval span) {
        return span.min == UNDEFINED || span.max == UNDEFINED;
    }

    private void validateLayoutParams() {
        // install default indices for cells if *none* are defined
        if (mHorizontalAxis.maxIndex1() == UNDEFINED || (mVerticalAxis.maxIndex1() == UNDEFINED)) {
            boolean horizontal = mOrientation == HORIZONTAL;
            int count = horizontal ? mHorizontalAxis.count : mVerticalAxis.count;
            if (count == UNDEFINED) {
                count = Integer.MAX_VALUE;
            }
            int x = 0;
            int y = 0;
            int maxSize = 0;
            for (int i = 0, size = getChildCount(); i < size; i++) {
                LayoutParams lp = getLayoutParams1(getChildAt(i));

                Interval hSpan = lp.columnGroup.span;
                int cellWidth = hSpan.size();

                Interval vSpan = lp.rowGroup.span;
                int cellHeight = vSpan.size();

                if (horizontal) {
                    if (x + cellWidth > count) {
                        x = 0;
                        y += maxSize;
                        maxSize = 0;
                    }
                } else {
                    if (y + cellHeight > count) {
                        y = 0;
                        x += maxSize;
                        maxSize = 0;
                    }
                }
                lp.setHorizontalGroupSpan(new Interval(x, x + cellWidth));
                lp.setVerticalGroupSpan(new Interval(y, y + cellHeight));

                if (horizontal) {
                    x = x + cellWidth;
                } else {
                    y = y + cellHeight;
                }
                maxSize = max(maxSize, horizontal ? cellHeight : cellWidth);
            }
        } else {
            /*
            At least one row and one column index have been defined.
            Assume missing row/cols are in error and set them to zero so that
            they will display top/left and the developer can add the right indices.
            Without this UNDEFINED would cause ArrayIndexOutOfBoundsException.
            */
            for (int i = 0, size = getChildCount(); i < size; i++) {
                LayoutParams lp = getLayoutParams1(getChildAt(i));
                if (isUndefined(lp.columnGroup.span)) {
                    lp.setHorizontalGroupSpan(LayoutParams.DEFAULT_SPAN);
                }
                if (isUndefined(lp.rowGroup.span)) {
                    lp.setVerticalGroupSpan(LayoutParams.DEFAULT_SPAN);
                }
            }
        }
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (DEBUG) {
            int height = getHeight() - getPaddingTop() - getPaddingBottom();
            int width = getWidth() - getPaddingLeft() - getPaddingRight();

            int[] xs = mHorizontalAxis.locations;
            for (int i = 0, length = xs.length; i < length; i++) {
                int x = xs[i];
                drawLine(canvas, x, 0, x, height - 1, GRID_PAINT);
            }
            int[] ys = mVerticalAxis.locations;
            for (int i = 0, length = ys.length; i < length; i++) {
                int y = ys[i];
                drawLine(canvas, 0, y, width - 1, y, GRID_PAINT);
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

    private static int getChildMeasureSpec2(int spec, int padding, int childDimension) {
        int resultSize;
        int resultMode;

        if (childDimension >= 0) {
            resultSize = childDimension;
            resultMode = EXACTLY;
        } else {
            /*
            using the following lines would replicate the logic of ViewGroup.getChildMeasureSpec()

            int specMode = MeasureSpec.getMode(spec);
            int specSize = MeasureSpec.getSize(spec);
            int size = Math.max(0, specSize - padding);

            resultSize = size;
            resultMode = (specMode == EXACTLY && childDimension == LayoutParams.WRAP_CONTENT) ?
                    AT_MOST : specMode;
            */
            resultSize = 0;
            resultMode = UNSPECIFIED;
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    @Override
    protected void measureChild(View child, int parentWidthSpec, int parentHeightSpec) {
        ViewGroup.LayoutParams lp = child.getLayoutParams();

        int childWidthMeasureSpec = getChildMeasureSpec2(parentWidthSpec,
                mPaddingLeft + mPaddingRight, lp.width);
        int childHeightMeasureSpec = getChildMeasureSpec2(parentHeightSpec,
                mPaddingTop + mPaddingBottom, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        measureChildren(widthSpec, heightSpec);

        int computedWidth = getPaddingLeft() + mHorizontalAxis.getMin() + getPaddingRight();
        int computedHeight = getPaddingTop() + mVerticalAxis.getMin() + getPaddingBottom();

        setMeasuredDimension(
                resolveSizeAndState(computedWidth, widthSpec, 0),
                resolveSizeAndState(computedHeight, heightSpec, 0));
    }

    private int protect(int alignment) {
        return (alignment == UNDEFINED) ? 0 : alignment;
    }

    private int getMeasurement(View c, boolean horizontal, int measurementType) {
        return horizontal ? c.getMeasuredWidth() : c.getMeasuredHeight();
    }

    private int getMeasurementIncludingMargin(View c, boolean horizontal, int measurementType) {
        int result = getMeasurement(c, horizontal, measurementType);
        if (mMarginsIncludedInAlignment) {
            int leadingMargin = getMargin(c, true, horizontal);
            int trailingMargin = getMargin(c, false, horizontal);
            return result + leadingMargin + trailingMargin;
        }
        return result;
    }

    private int getAlignmentValue(Alignment alignment, View c, int dim, boolean horizontal, View c1) {
        int result = alignment.getAlignmentValue(c, dim);
        if (mMarginsIncludedInAlignment) {
            int leadingMargin = getMargin(c1, true, horizontal);
            return result + leadingMargin;
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
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int targetWidth = r - l;
        int targetHeight = b - t;

        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        mHorizontalAxis.layout(targetWidth - paddingLeft - paddingRight);
        mVerticalAxis.layout(targetHeight - paddingTop - paddingBottom);

        for (int i = 0, size = getChildCount(); i < size; i++) {
            View view = getChildAt(i);
            LayoutParams lp = getLayoutParams(view);
            Group columnGroup = lp.columnGroup;
            Group rowGroup = lp.rowGroup;

            Interval colSpan = columnGroup.span;
            Interval rowSpan = rowGroup.span;

            int x1 = mHorizontalAxis.getLocationIncludingMargin(view, true, colSpan.min);
            int y1 = mVerticalAxis.getLocationIncludingMargin(view, true, rowSpan.min);

            int x2 = mHorizontalAxis.getLocationIncludingMargin(view, false, colSpan.max);
            int y2 = mVerticalAxis.getLocationIncludingMargin(view, false, rowSpan.max);

            int cellWidth = x2 - x1;
            int cellHeight = y2 - y1;

            int pWidth = getMeasurement(view, true, PRF);
            int pHeight = getMeasurement(view, false, PRF);

            Alignment hAlignment = columnGroup.alignment;
            Alignment vAlignment = rowGroup.alignment;

            int dx, dy;

            if (mMarginsIncludedInAlignment) {
                dx = protect(hAlignment.getAlignmentValue(view, cellWidth - pWidth));
                dy = protect(vAlignment.getAlignmentValue(view, cellHeight - pHeight));
            } else {
                Bounds colBounds = mHorizontalAxis.getGroupBounds().getValue(i);
                Bounds rowBounds = mVerticalAxis.getGroupBounds().getValue(i);

                int mx = protect(hAlignment.getAlignmentValue(null, cellWidth - colBounds.size()));
                int my = protect(vAlignment.getAlignmentValue(null, cellHeight - rowBounds.size()));

                dx = mx + -colBounds.below - hAlignment.getAlignmentValue(view, pWidth);
                dy = my + -rowBounds.below - vAlignment.getAlignmentValue(view, pHeight);
            }

            int width = hAlignment.getSizeInCell(view, pWidth, cellWidth);
            int height = vAlignment.getSizeInCell(view, pHeight, cellHeight);

            int cx = paddingLeft + x1 + dx;
            int cy = paddingTop + y1 + dy;
            view.layout(cx, cy, cx + width, cy + height);
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

        private static final int UNVISITED = 0;
        private static final int PENDING = 1;
        private static final int COMPLETE = 2;

        public final boolean horizontal;

        public int count = UNDEFINED;
        public boolean countValid = false;
        public boolean countWasExplicitySet = false;

        PackedMap<Group, Bounds> groupBounds;
        public boolean groupBoundsValid = false;

        PackedMap<Interval, MutableInt> spanSizes;
        public boolean spanSizesValid = false;

        public int[] leadingMargins;
        public boolean leadingMarginsValid = false;

        public int[] trailingMargins;
        public boolean trailingMarginsValid = false;

        public Arc[] arcs;
        public boolean arcsValid = false;

        public int[] minima;
        public boolean minimaValid = false;

        public float[] weights;
        public int[] locations;

        private boolean mOrderPreserved = DEFAULT_ORDER_PRESERVED;

        private Axis(boolean horizontal) {
            this.horizontal = horizontal;
        }

        private int maxIndex(boolean internal) {
            // note the number Integer.MIN_VALUE + 1 comes up in undefined cells
            int count = -1;
            for (int i = 0, size = getChildCount(); i < size; i++) {
                LayoutParams params = internal ?
                        getLayoutParams1(getChildAt(i)) :
                        getLayoutParams(getChildAt(i));
                Group g = horizontal ? params.columnGroup : params.rowGroup;
                count = max(count, g.span.min);
                count = max(count, g.span.max);
            }
            return count == -1 ? UNDEFINED : count;
        }

        private int maxIndex1() {
            return maxIndex(true);
        }

        public int getCount() {
            if (!countWasExplicitySet && !countValid) {
                count = max(0, maxIndex(false)); // if there are no cells, the count is zero
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
            int N = getChildCount();
            Group[] groups = new Group[N];
            Bounds[] bounds = new Bounds[N];
            for (int i = 0; i < N; i++) {
                LayoutParams lp = getLayoutParams(getChildAt(i));
                Group group = horizontal ? lp.columnGroup : lp.rowGroup;

                groups[i] = group;
                bounds[i] = new Bounds();
            }

            return new PackedMap<Group, Bounds>(groups, bounds);
        }

        private void computeGroupBounds() {
            for (int i = 0; i < groupBounds.values.length; i++) {
                groupBounds.values[i].reset();
            }
            for (int i = 0, N = getChildCount(); i < N; i++) {
                View c = getChildAt(i);
                LayoutParams lp = getLayoutParams(c);
                Group g = horizontal ? lp.columnGroup : lp.rowGroup;

                Bounds bounds = groupBounds.getValue(i);

                int size = getMeasurementIncludingMargin(c, horizontal, PRF);
                // todo test this works correctly when the returned value is UNDEFINED
                int below = getAlignmentValue(g.alignment, c, size, horizontal, c);
                bounds.include(-below, size - below);
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
        private PackedMap<Interval, MutableInt> createSpanSizes() {
            PackedMap<Group, Bounds> groupBounds = getGroupBounds();
            int N = groupBounds.keys.length;
            Interval[] spans = new Interval[N];
            MutableInt[] values = new MutableInt[N];
            for (int i = 0; i < N; i++) {
                Interval key = groupBounds.keys[i].span;

                spans[i] = key;
                values[i] = new MutableInt();
            }
            return new PackedMap<Interval, MutableInt>(spans, values);
        }

        private void computeSpanSizes() {
            MutableInt[] spans = spanSizes.values;
            for (int i = 0; i < spans.length; i++) {
                spans[i].reset();
            }

            Bounds[] bounds = getGroupBounds().values;  // use getter to trigger a re-evaluation
            for (int i = 0; i < bounds.length; i++) {
                int value = bounds[i].size();

                MutableInt valueHolder = spanSizes.getValue(i);
                valueHolder.value = max(valueHolder.value, value);
            }
        }

        private PackedMap<Interval, MutableInt> getSpanSizes() {
            if (spanSizes == null) {
                spanSizes = createSpanSizes();
            }
            if (!spanSizesValid) {
                computeSpanSizes();
                spanSizesValid = true;
            }
            return spanSizes;
        }

        private void include(List<Arc> arcs, Interval key, MutableInt size) {
            // this bit below should really be computed outside here -
            // its just to stop default (col>0) constraints obliterating valid entries
            for (Arc arc : arcs) {
                Interval span = arc.span;
                if (span.equals(key)) {
                    return;
                }
            }
            arcs.add(new Arc(key, size));
        }

        private void include2(List<Arc> arcs, Interval span, MutableInt min, MutableInt max,
                boolean both) {
            include(arcs, span, min);
            if (both) {
                // todo
//                include(arcs, span.inverse(), max.neg());
            }
        }

        private void include2(List<Arc> arcs, Interval span, int min, int max, boolean both) {
            include2(arcs, span, new MutableInt(min), new MutableInt(max), both);
        }

        // Group arcs by their first vertex, returning an array of arrays.
        // This is linear in the number of arcs.
        private Arc[][] groupArcsByFirstVertex(Arc[] arcs) {
            int N = getCount() + 1;// the number of vertices
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

        /*
        Topological sort.
         */
        private Arc[] topologicalSort(final Arc[] arcs, int start) {
        // todo ensure the <start> vertex is added in edge cases
            final List<Arc> result = new ArrayList<Arc>();
            new Object() {
                Arc[][] arcsByFirstVertex = groupArcsByFirstVertex(arcs);
                int[] visited = new int[getCount() + 1];

                boolean completesCycle(int loc) {
                    int state = visited[loc];
                    if (state == UNVISITED) {
                        visited[loc] = PENDING;
                        for (Arc arc : arcsByFirstVertex[loc]) {
                            Interval span = arc.span;
                            // the recursive call
                            if (completesCycle(span.max)) {
                                // which arcs get set here is dependent on the order
                                // in which we explore nodes
                                arc.completesCycle = true;
                            }
                            result.add(arc);
                        }
                        visited[loc] = COMPLETE;
                    } else if (state == PENDING) {
                        return true;
                    } else if (state == COMPLETE) {
                    }
                    return false;
                }
            }.completesCycle(start);
            Collections.reverse(result);
            assert arcs.length == result.size();
            return result.toArray(new Arc[result.size()]);
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
            int N = getCount() + 1;
            int[] leadingEdgeCount = new int[N];
            int[] trailingEdgeCount = new int[N];
            for (int i = 0, size = getChildCount(); i < size; i++) {
                LayoutParams lp = getLayoutParams(getChildAt(i));
                Group g = horizontal ? lp.columnGroup : lp.rowGroup;
                Interval span = g.span;
                leadingEdgeCount[span.min]++;
                trailingEdgeCount[span.max]++;
            }

            int lastTrailingEdge = 0;

            // treat the parent's edges like peer edges of the opposite type
            trailingEdgeCount[0] = 1;
            leadingEdgeCount[N - 1] = 1;

            for (int i = 0; i < N; i++) {
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

        private Arc[] createArcs() {
            List<Arc> spanToSize = new ArrayList<Arc>();

            // Add all the preferred elements that were not defined by the user.
            PackedMap<Interval, MutableInt> spanSizes = getSpanSizes();
            for (int i = 0; i < spanSizes.keys.length; i++) {
                Interval key = spanSizes.keys[i];
                MutableInt value = spanSizes.values[i];
                // todo remove value duplicate
                include2(spanToSize, key, value, value, accommodateBothMinAndMax);
            }

            // Find redundant rows/cols and glue them together with 0-length arcs to link the tree
            boolean[] used = findUsed(spanToSize);
            for (int i = 0; i < getCount(); i++) {
                if (!used[i]) {
                    Interval span = new Interval(i, i + 1);
                    include(spanToSize, span, new MutableInt(0));
                    include(spanToSize, span.inverse(), new MutableInt(0));
                }
            }

            if (mOrderPreserved) {
                // Add preferred gaps
                for (int i = 0; i < getCount(); i++) {
                    if (used[i]) {
                        include2(spanToSize, new Interval(i, i + 1), 0, 0, false);
                    }
                }
            } else {
                for (Interval gap : getSpacers()) {
                    include2(spanToSize, gap, 0, 0, false);
                }
            }
            Arc[] arcs = spanToSize.toArray(new Arc[spanToSize.size()]);
            return topologicalSort(arcs, 0);
        }

        public Arc[] getArcs() {
            if (arcs == null) {
                arcs = createArcs();
            }
            if (!arcsValid) {
                getSpanSizes();
                arcsValid = true;
            }
            return arcs;
        }

        private boolean relax(int[] locations, Arc entry) {
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
        private int[] solve(Arc[] arcs, int[] locations) {
            int N = getCount() + 1; // The number of vertices is the number of columns/rows + 1.

            boolean changed = false;
            // We take one extra pass over traditional Bellman-Ford (and omit their final step)
            for (int i = 0; i < N; i++) {
                changed = false;
                for (int j = 0, length = arcs.length; j < length; j++) {
                    changed = changed | relax(locations, arcs[j]);
                }
                if (!changed) {
                    if (DEBUG) {
                        Log.d(TAG, "Iteration " +
                                " completed after " + (1 + i) + " steps out of " + N);
                    }
                    break;
                }
            }
            if (changed) {
                Log.d(TAG, "*** Algorithm failed to terminate ***");
            }
            return locations;
        }

        private void computeMargins(boolean leading) {
            int[] margins = leading ? leadingMargins : trailingMargins;
            for (int i = 0, size = getChildCount(); i < size; i++) {
                View c = getChildAt(i);
                LayoutParams lp = getLayoutParams(c);
                Group g = horizontal ? lp.columnGroup : lp.rowGroup;
                Interval span = g.span;
                int index = leading ? span.min : span.max;
                margins[index] = max(margins[index], getMargin(c, leading, horizontal));
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
                minima[i + 1] += delta;
            }
        }

        private int getLocationIncludingMargin(View view, boolean leading, int index) {
            int location = locations[index];
            int margin;
            if (!mMarginsIncludedInAlignment) {
                margin = (leading ? leadingMargins : trailingMargins)[index];
            } else {
                margin = getMargin(view, leading, horizontal);
            }
            return leading ? (location + margin) : (location - margin);
        }

        private void computeMinima(int[] a) {
            Arrays.fill(a, MIN_VALUE);
            a[0] = 0;
            solve(getArcs(), a);
            if (!mMarginsIncludedInAlignment) {
                addMargins();
            }
        }

        private int[] getMinima() {
            if (minima == null) {
                int N = getCount() + 1;
                minima = new int[N];
            }
            if (!minimaValid) {
                computeMinima(minima);
                minimaValid = true;
            }
            return minima;
        }

        private void computeWeights() {
            for (int i = 0, N = getChildCount(); i < N; i++) {
                LayoutParams lp = getLayoutParams(getChildAt(i));
                Group g = horizontal ? lp.columnGroup : lp.rowGroup;
                Interval span = g.span;
                int penultimateIndex = span.max - 1;
                weights[penultimateIndex] += horizontal ? lp.columnWeight : lp.rowWeight;
            }
        }

        private float[] getWeights() {
            if (weights == null) {
                int N = getCount() + 1;
                weights = new float[N];
            }
            computeWeights();
            return weights;
        }

        private int[] getLocations() {
            if (locations == null) {
                int N = getCount() + 1;
                locations = new int[N];
            }
            return locations;
        }

        // External entry points

        private int size(int[] locations) {
            return locations[locations.length - 1] - locations[0];
        }

        private int getMin() {
            return size(getMinima());
        }

        private void layout(int targetSize) {
            int[] mins = getMinima();

            int totalDelta = max(0, targetSize - size(mins)); // confine to expansion

            float[] weights = getWeights();
            float totalWeight = sum(weights);

            if (totalWeight == 0f) {
                weights[weights.length - 1] = 1;
                totalWeight = 1;
            }

            int[] locations = getLocations();
            int cumulativeDelta = 0;

            for (int i = 0; i < locations.length; i++) {
                float weight = weights[i];
                int delta = (int) (totalDelta * weight / totalWeight);
                cumulativeDelta += delta;
                locations[i] = mins[i] + cumulativeDelta;

                totalDelta -= delta;
                totalWeight -= weight;
            }
        }

        private void invalidateStructure() {
            countValid = false;

            groupBounds = null;
            spanSizes = null;
            leadingMargins = null;
            trailingMargins = null;
            minima = null;
            weights = null;
            locations = null;

            invalidateValues();
        }

        private void invalidateValues() {
            groupBoundsValid = false;
            spanSizesValid = false;
            arcsValid = false;
            leadingMarginsValid = false;
            trailingMarginsValid = false;
            minimaValid = false;
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
     *          <code>false</code>; otherwise {@link #UNDEFINED}, to
     *          indicate that a default value should be computed on demand. </li>
     *     <li>{@link #leftMargin} = 0 when
     *          {@link GridLayout#setUseDefaultMargins(boolean) useDefaultMargins} is
     *          <code>false</code>; otherwise {@link #UNDEFINED}, to
     *          indicate that a default value should be computed on demand. </li>
     *     <li>{@link #bottomMargin} = 0 when
     *          {@link GridLayout#setUseDefaultMargins(boolean) useDefaultMargins} is
     *          <code>false</code>; otherwise {@link #UNDEFINED}, to
     *          indicate that a default value should be computed on demand. </li>
     *     <li>{@link #rightMargin} = 0 when
     *          {@link GridLayout#setUseDefaultMargins(boolean) useDefaultMargins} is
     *          <code>false</code>; otherwise {@link #UNDEFINED}, to
     *          indicate that a default value should be computed on demand. </li>
     *     <li>{@link #rowGroup}<code>.span</code> = <code>[0, 1]</code> </li>
     *     <li>{@link #rowGroup}<code>.alignment</code> = {@link #BASELINE} </li>
     *     <li>{@link #columnGroup}<code>.span</code> = <code>[0, 1]</code> </li>
     *     <li>{@link #columnGroup}<code>.alignment</code> = {@link #LEFT} </li>
     *     <li>{@link #rowWeight} = <code>0f</code> </li>
     *     <li>{@link #columnWeight} = <code>0f</code> </li>
     * </ul>
     *
     * @attr ref android.R.styleable#GridLayout_Layout_layout_row
     * @attr ref android.R.styleable#GridLayout_Layout_layout_rowSpan
     * @attr ref android.R.styleable#GridLayout_Layout_layout_rowWeight
     * @attr ref android.R.styleable#GridLayout_Layout_layout_column
     * @attr ref android.R.styleable#GridLayout_Layout_layout_columnSpan
     * @attr ref android.R.styleable#GridLayout_Layout_layout_columnWeight
     * @attr ref android.R.styleable#GridLayout_Layout_layout_gravity
     */
    public static class LayoutParams extends MarginLayoutParams {

        // Default values

        private static final int DEFAULT_WIDTH = WRAP_CONTENT;
        private static final int DEFAULT_HEIGHT = WRAP_CONTENT;
        private static final int DEFAULT_MARGIN = UNDEFINED;
        private static final int DEFAULT_ROW = UNDEFINED;
        private static final int DEFAULT_COLUMN = UNDEFINED;
        private static final Interval DEFAULT_SPAN = new Interval(0, 1);
        private static final int DEFAULT_SPAN_SIZE = DEFAULT_SPAN.size();
        private static final Alignment DEFAULT_HORIZONTAL_ALIGNMENT = LEFT;
        private static final Alignment DEFAULT_VERTCIAL_ALGIGNMENT = BASELINE;
        private static final Group DEFAULT_HORIZONTAL_GROUP =
                new Group(DEFAULT_SPAN, DEFAULT_HORIZONTAL_ALIGNMENT);
        private static final Group DEFAULT_VERTICAL_GROUP =
                new Group(DEFAULT_SPAN, DEFAULT_VERTCIAL_ALGIGNMENT);
        private static final int DEFAULT_WEIGHT_0 = 0;
        private static final int DEFAULT_WEIGHT_1 = 1;

        // Misc

        private static final Rect CONTAINER_BOUNDS = new Rect(0, 0, 2, 2);
        private static final Alignment[] HORIZONTAL_ALIGNMENTS = { LEFT, CENTER, RIGHT };
        private static final Alignment[] VERTICAL_ALIGNMENTS = { TOP, CENTER, BOTTOM };

        // TypedArray indices

        private static final int MARGIN = styleable.ViewGroup_MarginLayout_layout_margin;
        private static final int LEFT_MARGIN = styleable.ViewGroup_MarginLayout_layout_marginLeft;
        private static final int TOP_MARGIN = styleable.ViewGroup_MarginLayout_layout_marginTop;
        private static final int RIGHT_MARGIN = styleable.ViewGroup_MarginLayout_layout_marginRight;
        private static final int BOTTOM_MARGIN =
                styleable.ViewGroup_MarginLayout_layout_marginBottom;

        private static final int COLUMN = styleable.GridLayout_Layout_layout_column;
        private static final int COLUMN_SPAN = styleable.GridLayout_Layout_layout_columnSpan;
        private static final int COLUMN_WEIGHT = styleable.GridLayout_Layout_layout_columnWeight;
        private static final int ROW = styleable.GridLayout_Layout_layout_row;
        private static final int ROW_SPAN = styleable.GridLayout_Layout_layout_rowSpan;
        private static final int ROW_WEIGHT = styleable.GridLayout_Layout_layout_rowWeight;
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
         * The proportional space that should be taken by the associated row group
         * during excess space distribution.
         */
        public float rowWeight;
        /**
         * The proportional space that should be taken by the associated column group
         * during excess space distribution.
         */
        public float columnWeight;

        // Constructors

        private LayoutParams(
                int width, int height,
                int left, int top, int right, int bottom,
                Group rowGroup, Group columnGroup, float rowWeight, float columnWeight) {
            super(width, height);
            setMargins(left, top, right, bottom);
            this.rowGroup = rowGroup;
            this.columnGroup = columnGroup;
            this.rowWeight = rowWeight;
            this.columnWeight = columnWeight;
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
                    rowGroup, columnGroup, DEFAULT_WEIGHT_0, DEFAULT_WEIGHT_0);
        }

        /**
         * Constructs a new LayoutParams with default values as defined in {@link LayoutParams}.
         */
        public LayoutParams() {
            this(DEFAULT_HORIZONTAL_GROUP, DEFAULT_VERTICAL_GROUP);
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
            this.columnWeight = that.columnWeight;
            this.rowWeight = that.rowWeight;
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
        private static Alignment getHorizontalAlignment(int gravity, int width) {
            Rect r = new Rect(0, 0, 0, 0);
            Gravity.apply(gravity, 0, 0, CONTAINER_BOUNDS, r);

            boolean fill = width == MATCH_PARENT;
            Alignment defaultAlignment = fill ? FILL : DEFAULT_HORIZONTAL_ALIGNMENT;
            return getAlignment(HORIZONTAL_ALIGNMENTS, FILL, r.left, r.right,
                    !definesHorizontal(gravity), defaultAlignment);
        }

        private static Alignment getVerticalAlignment(int gravity, int height) {
            Rect r = new Rect(0, 0, 0, 0);
            Gravity.apply(gravity, 0, 0, CONTAINER_BOUNDS, r);

            boolean fill = height == MATCH_PARENT;
            Alignment defaultAlignment = fill ? FILL : DEFAULT_VERTCIAL_ALGIGNMENT;
            return getAlignment(VERTICAL_ALIGNMENTS, FILL, r.top, r.bottom,
                    !definesVertical(gravity), defaultAlignment);
        }

        private int getDefaultWeight(int size) {
            return (size == MATCH_PARENT) ? DEFAULT_WEIGHT_1 : DEFAULT_WEIGHT_0;
        }

        private void init(Context context, AttributeSet attrs, int defaultGravity) {
            TypedArray a = context.obtainStyledAttributes(attrs, styleable.GridLayout_Layout);
            try {
                int gravity = a.getInteger(GRAVITY, defaultGravity);

                int column = a.getInteger(COLUMN, DEFAULT_COLUMN);
                int columnSpan = a.getInteger(COLUMN_SPAN, DEFAULT_SPAN_SIZE);
                Interval hSpan = new Interval(column, column + columnSpan);
                this.columnGroup = new Group(hSpan, getHorizontalAlignment(gravity, width));
                this.columnWeight = a.getFloat(COLUMN_WEIGHT, getDefaultWeight(width));

                int row = a.getInteger(ROW, DEFAULT_ROW);
                int rowSpan = a.getInteger(ROW_SPAN, DEFAULT_SPAN_SIZE);
                Interval vSpan = new Interval(row, row + rowSpan);
                this.rowGroup = new Group(vSpan, getVerticalAlignment(gravity, height));
                this.rowWeight = a.getFloat(ROW_WEIGHT, getDefaultWeight(height));
            } finally {
                a.recycle();
            }
        }

        /**
         * Describes how the child views are positioned. Default is <code>LEFT | BASELINE</code>.
         *
         * @param gravity the new gravity. See {@link android.view.Gravity}.
         *
         * @attr ref android.R.styleable#GridLayout_Layout_layout_gravity
         */
        public void setGravity(int gravity) {
            columnGroup = columnGroup.copyWriteAlignment(getHorizontalAlignment(gravity, width));
            rowGroup = rowGroup.copyWriteAlignment(getVerticalAlignment(gravity, height));
        }

        @Override
        protected void setBaseAttributes(TypedArray attributes, int widthAttr, int heightAttr) {
            this.width = attributes.getLayoutDimension(widthAttr, DEFAULT_WIDTH);
            this.height = attributes.getLayoutDimension(heightAttr, DEFAULT_HEIGHT);
        }

        private void setVerticalGroupSpan(Interval span) {
            rowGroup = rowGroup.copyWriteSpan(span);
        }

        private void setHorizontalGroupSpan(Interval span) {
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
        public boolean completesCycle;

        public Arc(Interval span, MutableInt value) {
            this.span = span;
            this.value = value;
        }

        @Override
        public String toString() {
            return span + " " + (completesCycle ? "+>" : "->") + " " + value;
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

        private static int max(int[] a, int valueIfEmpty) {
            int result = valueIfEmpty;
            for (int i = 0, length = a.length; i < length; i++) {
                result = Math.max(result, a[i]);
            }
            return result;
        }

        /*
        Create a compact array of keys or values using the supplied index.
         */
        private static <K> K[] compact(K[] a, int[] index) {
            int size = a.length;
            Class<?> componentType = a.getClass().getComponentType();
            K[] result = (K[]) Array.newInstance(componentType, max(index, -1) + 1);

            // this overwrite duplicates, retaining the last equivalent entry
            for (int i = 0; i < size; i++) {
                result[index[i]] = a[i];
            }
            return result;
        }
    }

    /*
    For each Group (with a given alignment) we need to store the amount of space required
    above the alignment point and the amount of space required below it. One side of this
    calculation is always 0 for LEADING and TRAILING alignments but we don't make use of this.
    For CENTER and BASELINE alignments both sides are needed and in the BASELINE case no
    simple optimisations are possible.

    The general algorithm therefore is to create a Map (actually a PackedMap) from
    Group to Bounds and to loop through all Views in the group taking the maximum
    of the values for each View.
    */
    private static class Bounds {
        public int below;
        public int above;

        private Bounds(int below, int above) {
            this.below = below;
            this.above = above;
        }

        private Bounds() {
            reset();
        }

        private void reset() {
            below = Integer.MAX_VALUE;
            above = Integer.MIN_VALUE;
        }

        private void include(int below, int above) {
            this.below = min(this.below, below);
            this.above = max(this.above, above);
        }

        private int size() {
            return above - below;
        }

        @Override
        public String toString() {
            return "Bounds{" +
                    "below=" + below +
                    ", above=" + above +
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
     * Intervals are often written as <code>[min, max]</code> and represent the set of values
     * <em>x</em> such that <em>min <= x < max</em>.
     */
    /* package */ static class Interval {
        /**
         * The minimum value.
         */
        public final int min;

        /**
         * The maximum value.
         */
        public final int max;

        /**
         * Construct a new Interval, <code>interval</code>, where:
         * <ul>
         *     <li> <code>interval.min = min</code> </li>
         *     <li> <code>interval.max = max</code> </li>
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
         * Returns true if the {@link #getClass class}, {@link #min} and {@link #max} properties
         * of this Interval and the supplied parameter are pairwise equal; false otherwise.
         *
         * @param that the object to compare this interval with.
         *
         * @return {@code true} if the specified object is equal to this
         *         {@code Interval}; {@code false} otherwise.
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
     * <code>span</code> and <code>alignment</code>.
     */
    public static class Group {
        /**
         * The grid indices of the leading and trailing edges of this cell group for the
         * appropriate axis.
         * <p>
         * See {@link GridLayout} for a description of the conventions used by GridLayout
         * for grid indices.
         */
        /* package */ final Interval span;
        /**
         * Specifies how cells should be aligned in this group.
         * For row groups, this specifies the vertical alignment.
         * For column groups, this specifies the horizontal alignment.
         */
        public final Alignment alignment;

        /**
         * Construct a new Group, <code>group</code>, where:
         * <ul>
         *     <li> <code>group.span = span</code> </li>
         *     <li> <code>group.alignment = alignment</code> </li>
         * </ul>
         *
         * @param span      the span.
         * @param alignment the alignment.
         */
        /* package */ Group(Interval span, Alignment alignment) {
            this.span = span;
            this.alignment = alignment;
        }

        /**
         * Construct a new Group, <code>group</code>, where:
         * <ul>
         *     <li> <code>group.span = [min, max]</code> </li>
         *     <li> <code>group.alignment = alignment</code> </li>
         * </ul>
         *
         * @param min       the minimum.
         * @param max       the maximum.
         * @param alignment the alignment.
         */
        public Group(int min, int max, Alignment alignment) {
            this(new Interval(min, max), alignment);
        }

        /**
         * Construct a new Group, <code>group</code>, where:
         * <ul>
         *     <li> <code>group.span = [min, min + 1]</code> </li>
         *     <li> <code>group.alignment = alignment</code> </li>
         * </ul>
         *
         * @param min       the minimum.
         * @param alignment the alignment.
         */
        public Group(int min, Alignment alignment) {
            this(min, min + 1, alignment);
        }

        private Group copyWriteSpan(Interval span) {
            return new Group(span, alignment);
        }

        private Group copyWriteAlignment(Alignment alignment) {
            return new Group(span, alignment);
        }

        /**
         * Returns true if the {@link #getClass class}, {@link #alignment} and <code>span</code>
         * properties of this Group and the supplied parameter are pairwise equal; false otherwise.
         *
         * @param that the object to compare this group with.
         *
         * @return {@code true} if the specified object is equal to this
         *         {@code Group}; {@code false} otherwise.
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
     * An Alignment implementation must define the {@link #getAlignmentValue(View, int)}
     * to return the appropriate value for the type of alignment being defined.
     * The enclosing algorithms position the children
     * so that the values returned from the alignment
     * are the same for all of the views in a group.
     * <p>
     *  The GridLayout class defines the most common alignments used in general layout:
     * {@link #TOP}, {@link #LEFT}, {@link #BOTTOM}, {@link #RIGHT}, {@link #CENTER}, {@link
     * #BASELINE} and {@link #FILL}.
     */
    public static interface Alignment {
        /**
         * Returns an alignment value. In the case of vertical alignments the value
         * returned should indicate the distance from the top of the view to the
         * alignment location.
         * For horizontal alignments measurement is made from the left edge of the component.
         *
         * @param view     the view to which this alignment should be applied.
         * @param viewSize the measured size of the view.
         * @return the alignment value.
         */
        public int getAlignmentValue(View view, int viewSize);

        /**
         * Returns the size of the view specified by this alignment.
         * In the case of vertical alignments this method should return a height; for
         * horizontal alignments this method should return the width.
         *
         * @param view     the view to which this alignment should be applied.
         * @param viewSize the measured size of the view.
         * @param cellSize the size of the cell into which this view will be placed.
         * @return the aligned size.
         */
        public int getSizeInCell(View view, int viewSize, int cellSize);
    }

    private static abstract class AbstractAlignment implements Alignment {
        public int getSizeInCell(View view, int viewSize, int cellSize) {
            return viewSize;
        }
    }

    private static final Alignment LEADING = new AbstractAlignment() {
        public int getAlignmentValue(View view, int viewSize) {
            return 0;
        }

    };

    private static final Alignment TRAILING = new AbstractAlignment() {
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
    public static final Alignment CENTER = new AbstractAlignment() {
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
    public static final Alignment BASELINE = new AbstractAlignment() {
        public int getAlignmentValue(View view, int viewSize) {
            if (view == null) {
                return UNDEFINED;
            }
            // todo do we need to call measure first?
            int baseline = view.getBaseline();
            return baseline == -1 ? UNDEFINED : baseline;
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

        public int getSizeInCell(View view, int viewSize, int cellSize) {
            return cellSize;
        }
    };
}