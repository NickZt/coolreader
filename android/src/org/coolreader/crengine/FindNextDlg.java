package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.PopupWindow;

public class FindNextDlg {
	PopupWindow mWindow;
	View mAnchor;
	CoolReader mCoolReader;
	ReaderView mReaderView;
	View mPanel;
	final String pattern;
	final boolean caseInsensitive;
	static public void showDialog( CoolReader coolReader, ReaderView readerView, final String pattern, final boolean caseInsensitive )
	{
		FindNextDlg dlg = new FindNextDlg(coolReader, readerView, pattern, caseInsensitive);
		//dlg.mWindow.update(dlg.mAnchor, width, height)
		Log.d("cr3", "popup: " + dlg.mWindow.getWidth() + "x" + dlg.mWindow.getHeight());
		//dlg.update();
		//dlg.showAtLocation(readerView, Gravity.LEFT|Gravity.TOP, readerView.getLeft()+50, readerView.getTop()+50);
		//dlg.showAsDropDown(readerView);
		//dlg.update();
	}
	public FindNextDlg( CoolReader coolReader, ReaderView readerView, final String pattern, final boolean caseInsensitive )
	{
		this.pattern = pattern;
		this.caseInsensitive = caseInsensitive;
		mCoolReader = coolReader;
		mReaderView = readerView;
		mAnchor = readerView;

		View panel = (LayoutInflater.from(coolReader.getApplicationContext()).inflate(R.layout.search_popup, null));
		panel.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		
		//mReaderView.getS
		
		mWindow = new PopupWindow( mAnchor.getContext() );
		mWindow.setTouchInterceptor(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ( event.getAction()==MotionEvent.ACTION_OUTSIDE ) {
					mReaderView.clearSelection();
					mWindow.dismiss();
					return true;
				}
				return false;
			}
		});
		//super(panel);
		mPanel = panel;
		mPanel.findViewById(R.id.search_btn_prev).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mReaderView.findNext(pattern, true, caseInsensitive);
			}
		});
		mPanel.findViewById(R.id.search_btn_next).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mReaderView.findNext(pattern, false, caseInsensitive);
			}
		});
		mPanel.findViewById(R.id.search_btn_close).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mReaderView.clearSelection();
				mWindow.dismiss();
			}
		});
		mPanel.setFocusable(true);
		mPanel.setOnKeyListener( new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ( keyCode == KeyEvent.KEYCODE_BACK) {
					mReaderView.clearSelection();
					mWindow.dismiss();
					return true;
				}
				return false;
			}
			
		});

		
		mWindow.setBackgroundDrawable(new BitmapDrawable());
		//mWindow.setAnimationStyle(android.R.style.Animation_Toast);
		mWindow.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		mWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
//		setWidth(panel.getWidth());
//		setHeight(panel.getHeight());
		
		mWindow.setFocusable(true);
		mWindow.setTouchable(true);
		mWindow.setOutsideTouchable(true);
		mWindow.setContentView(panel);
		
		
		int [] location = new int[2];
		mAnchor.getLocationOnScreen(location);
		//mWindow.update(location[0], location[1], mPanel.getWidth(), mPanel.getHeight() );
		//mWindow.setWidth(mPanel.getWidth());
		//mWindow.setHeight(mPanel.getHeight());

		mWindow.showAtLocation(mAnchor, Gravity.TOP | Gravity.LEFT, location[0], location[1]);
//		if ( mWindow.isShowing() )
//			mWindow.update(mAnchor, 50, 50);
		//dlg.mWindow.showAsDropDown(dlg.mAnchor);
	
	}
	
}
