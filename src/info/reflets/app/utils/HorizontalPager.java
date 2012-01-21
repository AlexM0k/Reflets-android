package info.reflets.app.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Scroller;

/**
 * A view group that allows users to switch between multiple screens (layouts) in the same way as
 * the Android home screen (Launcher application).
 * <p>
 * You can add and remove views using the normal methods {@link ViewGroup#addView(View)},
 * {@link ViewGroup#removeView(View)} etc. You may want to listen for updates by calling
 * {@link HorizontalPager#setOnScreenSwitchListener(OnScreenSwitchListener)} in order to perform
 * operations once a new screen has been selected.
 *
 * Modifications from original version (ysamlan): Animate argument in setCurrentScreen and duration
 * in snapToScreen; onInterceptTouchEvent handling to support nesting a vertical Scrollview inside
 * the RealViewSwitcher; allowing snapping to a view even during an ongoing scroll; snap to
 * next/prev view on 25% scroll change; density-independent swipe sensitivity; width-independent
 * pager animation durations on scrolling to properly handle large screens without excessively
 * long animations.
 *
 * Other modifications:
 * (aveyD) Handle orientation changes properly and fully snap to the right position.
 *
 * @author Marc Reichelt, <a href="http://www.marcreichelt.de/">http://www.marcreichelt.de/</a>
 * @version 0.1.0
 */
public final class HorizontalPager extends ViewGroup
{
	public enum ScrollWay {
		BOTH,
		LEFT,
		RIGHT
	}
	
	private static final String  	LOG_TAG = HorizontalPager.class.getSimpleName();

    /*
     * How long to animate between screens when programmatically setting with setCurrentScreen using
     * the animate parameter
     */
    private static final int 		ANIMATION_SCREEN_SET_DURATION_MILLIS 	= 500;
    // What fraction (1/x) of the screen the user must swipe to indicate a page change
    private static final int 		FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE 		= 4;
    private static final int 		INVALID_SCREEN 							= -1;
    /*
     * Velocity of a swipe (in density-independent pixels per second) to force a swipe to the
     * next/previous screen. Adjusted into mDensityAdjustedSnapVelocity on init.
     */
    private static final int 		SNAP_VELOCITY_DIP_PER_SECOND 			= 600;
    // Argument to getVelocity for units to give pixels per second (1 = pixels per millisecond).
    private static final int 		VELOCITY_UNIT_PIXELS_PER_SECOND 		= 1000;

    private static final int		TOUCH_STATE_REST 						= 0;
    private static final int 		TOUCH_STATE_HORIZONTAL_SCROLLING 		= 1;
    private static final int 		TOUCH_STATE_VERTICAL_SCROLLING 			= -1;
    
    private	static Integer			EMPTY_VIEW 						= new Integer(0);
    
    private int						mCurrentScreen;
    private int						mLastScreen						= 0;
    private int 					mDensityAdjustedSnapVelocity;
    private boolean 				mFirstLayout 					= true;
    private float					mLastMotionX;
    private float 					mLastMotionY;
    private OnScreenSwitchListener	mOnScreenSwitchListener;
    private int 					mMaximumVelocity;
    private int 					mNextScreen 					= INVALID_SCREEN;
    private Scroller 				mScroller;
    private int 					mTouchSlop;
    private int 					mTouchState						= TOUCH_STATE_REST;
    private VelocityTracker 		mVelocityTracker;
    private int						mLastSeenLayoutWidth 			= -1;
    private int						mNbChild						= 0;
    private boolean					mScrollEnabled					= true;
    private ScrollWay				mScrollWayEnabled				= ScrollWay.BOTH;
    private int						mStartScrollY					= Integer.MAX_VALUE;
    private int						mEndScrollY						= 0;

    
    /**
     * Simple constructor to use when creating a view from code.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     */
    public HorizontalPager(final Context context, int startInit, int nbChild) {
        super(context);
        init(startInit, nbChild);
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme
     * and the given AttributeSet.
     *
     * <p>
     * The method onFinishInflate() will be called after all children have been
     * added.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @see #View(Context, AttributeSet, int)
     */
    public HorizontalPager(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        init(attrs.getAttributeIntValue(null, "start_child", 0), attrs.getAttributeIntValue(null, "nb_child", 0));
    }

    /**
     * Sets up the scroller and touch/fling sensitivity parameters for the pager.
     */
    private void init(int startInit, int nbChild)
    {
    	Log.d(LOG_TAG, "INIT nbSlide = " + nbChild);
    	
        mScroller = new Scroller(getContext());

        // Calculate the density-dependent snap velocity in pixels
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(displayMetrics);
        mDensityAdjustedSnapVelocity = (int) (displayMetrics.density * SNAP_VELOCITY_DIP_PER_SECOND);

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        setNbChild(startInit, nbChild);
    }
    
    public void setNbChild(int startInit, int nbChild)
    {
    	mNbChild = nbChild; // - startInit;
    	
        for (int i = startInit; i < nbChild; i++) {
        	addView(createEmptyView());
        }
    }
    
    public void setEnabled(boolean doScroll){
    	mScrollEnabled = doScroll;
    }

    public void setScrollLimitOff(int startY, int endY){
    	mStartScrollY = startY;
    	mEndScrollY = endY;
    }
    
    public void setScrollWay(ScrollWay way){
    	mScrollWayEnabled = way;
    }
    
    private View createEmptyView()
    {
    	View v = new View(getContext());
    	v.setTag(EMPTY_VIEW);
    	return v;
    }
    
    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("ViewSwitcher can only be used in EXACTLY mode.");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("ViewSwitcher can only be used in EXACTLY mode.");
        }

        // The children are given the same width and height as the workspace
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }
        
        if (mFirstLayout) {
            scrollTo(mCurrentScreen * width, 0);
            mFirstLayout = false;
        }

        else if (width != mLastSeenLayoutWidth) { // Width has changed
            /*
             * Recalculate the width and scroll to the right position to be sure we're in the right
             * place in the event that we had a rotation that didn't result in an activity restart
             * (code by aveyD). Without this you can end up between two pages after a rotation.
             */
            Display display =((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int displayWidth = display.getWidth();

            mNextScreen = Math.max(0, Math.min(getCurrentScreen(), getChildCount() - 1));

            final int newX = mNextScreen * displayWidth;
            final int delta = newX - getScrollX();

            mScroller.startScroll(getScrollX(), 0, delta, 0, 0);
        }

        mLastSeenLayoutWidth = width;
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b)
    {
        int childLeft = 0;
        final int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child != null && child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent ev)
    {	
		final int action = ev.getAction();
		boolean intercept = false;

		switch (action) {
		case MotionEvent.ACTION_MOVE:

            final float y = ev.getY();
            
//          Log.d(LOG_TAG, "y = " + y + " mStartScrollY = " + mStartScrollY);
          
            if (y < mStartScrollY || y > mEndScrollY){
            	
//            	int xDiff = (int)(ev.getX()-mLastMotionX);
//            	if (xDiff < 0 && mScrollWayEnabled == ScrollWay.RIGHT ||
//            			xDiff > 0 && mScrollWayEnabled == ScrollWay.LEFT){
//            		mTouchState = TOUCH_STATE_REST;
//            	}
//            	else{
            	
//    			final int xDiff = (int) Math.abs(ev.getX() - mLastMotionX);
            		final float xDiff = (int) Math.abs(ev.getX() - mLastMotionX);
	    			if (xDiff > mTouchSlop) {
	    				mTouchState = TOUCH_STATE_HORIZONTAL_SCROLLING;
	    				mLastMotionX = ev.getX();
	    			}
//            	}

    			final int yDiff = (int) Math.abs(ev.getY() - mLastMotionY);
    			if (yDiff > mTouchSlop) {
                   	mTouchState = TOUCH_STATE_VERTICAL_SCROLLING;
    			}

    			// magic sauce!
    			if (Math.abs(xDiff * 2) > Math.abs(yDiff) && xDiff > mTouchSlop)
    				return true;
            }
			break;

		case MotionEvent.ACTION_UP:
			mTouchState = TOUCH_STATE_REST;
			return false;

		case MotionEvent.ACTION_DOWN:
			mLastMotionY = ev.getY();
			mLastMotionX = ev.getX();
			break;

		default:
			break;
		}

		return intercept;
	}

    @Override
    public boolean onTouchEvent(final MotionEvent ev)
    {
    	if (!mScrollEnabled){
    		return false;
    	}

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getX();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
//            	Log.d(LOG_TAG, "onTouchEvent DOWN");

                /*
                 * If being flinged and user touches, stop the fling. isFinished will be false if
                 * being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionX = x;

                mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_HORIZONTAL_SCROLLING;
                break;

            case MotionEvent.ACTION_MOVE:
            	final int xDiff = (int)(x - mLastMotionX);
//                final int xDiff = (int) Math.abs(x - mLastMotionX);
                boolean xMoved = Math.abs(xDiff) > mTouchSlop;

                if (xMoved) {
                    // Scroll if the user moved far enough along the X axis
                    mTouchState = TOUCH_STATE_HORIZONTAL_SCROLLING;
                }

//                Log.d(LOG_TAG, "xDiff: " + xDiff + " mScrollWayEnabled: " + mScrollWayEnabled.toString());
                
            	if (xDiff > 0 && mScrollWayEnabled == ScrollWay.RIGHT ||
            			xDiff < 0 && mScrollWayEnabled == ScrollWay.LEFT){
            		mTouchState = TOUCH_STATE_REST;
            	}
                
                if (mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING) {
                    // Scroll to follow the motion event
                    final int deltaX = (int) (mLastMotionX - x);
                    mLastMotionX = x;
                    final int scrollX = getScrollX();

                    if (deltaX < 0) {
                        if (scrollX > 0) {
                            scrollBy(Math.max(-scrollX, deltaX), 0);
                        }
                    }
                    else if (deltaX > 0) {
                        final int availableToScroll = getChildAt(getChildCount() - 1).getRight() - scrollX - getWidth();

                        if (availableToScroll > 0) {
                            scrollBy(Math.min(availableToScroll, deltaX), 0);
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
//            	Log.d(LOG_TAG, "onTouchEvent UP");

                if (mTouchState == TOUCH_STATE_HORIZONTAL_SCROLLING) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(VELOCITY_UNIT_PIXELS_PER_SECOND, mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity();

                    if (velocityX > mDensityAdjustedSnapVelocity && mCurrentScreen > 0) {
                        // Fling hard enough to move left
                        snapToScreen(mCurrentScreen - 1);
                    }
                    else if (velocityX < -mDensityAdjustedSnapVelocity && mCurrentScreen < getChildCount() - 1) {
                        // Fling hard enough to move right
                        snapToScreen(mCurrentScreen + 1);
                    }
                    else {
                        snapToDestination();
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }

                mTouchState = TOUCH_STATE_REST;
                break;

            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    public void computeScroll()
    {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
        else if (mNextScreen != INVALID_SCREEN) {
        	mCurrentScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));

            displayScreen(mNextScreen);

            mNextScreen = INVALID_SCREEN;
        }
    }

    /**
     * Returns the index of the currently displayed screen.
     *
     * @return The index of the currently displayed screen.
     */
    public int getCurrentScreen() {
        return mCurrentScreen;
    }

    /**
     * Sets the current screen.
     *
     * @param currentScreen The new screen.
     * @param animate True to smoothly scroll to the screen, false to snap instantly
     */
    public void setCurrentScreen(final int currentScreen, final boolean animate)
    {
        mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));

        if (animate) {
        	snapToScreen(currentScreen, ANIMATION_SCREEN_SET_DURATION_MILLIS);
        }
        else {
            scrollTo(mCurrentScreen * getWidth(), 0);
        }

        invalidate();
    }

    /**
     * Sets the {@link OnScreenSwitchListener}.
     *
     * @param onScreenSwitchListener The listener for switch events.
     */
    public void setOnScreenSwitchListener(final OnScreenSwitchListener onScreenSwitchListener, int startPos)
    {
        mOnScreenSwitchListener = onScreenSwitchListener;
        
        Log.d(LOG_TAG, "setOnScreenSwitchListener startPos = " + startPos + " mNbChild = " + mNbChild);

        mLastScreen = startPos;
        
        // Create screen before
        if (startPos > 0){
        	createScreen(startPos - 1);
        }

        // Create current screen
        createScreen(startPos);
        
        // Create screen after
        if (startPos < mNbChild - 1){
        	createScreen(startPos + 1);
        }

        setCurrentScreen(startPos, false);
        
        // Notify observer about screen start to move
        mOnScreenSwitchListener.onDisplayView(startPos, getChildAt(startPos));
    }

    /**
     * Snaps to the screen we think the user wants (the current screen for very small movements; the
     * next/prev screen for bigger movements).
     */
    private void snapToDestination()
    {
        final int screenWidth = getWidth();
        int scrollX = getScrollX();
        int whichScreen = mCurrentScreen;
        int deltaX = scrollX - (screenWidth * mCurrentScreen);

        // Check if they want to go to the prev. screen
        if ((deltaX < 0) && mCurrentScreen != 0 && ((screenWidth / FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE) < -deltaX)) {
            whichScreen--;
            // Check if they want to go to the next screen
        }
        else if ((deltaX > 0) && (mCurrentScreen + 1 != getChildCount()) && ((screenWidth / FRACTION_OF_SCREEN_WIDTH_FOR_SWIPE) < deltaX)) {
            whichScreen++;
        }

        snapToScreen(whichScreen);
    }

    /**
     * Snap to a specific screen, animating automatically for a duration proportional to the
     * distance left to scroll.
     *
     * @param whichScreen Screen to snap to
     */
    private void snapToScreen(final int whichScreen) {
        snapToScreen(whichScreen, -1);
    }

    /**
     * Snaps to a specific screen, animating for a specific amount of time to get there.
     *
     * @param whichScreen Screen to snap to
     * @param duration -1 to automatically time it based on scroll distance; a positive number to
     *            make the scroll take an exact duration.
     */
    private void snapToScreen(final int whichScreen, final int duration)
    {
        /*
         * Modified by Yoni Samlan: Allow new snapping even during an ongoing scroll animation. This
         * is intended to make HorizontalPager work as expected when used in conjunction with a
         * RadioGroup used as "tabbed" controls. Also, make the animation take a percentage of our
         * normal animation time, depending how far they've already scrolled.
         */
        mNextScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        final int newX = mNextScreen * getWidth();
        final int delta = newX - getScrollX();

        if (duration < 0) {
             // E.g. if they've scrolled 80% of the way, only animation for 20% of the duration
            mScroller.startScroll(getScrollX(), 0, delta, 0, (int)(Math.abs(delta) / (float) getWidth() * ANIMATION_SCREEN_SET_DURATION_MILLIS));
        }
        else {
            mScroller.startScroll(getScrollX(), 0, delta, 0, duration);
        }

        invalidate();
    }

    public void createScreen(int screen){
    	try {
    		removeViewAt(screen);
    		addView(mOnScreenSwitchListener.getView(screen, null), screen);
    	} catch (Exception e){
    		// Exception should not create a force close
    		e.printStackTrace();
    	}
    }
    
    private void displayScreen(final int screenDisplayed)
    {
    	Log.d(LOG_TAG, "displayScreen = " + screenDisplayed + " mLastScreen = " + mLastScreen);

        if (mOnScreenSwitchListener == null) {
        	return;
        }

        // FromRight
        if (mLastScreen < screenDisplayed){
        	if (screenDisplayed < mNbChild - 1){
	        	if (getChildAt(screenDisplayed + 1).getTag() == EMPTY_VIEW){
	        		createScreen(screenDisplayed + 1);

	        		// Cleaning
	        		if (screenDisplayed - 2 >= 0){
	        			removeViewAt(screenDisplayed - 2);
	        			addView(createEmptyView(), screenDisplayed - 2);        			
	        		}
	        	}
        	}
        }
        // FromLeft
        else{
        	if (screenDisplayed > 0){
	        	if (getChildAt(screenDisplayed - 1).getTag() == EMPTY_VIEW){
	        		createScreen(screenDisplayed - 1);

	        		// Cleaning
	        		if (screenDisplayed + 2 < mNbChild){
	        			removeViewAt(screenDisplayed + 2);
	        			addView(createEmptyView(), screenDisplayed + 2);        			
	        		}
	        	}
        	}
        }
    	
        // Notify observer about screen start to move
        if (mOnScreenSwitchListener != null) {
            mOnScreenSwitchListener.onExitView(mLastScreen, getChildAt(mLastScreen));
            mOnScreenSwitchListener.onDisplayView(screenDisplayed, getChildAt(screenDisplayed));
        }
        
		mLastScreen = screenDisplayed;
    }
    
    /**
     * Listener for the event that the HorizontalPager switches to a new view.
     */
    public static interface OnScreenSwitchListener
    {
        void onExitView(int position, View ConvertView);
        void onDisplayView(int position, View ConvertView);
        View getView(int position, View ConvertView);
    }
}
