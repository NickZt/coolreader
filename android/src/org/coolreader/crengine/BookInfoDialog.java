package org.coolreader.crengine;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.coolreader.CoolReader;
import org.coolreader.R;
import org.coolreader.cloud.CloudAction;
import org.coolreader.dic.TranslationDirectionDialog;

import android.app.SearchManager;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.ClipboardManager;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import uk.co.deanwild.flowtextview.FlowTextView;

import static org.coolreader.crengine.FileInfo.OPDS_DIR_PREFIX;

public class BookInfoDialog extends BaseDialog {

	public final static int BOOK_INFO = 0;
	public final static int OPDS_INFO = 1;
	public final static int OPDS_FINAL_INFO = 2;

	private final BaseActivity mCoolReader;
	private final BookInfo mBookInfo;
	private int mActionType;
	private FileInfo mFileInfoOPDS;
	private FileBrowser mFileBrowser;
	private FileInfo mCurrDir;
	private final LayoutInflater mInflater;
	private FileInfo mFileInfoSearchDir = null;
	private String mAuthors = "";
	private int mWindowSize;
	private Bitmap mBookCover;
	private String sAuthors = "";
	private String sBookTitle = "";
	private String sFileName = "";
	ImageButton btnBack;
	ImageButton btnOpenBook;
	ImageButton btnBookFolderOpen;
	ImageButton btnBookShortcut;
	ImageButton btnBookEdit;
	Button btnMarkToRead;
	ImageButton btnBookDownload;
	String annot2 = "";
	ImageButton btnFindAuthors;
	boolean bMarkToRead;
	TextView tvWC;
	TextView tvSC;
	TextView tvML;
	Button btnCalc;

	public BookInfo getmBookInfo() {
		return mBookInfo;
	}

	private Map<String, Integer> mLabelMap;
	private void fillMap() {
		mLabelMap = new HashMap<String, Integer>();
		mLabelMap.put("section.system", R.string.book_info_section_system);
		mLabelMap.put("system.version", R.string.book_info_system_version);
		mLabelMap.put("system.battery", R.string.book_info_system_battery);
		mLabelMap.put("system.time", R.string.book_info_system_time);
		mLabelMap.put("system.resolution", R.string.book_info_system_resolution);
		mLabelMap.put("section.file", R.string.book_info_section_file_properties);
		mLabelMap.put("file.name", R.string.book_info_file_name);
		mLabelMap.put("file.path", R.string.book_info_file_path);
		mLabelMap.put("file.arcname", R.string.book_info_file_arcname);
		mLabelMap.put("file.arcpath", R.string.book_info_file_arcpath);
		mLabelMap.put("file.arcsize", R.string.book_info_file_arcsize);
		mLabelMap.put("file.size", R.string.book_info_file_size);
		mLabelMap.put("file.format", R.string.book_info_file_format);
		mLabelMap.put("file.opds_link", R.string.book_info_opds_link);
		mLabelMap.put("section.position", R.string.book_info_section_current_position);
		mLabelMap.put("position.percent", R.string.book_info_position_percent);
		mLabelMap.put("position.page", R.string.book_info_position_page);
		mLabelMap.put("position.chapter", R.string.book_info_position_chapter);
		mLabelMap.put("section.book", R.string.book_info_section_book_properties);
		mLabelMap.put("book.authors", R.string.book_info_book_authors);
		mLabelMap.put("book.title", R.string.book_info_book_title);
		mLabelMap.put("book.date", R.string.book_info_book_date);
		mLabelMap.put("book.series", R.string.book_info_book_series_name);
		mLabelMap.put("book.language", R.string.book_info_book_language);
		mLabelMap.put("book.genre", R.string.book_info_book_genre);
		mLabelMap.put("book.srclang", R.string.book_info_book_srclang);
		mLabelMap.put("book.translator", R.string.book_info_book_translator);
		mLabelMap.put("book.symcount", R.string.book_info_book_symcount);
		mLabelMap.put("book.wordcount", R.string.book_info_book_wordcount);
		mLabelMap.put("book.minleft", R.string.book_info_stats_minutes_left);
		mLabelMap.put("section.book_document", R.string.book_info_section_book_document);
		mLabelMap.put("book.docauthor", R.string.book_info_book_docauthor);
		mLabelMap.put("book.docprogram", R.string.book_info_book_docprogram);
		mLabelMap.put("book.docdate", R.string.book_info_book_docdate);
		mLabelMap.put("book.docsrcurl", R.string.book_info_book_docsrcurl);
		mLabelMap.put("book.docsrcocr", R.string.book_info_book_docsrcocr);
		mLabelMap.put("book.docversion", R.string.book_info_book_docversion);
		mLabelMap.put("section.book_publisher", R.string.book_info_section_book_publisher);
		mLabelMap.put("book.publname", R.string.book_info_book_publname);
		mLabelMap.put("book.publisher", R.string.book_info_book_publisher);
		mLabelMap.put("book.publcity", R.string.book_info_book_publcity);
		mLabelMap.put("book.publyear", R.string.book_info_book_publyear);
		mLabelMap.put("book.publisbn", R.string.book_info_book_publisbn);
		mLabelMap.put("book.publseries", R.string.book_info_book_publseries_name);
		mLabelMap.put("book.translation", R.string.book_info_book_translation);
		mLabelMap.put("section.book_translation", R.string.book_info_section_book_translation);
		mLabelMap.put("system.device_model", R.string.book_info_system_device_model);
		mLabelMap.put("system.device_flags", R.string.book_info_system_device_flags);
	}

	protected void updateGlobalMargin(ViewGroup v,
									  boolean left, boolean top, boolean right, boolean bottom) {
		if (v == null) return;
		ViewGroup.LayoutParams lp = v.getLayoutParams();
		int globalMargins = activity.settings().getInt(Settings.PROP_GLOBAL_MARGIN, 0);
		if (globalMargins > 0)
			if (lp instanceof ViewGroup.MarginLayoutParams) {
				if (top) ((ViewGroup.MarginLayoutParams) lp).topMargin = globalMargins;
				if (bottom) ((ViewGroup.MarginLayoutParams) lp).bottomMargin = globalMargins;
				if (left) ((ViewGroup.MarginLayoutParams) lp).leftMargin = globalMargins;
				if (right) ((ViewGroup.MarginLayoutParams) lp).rightMargin = globalMargins;
			}
	}
	
	private void addItem(TableLayout table, BookInfoEntry item) {
		String name = item.infoTitle;
		String name1 = name;
		String value = item.infoValue;
		String typ = item.infoType;
		if ( name.length()==0 || value.length()==0 )
			return;
		boolean isSection = false;
		if ( "section".equals(name) ) {
			name = "";
			Integer id = mLabelMap.get(value);
			if ( id==null )
				return;
			String section = getContext().getString(id);
			if ( section!=null )
				value = section;
			isSection = true;
		} else {
			Integer id = mLabelMap.get(name);
			String title = id!=null ? getContext().getString(id) : name;
			if ( title!=null )
				name = title;
		}
		TableRow tableRow = (TableRow)mInflater.inflate(isSection ? R.layout.book_info_section : R.layout.book_info_item, null);
		ImageView btnOptionAddInfo = null;
		if (isSection) btnOptionAddInfo = (ImageView)tableRow.findViewById(R.id.btn_book_add_info);
		if ((!item.infoValue.equals("section.book"))||((StrUtils.isEmptyStr(sBookTitle))&&(StrUtils.isEmptyStr(sFileName)))) {
		//if (name.equals("section=section.book")) {
			if (btnOptionAddInfo!=null) btnOptionAddInfo.setVisibility(View.INVISIBLE);
		} else {
			if (btnOptionAddInfo!=null) {
				String sFind = sBookTitle;
				if (StrUtils.isEmptyStr(sBookTitle)) sFind = StrUtils.stripExtension(sFileName);
				if (!StrUtils.isEmptyStr(sAuthors)) sFind = sFind + ", " + sAuthors;
				btnOptionAddInfo.setImageDrawable(
						activity.getResources().getDrawable(Utils.resolveResourceIdByAttr(activity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				final View view1 = view;
				final String sFind1 = sFind;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							final Intent emailIntent = new Intent(Intent.ACTION_WEB_SEARCH);
							emailIntent.putExtra(SearchManager.QUERY, sFind1.trim());
							mCoolReader.startActivity(emailIntent);
						}
					});
			}
		}
		TextView nameView = (TextView)tableRow.findViewById(R.id.name);
		TextView valueView = (TextView)tableRow.findViewById(R.id.value);
		nameView.setText(name);
		valueView.setText(value);
		valueView.setTag(name);
		if (name.equals(activity.getString(R.string.book_info_book_symcount))) tvSC = valueView;
		if (name.equals(activity.getString(R.string.book_info_book_wordcount))) tvWC = valueView;
		if (name.equals(activity.getString(R.string.book_info_stats_minutes_left))) tvML = valueView;
		ReaderView rv = ((CoolReader) mCoolReader).getReaderView();
		if (
			(name.equals(activity.getString(R.string.book_info_book_symcount))) &&
		   (value.equals("0")) && (rv != null)
		)
			if ((mBookInfo != null) && (rv.mBookInfo != null))
				if (rv.mBookInfo.getFileInfo().getFilename().equals(mBookInfo.getFileInfo().getFilename())) {
					int colorGrayC;
					int colorIcon;
					TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
							{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon, R.attr.colorIconL});
					colorGrayC = a.getColor(0, Color.GRAY);
					colorIcon = a.getColor(2, Color.BLACK);
					a.recycle();
					Button countButton = new Button(mCoolReader);
					btnCalc = countButton;
					countButton.setText(activity.getString(R.string.calc_stats));
					countButton.setTextColor(colorIcon);
					countButton.setBackgroundColor(colorGrayC);
					LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.MATCH_PARENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					llp.setMargins(20, 0, 8, 0);
					countButton.setLayoutParams(llp);
					countButton.setMaxLines(3);
					countButton.setEllipsize(TextUtils.TruncateAt.END);
					final ViewGroup vg = (ViewGroup) valueView.getParent();
					vg.addView(countButton);
					countButton.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							int iPageCnt = 0;
							int iSymCnt = 0;
							int iWordCnt = 0;
							ReaderView rv = ((CoolReader) mCoolReader).getReaderView();
							if ((rv != null)&&(mBookInfo!=null)) {
								if (rv.getArrAllPages() != null)
									iPageCnt = rv.getArrAllPages().size();
								else {
									rv.CheckAllPagesLoadVisual();
									iPageCnt = rv.getArrAllPages().size();
								}
								for (int i = 0; i < iPageCnt; i++) {
									String sPage = rv.getArrAllPages().get(i);
									if (sPage == null) sPage = "";
									sPage = sPage.replace("\\n", " ");
									sPage = sPage.replace("\\r", " ");
									iSymCnt = iSymCnt + sPage.replaceAll("\\s+", " ").length();
									iWordCnt = iWordCnt + sPage.replaceAll("\\p{Punct}", " ").
											replaceAll("\\s+", " ").split("\\s").length;
								}
								mBookInfo.getFileInfo().symCount = iSymCnt;
								mBookInfo.getFileInfo().wordCount = iWordCnt;
								BookInfo bi = new BookInfo(mBookInfo.getFileInfo());
								bi.getFileInfo().symCount = iSymCnt;
								bi.getFileInfo().wordCount = iWordCnt;
								((CoolReader) mCoolReader).getDB().saveBookInfo(bi);
								((CoolReader) mCoolReader).getDB().flush();
								vg.removeView(countButton);
								if (tvSC != null) tvSC.setText(""+iSymCnt);
								if (tvWC != null) tvWC.setText(""+iWordCnt);
								ReadingStatRes sres = ((CoolReader) mCoolReader).getReaderView().getBookInfo().getFileInfo().calcStats();
								double speedKoef = sres.val;
								int pagesLeft;
								double msecLeft;
								double msecFivePages;
								PositionProperties currpos = ((CoolReader) mCoolReader).getReaderView().getDoc().getPositionProps(null);
								if ((bi.getFileInfo().symCount>0) && (speedKoef > 0.000001)) {
									pagesLeft = ((CoolReader) mCoolReader).getReaderView().getDoc().getPageCount() - currpos.pageNumber;
									double msecAllPages;
									msecAllPages = speedKoef * (double) bi.getFileInfo().symCount;
									msecFivePages = msecAllPages / ((double) ((CoolReader) mCoolReader).getReaderView().getDoc().getPageCount()) * 5.0;
									msecLeft = (((double) pagesLeft) / 5.0) * msecFivePages;
									String sLeft = " ";
									int minutes = (int) ((msecLeft / 1000) / 60);
									int hours = (int) ((msecLeft / 1000) / 60 / 60);
									if (hours>0) {
										minutes = minutes - (hours * 60);
										sLeft = sLeft + hours + "h "+minutes + "min (calc count: "+ sres.cnt +")";
									} else {
										sLeft = sLeft + minutes + "min (calc count: "+ sres.cnt +")";
									}
									if (tvML != null) tvML.setText(sLeft);
								}
							}
						}
					});
		}
		if (
			(name.equals(activity.getString(R.string.book_info_book_translation)))
		) {
			int colorGrayC;
			int colorIcon;
			TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon, R.attr.colorIconL});
			colorGrayC = a.getColor(0, Color.GRAY);
			colorIcon = a.getColor(2, Color.BLACK);
			a.recycle();
			Button translButton = new Button(mCoolReader);
			translButton.setText(activity.getString(R.string.specify_translation_dir));
			translButton.setTextColor(colorIcon);
			translButton.setBackgroundColor(colorGrayC);
			LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
					ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			llp.setMargins(20, 0, 8, 0);
			translButton.setLayoutParams(llp);
			translButton.setMaxLines(3);
			translButton.setEllipsize(TextUtils.TruncateAt.END);
			final CoolReader cr = (CoolReader) mCoolReader;
			translButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String lang = StrUtils.getNonEmptyStr(mBookInfo.getFileInfo().lang_to,true);
					String langf = StrUtils.getNonEmptyStr(mBookInfo.getFileInfo().lang_from, true);
					FileInfo fi = mBookInfo.getFileInfo();
					FileInfo dfi = fi.parent;
					if (dfi == null) {
						dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
					}
					if (dfi != null) {
						cr.editBookTransl(dfi, fi, langf, lang, "", null, TranslationDirectionDialog.FOR_COMMON);
					}
					dismiss();
				}
			});
			final ViewGroup vg = (ViewGroup) valueView.getParent();
			vg.addView(translButton);
		}
		if (typ.startsWith("link")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			if (typ.contains("application/atom+xml")) {
				valueView.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						String text = ((TextView) v).getText().toString();
						if (text != null && text.length() > 0)
							if (mFileBrowser != null) {
								FileInfo item = new FileInfo();
								item.pathname = OPDS_DIR_PREFIX + text;
								item.parent = mCurrDir;
								if (((TextView) v).getTag()!=null) {
									item.title = ((TextView) v).getTag().toString();
									item.setFilename(item.title);
								}
								BackgroundThread.instance().postBackground(new Runnable() {
									@Override
									public void run() {
										BackgroundThread.instance().postGUI(new Runnable() {
											@Override
											public void run() {
												mFileBrowser.showOPDSDir(item, null, "");
											}
										}, 200);
									}
								});
								dismiss();
							}
						}
				});
			} else {
				Linkify.addLinks(valueView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
				valueView.setLinksClickable(true);
				int colorGrayC;
				int colorIcon;
				TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
						{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon, R.attr.colorIconL});
				colorGrayC = a.getColor(0, Color.GRAY);
				colorIcon = a.getColor(2, Color.BLACK);
				a.recycle();
				valueView.setLinkTextColor(colorIcon);
				valueView.setTextColor(colorIcon);
			}
		} else
		if (typ.startsWith("series_authors")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			valueView.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String text = typ.replace("series_authors:", "").trim();
					if (!StrUtils.isEmptyStr(text)) {
						FileInfo dir = new FileInfo();
						dir.isDirectory = true;
						dir.pathname = FileInfo.AUTHORS_TAG;
						dir.setFilename(mCoolReader.getString(R.string.folder_name_books_by_author));
						dir.isListed = true;
						dir.isScanned = true;
						((CoolReader)mCoolReader).showDirectory(dir, "series:"+text);
						dismiss();
					}
				}
			});
		} else
		if (typ.startsWith("series_books")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			valueView.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String text = typ.replace("series_books:", "").trim();
					if (!StrUtils.isEmptyStr(text)) {
						FileInfo dir = new FileInfo();
						dir.isDirectory = true;
						dir.pathname = FileInfo.SERIES_PREFIX;
						dir.setFilename(mCoolReader.getString(R.string.folder_name_books_by_series));
						dir.isListed = true;
						dir.isScanned = true;
						dir.id = 0L;
						((CoolReader)mCoolReader).showDirectory(dir, "series:"+text);
						dismiss();
					}
				}
			});
		} else
		if (typ.startsWith("author_series")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			valueView.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String text = typ.replace("author_series:", "").trim();
					if (!StrUtils.isEmptyStr(text)) {
						FileInfo dir = new FileInfo();
						dir.isDirectory = true;
						dir.pathname = FileInfo.SERIES_TAG;
						dir.setFilename(mCoolReader.getString(R.string.folder_name_books_by_series));
						dir.isListed = true;
						dir.isScanned = true;
						((CoolReader)mCoolReader).showDirectory(dir, "author:"+text);
						dismiss();
					}
				}
			});
		} else
		if (typ.startsWith("author_books")) {
			valueView.setPaintFlags(valueView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
			valueView.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String text = typ.replace("author_books:", "").trim();
					if (!StrUtils.isEmptyStr(text)) {
						FileInfo dir = new FileInfo();
						dir.isDirectory = true;
						dir.pathname = FileInfo.AUTHOR_PREFIX;
						dir.setFilename(mCoolReader.getString(R.string.folder_name_books_by_author));
						dir.isListed = true;
						dir.isScanned = true;
						dir.id = 0L;
						((CoolReader)mCoolReader).showDirectory(dir, "author:"+text);
						dismiss();
					}
				}
			});
		} else
		if (typ.equals("text")) {
			valueView.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String text = ((TextView) v).getText().toString();
					if (text != null && text.length() > 0) {
						ClipboardManager cm = activity.getClipboardmanager();
						cm.setText(text);
						L.i("Setting clipboard text: " + text);
						activity.showToast(activity.getString(R.string.copied_to_clipboard) + ": " + text, v);
					}
				}
			});
		}
		table.addView(tableRow);
	}

	private void setDashedButton(Button btn) {
		if (btn == null) return;
		if (DeviceInfo.getSDKLevel() >= DeviceInfo.LOLLIPOP_5_0)
			btn.setBackgroundResource(R.drawable.button_bg_dashed_border);
		else
			btn.setPaintFlags(btn.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
	}

	private void paintMarkButton() {
		int colorGrayC;
		int colorBlue;
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2Contrast, R.attr.colorThemeBlue});
		colorGrayC = a.getColor(0, Color.GRAY);
		colorBlue = a.getColor(1, Color.BLUE);
		a.recycle();
		int colorGrayCT=Color.argb(30,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		int colorGrayCT2=Color.argb(200,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		mCoolReader.tintViewIcons(btnMarkToRead, PorterDuff.Mode.CLEAR,true);
		if (bMarkToRead) {
			btnMarkToRead.setBackgroundColor(colorGrayCT2);
			mCoolReader.tintViewIcons(btnMarkToRead, true);
			btnMarkToRead.setPaintFlags(btnMarkToRead.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
		} else {
			btnMarkToRead.setBackgroundColor(colorGrayCT);
			btnMarkToRead.setPaintFlags( btnMarkToRead.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
		}
		/*if (bMarkToRead) setDashedButton(btnMarkToRead);
			else {
				btnMarkToRead.setBackgroundColor(colorGrayCT);
				btnMarkToRead.setPaintFlags( btnMarkToRead.getPaintFlags() & (~ Paint.UNDERLINE_TEXT_FLAG));
			}*/
		btnMarkToRead.setTextColor(colorBlue);
	}

	private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
		ImageView bmImage;

		public DownloadImageTask(ImageView bmImage) {
			this.bmImage = bmImage;
		}

		protected Bitmap doInBackground(String... urls) {
			String urldisplay = urls[0];
			Bitmap mIcon11 = null;
			Bitmap resizedBitmap = null;
			try {
				String redir_url = Utils.getUrlLoc(new java.net.URL(urldisplay));
				InputStream in = null;
				if (StrUtils.isEmptyStr(redir_url))
					in = new java.net.URL(urldisplay).openStream();
				else
					in = new java.net.URL(redir_url).openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
				final int maxSize = 200;
				int outWidth;
				int outHeight;
				if (mIcon11 != null) {
					int inWidth = mIcon11.getWidth();
					int inHeight = mIcon11.getHeight();
					if (inWidth > inHeight) {
						outWidth = maxSize;
						outHeight = (inHeight * maxSize) / inWidth;
					} else {
						outHeight = maxSize;
						outWidth = (inWidth * maxSize) / inHeight;
					}
					resizedBitmap = Bitmap.createScaledBitmap(mIcon11, outWidth, outHeight, false);
				}
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return resizedBitmap;
		}

		protected void onPostExecute(Bitmap result) {
			if (result != null) bmImage.setImageBitmap(result);
		}
	}
	
	public BookInfoDialog(final BaseActivity activity, Collection<BookInfoEntry> items, BookInfo bi, final String annt,
						  int actionType, FileInfo fiOPDS, FileBrowser fb, FileInfo currDir)
	{
		super("BookInfoDialog", activity, null, false, false);
		String annot = annt;
		if (StrUtils.isEmptyStr(annt))
			if (fiOPDS != null) {
				if (!StrUtils.isEmptyStr(fiOPDS.annotation)) annot = fiOPDS.annotation;
				try {
					annot = android.text.Html.fromHtml(fiOPDS.annotation).toString();
				} catch (Exception e) {
					annot = fiOPDS.annotation;
				}
			}
		mCoolReader = activity;
		mBookInfo = bi;
		mActionType = actionType;
		mFileInfoOPDS = fiOPDS;
		mFileBrowser = fb;
		mCurrDir = currDir;
		FileInfo file = null;
		if (mBookInfo!=null)
			file = mBookInfo.getFileInfo();
		else file = fiOPDS;
		mFileInfoSearchDir = null;
		if (currDir!=null) {
			if (currDir.isOPDSDir())  {
				FileInfo par = currDir;
				while (par.parent != null) par=par.parent;
				for (int i=0; i<par.dirCount(); i++)
					if (par.getDir(i).pathname.startsWith(OPDS_DIR_PREFIX + "search:")) {
						mFileInfoSearchDir = par.getDir(i);
						break;
					}
			}
		}
		for (BookInfoEntry s: items) {
			if (s.infoTitle.equals("book.authors")) {
				mAuthors = s.infoValue;
			}
		}
		DisplayMetrics outMetrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
		this.mWindowSize = outMetrics.widthPixels < outMetrics.heightPixels ? outMetrics.widthPixels : outMetrics.heightPixels;
		//setTitle(mCoolReader.getString(R.string.dlg_book_info));
		fillMap();
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.book_info_dialog, null);
		final ImageView image = (ImageView)view.findViewById(R.id.book_cover);
		image.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CoolReader cr = (CoolReader)mCoolReader;
				if (mBookInfo!=null) {
					FileInfo fi = mBookInfo.getFileInfo();
					cr.loadDocument(fi);
					dismiss();
				} else //cr.showToast(R.string.book_info_action_unavailable);
				{
					cr.showToast(R.string.book_info_action_downloading);
					if (mFileBrowser != null) mFileBrowser.showOPDSDir(mFileInfoOPDS, mFileInfoOPDS, annot2);
					dismiss();
				}
			}
		});
		btnBack = ((ImageButton)view.findViewById(R.id.base_dlg_btn_back));
		btnBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onNegativeButtonClick();
			}
		});

		btnOpenBook = ((ImageButton)view.findViewById(R.id.btn_open_book));
		btnOpenBook.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				CoolReader cr = (CoolReader)mCoolReader;
				if (mBookInfo!=null) {
					FileInfo fi = mBookInfo.getFileInfo();
					cr.loadDocument(fi);
					dismiss();
				} else {
					//cr.showToast(R.string.book_info_action_unavailable);
					cr.showToast(R.string.book_info_action_downloading);
					if (mFileBrowser != null) mFileBrowser.showOPDSDir(mFileInfoOPDS, mFileInfoOPDS, annot2);
					dismiss();
				}
			}
		});

		btnBookFolderOpen = ((ImageButton)view.findViewById(R.id.book_folder_open));

			btnBookFolderOpen.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CoolReader cr = (CoolReader)mCoolReader;
				if (mBookInfo != null) {
					cr.showDirectory(mBookInfo.getFileInfo(), "");
					dismiss();
				}
			}
		});

		btnBookShortcut = ((ImageButton)view.findViewById(R.id.book_create_shortcut));

		btnBookShortcut.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			CoolReader cr = (CoolReader)mCoolReader;
			if (mBookInfo!=null) {
				FileInfo fi = mBookInfo.getFileInfo();
				cr.createBookShortcut(fi,mBookCover);
			} else cr.showToast(R.string.book_info_action_unavailable);
			}
		});

		btnBookEdit = ((ImageButton)view.findViewById(R.id.book_edit));
		btnBookEdit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			CoolReader cr = (CoolReader) mCoolReader;
			if (mBookInfo!=null) {
				FileInfo fi = mBookInfo.getFileInfo();
				FileInfo dfi = fi.parent;
				if (dfi == null) {
					dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
				}
				if (dfi != null) {
					cr.editBookInfo(dfi, fi);
					dismiss();
				}
			} else cr.showToast(R.string.book_info_action_unavailable);
			}
		});

		ImageButton btnSendByEmail = (ImageButton)view.findViewById(R.id.save_to_email);

		btnSendByEmail.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			CloudAction.emailSendBook((CoolReader) mCoolReader, mBookInfo);
			}
		});

		ImageButton btnSendByYnd = (ImageButton)view.findViewById(R.id.save_to_ynd);

		btnSendByYnd.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CloudAction.yndOpenBookDialog((CoolReader) mCoolReader, mBookInfo.getFileInfo(),true);
			}
		});

		ImageButton btnDeleteBook = (ImageButton)view.findViewById(R.id.book_delete);
		btnDeleteBook.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((CoolReader)activity).askDeleteBook(mBookInfo.getFileInfo());
				dismiss();
			}
		});

		ImageButton btnCustomCover = (ImageButton)view.findViewById(R.id.book_custom_cover);
		btnCustomCover.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((CoolReader)activity).picReceived!=null) {
					if (((CoolReader)activity).picReceived.bmpReceived!=null) {
						PictureCameDialog dlg = new PictureCameDialog(((CoolReader) activity),
								mBookInfo, "", "");
						dlg.show();
					}
				} else {
					((CoolReader)activity).showToast(R.string.pic_no_pic);
				}
			}
		});

		btnMarkToRead = ((Button)view.findViewById(R.id.btn_mark_toread));

		Drawable img = getContext().getResources().getDrawable(R.drawable.icons8_toc_item_normal);
		Drawable img1 = img.getConstantState().newDrawable().mutate();
		if (btnMarkToRead!=null) btnMarkToRead.setCompoundDrawablesWithIntrinsicBounds(img1, null, null, null);
		bMarkToRead = activity.settings().getBool(Settings.PROP_APP_MARK_DOWNLOADED_TO_READ, false);

		btnMarkToRead.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			bMarkToRead = !bMarkToRead;
			Properties props = new Properties(activity.settings());
			props.setProperty(Settings.PROP_APP_MARK_DOWNLOADED_TO_READ, bMarkToRead?"1":"0");
			activity.setSettings(props, -1, true);
			paintMarkButton();
			}
		});
		paintMarkButton();
		btnBookDownload = ((ImageButton)view.findViewById(R.id.book_download));
		annot2 = annot;
		btnBookDownload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mFileBrowser != null) mFileBrowser.showOPDSDir(mFileInfoOPDS, mFileInfoOPDS, annot2);
				dismiss();
			}
		});
		btnFindAuthors = ((ImageButton)view.findViewById(R.id.btn_find_authors));
		btnFindAuthors.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((mFileBrowser != null) && (mFileInfoSearchDir!=null))
					mFileBrowser.showFindBookDialog(false, mAuthors, mFileInfoSearchDir);
				dismiss();
			}
		});
		int w = mWindowSize * 4 / 10;
		int h = w * 4 / 3;
		image.setMinimumHeight(h);
		image.setMaxHeight(h);
		image.setMinimumWidth(w);
		image.setMaxWidth(w);
		Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
		if (file != null) {
			if (mFileInfoOPDS!=null) {
				String sTitle = StrUtils.getNonEmptyStr(file.title, true);
				String sAuthors = StrUtils.getNonEmptyStr(file.authors, true).replace("\\|", "\n");
				if (StrUtils.isEmptyStr(sTitle))
					sTitle = StrUtils.stripExtension(file.getFilename());
				Bitmap bmp2 = Services.getCoverpageManager().getBookCoverWithTitleBitmap(sTitle, sAuthors,
					150, 200);
				image.setImageBitmap(bmp2);
				for (OPDSUtil.LinkInfo link : mFileInfoOPDS.links) {
					if (link.type.contains("image")) {
						new DownloadImageTask(image).execute(link.href);
						break;
					}
				}
			} else {
				Services.getCoverpageManager().drawCoverpageFor(mCoolReader.getDB(), file, bmp, new CoverpageManager.CoverpageBitmapReadyListener() {
					@Override
					public void onCoverpageReady(CoverpageManager.ImageItem file, Bitmap bitmap) {
						mBookCover = bitmap;
						BitmapDrawable drawable = new BitmapDrawable(bitmap);
						image.setImageDrawable(drawable);
					}
				});
			}
		}
		TableLayout table = (TableLayout)view.findViewById(R.id.table);
		FlowTextView txtAnnot = (FlowTextView) view.findViewById(R.id.lbl_annotation);
		txtAnnot.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String text = ((FlowTextView) v).getText().toString();
				if ( text!=null && text.length()>0 ) {
					ClipboardManager cm = activity.getClipboardmanager();
					cm.setText(text);
					L.i("Setting clipboard text: " + text);
					activity.showToast(activity.getString(R.string.copied_to_clipboard)+": "+text,v);
				}
			}
		});
		File f = activity.getSettingsFile(activity.getCurrentProfile());
		String sF = f.getAbsolutePath();
		sF = sF.replace("/storage/","/s/").replace("/emulated/","/e/");
		TextView prof = (TextView) view.findViewById(R.id.lbl_profile);
		String sprof = activity.getCurrentProfileName();
		if (!StrUtils.isEmptyStr(sprof)) sprof = sprof + " - ";
		prof.setText(activity.getString(R.string.settings_profile)+": "+sprof + sF);
		String sss = "";
		if (mActionType == OPDS_INFO) sss = "["+mCoolReader.getString(R.string.book_info_action_download1)+"]\n";
		if (mActionType == OPDS_FINAL_INFO) sss = sss = "["+mCoolReader.getString(R.string.book_info_action_download2)+"]\n";
		SpannableString ss = new SpannableString(sss+annot);
		txtAnnot.setText(ss);
		int colorGray;
		int colorGrayC;
		int colorIcon;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast, R.attr.colorIcon});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		colorIcon = a.getColor(2, Color.GRAY);
		txtAnnot.setTextColor(colorIcon);
		txtAnnot.setTextSize(txtAnnot.getTextsize()/4f*3f);
		int colorGrayCT=Color.argb(128,Color.red(colorGrayC),Color.green(colorGrayC),Color.blue(colorGrayC));
		if ((StrUtils.isEmptyStr(mAuthors))||(mFileInfoSearchDir==null)) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnFindAuthors);
		}
		if (actionType == BOOK_INFO) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnBookDownload);
			parent.removeView(btnMarkToRead);
		}
		if (actionType == OPDS_INFO) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnOpenBook);
			parent.removeView(btnBookFolderOpen);
			parent.removeView(btnBookShortcut);
			parent.removeView(btnBookEdit);
			parent.removeView(btnSendByEmail);
			parent.removeView(btnDeleteBook);
			parent.removeView(btnCustomCover);
		}
		if (actionType == OPDS_FINAL_INFO) {
			ViewGroup parent = ((ViewGroup)btnBookDownload.getParent());
			parent.removeView(btnBookDownload);
			parent.removeView(btnMarkToRead);
			parent.removeView(btnFindAuthors);
		}
		for ( BookInfoEntry item : items ) {
			String name = item.infoTitle;
			String value = item.infoValue;
			if (name.equals("file.name")) sFileName = value;
			if (name.equals("book.authors")) sAuthors = value.replace("|",", ");
			if (name.equals("book.title")) sBookTitle = value;
		}
		for ( BookInfoEntry item : items ) {
			addItem(table, item);
		}
		buttonsLayout = (ViewGroup)view.findViewById(R.id.base_dlg_button_panel);
		updateGlobalMargin(buttonsLayout, true, true, true, false);
		setView( view );
	}

}
