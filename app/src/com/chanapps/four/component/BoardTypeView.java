/**
 * 
 */
package com.chanapps.four.component;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.chanapps.four.activity.R;
import com.chanapps.four.data.ChanBoard;
import com.chanapps.four.data.ChanBoard.Type;
import com.chanapps.four.data.ChanPost;
import com.chanapps.four.data.ChanThread;
import com.chanapps.four.fragment.BoardGroupFragment;
import com.chanapps.four.loader.ChanWatchlistDataLoader;
import com.nostra13.universalimageloader.core.ImageLoader;

/**
 * @author "Grzegorz Nittner" <grzegorz.nittner@gmail.com>
 * 
 */
public class BoardTypeView extends View implements View.OnTouchListener {
	private static final String TAG = BoardTypeView.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int BOARD_FONT_SP = 14;
    private static final int BOARD_FONT_SP_LARGE = 18;

    private static final int BOX_HEIGHT_DP = 32;
    private static final int BOX_HEIGHT_DP_LARGE = 48;

    private static final int LONG_CLICK_DELAY = 500;
    
	private Type boardType;
	private int numCols, columnWidth;
	private float downX, downY;
	private long lastClickDown;
	private List<ChanBoard> boards = null;
	private List<ChanThreadData> watchedThreads = new ArrayList<ChanThreadData>();
	private int fontSize = -1;
	private Paint paint = new Paint();
	private Handler handler = null;
	
	private BitmapFactory.Options options = null;
	
	private BoardGroupFragment clickListener;

	public BoardTypeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BoardTypeView(Context context) {
		super(context);
	}
	
	public void setBoardGroupFragment(BoardGroupFragment clickListener) {
		this.clickListener = clickListener;
	}

	public void setBoardData(Handler handler, Type boardType, int numCols, int columnWidth) {
		this.boardType = boardType;
		this.numCols = numCols;
		this.columnWidth = columnWidth;
		this.handler = handler;
		if (boardType == Type.WATCHLIST) {
			fontSize = 16;
			updateWatchList(getContext(), false);
		} else {
			boards = ChanBoard.getBoardsByType(getContext(), this.boardType);
		}
		options = new BitmapFactory.Options();
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		setOnTouchListener(this);
	}

	private void updateWatchList(Context context, boolean callInvalidate) {
		long startTime = new Date().getTime();
		List<ChanThread> threads = new ChanWatchlistDataLoader(context).getWatchedThreads();
		List<ChanThreadData> watchedThreads = new ArrayList<ChanThreadData>();
		for (ChanThread thread : threads) {
			if (thread.posts.length > 0) {
				ChanThreadData watchedThread = new ChanThreadData();
				watchedThread.shortText = thread.posts[0].getBoardText();
				watchedThread.shortText = watchedThread.shortText.replaceAll("</?b>", "").replaceAll("<[^>]*>.*","");
				watchedThread.thumbUrl = thread.posts[0].getThumbnailUrl();
				watchedThread.no = thread.no;
				watchedThread.board = thread.board;
				if (DEBUG) Log.i(TAG, "Watched thread: " + thread.no 
						+ " thumb: " + watchedThread.thumbUrl + " text: " + watchedThread.shortText);
				watchedThreads.add(watchedThread);
			}
		}
		long endTime = new Date().getTime();
		Log.i(TAG, "New watchlist size: " + watchedThreads.size() + " calculated in " + (endTime - startTime) + "ms");
		if (!(this.watchedThreads.containsAll(watchedThreads) && this.watchedThreads.size() == watchedThreads.size())) {
			this.watchedThreads = watchedThreads;
			if (callInvalidate) {
				Log.i(TAG, "Calling invalidate");
				invalidate();
			}
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
	   int width = numCols * columnWidth + (numCols - 1) * getPaddingLeft();
	   int height = columnWidth;
	   if (boardType == Type.WATCHLIST) {
		   int numRows = watchedThreads.size() / numCols;
		   if (numRows * numCols < watchedThreads.size()) {
			   numRows++;
		   }
		   height = numRows * columnWidth + (numRows - 1) * getPaddingBottom();
	   } else {
		   int numRows = boards.size() / numCols;
		   if (numRows * numCols < boards.size()) {
			   numRows++;
		   }
		   height = numRows * columnWidth + (numRows - 1) * getPaddingBottom();
	   }
	   if (DEBUG) Log.i(TAG, "onMeasure w: " + width + ", h: " + height);
	   setMeasuredDimension(width, height);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.BLACK);

		if (boardType == Type.WATCHLIST) {
			drawWatchedThreads(canvas);
		} else {
			drawBoards(canvas);
		}
	}

	private void drawBoards(Canvas canvas) {
		int row = 0;
		int col = 0;
		int image = 0;
		if (boards != null) {
			for (ChanBoard board : boards) {
				row = image / numCols;
				col = image % numCols;

				Bitmap boardImage = null;
				try {
					boardImage = BitmapFactory.decodeResource(getResources(),
						board.getImageResourceId(), options);
				} catch (OutOfMemoryError ome) {
					Log.w(TAG, "Out of memory error thrown, trying to recover...");
					handler.postDelayed(new Runnable () {
						public void run() {
							invalidate();
						}
					}, 500);
				}
				int posX = col * columnWidth + (col - 1) * getPaddingLeft();
				int posY = row * columnWidth + (row - 1) * getPaddingBottom();
				RectF destRect = new RectF(posX, posY, posX + columnWidth, posY + columnWidth);
				if (boardImage != null) {
					canvas.drawBitmap(boardImage, null, destRect, paint);
				}
                calculateBoxHeight();
				RectF textRect = new RectF(posX, posY + columnWidth - boxHeight,
						posX + columnWidth, posY + columnWidth);
				paint.setColor(0xaa000000);
                canvas.drawRect(textRect, paint);

				paint.setColor(0xaaffffff);
                calculateFontMetrics();
                float textX = textRect.centerX();
                float textY = textRect.centerY() - ((paint.descent() + paint.ascent()) / 2);
                canvas.drawText(board.name, textX, textY, paint);

                image++;
			}
		}
	}

	private void drawWatchedThreads(Canvas canvas) {
		int row = 0;
		int col = 0;
		int image = 0;
		
		if (watchedThreads != null) {
			for (ChanThreadData thread : watchedThreads) {
				row = image / numCols;
				col = image % numCols;

				Bitmap threadImage = null;
				try {
					File thumbFile = thread.thumbUrl != null ? ImageLoader.getInstance().getDiscCache().get(thread.thumbUrl) : null;
					if (thread.thumbUrl != null && thumbFile.exists()) {
						threadImage = BitmapFactory.decodeFile(thumbFile.getAbsolutePath(), options);
					} else {
						threadImage = BitmapFactory.decodeResource(getResources(),
								R.drawable.stub_image, options);
					}
				} catch (OutOfMemoryError ome) {
					Log.w(TAG, "Out of memory error thrown, trying to recover...");
					handler.postDelayed(new Runnable () {
						public void run() {
							invalidate();
						}
					}, 500);
				}
				int posX = col * columnWidth + (col - 1) * getPaddingLeft();
				int posY = row * columnWidth + (row - 1) * getPaddingBottom();
				RectF destRect = new RectF(posX, posY, posX + columnWidth, posY + columnWidth);
				Log.i(TAG, "Paint watched thread " + col + " " + row);
				if (threadImage != null) {
					canvas.drawBitmap(threadImage, null, destRect, paint);
				}
                calculateBoxHeight();
				RectF textRect = new RectF(posX, posY + columnWidth - boxHeight,
						posX + columnWidth, posY + columnWidth);
				paint.setColor(0xaa000000);
				canvas.drawRect(textRect, paint);
				
				paint.setColor(0xaaffffff);
			    calculateFontMetrics();
                String abbrevText = ChanPost.abbreviate(thread.shortText, 22);
                float textX = textRect.centerX();
                float textY = textRect.centerY() - ((paint.descent() + paint.ascent()) / 2);
                canvas.drawText(abbrevText, textX, textY, paint);

				image++;

			}
		}
		handler.postDelayed(new Runnable () {
			public void run() {
				updateWatchList(getContext(), true);
			}
		}, 500);
	}

    private enum LayoutSize {
        NORMAL,
        LARGE
    }

    private boolean isLayoutSet = false;
    private LayoutSize layoutSize;
    private LayoutSize getLayoutSize() {
        if (!isLayoutSet) {
            int sizeMask = getContext().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            switch (sizeMask) {
                case Configuration.SCREENLAYOUT_SIZE_LARGE:
                case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                    layoutSize = LayoutSize.LARGE;
                    break;
                default:
                    layoutSize = LayoutSize.NORMAL;
            }
            isLayoutSet = true;
        }
        return layoutSize;
    }

    private boolean isDisplayMetricsSet;
    private DisplayMetrics displayMetrics;
    private DisplayMetrics getDisplayMetrics() {
        if (!isDisplayMetricsSet) {
            displayMetrics = getContext().getResources().getDisplayMetrics();
            isDisplayMetricsSet = true;
        }
        return displayMetrics;
    }

    private boolean isFontSet = false;
    private void calculateFontMetrics() {
        if (!isFontSet) {
            float fontSizeSp = getLayoutSize() == LayoutSize.LARGE ? BOARD_FONT_SP_LARGE : BOARD_FONT_SP;
            float pixelSize = fontSizeSp * getDisplayMetrics().scaledDensity;
            fontSize = Math.round(pixelSize);
            paint.setTextSize(fontSize);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            isFontSet = true;
        }
	}

    private boolean isBoxHeightSet = false;
    private int boxHeight;
    private void calculateBoxHeight() {
        if (!isBoxHeightSet) {
            float boxHeightDp = getLayoutSize() == LayoutSize.LARGE ? BOX_HEIGHT_DP_LARGE : BOX_HEIGHT_DP;
            float boxHeightPx = boxHeightDp * getDisplayMetrics().density;
            boxHeight = Math.round(boxHeightPx);
            isBoxHeightSet = true;
        }
    }

    @Override
	public boolean onTouch(View v, MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downX = ev.getX();
			downY = ev.getY();
			lastClickDown = new Date().getTime();
			if (DEBUG) Log.i(TAG, "Touch down " + (int)(downX / columnWidth) + "-" + (int)(downY / columnWidth));
			break;
		case MotionEvent.ACTION_UP:
			int positionX = (int)(downX / columnWidth);
			int positionY = (int)(downY / columnWidth);
			boolean longPress = new Date().getTime() - lastClickDown > LONG_CLICK_DELAY;
			if (DEBUG) Log.i(TAG, (longPress ? "Long pressed " : "Pressed ") + positionX + "-" + positionY);
			
			long threadId = -1;
			String board = null;
			if (boardType == Type.WATCHLIST) {
				if (DEBUG) Log.i(TAG, "Clicked on " + (positionY * numCols + positionX)
						+ " out of " + watchedThreads.size() + " watched threads");
				if (watchedThreads.size() > positionY * numCols + positionX) {
					threadId = watchedThreads.get(positionY * numCols + positionX).no;
					board = watchedThreads.get(positionY * numCols + positionX).board;
				} else {
					break;
				}
			}
			if (boardType != Type.WATCHLIST) {
				if (DEBUG) Log.i(TAG, "Clicked on " + (positionY * numCols + positionX)
						+ " out of " + boards.size() + " boards");
				if (boards.size() > positionY * numCols + positionX) {
					board = boards.get(positionY * numCols + positionX).board;
				} else {
					break;
				}
			}
			
			if (clickListener != null) {
				if (longPress) {
					clickListener.onItemLongClick(this, boardType, board, threadId);
				} else {
					clickListener.onItemClick(this, board, threadId);
				}
			}
			break;
		}
		return true;
	}

	private static class ChanThreadData {
		String board;
		long no;
		String thumbUrl;
		String shortText;
		
		@Override
		public boolean equals(Object o) {
			return o instanceof ChanThreadData ? ((ChanThreadData)o).no == no && ((ChanThreadData)o).board.equals(board) : false;
		}
		
		@Override
		public int hashCode() {
			return thumbUrl != null ? thumbUrl.hashCode() : (int)no;
		}
	}
}
