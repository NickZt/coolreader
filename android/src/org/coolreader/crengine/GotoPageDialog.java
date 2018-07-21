package org.coolreader.crengine;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;

public class GotoPageDialog extends BaseDialog {

	public interface GotoPageHandler {
		boolean validate(String s) throws Exception;
		void onOk(String s) throws Exception;
		void onCancel();
	};

	private GotoPageHandler handler;
	private EditText input;
	int minValue;
	int maxValue;
	private int mPageCount = 0;
	private BookPagesList mList;

	public final static int ITEM_POSITION=0;

	public ReaderView getReaderView() {
		return mReaderView;
	}
	private ReaderView mReaderView;
	private LayoutInflater mInflater;
	private ArrayList<String[]> arrFound = null;

	class BookPagesAdapter extends BaseAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			if (arrFound == null)
				return mPageCount;
			else return arrFound.size();
		}

		public Object getItem(int position) {
			if (arrFound == null) {
				if (position < 0 || position >= mPageCount)
					return null;
				if (mReaderView.getArrAllPages()==null) return null;
				return mReaderView.getArrAllPages().get(position);
			} else {
				if (position < 0 || position >= arrFound.size())
					return null;
				return arrFound.get(position);
			}

		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.page_item;
			view = mInflater.inflate(res, null);
			TextView labelView = (TextView)view.findViewById(R.id.page_item_shortcut);
			TextView titleTextView = (TextView)view.findViewById(R.id.page_item_title);
			String sPos = "0";
			String s = "";
			if (arrFound==null) {
				sPos = String.valueOf(position+1);
				s = (String) getItem(position);
			} else {
				sPos = ((String[]) getItem(position))[0];
				s = ((String[]) getItem(position))[1];
			}
			if ( labelView!=null ) {
				labelView.setText(sPos);
			}
			if ( s!=null ) {
				if ( titleTextView!=null )
					titleTextView.setText(s);
			} else {
				if ( titleTextView!=null )
					titleTextView.setText("");
			}
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			return mPageCount==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class BookPagesList extends BaseListView {

		public BookPagesList(Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			if (mReaderView.getArrAllPages()==null) mPageCount=0;
				else mPageCount = mReaderView.getArrAllPages().size();
			setAdapter(new BookPagesAdapter());
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
											   int position, long arg3) {
					//openContextMenu(DictList.this);
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			String value = "1";
			if (arrFound == null)
				value = String.valueOf(position+1);
			else {
				TextView labelView = (TextView)view.findViewById(R.id.page_item_shortcut);
				if (labelView != null) value = String.valueOf(Integer.valueOf(labelView.getText().toString().trim())+1);
			}
			try {
				if ( handler.validate(value) )
					handler.onOk(value);
				else
					handler.onCancel();
			} catch ( Exception e ) {
				handler.onCancel();
			}
			cancel();
			return true;
		}


	}
	
	public GotoPageDialog(BaseActivity activity, final String title, final String prompt, boolean isNumberEdit, int minValue, int maxValue, int currentValue, final GotoPageHandler handler )
	{
		super(activity, title, true, false);
		this.arrFound = null;
		this.handler = handler;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.mReaderView = ((CoolReader)activity).getReaderView();
		this.mReaderView.CheckAllPagesLoad();
        this.mInflater = LayoutInflater.from(getContext());
        ViewGroup layout = (ViewGroup)mInflater.inflate(R.layout.goto_page_dlg, null);
        input = (EditText)layout.findViewById(R.id.input_field);
        TextView promptView = (TextView)layout.findViewById(R.id.lbl_prompt);
        if (promptView != null) {
        	promptView.setText(prompt);
        }
		ViewGroup body = (ViewGroup)layout.findViewById(R.id.pages_list);
		mList = new GotoPageDialog.BookPagesList(activity, false);
		body.addView(mList);

        SeekBar seekBar = (SeekBar)layout.findViewById(R.id.goto_position_seek_bar);
        if (seekBar != null) {
        	seekBar.setMax(maxValue - minValue);
        	seekBar.setProgress(currentValue);
        	seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if (fromUser) {
						String value = String.valueOf(progress + GotoPageDialog.this.minValue);
						try {
							if (handler.validate(value))
								input.setText(value);
						} catch (Exception e) {
							// ignore
						}
					}
				}
			});
        }
        //input = new EditText(getContext());
        if ( isNumberEdit )
        	input.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
//	        input.getText().setFilters(new InputFilter[] {
//	        	new DigitsKeyListener()        
//	        });
        setView(layout);
	}

	public GotoPageDialog(BaseActivity activity, final String title, final String findtext, final GotoPageHandler handler )
	{
		super(activity, title, true, false);
		arrFound = new ArrayList<String[]>();
		this.handler = handler;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.mReaderView = ((CoolReader)activity).getReaderView();
		this.mReaderView.CheckAllPagesLoad();
		this.mInflater = LayoutInflater.from(getContext());
		ViewGroup layout = (ViewGroup)mInflater.inflate(R.layout.goto_page_find_dlg, null);
		TextView promptView = (TextView)layout.findViewById(R.id.lbl_find_text_pages);
		if (promptView != null) {
			promptView.setText("search results for: "+findtext);
		}
		ViewGroup body = (ViewGroup)layout.findViewById(R.id.find_pages_list);
		mList = new GotoPageDialog.BookPagesList(activity, false);
		body.addView(mList);
		setView(layout);
		int iPageCnt = 0;
		if (mReaderView.getArrAllPages()!=null) iPageCnt = mReaderView.getArrAllPages().size();
		for (int i=0;i<iPageCnt;i++) {
			String sPage = mReaderView.getArrAllPages().get(i);
			if (sPage == null) sPage = "";
			sPage = sPage.replace("\\n", " ");
			sPage = sPage.replace("\\r", " ");
			String[] arrPage = {String.valueOf(i), sPage};
			String sFindText = findtext;
			if (sFindText == null) sFindText = "";
			if (sFindText.equals("")) {
				arrFound.add(arrPage);
			} else
				if (sPage.toLowerCase().contains(sFindText.toLowerCase())) arrFound.add(arrPage);
		}
	}

	@Override
	protected void onNegativeButtonClick() {
        cancel();
        handler.onCancel();
	}
	@Override
	protected void onPositiveButtonClick() {
        String value = input.getText().toString().trim();
        try {
        	if ( handler.validate(value) ) {
				handler.onOk(value);
			}
        	else
        		handler.onCancel();
        } catch ( Exception e ) {
        	handler.onCancel();
        }
        cancel();
	}
}
