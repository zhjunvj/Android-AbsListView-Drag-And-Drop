package cn.gavinliu.android.lib.dragdrop;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import cn.gavinliu.android.lib.dragdrop.listener.OnDragDropListener;
import cn.gavinliu.android.lib.dragdrop.widget.DragOrDroppable;
import cn.gavinliu.android.lib.dragdrop.widget.MenuZone;

/**
 * Created by GavinLiu on 2015-5-13
 */
public class DDListView extends ListView implements DragOrDroppable, MultiChoosable,
        AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, AbsListView.OnScrollListener {

    private DragDropController mDDController;
    private DragDropAttacher mDragDropAttacher;
    private OnDragDropListener onDragDropListener;
    private ActionMode mActionMode;
    private SelectionMode mSelectionMode;
    private CheckLongClick mCheckLongClick;
    private static final int TOUCH_SLOP = 10;
    private int mLastMotionX, mLastMotionY;

    private boolean isMultChoise;

    private boolean isSwipChoise;

    private boolean isHandleItem;

    public DDListView(Context context) {
        super(context);
        init();
    }

    public DDListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DDListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mDDController = new DragDropController(getContext());
        mDDController.setDragOrDroppable(this);
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
        setOnScrollListener(this);
    }

    public void addMenuZone(View v, MenuZone.Type menuType) {
        MenuZone menuZone = new MenuZone(v, menuType);
        mDDController.addMenuZone(menuZone);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (isMultChoise && mSelectionMode == SelectionMode.Custom) {
            mDragDropAttacher.updateChooseCount(getCheckedItemCount());
        }

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {


        if (!isSwipChoise) {
            mDDController.startDrag(view, position, id);
        } else {
            isHandleItem = true;
            tempPositon = position;
        }

        switch (mSelectionMode) {
            case Custom:
                isMultChoise = true;
                setChoiceMode(DDListView.CHOICE_MODE_MULTIPLE);
                setItemChecked(position, true);

                mDragDropAttacher.show();
                mDragDropAttacher.updateChooseCount(getCheckedItemCount());

                break;
        }

        return true;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    @Override
    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        super.setMultiChoiceModeListener(new MultiChoiceModeWrapper(listener));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        mDDController.onInterceptTouchEvent(ev);

        return super.onInterceptTouchEvent(ev);
    }

    int tempPositon = -1;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        mDDController.onTouchEvent(ev);

        int x = (int) ev.getX();
        int y = (int) ev.getY();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:

                mLastMotionX = x;
                mLastMotionY = y;

                if (mActionMode != null) {
                    if (mCheckLongClick == null) {
                        mCheckLongClick = new CheckLongClick();
                    }
                    int longPressTimeout = ViewConfiguration.getLongPressTimeout();
                    postDelayed(mCheckLongClick, longPressTimeout);
                }

                break;

            case MotionEvent.ACTION_MOVE:
                if (mCheckLongClick != null && (Math.abs(mLastMotionX - x) > TOUCH_SLOP || Math.abs(mLastMotionY - y) > TOUCH_SLOP)) {
                    removeCallbacks(mCheckLongClick);
                }
                if (isHandleItem && isSwipChoise) {
                    int position = pointToPosition(x, y);
                    if (position > 0 && position < getAdapter().getCount() && position != tempPositon) {
                        boolean isChecked = isItemChecked(position);
                        setItemChecked(position, !isChecked);

                        Log.d("select:", "position:" + position + ", " + !isChecked);

                        tempPositon = position;
                        if (mDragDropAttacher != null) {
                            mDragDropAttacher.updateChooseCount(getCheckedItemCount());
                        }
                    }
                }

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mCheckLongClick != null) {
                    removeCallbacks(mCheckLongClick);
                }
                isHandleItem = false;
                tempPositon = -1;
                break;
        }

        return super.onTouchEvent(ev);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isMultChoise && mSelectionMode == SelectionMode.Custom) {
            exitMultiChoiceMode();
            mDragDropAttacher.hide();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void dragStart() {
        if (onDragDropListener != null) {
            onDragDropListener.onDragStart();
        }
    }

    @Override
    public void dragEnter() {
        if (onDragDropListener != null) {
            onDragDropListener.onDragEnter();
        }
    }

    @Override
    public void dragExit() {
        if (onDragDropListener != null) {
            onDragDropListener.onDragExit();
        }
    }

    @Override
    public void dragEnd() {
        if (onDragDropListener != null) {
            onDragDropListener.onDragEnd();
        }
    }

    @Override
    public void drop(int menuId, int itemPosition, long itemId) {
        if (onDragDropListener != null) {
            onDragDropListener.onDrop(menuId, itemPosition, itemId);
        }

        exitMultiChoiceMode();
        if (mSelectionMode == SelectionMode.Custom) {
            mDragDropAttacher.hide();
        }

    }

    @Override
    public void multiChooseAll() {
        int count = getAdapter().getCount();
        for (int i = 0; i < count; i++) {
            setItemChecked(i, true);
        }

        if (isMultChoise && mSelectionMode == SelectionMode.Custom) {
            mDragDropAttacher.updateChooseCount(getCheckedItemCount());
        }
    }

    @Override
    public void clearMultiChoose() {
        int count = getAdapter().getCount();
        for (int i = 0; i < count; i++) {
            setItemChecked(i, false);
        }

        if (isMultChoise && mSelectionMode == SelectionMode.Custom) {
            mDragDropAttacher.updateChooseCount(getCheckedItemCount());
        }
    }

    @Override
    public void exitMultiChoiceMode() {
        clearMultiChoose();
        isMultChoise = false;
        switch (mSelectionMode) {
            case Custom:
                // TODO bad code,
                ((BaseAdapter) getAdapter()).notifyDataSetChanged();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setChoiceMode(DDListView.CHOICE_MODE_NONE);
                    }
                }, 100);
                break;

            case Official:
                if (mActionMode != null) {
                    mActionMode.finish();
                }
                break;
        }
    }


    private class MultiChoiceModeWrapper implements MultiChoiceModeListener {
        MultiChoiceModeListener mWrapped;
        boolean isDraggable;

        public MultiChoiceModeWrapper(MultiChoiceModeListener listener) {
            this.mWrapped = listener;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            if (mWrapped.onCreateActionMode(mode, menu)) {
                isDraggable = true;
                return true;
            }
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;
            setLongClickable(true);
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            isDraggable = true;
            mActionMode = null;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            if (isDraggable) {
                int item = position - getFirstVisiblePosition();
                View v = getChildAt(item);
                isDraggable = false;

                onItemLongClick(DDListView.this, v, position, id);
            }
            mWrapped.onItemCheckedStateChanged(mode, position, id, checked);
        }
    }

    private class CheckLongClick implements Runnable {

        @Override
        public void run() {
            int position = pointToPosition(mLastMotionX, mLastMotionY);
            long id = getAdapter().getItemId(position);
            int itemNum = position - getFirstVisiblePosition();
            View selectedView = getChildAt(itemNum);

            setItemChecked(position, true);

            onItemLongClick(DDListView.this, selectedView, position, id);
        }
    }

    public void setOnDragDropListener(OnDragDropListener onDragDropListener) {
        this.onDragDropListener = onDragDropListener;
    }

    public void setDragDropAttacher(DragDropAttacher dragDropAttacher) {
        this.mDragDropAttacher = dragDropAttacher;
        mDragDropAttacher.setMultiChoosable(this);
    }

    public void setSelectionMode(SelectionMode selectionMode) {
        this.mSelectionMode = selectionMode;

        switch (selectionMode) {
            case Official:
                setChoiceMode(CHOICE_MODE_MULTIPLE_MODAL);
                break;
        }
    }

    public void setIsSwipChoise(boolean isSwipChoise) {
        this.isSwipChoise = isSwipChoise;
    }
}
