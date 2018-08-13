package org.coolreader.crengine;

import android.graphics.Paint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

public class UserDicPanel extends LinearLayout implements Settings {
		private CoolReader activity;
		private LinearLayout content;
		private TextView lblWordFound;
		private ArrayList<TextView> arrLblWords = new ArrayList<TextView>();

	public ArrayList<UserDicEntry> getArrUdeWords() {
		return arrUdeWords;
	}

	private ArrayList<UserDicEntry> arrUdeWords = new ArrayList<UserDicEntry>();
		private TextView lblWord;
		private int textSize = 14;
		private int color = 0;
		private int wc = 0;
		private boolean fullscreen;
		private boolean nightMode;

		FileInfo book;
		Bookmark position;
		PositionProperties props;

		public void updateFullscreen(boolean fullscreen) {
			if (this.fullscreen == fullscreen)
				return;
			this.fullscreen = fullscreen;
			requestLayout();
		}

		public boolean updateSettings(Properties props) {
			int newTextSize = props.getInt(Settings.PROP_STATUS_FONT_SIZE, 16);
			boolean needRelayout = (textSize != newTextSize);
			this.textSize = newTextSize;
			nightMode = props.getBool(PROP_NIGHT_MODE, false);
			this.color = props.getColor(Settings.PROP_STATUS_FONT_COLOR, 0);
			lblWordFound.setTextColor(0xFF000000 | color);
			for (TextView tv: arrLblWords) {
				tv.setTextColor(0xFF000000 | color);
				tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			}
			lblWordFound.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			if (needRelayout) {
				CoolReader.log.d("changing user dic layout");
				lblWordFound.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				for (TextView tv: arrLblWords) {
					tv.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				}
				content.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				content.forceLayout();
				forceLayout();
			}
			invalidate();
			return needRelayout;
		}

		public UserDicPanel(CoolReader context) {
			super(context);
			this.activity = context;
			setOrientation(VERTICAL);
			
			this.color = context.settings().getColor(Settings.PROP_STATUS_FONT_COLOR, 0);
			
			LayoutInflater inflater = LayoutInflater.from(activity);
			content = (LinearLayout)inflater.inflate(R.layout.user_dic_panel, null);
			lblWordFound = (TextView)content.findViewById(R.id.word_found);
			arrLblWords.clear();
			arrUdeWords.clear();
			lblWord = (TextView)content.findViewById(R.id.word1);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word2);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word3);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word4);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word5);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word6);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word7);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word8);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word9);
			arrLblWords.add(lblWord);
			lblWord = (TextView)content.findViewById(R.id.word10);
			arrLblWords.add(lblWord);

			lblWordFound.setText("");
			lblWordFound.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			lblWordFound.setTextColor(0xFF000000 | color);
            lblWordFound.setPaintFlags(lblWordFound.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

			lblWordFound.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					UserDicDlg dlg = new UserDicDlg(activity,0);
					dlg.show();
				}
			});

			int i = 0;

			for (TextView tv: arrLblWords) {
				i++;
				tv.setText(String.valueOf(""));
				tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
				tv.setTextColor(0xFF000000 | color);
				tv.setPaintFlags(tv.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
				tv.setOnClickListener(new OnClickListener() {

				@Override
					public void onClick(View v) {
						if (v instanceof TextView) {
							String sWord = ((TextView) v).getText().toString();
							for (UserDicEntry ude: arrUdeWords) {
								String sKey = ude.getDic_word();
								try {
									String[] arrKey = sKey.split("~");
									sKey = arrKey[0];
									sKey = sKey.replace("|", "");
								} catch (Exception e) {

								}
								if (sKey.equals(sWord)) {
									activity.showSuperToast(StrUtils.updateText(ude.getDic_word_translate(),true));
									activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_UPDATE_CNT);
									break;
								}
							}
						}
					}
				});
				tv.setOnLongClickListener(new OnLongClickListener() {

					@Override
					public boolean onLongClick(View v) {
						if (v instanceof TextView) {
							String sWord = ((TextView) v).getText().toString();
							for (final UserDicEntry ude: arrUdeWords) {
								if (ude.getDic_word().equals(sWord)) {
									activity.askConfirmation(R.string.win_title_confirm_ude_delete, new Runnable() {
										@Override
										public void run() {
											activity.getDB().saveUserDic(ude, UserDicEntry.ACTION_DELETE);
											activity.getmUserDic().remove(ude.getDic_word());
											activity.getmReaderFrame().getUserDicPanel().updateUserDicWords();
										}
									});
									break;
								}
							}
						}
						return true;
					}
				});
			}

			addView(content);
			onThemeChanged(context.getCurrentTheme());
			updateSettings(context.settings());
		}

		public void onThemeChanged(InterfaceTheme theme) {
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			content.measure(widthMeasureSpec, heightMeasureSpec);
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
		
		private static boolean empty(String s) {
			return s == null || s.length() == 0;
		}
		
		private static void append(StringBuilder buf, String text, String delimiter) {
			if (!Utils.empty(text)) {
				if (buf.length() != 0 && !empty(delimiter)) {
					buf.append(delimiter);
				}
				buf.append(text);
			}
		}
		
		public void updateCurrentPositionStatus(FileInfo book, Bookmark position, PositionProperties props) {
			this.book = book != null ? new FileInfo(book) : null;
			this.position = position != null ? new Bookmark(position) : null;
			this.props = props != null ? new PositionProperties(props) : null;
			BackgroundThread.instance().postGUI(new Runnable() {
				@Override
				public void run() {
					updateUserDicWords();
				}
			}, 500);
		}

		public void updateUserDicWords() {
			//PositionProperties currpos = activity.getReaderView().getDoc().getPositionProps(null);
			int curPage = activity.getReaderView().getDoc().getCurPage();
			activity.getReaderView().CheckAllPagesLoad();
            String sPrevPage = "";
            String sPageText = "";
			if ((activity.getReaderView().getArrAllPages().size()>curPage)&&(curPage>=0)) {
                sPageText = activity.getReaderView().getArrAllPages().get(curPage);
                if (curPage > 0)
                    sPrevPage = activity.getReaderView().getArrAllPages().get(curPage - 1);
            }
			if (sPageText==null) sPageText = "";
            if (sPrevPage==null) sPrevPage = "";
            this.wc = 0;
			this.arrUdeWords.clear();
			Iterator it = activity.getmUserDic().entrySet().iterator();
			String sCurPage = sPageText.toLowerCase();
			sPrevPage = sPrevPage.toLowerCase();
            int cnt = 50;
            try {
                if (sPrevPage.length() > 10) {
                    if (!Character.isWhitespace(sPrevPage.charAt(sPrevPage.length() - 1))) {
                        for (int i = sPrevPage.length() - 1; i > 0; i--) {
                            cnt++;
                            if (Character.isWhitespace(sPrevPage.charAt(i))) {
                                sCurPage = sPrevPage.substring(i, sPrevPage.length()) + sCurPage;
                                break;
                            }
                            if (cnt>100) break;
                        }
                    }
                }
            } catch (Exception e) {

            }
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry)it.next();
				String sKey = pair.getKey().toString().toLowerCase();
				String sType = sKey.substring(0,1);
				sKey = sKey.substring(1);
				if ((!StrUtils.isEmptyStr(sKey))&&(!sType.equals("1"))) {
					try {
						String[] arrKey = sKey.split("~");
						boolean bWas = false;
						for (String sK : arrKey) {
							CoolReader.log.i(sK);
							String sK2 = sK.replaceAll("-", " ");
							String sK3 = sK.replaceAll("-", "");
							String[] arrK = sK.split("\\|");
							String[] arrK2 = sK2.split("\\|");
							String[] arrK3 = sK3.split("\\|");
							if ((!bWas)&&
								  (sCurPage.contains(arrK[0]) || sCurPage.contains(arrK2[0]) || sCurPage.contains(arrK3[0]))) {
								bWas = true;
								this.wc = this.wc + 1;
								UserDicEntry ude = (UserDicEntry) pair.getValue();
								this.arrUdeWords.add(ude);
							}
						}
					} catch (Exception e) {

					}
				}
			}
			Collections.sort(arrUdeWords, new Comparator<UserDicEntry>() {
				@Override
				public int compare(UserDicEntry lhs, UserDicEntry rhs) {
					// -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
					return lhs.getDic_word().compareToIgnoreCase(rhs.getDic_word());
				}
			});
			updateViews();
		}

		private void updateViews() {
			String sWC = "wc: "+String.valueOf(this.wc);
			if (this.wc == 0) sWC="";
			String sAll = this.lblWordFound.getText().toString();
			for (TextView tv: arrLblWords) {
				sAll=sAll+tv.getText().toString();
			}
			boolean updated = false;
			if (!lblWordFound.getText().equals(sWC)) {
				this.lblWordFound.setText(sWC);
			}
			int i=0;
			for (TextView tv: arrLblWords) {
				tv.setText("");
			}
			for (UserDicEntry ude: arrUdeWords) {
				i++;
				if (i<10) {
					arrLblWords.get(i).setText("_");
					try {
						String sKey = ude.getDic_word();
						String[] arrKey = sKey.split("~");
						sKey = arrKey[0];
						arrLblWords.get(i).setText(sKey.replace("|", ""));
					} catch (Exception e) {

					}
				}
			}
			String sAll2 = this.lblWordFound.getText().toString();
			for (TextView tv: arrLblWords) {
				sAll2=sAll2+tv.getText().toString();
			}
			if (!sAll.equals(sAll2)) updated=true;
			if (updated && isShown()) {
				CoolReader.log.d("changing user dic layout");
				measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
				forceLayout();
			}
		}
	
	}