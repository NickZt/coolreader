package org.coolreader.crengine;

import android.content.Context;
import android.content.IntentSender;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.OpenFileActivityOptions;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;

public class OpenBookFromGdDlg extends BaseDialog {
	private CoolReader mCoolReader;
	private LayoutInflater mInflater;
	private OpenBookFromGDList mList;

	public boolean ismWholeSearchW() {
		if (mDriveIdList.size()<=1) return true;
		return mFoundWhole;
	}

	public boolean mWholeSearch = false;
	private TextView lblFolder;
	public EditText edtFind;
	private RadioButton btnWhole;
	private RadioButton btnFolder;

	public MetadataBuffer mBooksList;
	public ArrayList<DriveId> mDriveIdList;
	public ArrayList<String> mDriveStrList;
	public ArrayList<DriveId> mDriveIdList1;
	public ArrayList<String> mDriveStrList1;
	public DriveId driveToGoTo;
	public boolean mFoundWhole = false;

	public final static int ITEM_POSITION=0;

	public void listUpdated() {
		mList.updateAdapter(new OpenBookFromGDAdapter());
		lblFolder.setText(getFolderText());
	}

	class OpenBookFromGDAdapter extends BaseAdapter {

		public boolean areAllItemsEnabled() {
			return true;
		}

		public boolean isEnabled(int arg0) {
			return true;
		}

		public int getCount() {
			int cnt = 0;
			if (mBooksList != null) cnt = mBooksList.getCount();
			if (!ismWholeSearchW()) cnt++;
			return cnt;
		}

		public Object getItem(int position) {
			int cnt = 0;
			int pos = position;
			if (mBooksList != null) cnt = mBooksList.getCount();
			if (!ismWholeSearchW()) cnt++;
			if ( pos<0 || pos>=cnt )
				return null;
			if ((pos == 0 ) && (!ismWholeSearchW())) return null;
			return mBooksList.get(pos-1);
		}

		public long getItemId(int position) {
			return position;
		}

		public int getItemViewType(int position) {
			return ITEM_POSITION;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			View view1;
			View view2;
			int res = R.layout.open_book_from_gd_item;
			int res1 = R.layout.open_book_from_gd_item1;
			int res2 = R.layout.open_book_from_gd_item2;
			view = mInflater.inflate(res, null);
			TextView fileView = (TextView)view.findViewById(R.id.file_name);
			TextView descrView = (TextView)view.findViewById(R.id.file_descr);
			TextView timeView = (TextView)view.findViewById(R.id.file_time);
			view1 = mInflater.inflate(res1, null);
			TextView fileView1 = (TextView)view1.findViewById(R.id.file_name);
			view2 = mInflater.inflate(res2, null);
			TextView fileView2 = (TextView)view2.findViewById(R.id.file_name);
			TextView timeView2 = (TextView)view2.findViewById(R.id.file_time);
			Metadata md = null;
			String sFile = "";
			if (ismWholeSearchW()) {
				if (mBooksList != null) md = mBooksList.get(position);
			} else {
				if ((mBooksList != null) && (position>0)) md = mBooksList.get(position-1);
				if ((mBooksList != null) && (position==0)) sFile = "[..]";
			}
			String sTitle = "";
			String sFTime = "";
			if ( md!=null ) {
				sFile = md.getTitle();
				if (sFile == null) sFile ="";
				if (md.getMimeType().equals("application/vnd.google-apps.folder")) sFile = "["+sFile+"]";
						sTitle = md.getDescription();
				if (sTitle == null) sTitle ="";
				final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
				final CharSequence sFName0 = dfmt.format("yyyy-MM-dd kk:mm:ss", md.getCreatedDate());
				sFTime = sFName0.toString();
				if (sFTime == null) sFTime ="";
				if ((sFTime.isEmpty())&&(sTitle.isEmpty())) {
					fileView1.setText(sFile);
				}
				if ((!sFTime.isEmpty())&&(sTitle.isEmpty())) {
					fileView2.setText(sFile);
					timeView2.setText(sFTime);
				}
				if ((!sFTime.isEmpty())&&(!sTitle.isEmpty())) {
					fileView.setText(sFile);
					descrView.setText(StrUtils.textShrinkLines(sTitle, true));
					timeView.setText(sFTime);
				}
			} else {
				fileView1.setText(sFile);
				descrView.setText("");
				timeView.setText("");
			}
			if ((sFTime.isEmpty())&&(sTitle.isEmpty())) return view1;
			if ((!sFTime.isEmpty())&&(sTitle.isEmpty())) return view2;
			if ((!sFTime.isEmpty())&&(!sTitle.isEmpty())) return view;
			return null;
		}

		public boolean hasStableIds() {
			return true;
		}

		public boolean isEmpty() {
			int cnt = 0;
			if (mBooksList != null) cnt = mBooksList.getCount();
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

	class OpenBookFromGDList extends BaseListView {

		private ListAdapter mAdapter;
		private OpenBookFromGdDlg uDDlg;

		public OpenBookFromGDList( Context context, OpenBookFromGdDlg udd) {
			super(context, true);
			uDDlg = udd;
			setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			setLongClickable(true);
			setAdapter(new OpenBookFromGDAdapter());
			setOnItemLongClickListener(new OnItemLongClickListener() {
				@Override
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
						int position, long arg3) {
					openContextMenu(OpenBookFromGDList.this);
//					DictsDlg dlg = new DictsDlg(mCoolReader, mCoolReader.getReaderView(), mBooksList.get(position).getDic_word());
//					dlg.show();
//					dismiss();
					return true;
				}
			});
		}

		@Override
		public boolean performItemClick(View view, int position, long id) {
			Metadata md = null;
			if (ismWholeSearchW()) {
				if (mBooksList != null) md = mBooksList.get(position);
			} else {
				if ((mBooksList != null) && (position>0)) md = mBooksList.get(position-1);
				if ((mBooksList != null) && (position==0)) {
					if (mDriveIdList.size()>1) {
						btnFolder.setChecked(true);
						driveToGoTo = mDriveIdList.get(mDriveIdList.size() - 2);
						mDriveIdList.remove(mDriveIdList.size()-1);
						mDriveStrList.remove(mDriveStrList.size()-1);
						mBooksList = null;
						listUpdated();
					}
					mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_FOLDER_CONTENTS, OpenBookFromGdDlg.this);
				}
			}
			if (md != null)
				if (md.getMimeType().equals("application/vnd.google-apps.folder")) {
					btnFolder.setChecked(true);
					driveToGoTo = md.getDriveId();
					mDriveIdList.add(md.getDriveId());
					mDriveStrList.add(md.getTitle());
					mBooksList = null;
					listUpdated();
					mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_FOLDER_CONTENTS, OpenBookFromGdDlg.this);
				} else {
					mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_BOOK_FILE, md);
					dismiss();
				}
			return true;
		}

		public void updateAdapter( OpenBookFromGDAdapter adapter ) {
			mAdapter = adapter;
			setAdapter(mAdapter);
		}

	}

	private String getFolderText() {
		String res = "";
		String sRoot = "root";
		if (mFoundWhole) sRoot = "search";
		for (String didmd: mDriveStrList) {
			res = res + "\\" + didmd.toString();
			if (res.equals("\\"+sRoot)) res = "";
			if (res.equals("\\root")) res = "";
		}
		if (mDriveStrList.size()<=1) res=sRoot;
		return res;
	}

	public OpenBookFromGdDlg(final CoolReader activity, MetadataBuffer mdb, ArrayList<DriveId> adid,
							 ArrayList<String> adidmd)
	{
		super(activity, activity.getResources().getString(R.string.win_title_user_dic), false, true);
		mInflater = LayoutInflater.from(getContext());
		mCoolReader = activity;
		mBooksList = mdb;
		mDriveIdList = adid;
		mDriveStrList = adidmd;

		mDriveIdList1 = new ArrayList<DriveId>(adid);
		mDriveStrList1 = new ArrayList<String>(adidmd);

        View frame = mInflater.inflate(R.layout.open_book_from_gd_dialog, null);
		ViewGroup body = (ViewGroup)frame.findViewById(R.id.book_list);
		mList = new OpenBookFromGDList(activity, this);
		btnWhole = (RadioButton)frame.findViewById(R.id.rb_whole);
		btnWhole.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					mWholeSearch = true;
				}
			}
		});
		btnFolder = (RadioButton)frame.findViewById(R.id.rb_folder);
		btnFolder.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if ( isChecked ) {
					mWholeSearch = false;
				}
			}
		});
		btnFolder.setChecked(true);
		final ImageButton btnFind = (ImageButton)frame.findViewById(R.id.do_find);

		btnFind.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (btnWhole.isChecked()) {
					mDriveIdList.clear();
					mDriveStrList.clear();
					mDriveIdList.add(mDriveIdList1.get(0));
					mDriveStrList.add("search");
				}
				mBooksList = null;
				listUpdated();
				mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_FOLDER_CONTENTS, OpenBookFromGdDlg.this);
			}
		});

		final ImageButton btnRoot = (ImageButton)frame.findViewById(R.id.goto_root);

		final ArrayList<String> alist = new ArrayList<String>();
		alist.add("text/plain");

		btnRoot.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
//				OpenFileActivityOptions openOptions =
//						new OpenFileActivityOptions.Builder()
//								.setActivityStartFolder(mDriveIdList.get(0))
//								//.setSelectionFilter(Filters.eq(SearchableField.MIME_TYPE, "text/plain"))
//								//.setMimeType(alist)
//								.build();
//
//				activity.mGoogleDriveTools.getmDriveClient().newOpenFileActivityIntentSender(openOptions).continueWith(new Continuation<IntentSender, Object>() {
//					@Override
//					public Void then(@NonNull Task<IntentSender> task) throws Exception {
//						activity.showToast("ds1");
//						return null;
//					}
//				});;
				mDriveIdList = new ArrayList<DriveId>(mDriveIdList1);
				mDriveStrList = new ArrayList<String>(mDriveStrList1);
				driveToGoTo = mDriveIdList.get(mDriveIdList.size()-1);
				btnFolder.setChecked(true);
				mWholeSearch = false;
				mFoundWhole = false;
				mBooksList = null;
				edtFind.setText("");
				listUpdated();
				mCoolReader.mGoogleDriveTools.signInAndDoAnAction(mCoolReader.mGoogleDriveTools.REQUEST_CODE_LOAD_FOLDER_CONTENTS, OpenBookFromGdDlg.this);
			}
		});

		lblFolder = (TextView)frame.findViewById(R.id.lbl_folder_text);
		lblFolder.setText(getFolderText());

		driveToGoTo = mDriveIdList.get(mDriveIdList.size()-1);

		edtFind = (EditText)frame.findViewById(R.id.find_text);
		edtFind.setText("");
		body.addView(mList);
		setView(frame);
		setFlingHandlers(mList, null, null);
		btnFind.requestFocus();

//		btnPage.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//				if ( isChecked ) {
//					mUserDic.clear();
//					for (UserDicEntry ude: activity.getmReaderFrame().getUserDicPanel().getArrUdeWords()) {
//						mUserDic.add(ude);
//					}
//					listUpdated();
//				}
//			}
//		});
//		btnBook.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//				if ( isChecked ) {
//					final String sBookFName = mCoolReader.getReaderView().getBookInfo().getFileInfo().filename;
//					CRC32 crc = new CRC32();
//					crc.update(sBookFName.getBytes());
//					String sCRC = String.valueOf(crc.getValue());
//					updUserDic(sCRC);
//					listUpdated();
//				}
//			}
//		});
//		btnAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//				if ( isChecked ) {
//					updUserDic("");
//					listUpdated();
//				}
//			}
//		});
	}

	@Override
	protected void onPositiveButtonClick() {
		// add Dict
		OpenBookFromGdDlg.this.dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		OpenBookFromGdDlg.this.dismiss();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.v("cr3", "creating OpenBookFromGdDlg");
		setTitle(mCoolReader.getResources().getString(R.string.win_title_open_book_from_gd));
		setCancelable(true);
		super.onCreate(savedInstanceState);
		//registerForContextMenu(mList);
	}

}
