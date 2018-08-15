package org.coolreader.crengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.coolreader.CoolReader;
import org.coolreader.Dictionaries;
import org.coolreader.R;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class DictsDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private ReaderView mReaderView;
	private LayoutInflater mInflater;
	private DictList mList;
	private String mSearchText;
	private EditText selEdit;

	//DictsDlg mThis;

	public final static int ITEM_POSITION=0;

	class DictListAdapter extends BaseAdapter {
		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			return Dictionaries.getDictListExt(mCoolReader,true).size();
		}

		public Object getItem(int position) {
			if ( position<0 || position>=Dictionaries.getDictListExt(mCoolReader,true).size() )
				return null;
			return Dictionaries.getDictListExt(mCoolReader,true).get(position);
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
			Dictionaries.DictInfo b = (Dictionaries.DictInfo)getItem(position);
			if ( labelView!=null ) {
				labelView.setText(String.valueOf(position+1));
			}
			if ( b!=null ) {
				if ( titleTextView!=null )
					titleTextView.setText(b.name);
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
			return Dictionaries.getDictList().length==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();
		
		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}
	
	class DictList extends BaseListView {

		public DictList( Context context, boolean shortcutMode ) {
			super(context, true);
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new DictListAdapter());
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					openContextMenu(DictList.this);
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			mCoolReader.mDictionaries.setAdHocDict(Dictionaries.getDictListExt(mCoolReader,true).get(position));
			String sSText = mSearchText.trim();
			if (selEdit!=null) sSText = selEdit.getText().toString().trim();
			mCoolReader.findInDictionary(sSText);
			if (!mReaderView.getSettings().getBool(mReaderView.PROP_APP_SELECTION_PERSIST, false))
				mReaderView.clearSelection();
			dismiss();
			return true;
		}
		
		
	}

	public DictsDlg( CoolReader activity, ReaderView readerView, String search_text )
	{
		super(activity, activity.getResources().getString(R.string.win_title_dicts), true, true);
		mInflater = LayoutInflater.from(getContext());
		mSearchText = StrUtils.updateText(search_text.trim(),false);
		mCoolReader = activity;
		mReaderView = readerView;
		View frame = mInflater.inflate(R.layout.dict_dialog, null);
		ImageButton btnMinus1 = (ImageButton)frame.findViewById(R.id.dict_dlg_minus1_btn);
		ImageButton btnMinus2 = (ImageButton)frame.findViewById(R.id.dict_dlg_minus2_btn);
		selEdit = (EditText)frame.findViewById(R.id.selection_text);
		selEdit.setText(mSearchText);
		setPositiveButtonImage(0,0);
		setThirdButtonImage(R.drawable.icons8_search_history, R.string.dlg_button_search_hist);
		setAddButtonImage(R.drawable.icons8_me_smb, R.string.dlg_button_pronoun_replace);
		btnMinus1.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				String s = StrUtils.replacePuncts(selEdit.getText().toString(),true);
				String res = "";
				String [] arrS = s.split(" ");
				boolean bFirst = true;
				for (String ss: arrS) {
					String repl = StrUtils.replacePuncts(ss.trim().toLowerCase(), false);
					if (!repl.trim().equals("")) {
						if (bFirst) {
							bFirst = false;
						} else {
							res=res.trim()+" "+ss.trim();
						}
					}
				}
				selEdit.setText(res.trim());
			}
		});
		btnMinus2.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				String s = StrUtils.replacePuncts(selEdit.getText().toString(),true);
				String res = "";
				String [] arrS = s.split(" ");
				List<String> list = Arrays.asList(arrS);
				Collections.reverse(list);
				arrS = (String[]) list.toArray();
				boolean bFirst = true;
				for (String ss: arrS) {
					String repl = StrUtils.replacePuncts(ss.trim().toLowerCase(),false);
					if (!repl.trim().equals("")) {
						if (bFirst) {
							bFirst = false;
						} else {
							res=ss.trim()+" "+res.trim();
						}
					}
				}
				selEdit.setText(res.trim());
			}
		});
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.dict_list);
		mList = new DictList(activity, false);
		body.addView(mList);
		setView(frame);
		selEdit.clearFocus();
		btnMinus2.requestFocus();
		setFlingHandlers(mList, null, null);
	}

	@Override
	protected void onPositiveButtonClick() {
		// add Dict
		DictsDlg.this.dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		DictsDlg.this.dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating DictsDlg");
		//setTitle(mCoolReader.getResources().getString(R.string.win_title_Dicts));
		setCancelable(true);
		super.onCreate(savedInstanceState);
		registerForContextMenu(mList);
	}

	@Override
	protected void onAddButtonClick() {
		String s = StrUtils.updateDictSelText(selEdit.getText().toString());
		selEdit.setText(s.trim());
	}

	protected void onThirdButtonClick() {
		mCoolReader.showToast("to come...");
	}

}
