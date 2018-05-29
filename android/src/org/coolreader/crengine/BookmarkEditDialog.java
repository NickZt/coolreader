package org.coolreader.crengine;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.content.ClipboardManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class BookmarkEditDialog extends BaseDialog {
	
	private final CoolReader mCoolReader;
	private final LayoutInflater mInflater;
	private final ReaderView mReaderView;
	private final Bookmark mOriginalBookmark;
	private final Bookmark mBookmark;
	private final boolean mIsNew;
	final EditText commentEdit;
	
	public BookmarkEditDialog( CoolReader activity, ReaderView readerView, Bookmark bookmark, boolean isNew)
	{
		super(activity, "", true, false);
		mCoolReader = activity;
		mReaderView = readerView;
		mIsNew = isNew;
		mOriginalBookmark = bookmark;
		//if ( !isNew )
			mBookmark = new Bookmark(bookmark);
		//else
		//	mBookmark = bookmark;
		if (!isNew) {
			setThirdButtonImage(R.drawable.cr3_button_remove, R.string.mi_bookmark_delete);
		}
		boolean isComment = bookmark.getType()==Bookmark.TYPE_COMMENT;
		setTitle(mCoolReader.getString( mIsNew ? R.string.dlg_bookmark_create : R.string.dlg_bookmark_edit));
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.bookmark_edit_dialog, null);
		final RadioButton btnComment = (RadioButton)view.findViewById(R.id.rb_comment);
		final RadioButton btnCorrection = (RadioButton)view.findViewById(R.id.rb_correction);
		final TextView posLabel = (TextView)view.findViewById(R.id.lbl_position); 
		final TextView commentLabel = (TextView)view.findViewById(R.id.lbl_comment_text); 
		final EditText posEdit = (EditText)view.findViewById(R.id.position_text);
		Button btnSetComment = (Button)view.findViewById(R.id.set_comment_from_cb);

		android.text.ClipboardManager cm = mCoolReader.getClipboardmanager();
		String cbtext = cm.getText().toString();
		if (cbtext.length()>100) {
			cbtext = cbtext.substring(0, 100)+"...";
		}
		final String cbtextFull = cm.getText().toString().trim();
		btnSetComment.setText(mCoolReader.getString(R.string.set_comment_from_cb)+": "+cbtext.trim());
		if (cbtext.trim().equals("")) {
			btnSetComment.setEnabled(false);
		}
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
		commentLabel.setText(isComment ? R.string.dlg_bookmark_edit_comment : R.string.dlg_bookmark_edit_correction);
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
					}
				}
			});
		} else {
			btnComment.setClickable(false);
			btnCorrection.setClickable(false);
		}
		setView( view );
	}

	@Override
	protected void onPositiveButtonClick() {
		if ( mIsNew ) {
			mBookmark.setCommentText( commentEdit.getText().toString() );
			mReaderView.addBookmark(mBookmark);
		} else {
			if ( mOriginalBookmark.setCommentText(commentEdit.getText().toString()) ) {
				mOriginalBookmark.setTimeStamp(System.currentTimeMillis());
				mReaderView.updateBookmark(mOriginalBookmark);
			}
		}
		super.onPositiveButtonClick();
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
