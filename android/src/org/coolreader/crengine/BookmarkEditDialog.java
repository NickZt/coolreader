package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.zip.CRC32;

public class BookmarkEditDialog extends BaseDialog {
	
	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final ReaderView mReaderView;
	private final Bookmark mOriginalBookmark;
	private final Bookmark mBookmark;
	private final boolean mIsNew;
	private final String cbtextFull;
	final TextView lblSetComment;
	final TextView lblBookmarkLink;
	BookInfo mBookInfo;
	final EditText commentEdit;
	final EditText posEdit;
	final TextView rb_descr;
	final TableRow tr_descr;
	int mChosenType = 0;
	final ImageButton btnComment;
	final ImageButton btnCorrection;
	final ImageButton btnUserDic;
	final ImageButton btnInternalLink;
	final ImageButton btnCitation;

	@Override
	protected void onPositiveButtonClick() {
		if (
			 (mBookmark.getType() == Bookmark.TYPE_USER_DIC) ||
			 (mBookmark.getType() == Bookmark.TYPE_CITATION)
		   ) {
			UserDicEntry ude = new UserDicEntry();
			ude.setId(0L);
			ude.setDic_word(posEdit.getText().toString().toLowerCase());
			ude.setDic_word_translate(commentEdit.getText().toString());
			final String sBookFName = mBookInfo.getFileInfo().filename;
			CRC32 crc = new CRC32();
			crc.update(sBookFName.getBytes());
			ude.setDic_from_book(String.valueOf(crc.getValue()));
			ude.setCreate_time(System.currentTimeMillis());
			ude.setLast_access_time(System.currentTimeMillis());
			ude.setLanguage(mBookInfo.getFileInfo().language);
			ude.setSeen_count(0L);
			ude.setIs_citation(0);
			if (mBookmark.getType() == Bookmark.TYPE_CITATION) ude.setIs_citation(1);
			activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_NEW);
			if (activity instanceof CoolReader) {
				((CoolReader) activity).getmUserDic().put(ude.getIs_citation()+ude.getDic_word(), ude);
				BackgroundThread.instance().postGUI(new Runnable() {
					@Override
					public void run() {
						((CoolReader) activity).getmReaderFrame().getUserDicPanel().updateUserDicWords();
					}
				}, 1000);
			}
		} else {
			if (mIsNew) {
				mBookmark.setCommentText(commentEdit.getText().toString());
				if (mBookmark.getType() == Bookmark.TYPE_INTERNAL_LINK)
					mBookmark.setType(Bookmark.TYPE_COMMENT);
				mReaderView.addBookmark(mBookmark);
			} else {
				if (mOriginalBookmark.setCommentText(commentEdit.getText().toString())) {
					mOriginalBookmark.setTimeStamp(System.currentTimeMillis());
					mReaderView.updateBookmark(mOriginalBookmark);
				}
			}
		}
		super.onPositiveButtonClick();
	}

	public void BookmarkChooseCallback(Bookmark bm) {
		if (bm!=null) {
			String s1 = bm.getTitleText();
			String s2 = bm.getPosText();
			String s3 = bm.getCommentText();
			lblBookmarkLink.setText("");
			if (!StrUtils.isEmptyStr(s1)) lblBookmarkLink.setText(activity.getString(R.string.dlg_bookmark_link) +": " +s1);
			else if (!StrUtils.isEmptyStr(s2)) lblBookmarkLink.setText(activity.getString(R.string.dlg_bookmark_link) +": " +s2);
			else if (!StrUtils.isEmptyStr(s3)) lblBookmarkLink.setText(activity.getString(R.string.dlg_bookmark_link) +": " + s3);
			mBookmark.setLinkPos(bm.getStartPos());
		} else {
			mBookmark.setLinkPos("");
			lblBookmarkLink.setText("");
		}
	}

	private void setChecked(ImageButton btn) {
		rb_descr.setText(btn.getContentDescription()+" ");
		if (btn.getContentDescription().equals(R.string.dlg_bookmark_type_comment)) {
			mChosenType = Bookmark.TYPE_COMMENT;
		}
		if (btn.getContentDescription().equals(R.string.dlg_bookmark_type_correction)) {
			mChosenType = Bookmark.TYPE_CORRECTION;
		}
		if (btn.getContentDescription().equals(R.string.dlg_bookmark_internal_link)) {
			mChosenType = Bookmark.TYPE_INTERNAL_LINK;
		}
		if (btn.getContentDescription().equals(R.string.dlg_bookmark_user_dic)) {
			mChosenType = Bookmark.TYPE_USER_DIC;
		}
		if (btn.getContentDescription().equals(R.string.dlg_bookmark_citation)) {
			mChosenType = Bookmark.TYPE_CITATION;
		}
		int colorGray;
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		rb_descr.setBackgroundColor(colorGrayC);
		//tr_descr.setBackgroundColor(colorGrayC);
		btnComment.setBackgroundColor(colorGrayC);
		btnCorrection.setBackgroundColor(colorGrayC);
		btnUserDic.setBackgroundColor(colorGrayC);
		btnInternalLink.setBackgroundColor(colorGrayC);
		btnCitation.setBackgroundColor(colorGrayC);
		lblSetComment.setTextColor(colorGray);
		lblBookmarkLink.setTextColor(colorGray);
		btn.setBackgroundColor(colorGray);
	}

	private boolean getChecked(ImageButton btn) {
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_type_comment))) {
			return mChosenType == Bookmark.TYPE_COMMENT;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_type_correction))) {
			return mChosenType == Bookmark.TYPE_CORRECTION;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_internal_link))) {
			return mChosenType == Bookmark.TYPE_INTERNAL_LINK;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_user_dic))) {
			return mChosenType == Bookmark.TYPE_USER_DIC;
		}
		if (btn.getContentDescription().equals(mCoolReader.getString(R.string.dlg_bookmark_citation))) {
			return mChosenType == Bookmark.TYPE_CITATION;
		}
		return false;
	}

	public BookmarkEditDialog(final CoolReader activity, ReaderView readerView, Bookmark bookmark, boolean isNew, int chosenType)
	{
		super(activity, "", true, false);
		mCoolReader = activity;
		mReaderView = readerView;
		mIsNew = isNew;
		mOriginalBookmark = bookmark;
		this.mBookInfo = mReaderView.getBookInfo();
		//if ( !isNew )
		mBookmark = new Bookmark(bookmark);
		//else
		//	mBookmark = bookmark;
		if (!isNew) {
			setThirdButtonImage(R.drawable.icons8_minus, R.string.mi_bookmark_delete);
		}
		boolean isComment = bookmark.getType()==Bookmark.TYPE_COMMENT;
		boolean isCorrection = bookmark.getType()==Bookmark.TYPE_CORRECTION;
		boolean isUserDic = bookmark.getType()==Bookmark.TYPE_USER_DIC;
		boolean isInternalLink = bookmark.getType()==Bookmark.TYPE_INTERNAL_LINK;
		boolean isCitation = bookmark.getType()==Bookmark.TYPE_CITATION;
		setTitle(mCoolReader.getString( mIsNew ? R.string.dlg_bookmark_create : R.string.dlg_bookmark_edit));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.bookmark_edit_dialog, null);
		btnComment = (ImageButton)view.findViewById(R.id.rb_comment);
		btnCorrection = (ImageButton)view.findViewById(R.id.rb_correction);
		btnUserDic = (ImageButton)view.findViewById(R.id.rb_user_dic);
		btnInternalLink = (ImageButton)view.findViewById(R.id.rb_internal_link);
		btnCitation = (ImageButton)view.findViewById(R.id.rb_citation);
		rb_descr = (TextView)view.findViewById(R.id.lbl_rb_descr);
		tr_descr = (TableRow)view.findViewById(R.id.tr_rb_descr);
		final TextView posLabel = (TextView)view.findViewById(R.id.lbl_position);
		final TextView commentLabel = (TextView)view.findViewById(R.id.lbl_comment_text);

		final CoolReader cr = activity;

		commentLabel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (getChecked(btnUserDic))
					cr.showToast(R.string.dlg_bookmark_info);
			}
		});

		posEdit = (EditText)view.findViewById(R.id.position_text);
		final KeyListener keyList;
		keyList = posEdit.getKeyListener();
		posEdit.setKeyListener(null);

		final ImageButton btnSetComment = (ImageButton)view.findViewById(R.id.base_dlg_btn_add);
		lblSetComment = (TextView)view.findViewById(R.id.lbl_comment_from_cb);
		lblBookmarkLink = (TextView)view.findViewById(R.id.lbl_bookmark_link);
		String sL = mBookmark.getLinkPos();
		if (StrUtils.isEmptyStr(sL)) lblBookmarkLink.setText(""); else
			lblBookmarkLink.setText(activity.getString(R.string.dlg_bookmark_link) +": " +sL);
		if (!StrUtils.isEmptyStr(sL)) {
			for (Bookmark b: mBookInfo.getAllBookmarks()) {
				if (b.getStartPos().equals(sL)) {
					BookmarkChooseCallback(b);
					break;
				}
			}
		}
		String sClpb = "<empty>";
		try {
			android.text.ClipboardManager cm = mCoolReader.getClipboardmanager();
			sClpb = cm.getText().toString();
		} catch (Exception e) {

		}
		String cbtext = StrUtils.textShrinkLines(sClpb,true);
		if (cbtext.length()>100) {
			cbtext = cbtext.substring(0, 100)+"...";
		}
		if (cbtext.trim().length()==0) {
			cbtext = "<empty>";
		}
		cbtextFull = StrUtils.textShrinkLines(sClpb.trim(),false);
		lblSetComment.setText("");
		if (!cbtext.trim().equals(""))
			lblSetComment.setText(activity.getString(R.string.clipb_contents) +" "+cbtext.trim());
		if (!cbtext.trim().equals("")) {
			setAddButtonImage(R.drawable.icons8_ok_from_clipboard, R.string.set_comment_from_cb_capt);
		}
		lblSetComment.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!StrUtils.isEmptyStr(cbtextFull))
					commentEdit.setText(cbtextFull);
			}
		});
		lblBookmarkLink.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsNew)
					activity.showBookmarksDialog(true, BookmarkEditDialog.this);
			}
		});
		commentEdit = (EditText)view.findViewById(R.id.comment_edit);
		String postext = mBookmark.getPercent()/100 + "%";
		if ( mBookmark.getTitleText()!=null )
			postext = postext + "  " + mBookmark.getTitleText();
		posLabel.setText(postext);
		if (isComment) commentLabel.setText(R.string.dlg_bookmark_edit_comment);
		if (isCorrection) commentLabel.setText(R.string.dlg_bookmark_edit_correction);
		if (isUserDic) commentLabel.setText(R.string.dlg_bookmark_edit_translation);
		if (isInternalLink) commentLabel.setText(R.string.dlg_bookmark_edit_comment);

		posEdit.setText(mBookmark.getPosText());
		commentEdit.setText(bookmark.getCommentText());
		if ( isNew ) {
			if (isComment) setChecked(btnComment);
			if (!isComment) setChecked(btnCorrection);
			btnComment.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mBookmark.setType(Bookmark.TYPE_COMMENT);
					commentLabel.setText(R.string.dlg_bookmark_edit_comment); // : R.string.dlg_bookmark_edit_correction
					posEdit.setKeyListener(null);
					posEdit.setText(mBookmark.getPosText());
					setChecked(btnComment);
				}
			});
			btnCorrection.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mBookmark.setType(Bookmark.TYPE_CORRECTION);
					commentLabel.setText(R.string.dlg_bookmark_edit_correction);
					String oldText = commentEdit.getText().toString();
					if ( oldText==null || oldText.length()==0 )
						commentEdit.setText(mBookmark.getPosText());
					posEdit.setKeyListener(null);
					posEdit.setText(mBookmark.getPosText());
					setChecked(btnCorrection);
				}
			});
			btnInternalLink.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mBookmark.setType(Bookmark.TYPE_INTERNAL_LINK);
					commentLabel.setText(R.string.dlg_bookmark_edit_comment);
					posEdit.setKeyListener(null);
					posEdit.setText(mBookmark.getPosText());
					setChecked(btnInternalLink);
					if (mIsNew)
						activity.showBookmarksDialog(true, BookmarkEditDialog.this);
				}
			});
			btnUserDic.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mBookmark.setType(Bookmark.TYPE_USER_DIC);
					commentLabel.setText(R.string.dlg_bookmark_edit_translation);
					posEdit.setKeyListener(keyList);
					posEdit.setText(StrUtils.updateText(mBookmark.getPosText(),true));
					setChecked(btnUserDic);
				}
			});
			btnCitation.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mBookmark.setType(Bookmark.TYPE_CITATION);
					commentLabel.setText(R.string.dlg_bookmark_edit_comment); // : R.string.dlg_bookmark_edit_correction
					posEdit.setKeyListener(keyList);
					posEdit.setText(mBookmark.getPosText());
					setChecked(btnCitation);
				}
			});
		} else {
			btnComment.setClickable(false);
			btnCorrection.setClickable(false);
			btnInternalLink.setClickable(false);
			btnUserDic.setClickable(false);
			btnCitation.setClickable(false);
		}
		if (chosenType==Bookmark.TYPE_COMMENT) setChecked(btnComment);
		if (chosenType==Bookmark.TYPE_CORRECTION) setChecked(btnCorrection);
		if (chosenType==Bookmark.TYPE_INTERNAL_LINK) setChecked(btnInternalLink);
		if (chosenType==Bookmark.TYPE_USER_DIC) {
			setChecked(btnUserDic);
			posEdit.setText(StrUtils.updateText(mBookmark.getPosText(),true));
			posEdit.setKeyListener(keyList);
		}
		if (chosenType==Bookmark.TYPE_CITATION) {
			setChecked(btnCitation);
			posEdit.setKeyListener(keyList);
		}
		setView( view );
	}

	@Override
	protected void onNegativeButtonClick() {
		super.onNegativeButtonClick();
	}

	@Override
	protected void onThirdButtonClick() {
		mCoolReader.askConfirmation(R.string.win_title_confirm_bookmark_delete, new Runnable() {
			@Override
			public void run() {
				mReaderView.removeBookmark(mBookmark);
				onNegativeButtonClick();
			}
		});
	}

	@Override
	protected void onAddButtonClick() {
		commentEdit.setText(cbtextFull);
		onPositiveButtonClick();
	}

	
}
