package lx.af.view.SwipeRefresh;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

/**
 * author: lx
 * date: 15-12-15
 */
public class SwipeRefreshListLayout extends SwipeRefreshLayout implements
        AbsListView.OnScrollListener {

    /**
     * callback when scrolled to the bottom and load more data is required
     */
    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public enum LoadState {
        IDLE, LOADING, NO_MORE,
    }

    // ============================================

    private static final boolean DEBUG = false;
    private static final int LOAD_FAIL_SCROLL_DURATION = 300;

    private ListView mListView;
    private ILoadMoreFooter mLoadMoreFooter;
    private OnLoadMoreListener mOnLoadMoreListener;
    private AbsListView.OnScrollListener mOnScrollListener;

    private LoadState mState = LoadState.IDLE;
    private int mLoadMorePreCount = 0;
    private boolean mIsFooterViewInit = false;
    private boolean mIsPrevLoadMoreFailed = false;

    public SwipeRefreshListLayout(Context context) {
        this(context, null);
        setColorSchemeColors(Color.parseColor("#01c6f0"));
    }

    public SwipeRefreshListLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setColorSchemeColors(Color.parseColor("#01c6f0"));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mListView == null) {
            getListView();
        }
    }

    private void getListView() {
        int count = getChildCount();
        if (count <= 0) {
            return;
        }

        for (int i = 0; i < count; i ++) {
            View childView = getChildAt(i);
            if (childView instanceof ListView) {
                mListView = (ListView) childView;
                mListView.setOnScrollListener(this);
                if (!mIsFooterViewInit && mLoadMoreFooter != null) {
                    mLoadMoreFooter.init(this, mListView);
                    mLoadMoreFooter.refreshLoadState(mListView, mState);
                    mIsFooterViewInit = true;
                }
                break;
            }
        }
    }

    private void loadMore() {
        setLoading(true);
        log("load more, state=" + mState);
        if (mOnLoadMoreListener != null) {
            mOnLoadMoreListener.onLoadMore();
        }
    }

    private static void log(String msg) {
        if (DEBUG) {
            Log.d("SwipeRefresh", msg);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        log("onScroll, state=" + mState + ", preCount=" + mLoadMorePreCount +
                ", prevLoadFailed=" + mIsPrevLoadMoreFailed + ", " +
                firstVisibleItem + "|" + visibleItemCount + "|" + totalItemCount);
        int preCount = mIsPrevLoadMoreFailed ? 0 : mLoadMorePreCount;
        if (mOnLoadMoreListener != null && mState == LoadState.IDLE && !isRefreshing() &&
                totalItemCount != 0 &&
                firstVisibleItem + visibleItemCount + preCount >= totalItemCount &&
                totalItemCount != mListView.getHeaderViewsCount() + mListView.getFooterViewsCount()) {
            int delay = mIsPrevLoadMoreFailed ? LOAD_FAIL_SCROLL_DURATION + 100 : 100;
            removeCallbacks(mDelayLoadMoreRunnable);
            postDelayed(mDelayLoadMoreRunnable, delay);
        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    private Runnable mDelayLoadMoreRunnable = new Runnable() {
        @Override
        public void run() {
            loadMore();
        }
    };

    // =============================================

    public LoadState getLoadState() {
        return mState;
    }

    public void setLoadState(LoadState state) {
        mState = state;
        post(new Runnable() {
            @Override
            public void run() {
                if (mState == LoadState.LOADING) {
                    mIsPrevLoadMoreFailed = false;
                } else if (isRefreshing()) {
                    setRefreshing(false);
                }
                if (mOnLoadMoreListener == null) {
                    return;
                }
                if (mLoadMoreFooter == null) {
                    mLoadMoreFooter = new DefaultLoadMoreFooter(getContext());
                    if (mListView != null) {
                        mLoadMoreFooter.init(SwipeRefreshListLayout.this, mListView);
                        mIsFooterViewInit = true;
                    }
                }
                if (mIsFooterViewInit) {
                    mLoadMoreFooter.refreshLoadState(mListView, mState);
                }
            }
        });
    }

    public void setLoading(boolean loading) {
        if (loading) {
            setLoadState(LoadState.LOADING);
        } else {
            setRefreshing(false);
            if (mState == LoadState.LOADING) {
                setLoadState(LoadState.IDLE);
            }
        }
        log("setLoading, end, state=" + mState);
    }

    public void setLoadMoreEnabled(boolean enabled) {
        setLoadState(enabled ? LoadState.IDLE : LoadState.NO_MORE);
    }

    public void setLoadMoreFailed() {
        log("setLoadMoreFailed, state=" + mState);
        mIsPrevLoadMoreFailed = true;
        if (mLoadMoreFooter != null) {
            View footerView = (View) mLoadMoreFooter;
            int scroll = footerView.getHeight() + mListView.getDividerHeight() + 2;
            mListView.smoothScrollBy(-scroll, LOAD_FAIL_SCROLL_DURATION);
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                setLoading(false);
            }
        }, LOAD_FAIL_SCROLL_DURATION + 150);
    }

    public void setLoadMorePreCount(int count) {
        mLoadMorePreCount = count;
    }

    public void setLoadMoreFooterView(ILoadMoreFooter footer) {
        if (!(footer instanceof View)) {
            throw new IllegalArgumentException("footer must be instance of view");
        }
        mLoadMoreFooter = footer;
        if (mListView != null) {
            mLoadMoreFooter.init(this, mListView);
            mLoadMoreFooter.refreshLoadState(mListView, mState);
            mIsFooterViewInit = true;
        }
    }

    public View getLoadMoreFooterView() {
        if (mLoadMoreFooter == null) {
            mLoadMoreFooter = new DefaultLoadMoreFooter(getContext());
        }
        return (View) mLoadMoreFooter;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener loadListener) {
        mOnLoadMoreListener = loadListener;
    }

    public void setOnScrollListener(AbsListView.OnScrollListener listener) {
        mOnScrollListener = listener;
    }

    // =============================================

}
