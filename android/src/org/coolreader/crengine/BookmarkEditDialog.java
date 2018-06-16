package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.content.ClipboardManager;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.zip.CRC32;

public class BookmarkEditDialog extends BaseDialog {
	
	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final ReaderView mReaderView;
	private final Bookmark mOriginalBookmark;
	private final Bookmark mBookmark;
	private final boolean mIsNew;
	BookInfo mBookInfo;
	final EditText commentEdit;
	final EditText posEdit;
	
	@Override
	protected void onPositiveButtonClick() {
		if (mBookmark.getType() == Bookmark.TYPE_USER_DIC) {
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
			activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_NEW);
			if (activity instanceof CoolReader) {
				((CoolReader) activity).getmUserDic().put(ude.getDic_word(), ude);
				((CoolReader) activity).getmReaderFrame().getUserDicPanel().updateUserDicWords();
			}
		} else {
			if (mIsNew) {
				mBookmark.setCommentText(commentEdit.getText().toString());
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

	public BookmarkEditDialog( CoolReader activity, ReaderView readerView, Bookmark bookmark, boolean isNew)
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
			setThirdButtonImage(R.drawable.cr3_button_remove, R.string.mi_bookmark_delete);
		}
		boolean isComment = bookmark.getType()==Bookmark.TYPE_COMMENT;
		boolean isCorrection = bookmark.getType()==Bookmark.TYPE_CORRECTION;
		boolean isUserDic = bookmark.getType()==Bookmark.TYPE_USER_DIC;
		boolean isInternalLink = bookmark.getType()==Bookmark.TYPE_INTERNAL_LINK;
		setTitle(mCoolReader.getString( mIsNew ? R.string.dlg_bookmark_create : R.string.dlg_bookmark_edit));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.bookmark_edit_dialog, null);
		final RadioButton btnComment = (RadioButton)view.findViewById(R.id.rb_comment);
		final RadioButton btnCorrection = (RadioButton)view.findViewById(R.id.rb_correction);
		final RadioButton btnUserDic = (RadioButton)view.findViewById(R.id.rb_user_dic);
		final RadioButton btnInternalLink = (RadioButton)view.findViewById(R.id.rb_internal_link);
		btnInternalLink.setEnabled(false);
		final TextView posLabel = (TextView)view.findViewById(R.id.lbl_position);
		final TextView commentLabel = (TextView)view.findViewById(R.id.lbl_comment_text);

		final CoolReader cr = activity;

		commentLabel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (btnUserDic.isChecked())
					cr.showToast(R.string.dlg_bookmark_info);
			}
		});

		posEdit = (EditText)view.findViewById(R.id.position_text);
		final KeyListener keyList;
		keyList = posEdit.getKeyListener();
		posEdit.setKeyListener(null);

		final ImageButton btnSetComment = (ImageButton)view.findViewById(R.id.set_comment_from_cb);
		final TextView lblSetComment = (TextView)view.findViewById(R.id.lbl_comment_from_cb);

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
		final String cbtextFull = StrUtils.textShrinkLines(sClpb.trim(),false);
		lblSetComment.setText(cbtext.trim());
		if (cbtext.trim().equals("")) {
			btnSetComment.setEnabled(false);
		}
		lblSetComment.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!StrUtils.isEmptyStr(cbtextFull))
					commentEdit.setText(cbtextFull);
			}
		});
		btnSetComment.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				commentEdit.setText(cbtextFull);
				onPositiveButtonClick();
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
			btnComment.setChecked(isComment);
			btnCorrection.setChecked(!isComment);
			btnComment.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if ( isChecked ) {
						mBookmark.setType(Bookmark.TYPE_COMMENT);
						commentLabel.setText(R.string.dlg_bookmark_edit_comment); // : R.string.dlg_bookmark_edit_correction
						btnCorrection.setChecked(false);
						btnInternalLink.setChecked(false);
						btnUserDic.setChecked(false);
						posEdit.setKeyListener(null);
						posEdit.setText(mBookmark.getPosText());
					}
				}
			});
			btnCorrection.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if ( isChecked ) {
						mBookmark.setType(Bookmark.TYPE_CORRECTION);
						commentLabel.setText(R.string.dlg_bookmark_edit_correction);
						String oldText = commentEdit.getText().toString();
						if ( oldText==null || oldText.length()==0 )
							commentEdit.setText(mBookmark.getPosText());
						btnComment.setChecked(false);
						btnInternalLink.setChecked(false);
						btnUserDic.setChecked(false);
						posEdit.setKeyListener(null);
						posEdit.setText(mBookmark.getPosText());
					}
				}
			});
			btnInternalLink.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if ( isChecked ) {
						mBookmark.setType(Bookmark.TYPE_INTERNAL_LINK);
						commentLabel.setText(R.string.dlg_bookmark_edit_comment);
						btnComment.setChecked(false);
						btnCorrection.setChecked(false);
						btnUserDic.setChecked(false);
						posEdit.setKeyListener(null);
						posEdit.setText(mBookmark.getPosText());
					}
				}
			});
			btnUserDic.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					if ( isChecked ) {
						mBookmark.setType(Bookmark.TYPE_USER_DIC);
						commentLabel.setText(R.string.dlg_bookmark_edit_translation);
						btnComment.setChecked(false);
						btnCorrection.setChecked(false);
						btnInternalLink.setChecked(false);
						posEdit.setKeyListener(keyList);
						posEdit.setText(StrUtils.updateText(mBookmark.getPosText(),true));
					}
				}
			});
		} else {
			btnComment.setClickable(false);
			btnCorrection.setClickable(false);
			btnInternalLink.setClickable(false);
			btnUserDic.setClickable(false);
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

	
}
