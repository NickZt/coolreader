package org.coolreader.crengine;

import org.coolreader.R;
import org.coolreader.db.CRDBService;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class SearchDlg extends BaseDialog {
	BaseActivity mCoolReader;
	ReaderView mReaderView;
	private LayoutInflater mInflater;
	View mDialogView;
	EditText mEditView;
	CheckBox mCaseSensitive;
	CheckBox mReverse;
	BookInfo mBookInfo;
	Button mSearchPages;
	ArrayList<String> mSearches;
	private SearchList mList;


	@Override
	protected void onPositiveButtonClick()
	{
		// override it
    	String pattern = mEditView.getText().toString();
    	if ( pattern==null || pattern.length()==0 ) 
    		mCoolReader.showToast("No pattern specified");
    	else
			activity.getDB().saveSearchHistory(mBookInfo,
					mEditView.getText().toString());
    		mReaderView.findText( mEditView.getText().toString(), mReverse.isChecked(), !mCaseSensitive.isChecked() );
        cancel();
	}
	
	@Override
	protected void onNegativeButtonClick()
	{
		// override it
        cancel();
	}

	public final static int ITEM_POSITION=0;

	class SearchListAdapter extends BaseAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return mSearches.size();
		}

		public Object getItem(int position) {
			if ( position<0 || position>=mSearches.size() )
				return null;
			return mSearches.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public int getViewTypeCount() {
			return 4;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.dict_item;
			view = mInflater.inflate(res, null);
			TextView labelView = (TextView)view.findViewById(R.id.dict_item_shortcut);
			TextView titleTextView = (TextView)view.findViewById(R.id.dict_item_title);
			String s = (String)getItem(position);
			if ( labelView!=null ) {
				labelView.setText(String.valueOf(position+1));
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
			return mSearches.size()==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class SearchList extends BaseListView {

		public SearchList(Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new SearchDlg.SearchListAdapter());
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
											   int position, long arg3) {
					//openContextMenu(SearchDlg.SearchList.this);
					mEditView.setText(mSearches.get(position));
					mSearchPages.callOnClick();
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			mEditView.setText(mSearches.get(position));
			return true;
		}
	}
	
	public SearchDlg(BaseActivity coolReader, ReaderView readerView, String initialText)
	{
		super(coolReader, coolReader.getResources().getString(R.string.win_title_search), true, false);
        setCancelable(true);
		this.mCoolReader = coolReader;
		this.mReaderView = readerView;
		this.mBookInfo = mReaderView.getBookInfo();
		setPositiveButtonImage(R.drawable.icons8_search, R.string.action_search);
        mInflater = LayoutInflater.from(getContext());
        mDialogView = mInflater.inflate(R.layout.search_dialog, null);
    	mEditView = (EditText)mDialogView.findViewById(R.id.search_text);
    	if (initialText != null)
    		mEditView.setText(initialText);
    	mCaseSensitive = (CheckBox)mDialogView.findViewById(R.id.search_case_sensitive);
    	mReverse = (CheckBox)mDialogView.findViewById(R.id.search_reverse);
    	mSearchPages = (Button)mDialogView.findViewById(R.id.btn_search_pages);
    	mSearchPages.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				final String sText = mEditView.getText().toString().trim();
				final GotoPageDialog dlg = new GotoPageDialog(activity, title, sText,
						new GotoPageDialog.GotoPageHandler() {
							int pageNumber = 0;
							@Override
							public boolean validate(String s) {
								pageNumber = Integer.valueOf(s);
								return pageNumber>0; // && pageNumber <= mReaderView.props.pageCount;
							}
							@Override
							public void onOk(String s) {
								mReaderView.goToPage(pageNumber);
							}
							@Override
							public void onOkPage(String s) {
								mReaderView.goToPage(pageNumber);
							}
							@Override
							public void onCancel() {
							}
						});
				dlg.show();
				dismiss();
			}
		});
		ImageButton btnMinus1 = (ImageButton)mDialogView.findViewById(R.id.search_dlg_clear_hist_btn);

		btnMinus1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				activity.getDB().clearSearchHistory(mBookInfo);
				mCoolReader.showToast(mCoolReader.getString(R.string.search_hist_will_be_cleared));
				dismiss();
			}
		});

		activity.getDB().loadSearchHistory(this.mBookInfo, new CRDBService.SearchHistoryLoadingCallback() {
			@Override
			public void onSearchHistoryLoaded(ArrayList<String> searches) {
				mSearches = searches;
				ViewGroup body = (ViewGroup)mDialogView.findViewById(R.id.history_list);
				mList = new SearchDlg.SearchList(activity, false);
				body.addView(mList);
			}
		});
		//setView(mDialogView);
		//setFlingHandlers(mList, null, null);
		// setup buttons
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setView(mDialogView);
	}
}
