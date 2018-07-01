package org.coolreader.crengine;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CRC32;

public class UserDicDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private UserDicList mList;

	private ArrayList<UserDicEntry> mUserDic = new ArrayList<UserDicEntry>();

	public final static int ITEM_POSITION=0;

	private void listUpdated() {
		mList.updateAdapter(new UserDicAdapter());
	}

	class UserDicAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			int cnt = 0;
			if (mUserDic != null) cnt = mUserDic.size();
			return cnt;
		}

		public Object getItem(int position) {
			int cnt = 0;
			if (mUserDic != null) cnt = mUserDic.size();
			if ( position<0 || position>=cnt )
				return null;
			return mUserDic.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			int res = R.layout.userdic_item;
			view = mInflater.inflate(res, null);
			TextView wordView = (TextView)view.findViewById(R.id.userdic_word);
			TextView wordTranslateView = (TextView)view.findViewById(R.id.userdic_word_translate);
			UserDicEntry ude = null;
			if (mUserDic!=null) ude = mUserDic.get(position);
			if ( wordView!=null ) {
				wordView.setText(String.valueOf(position+1));
			}
			String sTitle = "";
			if ( ude!=null ) {
				String word = StrUtils.textShrinkLines(ude.getDic_word(),true);
				if (!StrUtils.isEmptyStr(ude.getLanguage())) word = word+" ["+ude.getLanguage()+"]";
				if ( wordView!=null )
					wordView.setText(word);
					wordTranslateView.setText(StrUtils.textShrinkLines(ude.getDic_word_translate(),true));
			} else {
				if ( wordView!=null )
					wordView.setText("");
					wordTranslateView.setText("");
			}
			return view;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			int cnt = 0;
			if (mUserDic != null) cnt = mUserDic.size();
			return cnt==0;
		}

		private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

		public void registerDataSetObserver(DataSetObserver observer) {
			observers.add(observer);
		}

		public void unregisterDataSetObserver(DataSetObserver observer) {
			observers.remove(observer);
		}
	}

	class UserDicList extends BaseListView {

		private ListAdapter mAdapter;
		private UserDicDlg uDDlg;

		public UserDicList( Context context, UserDicDlg udd) {
			super(context, true);
			uDDlg = udd;
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new UserDicAdapter());
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					openContextMenu(UserDicList.this);
					DictsDlg dlg = new DictsDlg(mCoolReader, mCoolReader.getReaderView(), mUserDic.get(position).getDic_word());
					dlg.show();
					dismiss();
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			if ( mUserDic==null )
				return true;
			final UserDicEntry ude = mUserDic.get(position);
			final UserDicDlg thisDlg = uDDlg;
			mCoolReader.askConfirmation(R.string.win_title_confirm_ude_delete, new Runnable() {
				@Override
				public void run() {
					activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_DELETE);
					mCoolReader.getmUserDic().remove(ude.getDic_word());
					mUserDic.remove(ude);
					if (thisDlg!=null)
						thisDlg.listUpdated();
					mCoolReader.getmReaderFrame().getUserDicPanel().updateUserDicWords();
				}
			});
			return true;
		}

		public void updateAdapter( UserDicAdapter adapter ) {
			mAdapter = adapter;
			setAdapter(mAdapter);
		}

	}

	public UserDicDlg(final CoolReader activity)
	{
		super(activity, activity.getResources().getString(R.string.win_title_user_dic), false, true);
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
        mUserDic.clear();

		mUserDic.clear();
		for (UserDicEntry ude: activity.getmReaderFrame().getUserDicPanel().getArrUdeWords()) {
			mUserDic.add(ude);
		}

		View frame = mInflater.inflate(R.layout.userdic_list_dialog, null);
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.userdic_list);
		mList = new UserDicList(activity, this);
		final RadioButton btnPage = (RadioButton)frame.findViewById(R.id.rb_page);
		final RadioButton btnBook = (RadioButton)frame.findViewById(R.id.rb_book);
		final RadioButton btnAll = (RadioButton)frame.findViewById(R.id.rb_userdic_all);
		body.addView(mList);
		setView(frame);
		setFlingHandlers(mList, null, null);

		btnPage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					mUserDic.clear();
					for (UserDicEntry ude: activity.getmReaderFrame().getUserDicPanel().getArrUdeWords()) {
						mUserDic.add(ude);
					}
					listUpdated();
				}
			}
		});
		btnBook.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					final String sBookFName = mCoolReader.getReaderView().getBookInfo().getFileInfo().filename;
					CRC32 crc = new CRC32();
					crc.update(sBookFName.getBytes());
					String sCRC = String.valueOf(crc.getValue());
					updUserDic(sCRC);
					listUpdated();
				}
			}
		});
		btnAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					updUserDic("");
					listUpdated();
				}
			}
		});
	}

	private void updUserDic(String sCRC) {
		mUserDic.clear();
		Iterator it = mCoolReader.getmUserDic().entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			UserDicEntry ude = (UserDicEntry)pair.getValue();
			if ((ude.getDic_from_book().equals(sCRC))||(sCRC.equals(""))) mUserDic.add(ude);
		}
		Collections.sort(mUserDic, new Comparator<UserDicEntry>() {
			@Override
			public int compare(UserDicEntry lhs, UserDicEntry rhs) {
				// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
				return lhs.getDic_word().compareToIgnoreCase(rhs.getDic_word());
			}
		});
	}

	@Override
	protected void onPositiveButtonClick() {
		// add Dict
		UserDicDlg.this.dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		UserDicDlg.this.dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating UserDicDlg");
		setTitle(mCoolReader.getResources().getString(R.string.win_title_user_dic));
		setCancelable(true);
		super.onCreate(savedInstanceState);
		//registerForContextMenu(mList);
	}

}
