package org.coolreader.crengine;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.coolreader.CoolReader;
import org.coolreader.dic.Dictionaries;
import org.coolreader.dic.Dictionaries.DictInfo;
import org.coolreader.R;
import org.coolreader.cloud.CloudSync;
import org.coolreader.cloud.dropbox.DBXInputTokenDialog;
import org.coolreader.cloud.yandex.YNDInputTokenDialog;
import org.coolreader.crengine.ColorPickerDialog.OnColorChangedListener;
import org.coolreader.geo.MetroLocation;
import org.coolreader.geo.TransportStop;
import org.coolreader.plugins.OnlineStorePluginManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabWidget;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class OptionsDialog extends BaseDialog implements TabContentFactory, OptionOwner, Settings {

	public static int toastShowCnt = 0;

	ReaderView mReaderView;
	BaseActivity mActivity;
	String optionFilter;

	public String selectedOption;
	public int selectedTab = -1;

	String[] mFontFaces;
	String[] mFontFacesFiles;

	public static int[] mFontSizes = new int[] {
		9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
		31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 42, 44, 48, 50, 52, 54, 56, 60, 64, 68, 72, 78, 84, 90, 110, 130, 150, 170, 200, 230, 260, 300, 340
	};

	int[] mStatusFontSizes = new int[] {
			9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 20, 22, 24, 25, 26, 27, 28, 29, 30,
			31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 42, 44, 48, 52, 56, 60, 64, 68, 72, 78, 84, 90, 110, 130, 150, 170, 200, 230, 260, 300, 340
		};
		
	int[] filterFontSizes(int[] fontSizes) {
	    ArrayList<Integer> list = new ArrayList<Integer>();
	    for (int i = 0; i < fontSizes.length; i++) {
	        int sz = fontSizes[i];
	        if (sz >= mActivity.getMinFontSize() && sz <= mActivity.getMaxFontSize())
    	        list.add(sz);
	    }
	    int[] res = new int[list.size()];
	    for (int i = 0; i < list.size(); i++)
	        res[i] = list.get(i);
	    return res;
	}

	public static int getFontSizeShift(int curSize, int shift) {
		for (int i = 0; i < mFontSizes.length; i++)
			if (mFontSizes[i] == curSize) {
				if (i + shift < 0) return mFontSizes[0];
				if (i + shift >= mFontSizes.length) return mFontSizes[mFontSizes.length-1];
				return mFontSizes[i + shift];
			}
		return curSize;
	}

	public static int getMarginShift(int curSize, int shift) {
		for (int i = 0; i < mMargins.length; i++)
			if (mMargins[i] == curSize) {
				if (i + shift < 0) return mMargins[0];
				if (i + shift >= mMargins.length) return mMargins[mMargins.length-1];
				return mMargins[i + shift];
			}
		return curSize;
	}
	
	public static int findBacklightSettingIndex( int value ) {
		int bestIndex = 0;
		int bestDiff = -1;
		for ( int i=0; i<mBacklightLevels.length; i++ ) {
			int diff = mBacklightLevels[i] - value;
			if (diff<0)
				diff = -diff;
			if ( bestDiff==-1 || diff < bestDiff ) {
				bestDiff = diff;
				bestIndex = i;
			}
		}
		return bestIndex;
	}
	public static final int[] mBacklightLevels = new int[] {
		-1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100
	};
	public static final String[] mBacklightLevelsTitles = new String[] {
			"Default", "1%", "2%", "3%", "4%", "5%", "6%", "7%", "8%", "9%", 
			"10%", "12%", "15%", "20%", "25%", "30%", "35%", "40%", "45%", "50%", "55%", "60%", "65%", "70%", "75%", "80%", "85%", "90%", "95%", "100%",
	};
	public static final int[] mBacklightLevelsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	public static final int[] mToolbarButtons = new int[] {
			0, 1, 2, 3, 4, 5, 6
	};
	public static final int[] mToolbarButtonsTitles = new int[] {
			R.string.option_toolbar_buttons_none,
			R.string.option_toolbar_buttons_toolbar,
			R.string.option_toolbar_buttons_more,
			R.string.option_toolbar_buttons_both,
			R.string.option_toolbar_buttons_toolbar_1st,
			R.string.option_toolbar_buttons_more_1st,
			R.string.option_toolbar_buttons_both_1st
	};

	public static final int[] mToolbarAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	public static int[] mMotionTimeouts;
	public static String[] mMotionTimeoutsTitles;
	public static int[] mMotionTimeoutsAddInfos;
	public static int[] mMotionTimeoutsSec;
	public static String[] mMotionTimeoutsTitlesSec;
	public static int[] mMotionTimeoutsAddInfosSec;
	public static int[] mMotionTimeouts1;
    public static String[] mMotionTimeoutsTitles1;
    public static int[] mMotionTimeoutsAddInfos1;
	public static int[] mPagesPerFullSwipe;
	public static int[] mPagesPerFullSwipeAddInfos;
	public static String[] mPagesPerFullSwipeTitles;
	public static int[] mForceTTS;
	public static String[] mForceTTSTitles;
	public static int[] mForceTTSAddInfos;
	public static int[] mOrient;
	public static String[] mOrientTitles;
	public static int[] mOrientAddInfos;
	public static int[] mBrowserMaxGroupItems;

    int[] mInterlineSpaces = new int[] {
			80, 85, 90, 95, 100, 105, 110, 115, 120, 130, 140, 150, 160, 180, 200
		};
	int[] mMinSpaceWidths = new int[] {
			25, 30, 40, 50, 60, 70, 80, 90, 100
		};
	public static int[] mMargins = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 15, 20, 25, 30, 40, 50, 60, 80, 100, 130, 150, 200, 300
		};
	int[] mRoundedCornersMargins = new int[] {
			0, 5, 10, 15, 20, 30, 40, 50, 60, 70,80, 90, 100, 120, 140, 160
	};
	double[] mGammas = new double[] {
			0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.5, 1.9
		};
	int[] mScreenFullUpdateInterval = new int[] {
			0, 2, 3, 4, 5, 7, 10, 15, 20
		};
	int[] mScreenBlackPageDuration = new int[] {
			0, 100, 200, 300, 500, 700, 1000, 2000, 3000, 4000, 5000
	};
	int[] mScreenUpdateModes = new int[] {
			0, 1, 2//, 2, 3
		};
	int[] mScreenUpdateModesTitles = new int[] {
			R.string.options_screen_update_mode_quality, R.string.options_screen_update_mode_fast, R.string.options_screen_update_mode_fast2
		};
	int[] mScreenUpdateModesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mCoverPageSizes = new int[] {
			0, 1, 2//, 2, 3
		};
	int[] mCoverPageSizeTitles = new int[] {
			R.string.options_app_cover_page_size_small, R.string.options_app_cover_page_size_medium, R.string.options_app_cover_page_size_big
		};
	int[] mCoverPageSizeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mHinting = new int[] {
			0, 1, 2
		};
	int[] mHintingTitles = new int[] {
			R.string.options_font_hinting_disabled, R.string.options_font_hinting_bytecode, 
			R.string.options_font_hinting_auto
		};
	int[] mHintingTitlesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};
	int[] mShaping = new int[] {
			0, 1, 2
	};
	int[] mShapingTitles = new int[] {
			R.string.options_text_shaping_simple, R.string.options_text_shaping_light,
			R.string.options_text_shaping_full
	};
	int[] mShapingTitlesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};
	int[] mOrientations = new int[] {
			0, 1, 4, 5
		};
	int[] mOrientationsTitles = new int[] {
			R.string.options_page_orientation_0, R.string.options_page_orientation_90, 
			R.string.options_page_orientation_sensor, R.string.options_page_orientation_system
		};
	int[] mOrientationsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mOrientations_API9 = new int[] {
			0, 1, 2, 3, 4, 5
		};
	int[] mOrientationsTitles_API9 = new int[] {
			R.string.options_page_orientation_0, R.string.options_page_orientation_90, R.string.options_page_orientation_180, R.string.options_page_orientation_270
			,R.string.options_page_orientation_sensor,R.string.options_page_orientation_system
		};
	int[] mOrientationsAddInfos_API9 = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mToolbarPositions = new int[] {
			Settings.VIEWER_TOOLBAR_NONE, Settings.VIEWER_TOOLBAR_TOP, Settings.VIEWER_TOOLBAR_BOTTOM, Settings.VIEWER_TOOLBAR_LEFT, Settings.VIEWER_TOOLBAR_RIGHT, Settings.VIEWER_TOOLBAR_SHORT_SIDE, Settings.VIEWER_TOOLBAR_LONG_SIDE
		};
	int[] mToolbarPositionsTitles = new int[] {
			R.string.options_view_toolbar_position_none, R.string.options_view_toolbar_position_top, R.string.options_view_toolbar_position_bottom, R.string.options_view_toolbar_position_left, R.string.options_view_toolbar_position_right, R.string.options_view_toolbar_position_short_side, R.string.options_view_toolbar_position_long_side
		};
	int[] mToolbarPositionsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mToolbarApperance = new int[] {
			Settings.VIEWER_TOOLBAR_100, Settings.VIEWER_TOOLBAR_100_gray, Settings.VIEWER_TOOLBAR_100_inv,
			Settings.VIEWER_TOOLBAR_75, Settings.VIEWER_TOOLBAR_75_gray, Settings.VIEWER_TOOLBAR_75_inv,
			Settings.VIEWER_TOOLBAR_50, Settings.VIEWER_TOOLBAR_50_gray, Settings.VIEWER_TOOLBAR_50_inv
	};
	int[] mToolbarApperanceTitles = new int[] {
			R.string.options_view_toolbar_appear_100, R.string.options_view_toolbar_appear_100_gray, R.string.options_view_toolbar_appear_100_inv,
			R.string.options_view_toolbar_appear_75, R.string.options_view_toolbar_appear_75_gray, R.string.options_view_toolbar_appear_75_inv,
			R.string.options_view_toolbar_appear_50, R.string.options_view_toolbar_appear_50_gray, R.string.options_view_toolbar_appear_50_inv
	};

	int[] mToolbarApperanceAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	
	int[] mStatusPositions = new int[] {
			Settings.VIEWER_STATUS_NONE, 
			//Settings.VIEWER_STATUS_TOP, Settings.VIEWER_STATUS_BOTTOM,
			Settings.VIEWER_STATUS_PAGE,
			Settings.VIEWER_STATUS_PAGE_2LINES
		};
	int[] mStatusPositionsTitles = new int[] {
			R.string.options_page_show_titlebar_hidden, 
			//R.string.options_page_show_titlebar_top, R.string.options_page_show_titlebar_bottom,
			R.string.options_page_show_titlebar_page_header,
			R.string.options_page_show_titlebar_page_header_2lines,
		};

	int[] mStatusPositionsAddInfos = new int[] {
		R.string.option_add_info_empty_text,
		R.string.option_add_info_empty_text,
		R.string.option_add_info_empty_text
	};
	
	int[] mImageScalingModes = new int[] {
			0, 1, 2
		};
	int[] mImageScalingModesTitles = new int[] {
			R.string.options_format_image_scaling_mode_disabled, R.string.options_format_image_scaling_mode_integer_factor, R.string.options_format_image_scaling_mode_arbitrary
		};

	int[] mImageScalingModesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mImageScalingFactors = new int[] {
			0, 1, 2, 3
		};
	int[] mImageScalingFactorsTitles = new int[] {
			R.string.options_format_image_scaling_scale_auto, R.string.options_format_image_scaling_scale_1, R.string.options_format_image_scaling_scale_2, R.string.options_format_image_scaling_scale_3
		};

	int[] mImageScalingFactorsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mFlickBrightness = new int[] {
			0, 1, 2
		};
	int[] mFlickBrightnessTitles = new int[] {
			R.string.options_controls_flick_brightness_none, R.string.options_controls_flick_brightness_left, R.string.options_controls_flick_brightness_right
		};
	int[] mFlickBrightnessAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mBacklightTimeout = new int[] {
			0, 2, 3, 4, 5, 6
		};
	int[] mBacklightTimeoutTitles = new int[] {
			R.string.options_app_backlight_timeout_0, R.string.options_app_backlight_timeout_2, R.string.options_app_backlight_timeout_3, R.string.options_app_backlight_timeout_4, R.string.options_app_backlight_timeout_5, R.string.options_app_backlight_timeout_6
		};
	int[] mTapSecondaryActionType = new int[] {
			TAP_ACTION_TYPE_LONGPRESS, TAP_ACTION_TYPE_DOUBLE
		};
	int[] mTapSecondaryActionTypeTitles = new int[] {
			R.string.options_controls_tap_type_long, R.string.options_controls_tap_type_double
		};
	int[] mTapSecondaryActionTypeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mAnimation = new int[] {
			ReaderView.PAGE_ANIMATION_NONE, ReaderView.PAGE_ANIMATION_SLIDE, ReaderView.PAGE_ANIMATION_SLIDE2, 
			ReaderView.PAGE_ANIMATION_PAPER
		};
	int[] mAnimationTitles = new int[] {
			R.string.options_page_animation_none, R.string.options_page_animation_slide, R.string.options_page_animation_slide_2_pages,
			R.string.options_page_animation_paperbook
		};
	int[] mAnimationAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};
	int[] mHighlightMode = new int[] {
			0, 1, 2
		};
	int[] mHighlightModeTitles = new int[] {
			R.string.options_view_bookmarks_highlight_none, R.string.options_view_bookmarks_highlight_solid,
			R.string.options_view_bookmarks_highlight_underline
		};
	int[] mHighlightModeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};
	int[] mSelectionAction = new int[] {
			ReaderView.SELECTION_ACTION_TOOLBAR,
			ReaderView.SELECTION_ACTION_COPY, 
			ReaderView.SELECTION_ACTION_DICTIONARY,
			ReaderView.SELECTION_ACTION_BOOKMARK,
			ReaderView.SELECTION_ACTION_FIND,
			ReaderView.SELECTION_ACTION_DICTIONARY_1,
			ReaderView.SELECTION_ACTION_DICTIONARY_2,
			ReaderView.SELECTION_ACTION_SEARCH_WEB,
			ReaderView.SELECTION_ACTION_SEND_TO,
			ReaderView.SELECTION_ACTION_USER_DIC,
			ReaderView.SELECTION_ACTION_CITATION,
			ReaderView.SELECTION_ACTION_DICTIONARY_LIST
	};
	int[] mSelectionActionTitles = new int[] {
			R.string.options_selection_action_toolbar, 
			R.string.options_selection_action_copy, 
			R.string.options_selection_action_dictionary, 
			R.string.options_selection_action_bookmark, 
			R.string.mi_search,
			R.string.options_selection_action_dictionary_1,
			R.string.options_selection_action_dictionary_2,
			R.string.mi_search_web,
			R.string.options_selection_action_mail,
			R.string.mi_user_dic,
			R.string.mi_citation,
			R.string.options_selection_action_dictionary_list,
		};
	int[] mSelectionActionAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};
	int[] mMultiSelectionAction = new int[] {
			ReaderView.SELECTION_ACTION_TOOLBAR,
			ReaderView.SELECTION_ACTION_COPY, 
			ReaderView.SELECTION_ACTION_DICTIONARY,
			ReaderView.SELECTION_ACTION_BOOKMARK,
			ReaderView.SELECTION_ACTION_FIND,
			ReaderView.SELECTION_ACTION_DICTIONARY_1,
			ReaderView.SELECTION_ACTION_DICTIONARY_2,
			ReaderView.SELECTION_ACTION_SEARCH_WEB,
			ReaderView.SELECTION_ACTION_SEND_TO,
			ReaderView.SELECTION_ACTION_USER_DIC,
			ReaderView.SELECTION_ACTION_CITATION,
			ReaderView.SELECTION_ACTION_DICTIONARY_LIST
		};
	int[] mMultiSelectionActionTitles = new int[] {
			R.string.options_selection_action_toolbar, 
			R.string.options_selection_action_copy, 
			R.string.options_selection_action_dictionary, 
			R.string.options_selection_action_bookmark,
			R.string.mi_search,
			R.string.options_selection_action_dictionary_1,
			R.string.options_selection_action_dictionary_2,
			R.string.mi_search_web,
			R.string.options_selection_action_mail,
			R.string.mi_user_dic,
			R.string.mi_citation,
			R.string.options_selection_action_dictionary_list,
		};
	int[] mMultiSelectionActionAddInfo = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};
	int[] mAntialias = new int[] {
			0, 1, 2
		};
	int[] mAntialiasTitles = new int[] {
			R.string.options_font_antialias_off, R.string.options_font_antialias_on_for_big, R.string.options_font_antialias_on_for_all
		};
	int[] mAntialiasAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mLandscapePages = new int[] {
			1, 2
		};
	int[] mLandscapePagesTitles = new int[] {
			R.string.options_page_landscape_pages_one, R.string.options_page_landscape_pages_two
		};
	int[] mLandscapePagesAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mViewModes = new int[] {
			1, 0
		};
	int[] mViewModeAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mViewModeTitles = new int[] {
			R.string.options_view_mode_pages, R.string.options_view_mode_scroll
		};
	int[] mRoundedCornersMarginPos = new int[] {
			0, 1, 2
	};
	int[] mRoundedCornersMarginPosAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};
	int[] mRoundedCornersMarginPosTitles = new int[] {
			R.string.rounded_corners_margin_pos_0, R.string.rounded_corners_margin_pos_1,
			R.string.rounded_corners_margin_pos_2,
	};

	int[] mScreenMod = new int[] {
			0, 1, 2
	};
	int[] mScreenModAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};
	int[] mScreenModTitles = new int[] {
			R.string.screen_mod_0, R.string.screen_mod_1,
			R.string.screen_mod_2,
	};

	int[] mExtFullscreenMargin = new int[] {
			0, 1, 2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 80, 90, 100
	};
	int[] mExtFullscreenMarginAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mExtFullscreenMarginPosTitles = new int[] {
			R.string.ext_fullscreen_margin_0, R.string.ext_fullscreen_margin_1,
			R.string.ext_fullscreen_margin_3,
			-5, -10, -15, -20, -25, -30, -35, -40, -45, -50, -55, -60, -65, -70, -80, -90, -100
	};

	int[] mCloudSyncVariants = new int[] {
			Settings.CLOUD_SYNC_VARIANT_DISABLED, Settings.CLOUD_SYNC_VARIANT_FILESYSTEM, Settings.CLOUD_SYNC_VARIANT_YANDEX
	};
	int[] mCloudSyncVariantsTitles = new int[] {
			R.string.cloud_sync_variant1, R.string.cloud_sync_variant2, R.string.cloud_sync_variant3
	};
	int[] mCloudSyncVariantsAddInfos = new int[] {
			R.string.cloud_sync_variant1_v, R.string.cloud_sync_variant2_v, R.string.cloud_sync_variant3_v
	};

	int[] sortOrderLabels = {
			FileInfo.SortOrder.FILENAME.resourceId,
			FileInfo.SortOrder.FILENAME_DESC.resourceId,
			FileInfo.SortOrder.AUTHOR_TITLE.resourceId,
			FileInfo.SortOrder.AUTHOR_TITLE_DESC.resourceId,
			FileInfo.SortOrder.TITLE_AUTHOR.resourceId,
			FileInfo.SortOrder.TITLE_AUTHOR_DESC.resourceId,
			FileInfo.SortOrder.TIMESTAMP.resourceId,
			FileInfo.SortOrder.TIMESTAMP_DESC.resourceId,
	};
	String[] sortOrderValues = {
			FileInfo.SortOrder.FILENAME.name(),
			FileInfo.SortOrder.FILENAME_DESC.name(),
			FileInfo.SortOrder.AUTHOR_TITLE.name(),
			FileInfo.SortOrder.AUTHOR_TITLE_DESC.name(),
			FileInfo.SortOrder.TITLE_AUTHOR.name(),
			FileInfo.SortOrder.TITLE_AUTHOR_DESC.name(),
			FileInfo.SortOrder.TIMESTAMP.name(),
			FileInfo.SortOrder.TIMESTAMP_DESC.name(),
	};
	int[] sortOrderAddInfos = {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};

	int[] mScreenMargins = new int[] {
			0, 5, 10, 15, 20, 30, 40, 50, 60, 70, 80, 90, 100
	};

	int[] mBrowserAction = new int[] {
			0, 1, 2, 4
	};
	int[] mBrowserActionTitles = new int[] {
			R.string.browser_tap_option1, R.string.browser_tap_option2,
			R.string.browser_tap_option3, R.string.browser_tap_option4
	};
	int[] mBrowserActionAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};

	int[] mRenderingPresets = new int[] {
			Engine.BLOCK_RENDERING_FLAGS_LEGACY, Engine.BLOCK_RENDERING_FLAGS_FLAT,
			Engine.BLOCK_RENDERING_FLAGS_BOOK, Engine.BLOCK_RENDERING_FLAGS_WEB
	};
	int[] mRenderingPresetsTitles = new int[] {
			R.string.options_rendering_preset_legacy, R.string.options_rendering_preset_flat, R.string.options_rendering_preset_book, R.string.options_rendering_preset_web
	};
	int[] mRenderingPresetsAddInfos = new int[] {
			R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text, R.string.option_add_info_empty_text
	};
	int[] mDOMVersionPresets = new int[] {
			0, Engine.DOM_VERSION_CURRENT
	};
	int[] mDOMVersionPresetTitles = new int[] {
			R.string.options_requested_dom_level_legacy, R.string.options_requested_dom_level_newest
	};

	int[] mSecGroupCommon = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8
	};

	int[] mSecGroupCommonTitles = new int[] {
		R.string.folder_name_books_by_author,
		R.string.folder_name_books_by_series,
		R.string.folder_name_books_by_genre,
		R.string.folder_name_books_by_bookdate,
		R.string.folder_name_books_by_docdate,
		R.string.folder_name_books_by_publyear,
		R.string.folder_name_books_by_filedate,
		R.string.folder_name_books_by_rating,
		R.string.folder_name_books_by_state
	};

	int[] mSecGroupCommonTitlesAddInfos = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mSecGroupCommon2 = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8, 9
	};

	int[] mSecGroupCommonTitles2 = new int[] {
			R.string.same_as_common,
			R.string.folder_name_books_by_author,
			R.string.folder_name_books_by_series,
			R.string.folder_name_books_by_genre,
			R.string.folder_name_books_by_bookdate,
			R.string.folder_name_books_by_docdate,
			R.string.folder_name_books_by_publyear,
			R.string.folder_name_books_by_filedate,
			R.string.folder_name_books_by_rating,
			R.string.folder_name_books_by_state
	};

	int[] mSecGroupCommonTitlesAddInfos2 = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	int[] mSecGroupCommon3 = new int[] {
			0, 1, 2, 3, 4, 5, 6
	};

	int[] mSecGroupCommonTitles3 = new int[] {
			R.string.same_as_common,
			R.string.folder_name_books_by_author,
			R.string.folder_name_books_by_series,
			R.string.folder_name_books_by_genre,
			R.string.folder_name_books_by_date,
			R.string.folder_name_books_by_rating,
			R.string.folder_name_books_by_state
	};

	int[] mSecGroupCommonTitlesAddInfos3 = new int[] {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text
	};

	ViewGroup mContentView;
	TabHost mTabs;
	LayoutInflater mInflater;
	Properties mProperties;
	ArrayList<String> mFilteredProps;
	Properties mOldProperties;
	OptionsListView mOptionsStyles;
	OptionsListView mOptionsCSS;
	OptionsListView mOptionsPage;
	OptionsListView mOptionsApplication;
	OptionsListView mOptionsControls;
	OptionsListView mOptionsBrowser;
	// Disable options
	OptionBase mHyphDictOption;
	OptionBase mEmbedFontsOptions;
	OptionBase mFootNotesOption;
	OptionBase mEnableHyphOption;

	public final static int OPTION_VIEW_TYPE_NORMAL = 0;
	public final static int OPTION_VIEW_TYPE_BOOLEAN = 1;
	public final static int OPTION_VIEW_TYPE_COLOR = 2;
	public final static int OPTION_VIEW_TYPE_SUBMENU = 3;
	//public final static int OPTION_VIEW_TYPE_COUNT = 3;

	public BaseActivity getActivity() { return mActivity; }
	public Properties getProperties() { return mProperties; }
	public LayoutInflater getInflater() { return mInflater; }

	public abstract static class OptionBase {
		protected View myView;
		Properties mProperties;
		BaseActivity mActivity;
		OptionOwner mOwner;
		LayoutInflater mInflater;
		public boolean enabled = true;
		public String label;
		public String property;
		public String defaultValue;
		public String addInfo;
		public ArrayList<String> quickFilters = new ArrayList<String>();
		public HashMap<String, String> usefulLinks = new HashMap<String, String>();
		public boolean lastFiltered = false;
		public String lastFilteredValue = "";
		public int drawableAttrId = R.attr.cr3_option_other_drawable;
		public int fallbackIconId = R.drawable.cr3_option_other;
		public int fallbackIconId2 = R.drawable.cr3_option_other;
		public OptionsListView optionsListView;
		protected Runnable onChangeHandler;
		public OptionBase( OptionOwner owner, String label, String property, String addInfo, String filter) {
			this.mOwner = owner;
			this.mActivity = owner.getActivity();
			this.mInflater = owner.getInflater();
			this.mProperties = owner.getProperties();
			this.label = label;
			this.property = property;
			this.addInfo = addInfo;
			this.setFilteredMark(filter);
		}
		public boolean setFilteredMark(String filter) {
			this.lastFilteredValue = filter.toLowerCase();
			if (filter.trim().equals("")) this.lastFiltered = true;
			else {
				this.lastFiltered = this.label.toLowerCase().contains(filter.toLowerCase());
				this.lastFiltered = this.lastFiltered || this.property.toLowerCase().contains(filter.toLowerCase());
				this.lastFiltered = this.lastFiltered || this.addInfo.toLowerCase().contains(filter.toLowerCase());
			}
			return this.lastFiltered;
		}

		public boolean updateFilteredMark(String val) {
			if (this.lastFilteredValue.trim().equals("")) this.lastFiltered = true;
			else {
				this.lastFiltered = this.lastFiltered || val.toLowerCase().contains(this.lastFilteredValue);
			}
			return this.lastFiltered;
		}

		public boolean updateFilteredMark(String val1, String val2, String val3) {
			if (this.lastFilteredValue.trim().equals("")) this.lastFiltered = true;
			else {
				this.lastFiltered = this.lastFiltered || val1.toLowerCase().contains(this.lastFilteredValue);
				this.lastFiltered = this.lastFiltered || val2.toLowerCase().contains(this.lastFilteredValue);
				this.lastFiltered = this.lastFiltered || val3.toLowerCase().contains(this.lastFilteredValue);
			}
			return this.lastFiltered;
		}

		public boolean updateFilteredMark(boolean val) {
			if (this.lastFilteredValue.trim().equals("")) this.lastFiltered = true;
			else {
				this.lastFiltered = this.lastFiltered || val;
			}
			return this.lastFiltered;
		}

		public OptionBase setIconId(int id) {
			drawableAttrId = 0;
			fallbackIconId = id;
			return this;
		}
		public OptionBase setIcon2Id(int id) {
			fallbackIconId2 = id;
			return this;
		}
		public OptionBase setIconIdByAttr(int drawableAttrId, int fallbackIconId) {
			this.drawableAttrId = drawableAttrId;
			this.fallbackIconId = fallbackIconId;
			return this;
		}
		public OptionBase noIcon() {
			drawableAttrId = 0;
			fallbackIconId = 0;
			fallbackIconId2 = 0;
			return this;
		}
		public OptionBase setDefaultValue(String value) {
			this.defaultValue = value;
			if ( mProperties.getProperty(property)==null )
				mProperties.setProperty(property, value);
			return this;
		}
		public OptionBase setOnChangeHandler( Runnable handler ) {
			onChangeHandler = handler;
			return this;
		}

		public int getItemViewType() {
			return OPTION_VIEW_TYPE_NORMAL;
		}

		protected void refreshItem()
		{
			getView(null, null).invalidate();
			//if ( optionsListView!=null )
			//	optionsListView.refresh();
		}

		protected void refreshList()
		{
			getView(null, null).invalidate();
			if ( optionsListView!=null )
				optionsListView.refresh();
		}

		protected void setupIconView(ImageView icon) {
			if (null == icon)
				return;
			int resId = 0;
			if (showIcons) {
				if (drawableAttrId != 0) {
					resId = Utils.resolveResourceIdByAttr(mActivity, drawableAttrId, fallbackIconId);
				} else if (fallbackIconId != 0) {
					resId = fallbackIconId;
				}
			}
			if (resId != 0) {
				icon.setImageResource(resId);
				icon.setVisibility(View.VISIBLE);
				mActivity.tintViewIcons(icon,true);
			} else {
				icon.setImageResource(0);
				icon.setVisibility(View.INVISIBLE);
			}
		}

		protected void setup2IconView(ImageView icon1, ImageView icon2) {
			if (null != icon1) {
				int resId = 0;
				if (showIcons) {
					if (drawableAttrId != 0) {
						resId = Utils.resolveResourceIdByAttr(mActivity, drawableAttrId, fallbackIconId);
					} else if (fallbackIconId != 0) {
						resId = fallbackIconId;
					}
				}
				if (resId != 0) {
					icon1.setImageResource(resId);
					icon1.setVisibility(View.VISIBLE);
					mActivity.tintViewIcons(icon1,true);
				} else {
					icon1.setImageResource(0);
					icon1.setVisibility(View.INVISIBLE);
				}
			}
			if (null != icon2) {
				int resId = 0;
				if (showIcons) {
					resId = fallbackIconId2;
				}
				if (resId != 0) {
					icon2.setImageResource(resId);
					mActivity.tintViewIcons(icon2,true);
					icon2.setVisibility(View.VISIBLE);
				} else {
					icon2.setImageResource(0);
					icon2.setVisibility(View.INVISIBLE);
				}
			}
		}

		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				view = mInflater.inflate(R.layout.option_item, null);
				if (view != null) {
					TextView label = (TextView) view.findViewById(R.id.option_label);
					if (label != null)
						if (mOwner instanceof OptionsDialog) {
							if (!((((OptionsDialog)mOwner).mFilteredProps).contains(property))) {
								if (!StrUtils.isEmptyStr(property))
									label.setTypeface(null, Typeface.ITALIC);
							}
						}
				}
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			TextView valueView = (TextView)view.findViewById(R.id.option_value);
			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();
			valueView.setTextColor(colorIcon);
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							//mActivity.showToast("to come...");
							//Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
							//toast.show();
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
							//ToastView.showToast(
							//	view1, addInfo, Toast.LENGTH_LONG);
							//	Toast.LENGTH_LONG, 20);
						}
					});
			}
			labelView.setText(label);
			labelView.setEnabled(enabled);
			if (valueView != null) {
				String valueLabel = getValueLabel();
				if (valueLabel != null && valueLabel.length() > 0) {
					valueView.setText(valueLabel);
					valueView.setVisibility(View.VISIBLE);
				} else {
					valueView.setText("");
					valueView.setVisibility(View.INVISIBLE);
				}
				valueView.setEnabled(enabled);
			}
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
			return view;
		}

		public String getValueLabel() { return mProperties.getProperty(property); }
		public void onSelect() {
			if (!enabled)
				return;
			refreshList();
		}
	}
	
	class ColorOption extends OptionBase {
		final int defColor;
		public ColorOption( OptionOwner owner, String label, String property, int defColor, String addInfo, String filter) {
			super(owner, label, property, addInfo, filter);
			String[] colorNames = activity.getResources().getStringArray(R.array.colorNames);
			for(int i=0; i<colorNames.length; i++)
				this.updateFilteredMark(colorNames[i]);
			this.defColor = defColor;
		}
		public String getValueLabel() { return mProperties.getProperty(property); }
		public void onSelect()
		{
			if (!enabled)
				return;
			ColorPickerDialog dlg = new ColorPickerDialog(mActivity, new OnColorChangedListener() {
				public void colorChanged(int color) {
					mProperties.setColor(property, color);
					if ( property.equals(PROP_BACKGROUND_COLOR) ) {
						String texture = mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE, Engine.NO_TEXTURE.id);
						if ( texture!=null && !texture.equals(Engine.NO_TEXTURE.id) ) {
							// reset background image
							mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE, Engine.NO_TEXTURE.id);
							// TODO: show notification?
						}
					}
					refreshList();
				}
			}, mProperties.getColor(property, defColor), label);
			dlg.show();
		}
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_COLOR;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				view = mInflater.inflate(R.layout.option_item_color, null);
				if (view != null) {
					TextView label = (TextView) view.findViewById(R.id.option_label);
					if (label != null)
						if (mOwner instanceof OptionsDialog) {
							if (!((((OptionsDialog)mOwner).mFilteredProps).contains(property))) {
								if (!StrUtils.isEmptyStr(property))
									label.setTypeface(null, Typeface.ITALIC);
							}
						}
				}
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			ImageView valueView = (ImageView)view.findViewById(R.id.option_value_color);
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
						//	Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
						//	toast.show();
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			labelView.setText(label);
			labelView.setEnabled(enabled);
			int cl = mProperties.getColor(property, defColor);
			valueView.setBackgroundColor(cl);
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
			view.setEnabled(enabled);
			return view;
		}
	}
	
	private static boolean showIcons = true;
	private static boolean isTextFormat = false;
	private static boolean isEpubFormat = false;
	private static boolean isHtmlFormat = false;
	private Mode mode;
	
	class IconsBoolOption extends BoolOption {
		public IconsBoolOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
		}
		public void onSelect() {
			if (!enabled)
				return;
			mProperties.setProperty(property, "1".equals(mProperties.getProperty(property)) ? "0" : "1");
			showIcons = mProperties.getBool(property, true);
			mOptionsStyles.refresh();
			mOptionsCSS.refresh();
			mOptionsPage.refresh();
			mOptionsApplication.refresh();
			mOptionsControls.refresh();
		}
	}
	class BoolOption extends OptionBase {
		private boolean inverse = false;
		public BoolOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
		}
		private boolean getValueBoolean() { return "1".equals(mProperties.getProperty(property)) ^ inverse; }
		public String getValueLabel() { return getValueBoolean()  ? getString(R.string.options_value_on) : getString(R.string.options_value_off); }
		public void onSelect() {
			if (!enabled)
				return;
			mProperties.setProperty(property, "1".equals(mProperties.getProperty(property)) ? "0" : "1");
			if (null != onChangeHandler)
				onChangeHandler.run();
			refreshList();
		}
		public BoolOption setInverse() { inverse = true; return this; }
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_BOOLEAN;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				view = mInflater.inflate(R.layout.option_item_boolean, null);
				if (view != null) {
					TextView label = (TextView) view.findViewById(R.id.option_label);
					if (label != null)
						if (mOwner instanceof OptionsDialog) {
							if (!((((OptionsDialog)mOwner).mFilteredProps).contains(property))) {
								if (!StrUtils.isEmptyStr(property))
									label.setTypeface(null, Typeface.ITALIC);
							}
						}
				}
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			CheckBox valueView = (CheckBox)view.findViewById(R.id.option_value_cb);
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							//Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
							//toast.show();
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}

//			valueView.setFocusable(false);
//			valueView.setClickable(false);
			labelView.setText(label);
			labelView.setEnabled(enabled);
			valueView.setChecked(getValueBoolean());
			valueView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean checked) {
//						mProperties.setBool(property, checked);
//						refreshList();
					}
				});
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
//			view.setClickable(true);
//			view.setFocusable(true);
			valueView.setEnabled(enabled);
			return view;
		}
	}

	class SaveOptionsToGDOption extends OptionBase {
		private boolean inverse = false;
		public SaveOptionsToGDOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
		}
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_NORMAL;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				view = mInflater.inflate(R.layout.option_item, null);
				if (view != null) {
					TextView label = (TextView) view.findViewById(R.id.option_label);
					if (label != null)
						if (mOwner instanceof OptionsDialog) {
							if (!((((OptionsDialog)mOwner).mFilteredProps).contains(property))) {
								if (!StrUtils.isEmptyStr(property))
									label.setTypeface(null, Typeface.ITALIC);
							}
						}
				}
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			TextView valueView = (TextView)view.findViewById(R.id.option_value);
			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();
			valueView.setTextColor(colorIcon);
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							//Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
							//toast.show();
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			labelView.setText(label);
			labelView.setEnabled(enabled);
			valueView.setText(property);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					CloudSync.saveSettingsFiles(((CoolReader)mActivity),false);
					return;
				}
			});
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
			return view;
		}
	}

	class LoadOptionsFromGDOption extends OptionBase {
		private boolean inverse = false;
		public LoadOptionsFromGDOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
		}
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_NORMAL;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				view = mInflater.inflate(R.layout.option_item, null);
				if (view != null) {
					TextView label = (TextView) view.findViewById(R.id.option_label);
					if (label != null)
						if (mOwner instanceof OptionsDialog) {
							if (!((((OptionsDialog)mOwner).mFilteredProps).contains(property))) {
								if (!StrUtils.isEmptyStr(property))
									label.setTypeface(null, Typeface.ITALIC);
							}
						}
				}
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			TextView valueView = (TextView)view.findViewById(R.id.option_value);
			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();
			valueView.setTextColor(colorIcon);
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							//Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
							//toast.show();
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			labelView.setText(label);
			labelView.setEnabled(enabled);
			valueView.setText(property);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					CloudSync.loadSettingsFiles(((CoolReader)mActivity),false);
					return;
				}
			});
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
			return view;
		}
	}

	class InfoOption extends OptionBase {
		private boolean inverse = false;
		public InfoOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
		}
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_NORMAL;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				view = mInflater.inflate(R.layout.option_item, null);
				if (view != null) {
					TextView label = (TextView) view.findViewById(R.id.option_label);
					if (label != null)
						if (mOwner instanceof OptionsDialog) {
							if (!((((OptionsDialog)mOwner).mFilteredProps).contains(property))) {
								if (!StrUtils.isEmptyStr(property))
									label.setTypeface(null, Typeface.ITALIC);
							}
						}
				}
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			TextView valueView = (TextView)view.findViewById(R.id.option_value);
			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();
			valueView.setTextColor(colorIcon);
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			labelView.setText(label);
			labelView.setEnabled(enabled);
			String currValue = mProperties.getProperty(property);
			valueView.setText(currValue);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final View view1 = view;
					mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
					return;
				}
			});
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
			return view;
		}
	}

	int YANDEX_SETTINGS_OPTION = 1;
	int DROPBOX_SETTINGS_OPTION = 2;

	class CloudSettingsOption extends OptionBase {
		private boolean inverse = false;
		private int whichCloud = 0;

		public CloudSettingsOption( OptionOwner owner, String label, String property, String addInfo, String filter,
									int whichCloud) {
			super(owner, label, property, addInfo, filter);
			this.whichCloud = whichCloud;
		}
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_NORMAL;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				view = mInflater.inflate(R.layout.option_item, null);
				if (view != null) {
					TextView label = (TextView) view.findViewById(R.id.option_label);
					if (label != null)
						if (mOwner instanceof OptionsDialog) {
							if (!((((OptionsDialog)mOwner).mFilteredProps).contains(property))) {
								if (!StrUtils.isEmptyStr(property))
									label.setTypeface(null, Typeface.ITALIC);
							}
						}
				}
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			TextView valueView = (TextView)view.findViewById(R.id.option_value);
			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();
			valueView.setTextColor(colorIcon);
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			labelView.setText(label);
			labelView.setEnabled(enabled);
			valueView.setText(property);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (whichCloud == YANDEX_SETTINGS_OPTION) {
						((CoolReader) mActivity).yndInputTokenDialog = new YNDInputTokenDialog(((CoolReader) mActivity));
						((CoolReader) mActivity).yndInputTokenDialog.show();
					}
					if (whichCloud == DROPBOX_SETTINGS_OPTION) {
						((CoolReader) mActivity).dbxInputTokenDialog = new DBXInputTokenDialog(((CoolReader) mActivity));
						((CoolReader) mActivity).dbxInputTokenDialog.show();
					}
					return;
				}
			});
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
			return view;
		}
	}

	static public void saveColor( Properties mProperties, boolean night )
	{
		if ( night ) {
			mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE_NIGHT, mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE, "(NONE)"));
			mProperties.setColor(PROP_BACKGROUND_COLOR_NIGHT, mProperties.getColor(PROP_BACKGROUND_COLOR, 0x000000));
			mProperties.setColor(PROP_FONT_COLOR_NIGHT, mProperties.getColor(PROP_FONT_COLOR, 0xFFFFFF));
			mProperties.setColor(PROP_STATUS_FONT_COLOR_NIGHT, mProperties.getColor(PROP_STATUS_FONT_COLOR, 0xFFFFFF));
			mProperties.setInt(PROP_APP_SCREEN_BACKLIGHT_NIGHT, mProperties.getInt(PROP_APP_SCREEN_BACKLIGHT, -1));
			mProperties.setProperty(PROP_FONT_GAMMA_NIGHT, mProperties.getProperty(PROP_FONT_GAMMA, "1.0"));
			mProperties.setProperty(PROP_APP_THEME_NIGHT, mProperties.getProperty(PROP_APP_THEME, "BLACK"));
			mProperties.setInt(PROP_APP_HIGHLIGHT_BOOKMARKS_NIGHT, mProperties.getInt(PROP_APP_HIGHLIGHT_BOOKMARKS, 1));
			mProperties.setColor(PROP_HIGHLIGHT_SELECTION_COLOR_NIGHT, mProperties.getColor(PROP_HIGHLIGHT_SELECTION_COLOR, 0xCCCCCC));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_NIGHT, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, 0xFFFF40));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_NIGHT, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, 0xFF8000));
		} else {
			mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE_DAY, mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE, "(NONE)"));
			mProperties.setColor(PROP_BACKGROUND_COLOR_DAY, mProperties.getColor(PROP_BACKGROUND_COLOR, 0xFFFFFF));
			mProperties.setColor(PROP_FONT_COLOR_DAY, mProperties.getColor(PROP_FONT_COLOR, 0x000000));
			mProperties.setColor(PROP_STATUS_FONT_COLOR_DAY, mProperties.getColor(PROP_STATUS_FONT_COLOR, 0x000000));
			mProperties.setInt(PROP_APP_SCREEN_BACKLIGHT_DAY, mProperties.getInt(PROP_APP_SCREEN_BACKLIGHT, -1));
			mProperties.setProperty(PROP_FONT_GAMMA_DAY, mProperties.getProperty(PROP_FONT_GAMMA, "1.0"));
			mProperties.setProperty(PROP_APP_THEME_DAY, mProperties.getProperty(PROP_APP_THEME, "WHITE"));
			mProperties.setInt(PROP_APP_HIGHLIGHT_BOOKMARKS_DAY, mProperties.getInt(PROP_APP_HIGHLIGHT_BOOKMARKS, 1));
			mProperties.setColor(PROP_HIGHLIGHT_SELECTION_COLOR_DAY, mProperties.getColor(PROP_HIGHLIGHT_SELECTION_COLOR, 0xCCCCCC));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_DAY, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, 0xFFFF40));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_DAY, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, 0xFF8000));
		}
		for (String code : styleCodes) {
			String styleName = "styles." + code + ".color";
			String v = mProperties.getProperty(styleName); 
			if (v != null) {
				if ( night )
					mProperties.setProperty(styleName + ".night", v);
				else
					mProperties.setProperty(styleName + ".day", v);
			}
		}
	}

	static public void restoreColor( Properties mProperties,  boolean night )
	{
		if ( night ) {
			mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE, mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE_NIGHT, "(NONE)"));
			mProperties.setColor(PROP_BACKGROUND_COLOR, mProperties.getColor(PROP_BACKGROUND_COLOR_NIGHT, 0x000000));
			mProperties.setColor(PROP_FONT_COLOR, mProperties.getColor(PROP_FONT_COLOR_NIGHT, 0xFFFFFF));
			mProperties.setColor(PROP_STATUS_FONT_COLOR, mProperties.getColor(PROP_STATUS_FONT_COLOR_NIGHT, 0xFFFFFF));
			mProperties.setInt(PROP_APP_SCREEN_BACKLIGHT, mProperties.getInt(PROP_APP_SCREEN_BACKLIGHT_NIGHT, 70));
			mProperties.setProperty(PROP_FONT_GAMMA, mProperties.getProperty(PROP_FONT_GAMMA_NIGHT, "1.0"));
			mProperties.setProperty(PROP_APP_THEME, mProperties.getProperty(PROP_APP_THEME_NIGHT, "BLACK"));
			mProperties.setInt(PROP_APP_HIGHLIGHT_BOOKMARKS, mProperties.getInt(PROP_APP_HIGHLIGHT_BOOKMARKS_NIGHT, 1));
			mProperties.setColor(PROP_HIGHLIGHT_SELECTION_COLOR, mProperties.getColor(PROP_HIGHLIGHT_SELECTION_COLOR_NIGHT, 0xCCCCCC));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_NIGHT, 0xFFFF40));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_NIGHT, 0xFF8000));
		} else {
			mProperties.setProperty(PROP_PAGE_BACKGROUND_IMAGE, mProperties.getProperty(PROP_PAGE_BACKGROUND_IMAGE_DAY, "(NONE)"));
			mProperties.setColor(PROP_BACKGROUND_COLOR, mProperties.getColor(PROP_BACKGROUND_COLOR_DAY, 0xFFFFFF));
			mProperties.setColor(PROP_FONT_COLOR, mProperties.getColor(PROP_FONT_COLOR_DAY, 0x000000));
			mProperties.setColor(PROP_STATUS_FONT_COLOR, mProperties.getColor(PROP_STATUS_FONT_COLOR_DAY, 0x000000));
			mProperties.setInt(PROP_APP_SCREEN_BACKLIGHT, mProperties.getInt(PROP_APP_SCREEN_BACKLIGHT_DAY, 80));
			mProperties.setProperty(PROP_FONT_GAMMA, mProperties.getProperty(PROP_FONT_GAMMA_DAY, "1.0"));
			mProperties.setProperty(PROP_APP_THEME, mProperties.getProperty(PROP_APP_THEME_DAY, "WHITE"));
			mProperties.setInt(PROP_APP_HIGHLIGHT_BOOKMARKS, mProperties.getInt(PROP_APP_HIGHLIGHT_BOOKMARKS_DAY, 1));
			mProperties.setColor(PROP_HIGHLIGHT_SELECTION_COLOR, mProperties.getColor(PROP_HIGHLIGHT_SELECTION_COLOR_DAY, 0xCCCCCC));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT_DAY, 0xFFFF40));
			mProperties.setColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION, mProperties.getColor(PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION_DAY, 0xFF8000));
		}
		for (String code : styleCodes) {
			String styleName = "styles." + code + ".color";
			String pname = night ? styleName + ".night" : styleName + ".day";
			String v = mProperties.getProperty(pname);
			if (v != null)
				mProperties.setProperty(styleName, mProperties.getProperty(pname));
		}
	}

	static public void toggleDayNightMode( Properties mProperties ) {
		boolean oldMode = mProperties.getBool(PROP_NIGHT_MODE, false);
		saveColor(mProperties, oldMode);
		boolean newMode = !oldMode;
		restoreColor(mProperties, newMode);
		mProperties.setBool(PROP_NIGHT_MODE, newMode);
	}

	class NightModeOption extends BoolOption {
		public NightModeOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
			this.updateFilteredMark("night");
		}
		public void onSelect() {
			if (!enabled)
				return;
			toggleDayNightMode(mProperties);
			refreshList();
		}
	}
	
	class LangOption extends ListOption {
		public LangOption(OptionOwner owner, String filter) {
			super(owner, getString(R.string.options_app_locale), PROP_APP_LOCALE, getString(R.string.options_app_locale_add_info), filter);
			for (Lang lang : Lang.values()) {
				Locale l =  lang.getLocale();
				String s = "";
				if (l!=null) s = lang.getLocale().getDisplayName();
				add(lang.code, getString(lang.nameId), s);
			}
			if ( mProperties.getProperty(property)==null )
				mProperties.setProperty(property, Lang.DEFAULT.code);
			this.updateFilteredMark(Lang.DEFAULT.code);
		}
	}

	class GeoOption extends ListOption {
		public GeoOption(OptionOwner owner, String filter) {
			super(owner, getString(R.string.options_app_geo), PROP_APP_GEO,
					getString(R.string.options_app_geo_add_info), filter);
			add("1", getString(R.string.options_app_geo_1),"");
			add("2", getString(R.string.options_app_geo_2),getString(R.string.options_app_geo_2_add_info));
			add("3", getString(R.string.options_app_geo_3),getString(R.string.options_app_geo_3_add_info));
			add("4", getString(R.string.options_app_geo_4),"");
			if ( mProperties.getProperty(property)==null )
				mProperties.setProperty(property, "1");
		}

		@Override
		public void onClick( Three item ) {
		    super.onClick(item);
			CoolReader cr = (CoolReader)mActivity;
			if ((item.value.equals("2"))||(item.value.equals("3"))||(item.value.equals("4"))) {
				if (cr.geoLastData != null) {
					cr.geoLastData.gps.stop();
					cr.geoLastData.netw.stop();
					if ((item.value.equals("2")) || (item.value.equals("4")))
						cr.geoLastData.loadMetroStations(cr);
					if ((item.value.equals("3")) || (item.value.equals("4")))
						cr.geoLastData.loadTransportStops(cr);
					cr.geoLastData.gps.start(cr.geoLastData.geoListener);
					cr.geoLastData.netw.start(cr.geoLastData.netwListener);
				}
				((CoolReader) mActivity).checkLocationPermission();
			} else {
				if (cr.geoLastData != null) {
					cr.geoLastData.gps.stop();
					cr.geoLastData.netw.stop();
					if (cr.geoLastData.metroLocations == null)
						cr.geoLastData.metroLocations = new ArrayList<MetroLocation>();
					cr.geoLastData.metroLocations.clear();
					if (cr.geoLastData.transportStops == null)
						cr.geoLastData.transportStops = new ArrayList<TransportStop>();
					cr.geoLastData.transportStops.clear();
				}
			}
		}
	}

	class ActionOption extends ListOption {
		public ActionOption( OptionOwner owner, String label, String property, boolean isTap, boolean allowRepeat,
							 String addInfo, String filter) {
			super(owner, label, property, addInfo, filter);
			ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;
			for ( ReaderAction a : actions )
				if ( !isTap || a.mayAssignOnTap() )
					add(a.id, getString(a.nameId), getString(a.addInfoR));
			if ( allowRepeat )
				add(ReaderAction.REPEAT.id, getString(ReaderAction.REPEAT.nameId), getString(ReaderAction.REPEAT.addInfoR));
			if ( mProperties.getProperty(property)==null )
				mProperties.setProperty(property, ReaderAction.NONE.id);
		}

		protected int getItemLayoutId(int position, final Three item) {
			return R.layout.option_value;
		}

		protected void updateItemContents( final View layout, final Three item, final ListView listView, final int position ) {
			super.updateItemContents(layout, item, listView, position);
			ImageView img = (ImageView) layout.findViewById(R.id.option_value_icon);
			ImageView imgAddInfo = (ImageView) layout.findViewById(R.id.btn_option_add_info);
			ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;
			for ( ReaderAction a : actions )
				if ( item.value.equals(a.id) ) {
					if (a.getIconId()!=0) {
						img.setImageDrawable(mActivity.getResources().getDrawable(
								a.getIconId()));
						mActivity.tintViewIcons(img, true);
					}
					final String addInfo = getString(a.addInfoR);
					if (!addInfo.equals("")) {
						imgAddInfo.setImageDrawable(
								mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
										R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
						mActivity.tintViewIcons(imgAddInfo);
						imgAddInfo.setVisibility(View.VISIBLE);
						imgAddInfo.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								mActivity.showToast(addInfo, Toast.LENGTH_LONG, layout, true, 0);
							}
						});
					}
				}
		}
	}

	public enum KeyActionFlag {
		KEY_ACTION_FLAG_NORMAL,
		KEY_ACTION_FLAG_LONG,
		KEY_ACTION_FLAG_DOUBLE
	}
	class KeyMapOption extends SubmenuOption {

		private ListView listView;

		public KeyMapOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_APP_KEY_ACTIONS_PRESS, addInfo, filter);
		}
		private void addKey( OptionsListView list, int keyCode, String keyName) {
			addKey(list, keyCode, keyName, EnumSet.allOf(KeyActionFlag.class),
					0, 0,
					0, 0,
					0, 0);
		}
		private void addKey( OptionsListView list, int keyCode, String keyName,
							 int drawableAttrId, int fallbackIconId,
							 int drawableAttrId_long, int fallbackIconId_long,
							 int drawableAttrId_double, int fallbackIconId_double) {
			addKey(list, keyCode, keyName, EnumSet.allOf(KeyActionFlag.class),
					drawableAttrId, fallbackIconId,
					drawableAttrId_long, fallbackIconId_long,
					drawableAttrId_double, fallbackIconId_double);
		}

		private void addKey( OptionsListView list, int keyCode, String keyName, EnumSet<KeyActionFlag> keyFlags) {
			addKey(list, keyCode, keyName, keyFlags, 0, 0, 0, 0, 0, 0);
		}

		private void addKey( OptionsListView list, int keyCode, String keyName, EnumSet<KeyActionFlag> keyFlags,
							 int drawableAttrId, int fallbackIconId,
							 int drawableAttrId_long, int fallbackIconId_long,
							 int drawableAttrId_double, int fallbackIconId_double) {
			if (keyFlags.contains(KeyActionFlag.KEY_ACTION_FLAG_NORMAL)) {
				final String propName = ReaderAction.getKeyProp(keyCode, ReaderAction.NORMAL);
				OptionBase ac = new ActionOption(mOwner, keyName, propName, false, false,
						getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
				list.add(ac.setIconIdByAttr(drawableAttrId, fallbackIconId));
			}
			if (keyFlags.contains(KeyActionFlag.KEY_ACTION_FLAG_LONG)) {
				final String longPropName = ReaderAction.getKeyProp(keyCode, ReaderAction.LONG);
				OptionBase ac = new ActionOption(mOwner, keyName + " " + getContext().getString(R.string.options_app_key_long_press),
						longPropName, false, true, getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
				list.add(ac.setIconIdByAttr(drawableAttrId_long, fallbackIconId_long));
			}
			if (keyFlags.contains(KeyActionFlag.KEY_ACTION_FLAG_DOUBLE)) {
				final String dblPropName = ReaderAction.getKeyProp(keyCode, ReaderAction.DOUBLE);
				OptionBase ac = new ActionOption(mOwner, keyName + " " + getContext().getString(R.string.options_app_key_double_press),
						dblPropName, false, false, getString(R.string.option_add_info_empty_text), this.lastFilteredValue);
				list.add(ac.setIconIdByAttr(drawableAttrId_double, fallbackIconId_double));
			}
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("KeyMapDialog", mActivity, label, false, false);
			View view = mInflater.inflate(R.layout.searchable_listview, null);
			LinearLayout viewList = (LinearLayout)view.findViewById(R.id.lv_list);
			final EditText tvSearchText = (EditText)view.findViewById(R.id.search_text);
			ImageButton ibSearch = (ImageButton)view.findViewById(R.id.btn_search);
			final OptionsListView listView = new OptionsListView(getContext(), this);
			tvSearchText.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
					listView.listUpdated(cs.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

				@Override
				public void afterTextChanged(Editable arg0) {}
			});

			if ( DeviceInfo.NOOK_NAVIGATION_KEYS ) {
				addKey(listView, ReaderView.KEYCODE_PAGE_TOPLEFT, "Top left navigation button");
				addKey(listView, ReaderView.KEYCODE_PAGE_BOTTOMLEFT, "Bottom left navigation button");
				addKey(listView, ReaderView.KEYCODE_PAGE_TOPRIGHT, "Top right navigation button");
				addKey(listView, ReaderView.NOOK_12_KEY_NEXT_LEFT, "Bottom right navigation button");
//				addKey(listView, ReaderView.KEYCODE_PAGE_BOTTOMRIGHT, "Bottom right navigation button");

				// on rooted Nook, side navigation keys may be reassigned on some standard android keycode
				addKey(listView, KeyEvent.KEYCODE_MENU, "Menu",
						R.attr.attr_icons8_menu_key, R.drawable.icons8_menu_key,
						R.attr.attr_icons8_menu_key_long, R.drawable.icons8_menu_key_long,
						R.attr.attr_icons8_menu_key_double, R.drawable.icons8_menu_key_double
				);
				addKey(listView, KeyEvent.KEYCODE_BACK, "Back",
						R.attr.attr_icons8_back_key, R.drawable.icons8_back_key,
						R.attr.attr_icons8_back_key_long, R.drawable.icons8_back_key_long,
						R.attr.attr_icons8_back_key_double, R.drawable.icons8_back_key_double
				);
				addKey(listView, KeyEvent.KEYCODE_SEARCH, "Search",
						R.attr.attr_icons8_search_key, R.drawable.icons8_search_key,
						R.attr.attr_icons8_search_key_long, R.drawable.icons8_search_key_long,
						R.attr.attr_icons8_search_key_double, R.drawable.icons8_search_key_double);
				
				addKey(listView, KeyEvent.KEYCODE_HOME, "Home");
				
				addKey(listView, KeyEvent.KEYCODE_2, "Up",
						R.attr.attr_icons8_up_key, R.drawable.icons8_up_key,
						R.attr.attr_icons8_up_key_long, R.drawable.icons8_up_key_long,
						R.attr.attr_icons8_up_key_double, R.drawable.icons8_up_key_double);
				addKey(listView, KeyEvent.KEYCODE_8, "Down",
						R.attr.attr_icons8_down_key, R.drawable.icons8_down_key,
						R.attr.attr_icons8_down_key_long, R.drawable.icons8_down_key_long,
						R.attr.attr_icons8_down_key_double, R.drawable.icons8_down_key_double);
			} else if ( DeviceInfo.SONY_NAVIGATION_KEYS ) {
//				addKey(listView, KeyEvent.KEYCODE_DPAD_UP, "Prev button");
//				addKey(listView, KeyEvent.KEYCODE_DPAD_DOWN, "Next button");
				addKey(listView, ReaderView.SONY_DPAD_UP_SCANCODE, "Prev button");
				addKey(listView, ReaderView.SONY_DPAD_DOWN_SCANCODE, "Next button");
				addKey(listView, ReaderView.SONY_DPAD_LEFT_SCANCODE, "Left button",
						R.attr.attr_icons8_left_key, R.drawable.icons8_left_key,
						R.attr.attr_icons8_left_key_long, R.drawable.icons8_left_key_long,
						R.attr.attr_icons8_left_key_double, R.drawable.icons8_left_key_double);
				addKey(listView, ReaderView.SONY_DPAD_RIGHT_SCANCODE, "Right button",
						R.attr.attr_icons8_right_key, R.drawable.icons8_right_key,
						R.attr.attr_icons8_right_key_long, R.drawable.icons8_right_key_long,
						R.attr.attr_icons8_right_key_double, R.drawable.icons8_right_key_double);
//				addKey(listView, ReaderView.SONY_MENU_SCANCODE, "Menu");
//				addKey(listView, ReaderView.SONY_BACK_SCANCODE, "Back");
//				addKey(listView, ReaderView.SONY_HOME_SCANCODE, "Home");
				addKey(listView, KeyEvent.KEYCODE_MENU, "Menu",
						R.attr.attr_icons8_menu_key, R.drawable.icons8_menu_key,
						R.attr.attr_icons8_menu_key_long, R.drawable.icons8_menu_key_long,
						R.attr.attr_icons8_menu_key_double, R.drawable.icons8_menu_key_double
				);
				addKey(listView, KeyEvent.KEYCODE_BACK, "Back",
						R.attr.attr_icons8_back_key, R.drawable.icons8_back_key,
						R.attr.attr_icons8_back_key_long, R.drawable.icons8_back_key_long,
						R.attr.attr_icons8_back_key_double, R.drawable.icons8_back_key_double
				);
				addKey(listView, KeyEvent.KEYCODE_HOME, "Home");
			} else {
				EnumSet<KeyActionFlag> keyFlags;
				if (DeviceInfo.EINK_ONYX && DeviceInfo.ONYX_BUTTONS_LONG_PRESS_NOT_AVAILABLE) {
				    keyFlags = EnumSet.of(
				    		KeyActionFlag.KEY_ACTION_FLAG_NORMAL,
							KeyActionFlag.KEY_ACTION_FLAG_DOUBLE
					);
				} else {
					keyFlags = EnumSet.allOf(KeyActionFlag.class);
				}

				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_MENU))
					addKey(listView, KeyEvent.KEYCODE_MENU, "Menu", keyFlags,
							R.attr.attr_icons8_menu_key, R.drawable.icons8_menu_key,
							R.attr.attr_icons8_menu_key_long, R.drawable.icons8_menu_key_long,
							R.attr.attr_icons8_menu_key_double, R.drawable.icons8_menu_key_double
							);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK))
					addKey(listView, KeyEvent.KEYCODE_BACK, "Back", keyFlags,
							R.attr.attr_icons8_back_key, R.drawable.icons8_back_key,
							R.attr.attr_icons8_back_key_long, R.drawable.icons8_back_key_long,
							R.attr.attr_icons8_back_key_double, R.drawable.icons8_back_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_LEFT))
					addKey(listView, KeyEvent.KEYCODE_DPAD_LEFT, "Left", keyFlags,
							R.attr.attr_icons8_left_key, R.drawable.icons8_left_key,
							R.attr.attr_icons8_left_key_long, R.drawable.icons8_left_key_long,
							R.attr.attr_icons8_left_key_double, R.drawable.icons8_left_key_double);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_RIGHT))
					addKey(listView, KeyEvent.KEYCODE_DPAD_RIGHT, "Right", keyFlags,
							R.attr.attr_icons8_right_key, R.drawable.icons8_right_key,
							R.attr.attr_icons8_right_key_long, R.drawable.icons8_right_key_long,
							R.attr.attr_icons8_right_key_double, R.drawable.icons8_right_key_double);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_UP))
					addKey(listView, KeyEvent.KEYCODE_DPAD_UP, "Up", keyFlags,
							R.attr.attr_icons8_up_key, R.drawable.icons8_up_key,
							R.attr.attr_icons8_up_key_long, R.drawable.icons8_up_key_long,
							R.attr.attr_icons8_up_key_double, R.drawable.icons8_up_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_DOWN))
					addKey(listView, KeyEvent.KEYCODE_DPAD_DOWN, "Down", keyFlags,
							R.attr.attr_icons8_down_key, R.drawable.icons8_down_key,
							R.attr.attr_icons8_down_key_long, R.drawable.icons8_down_key_long,
							R.attr.attr_icons8_down_key_double, R.drawable.icons8_down_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_DPAD_CENTER))
					addKey(listView, KeyEvent.KEYCODE_DPAD_CENTER, "Center", keyFlags);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_SEARCH))
					addKey(listView, KeyEvent.KEYCODE_SEARCH, "Search", keyFlags,
							R.attr.attr_icons8_search_key, R.drawable.icons8_search_key,
							R.attr.attr_icons8_search_key_long, R.drawable.icons8_search_key_long,
							R.attr.attr_icons8_search_key_double, R.drawable.icons8_search_key_double
					);
				if (DeviceInfo.EINK_ONYX) {
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_UP))
						addKey(listView, KeyEvent.KEYCODE_VOLUME_UP, "Left Side Button (Volume Up)", keyFlags,
								R.attr.attr_icons8_volume_up_key, R.drawable.icons8_volume_up_key,
								R.attr.attr_icons8_volume_up_key_long, R.drawable.icons8_volume_up_key_long,
								R.attr.attr_icons8_volume_up_key_double, R.drawable.icons8_volume_up_key_double
						);
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN))
						addKey(listView, KeyEvent.KEYCODE_VOLUME_DOWN, "Right Side Button (Volume Down)", keyFlags,
								R.attr.attr_icons8_volume_down_key, R.drawable.icons8_volume_down_key,
								R.attr.attr_icons8_volume_down_key_long, R.drawable.icons8_volume_down_key_long,
								R.attr.attr_icons8_volume_down_key_double, R.drawable.icons8_volume_down_key_double
						);
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_UP))
						addKey(listView, KeyEvent.KEYCODE_PAGE_UP, "Left Side Button", keyFlags,
								R.attr.attr_icons8_page_up_key, R.drawable.icons8_page_up_key,
								R.attr.attr_icons8_page_up_key_long, R.drawable.icons8_page_up_key_long,
								R.attr.attr_icons8_page_up_key_double, R.drawable.icons8_page_up_key_double
						);
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_DOWN))
						addKey(listView, KeyEvent.KEYCODE_PAGE_DOWN, "Right Side Button", keyFlags,
								R.attr.attr_icons8_page_down_key, R.drawable.icons8_page_down_key,
								R.attr.attr_icons8_page_down_key_long, R.drawable.icons8_page_down_key_long,
								R.attr.attr_icons8_page_down_key_double, R.drawable.icons8_page_down_key_double
						);
				}
				else {
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_UP))
						addKey(listView, KeyEvent.KEYCODE_VOLUME_UP, "Volume Up",
								R.attr.attr_icons8_volume_up_key, R.drawable.icons8_volume_up_key,
								R.attr.attr_icons8_volume_up_key_long, R.drawable.icons8_volume_up_key_long,
								R.attr.attr_icons8_volume_up_key_double, R.drawable.icons8_volume_up_key_double
						);
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN))
						addKey(listView, KeyEvent.KEYCODE_VOLUME_DOWN, "Volume Down",
								R.attr.attr_icons8_volume_down_key, R.drawable.icons8_volume_down_key,
								R.attr.attr_icons8_volume_down_key_long, R.drawable.icons8_volume_down_key_long,
								R.attr.attr_icons8_volume_down_key_double, R.drawable.icons8_volume_down_key_double
						);
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_UP))
						addKey(listView, KeyEvent.KEYCODE_PAGE_UP, "Page Up",
								R.attr.attr_icons8_page_up_key, R.drawable.icons8_page_up_key,
								R.attr.attr_icons8_page_up_key_long, R.drawable.icons8_page_up_key_long,
								R.attr.attr_icons8_page_up_key_double, R.drawable.icons8_page_up_key_double
						);
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_DOWN))
						addKey(listView, KeyEvent.KEYCODE_PAGE_DOWN, "Page Down",
								R.attr.attr_icons8_page_down_key, R.drawable.icons8_page_down_key,
								R.attr.attr_icons8_page_down_key_long, R.drawable.icons8_page_down_key_long,
								R.attr.attr_icons8_page_down_key_double, R.drawable.icons8_page_down_key_double
						);
				}
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_CAMERA))
					addKey(listView, KeyEvent.KEYCODE_CAMERA, "Camera", keyFlags,
							R.attr.attr_icons8_camera_key, R.drawable.icons8_camera_key,
							R.attr.attr_icons8_camera_key_long, R.drawable.icons8_camera_key_long,
							R.attr.attr_icons8_camera_key_double, R.drawable.icons8_camera_key_double
					);
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_ESCAPE))
					addKey(listView, ReaderView.KEYCODE_ESCAPE, "Escape", keyFlags,
							R.attr.attr_icons8_esc_key, R.drawable.icons8_esc_key,
							R.attr.attr_icons8_esc_key_long, R.drawable.icons8_esc_key_long,
							R.attr.attr_icons8_esc_key_double, R.drawable.icons8_esc_key_double
					);
				addKey(listView, KeyEvent.KEYCODE_HEADSETHOOK, "Headset Hook",
						R.attr.attr_icons8_headset_key, R.drawable.icons8_headset_key,
						R.attr.attr_icons8_headset_key_long, R.drawable.icons8_headset_key_long,
						R.attr.attr_icons8_headset_key_double, R.drawable.icons8_headset_key_double
				);
			}

			viewList.addView(listView);
			ibSearch.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					tvSearchText.setText("");
					listView.listUpdated("");
				}
			});
			dlg.setView(view);
			ibSearch.requestFocus();
			//dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			if ( DeviceInfo.NOOK_NAVIGATION_KEYS ) {
				this.updateFilteredMark("Top left navigation button");
				this.updateFilteredMark("Bottom left navigation button");
				this.updateFilteredMark("Top right navigation button");
				this.updateFilteredMark("Bottom right navigation button");

				// on rooted Nook, side navigation keys may be reassigned on some standard android keycode
				this.updateFilteredMark( "Menu");
				this.updateFilteredMark("Back");
				this.updateFilteredMark("Search");
				this.updateFilteredMark("Home");
				this.updateFilteredMark( "Up");
				this.updateFilteredMark("Down");
			} else if ( DeviceInfo.SONY_NAVIGATION_KEYS ) {
				this.updateFilteredMark("Prev button");
				this.updateFilteredMark("Next button");
				this.updateFilteredMark("Left button");
				this.updateFilteredMark("Right button");
				this.updateFilteredMark( "Menu");
				this.updateFilteredMark("Back");
				this.updateFilteredMark("Home");
			} else {
				this.updateFilteredMark("Menu");
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK))
					this.updateFilteredMark("Back");
				this.updateFilteredMark("Left");
				this.updateFilteredMark("Right");
				this.updateFilteredMark("Up");
				this.updateFilteredMark("Down");
				this.updateFilteredMark("Center");
				this.updateFilteredMark("Search");
				if (DeviceInfo.EINK_ONYX) {
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_UP))
						this.updateFilteredMark("Left Side Button (Volume Up)");
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN))
						this.updateFilteredMark("Right Side Button (Volume Down)");
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_UP))
						this.updateFilteredMark("Left Side Button");
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_DOWN))
						this.updateFilteredMark("Right Side Button");
				}
				else {
					this.updateFilteredMark("Volume Up");
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_VOLUME_DOWN))
						this.updateFilteredMark("Volume Down");
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_UP))
						this.updateFilteredMark("Page Up");
					if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_PAGE_DOWN))
						this.updateFilteredMark("Page Down");
				}
				this.updateFilteredMark("Camera");
				if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_ESCAPE))
					this.updateFilteredMark("Escape");
				this.updateFilteredMark("Headset Hook");
			}
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class ReaderToolbarOption extends SubmenuOption {

		private ListView listView;

		public ReaderToolbarOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_TOOLBAR_BUTTONS, addInfo, filter);
		}

		private void addAction( OptionsListView list, ReaderAction action) {

			ReaderAction[] ReaderActionDef =
			new ReaderAction[]{
					ReaderAction.GO_BACK,
					ReaderAction.TOC,
					ReaderAction.BOOK_INFO,
					ReaderAction.FONTS_MENU,
					ReaderAction.SEARCH,
					ReaderAction.OPTIONS,
					ReaderAction.BOOKMARKS,
					ReaderAction.FILE_BROWSER_ROOT,
					ReaderAction.TOGGLE_DAY_NIGHT,
					ReaderAction.TOGGLE_SELECTION_MODE,
					ReaderAction.GO_PAGE,
					ReaderAction.GO_PERCENT,
					ReaderAction.FILE_BROWSER,
					ReaderAction.TTS_PLAY,
					ReaderAction.GO_FORWARD,
					ReaderAction.RECENT_BOOKS,
					ReaderAction.OPEN_PREVIOUS_BOOK,
					ReaderAction.TOGGLE_AUTOSCROLL,
					ReaderAction.ABOUT,
					ReaderAction.HIDE
			};

			boolean bIsDef = false;

			for (ReaderAction act: ReaderActionDef)
				if (act.cmd.nativeId == action.cmd.nativeId) {
					bIsDef = true;
					break;
				}

			String lab1 = activity.getString(action.nameId);
			int mirrIcon = 0;
			if (action.getMirrorAction()!=null) {
				lab1 = lab1 + "~long tap: " +activity.getString(action.getMirrorAction().nameId);
				mirrIcon = action.getMirrorAction().iconId;
			}

			list.add(new ListOption2Text(mOwner,
					lab1, PROP_TOOLBAR_BUTTONS+"."
						+String.valueOf(action.cmd.nativeId)+"."+String.valueOf(action.param),
					 getString(action.addInfoR), this.lastFilteredValue)
					.add(mToolbarButtons, mToolbarButtonsTitles, mToolbarAddInfos).setDefaultValue(
							bIsDef ?  Integer.toString(mToolbarButtons[3]) : Integer.toString(mToolbarButtons[0])).
							  setIconId(action.iconId).setIcon2Id(mirrIcon));
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("ReaderToolbarDialog", mActivity, label, false, false);
			View view = mInflater.inflate(R.layout.searchable_listview, null);
			LinearLayout viewList = (LinearLayout)view.findViewById(R.id.lv_list);
			final EditText tvSearchText = (EditText)view.findViewById(R.id.search_text);
			ImageButton ibSearch = (ImageButton)view.findViewById(R.id.btn_search);
			final OptionsListView listView = new OptionsListView(getContext(), this);
			tvSearchText.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
					listView.listUpdated(cs.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

				@Override
				public void afterTextChanged(Editable arg0) {}
			});


			ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;

			for ( ReaderAction a : actions )
				if (
						((a != ReaderAction.NONE) && (a != ReaderAction.EXIT) && (a != ReaderAction.ABOUT))
						)
					addAction(listView, a);

			viewList.addView(listView);
			ibSearch.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					tvSearchText.setText("");
					listView.listUpdated("");
				}
			});
			dlg.setView(view);
			ibSearch.requestFocus();
			//dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;

			for ( ReaderAction a : actions )
				if (
					 ((a != ReaderAction.NONE) && (a != ReaderAction.EXIT) && (a != ReaderAction.ABOUT))
				   ) {
					String lab1 = activity.getString(a.nameId);
					this.updateFilteredMark(lab1);
					this.updateFilteredMark(PROP_TOOLBAR_BUTTONS+"."
							+String.valueOf(a.cmd.nativeId)+"."+String.valueOf(a.param));
					this.updateFilteredMark(getString(a.addInfoR));
				}
			for (int i: mToolbarButtonsTitles) this.updateFilteredMark(getString(i));
			for (int i: mToolbarAddInfos) this.updateFilteredMark(getString(i));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class SkippedResOption extends SubmenuOption {

		private ListView listView;

		public SkippedResOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_SKIPPED_RES, addInfo,filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("SkippedResDialog", mActivity, label, false, false);
			View view = mInflater.inflate(R.layout.searchable_listview, null);
			LinearLayout viewList = (LinearLayout)view.findViewById(R.id.lv_list);
			final EditText tvSearchText = (EditText)view.findViewById(R.id.search_text);
			ImageButton ibSearch = (ImageButton)view.findViewById(R.id.btn_search);
			final OptionsListView listView = new OptionsListView(getContext(), this);
			tvSearchText.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
					listView.listUpdated(cs.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

				@Override
				public void afterTextChanged(Editable arg0) {}
			});

			((CoolReader)mActivity).readResizeHistory();
			for (CoolReader.ResizeHistory rh: ((CoolReader)mActivity).getResizeHist()) {
				String sProp = rh.X+"."+rh.Y;
				String sText = rh.X+" x "+rh.Y+" ("+Utils.formatDate2(activity, rh.lastSet)+" "+
						Utils.formatTime(activity, rh.lastSet)+")";
				listView.add(new BoolOption(mOwner, sText, PROP_SKIPPED_RES+"."+sProp, "", this.lastFilteredValue).setDefaultValue("0").
						setIconId(0));
			}
			viewList.addView(listView);
			ibSearch.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					tvSearchText.setText("");
					listView.listUpdated("");
				}
			});
			dlg.setView(view);
			ibSearch.requestFocus();
			//dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			for (CoolReader.ResizeHistory rh: ((CoolReader)mActivity).getResizeHist()) {
				String sProp = rh.X+"."+rh.Y;
				String sText = rh.X+" x "+rh.Y+" ("+Utils.formatDate2(activity, rh.lastSet)+" "+
						Utils.formatTime(activity, rh.lastSet)+")";
				this.updateFilteredMark(sText);
				this.updateFilteredMark(PROP_SKIPPED_RES+"."+sProp);
			}
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class StatusBarOption extends SubmenuOption {
		public StatusBarOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("StatusBarDialog", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			listView.add(new ListOption(mOwner, getString(R.string.options_page_show_titlebar), PROP_STATUS_LOCATION, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mStatusPositions,
					mStatusPositionsTitles, mStatusPositionsAddInfos).setDefaultValue("1"));
			listView.add(new FontsOptions(mOwner, getString(R.string.options_page_titlebar_font_face), PROP_STATUS_FONT_FACE,
					getString(R.string.option_add_info_empty_text), false, this.lastFilteredValue).setIconIdByAttr(R.attr.cr3_option_font_face_drawable,
					R.drawable.cr3_option_font_face));
			listView.add(new ListOption(mOwner, getString(R.string.options_page_titlebar_font_size), PROP_STATUS_FONT_SIZE, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(
					filterFontSizes(mStatusFontSizes)).setDefaultValue("18").setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size));
			listView.add(new ColorOption(mOwner, getString(R.string.options_page_titlebar_font_color), PROP_STATUS_FONT_COLOR, 0x000000, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_font_color,
							R.drawable.icons8_font_color));
			listView.add(new BoolOption(mOwner, getString(R.string.options_page_show_titlebar_title), PROP_SHOW_TITLE,
                    getString(R.string.option_add_info_empty_text), this.lastFilteredValue).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_book_title2,
							R.drawable.icons8_book_title2));
			listView.add(new BoolOption(mOwner, getString(R.string.options_page_show_titlebar_page_number), PROP_SHOW_PAGE_NUMBER, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_page_num,
					R.drawable.icons8_page_num));
			listView.add(new BoolOption(mOwner, getString(R.string.options_page_show_titlebar_page_count), PROP_SHOW_PAGE_COUNT, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_pages_total,
					R.drawable.icons8_pages_total));
			listView.add(new BoolOption(mOwner, getString(R.string.options_page_show_titlebar_pages_to_chapter), PROP_SHOW_PAGES_TO_CHAPTER,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_page_num,
					R.drawable.icons8_page_num));
			listView.add(new BoolOption(mOwner, getString(R.string.options_page_show_titlebar_time_left), PROP_SHOW_TIME_LEFT,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_page_num,
					R.drawable.icons8_page_num));
			listView.add(new BoolOption(mOwner, getString(R.string.options_page_show_titlebar_percent), PROP_SHOW_POS_PERCENT, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_page_percent,
					R.drawable.icons8_page_percent));
			listView.add(new BoolOption(mOwner, getString(R.string.options_page_show_titlebar_chapter_marks), PROP_STATUS_CHAPTER_MARKS, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_chapter_marks,
					R.drawable.icons8_chapter_marks));
			listView.add(new BoolOption(mOwner, getString(R.string.options_page_show_titlebar_battery_percent), PROP_SHOW_BATTERY_PERCENT, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_battery_percent,
					R.drawable.icons8_battery_percent));
			listView.add(new ListOption(mOwner, getString(R.string.options_rounded_corners_margin), PROP_ROUNDED_CORNERS_MARGIN,
					getString(R.string.options_rounded_corners_margin_add_info), this.lastFilteredValue).add(mRoundedCornersMargins).setDefaultValue("0")
					.setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin, R.drawable.icons8_rounded_corners_margin));
			listView.add(new ListOption(mOwner, getString(R.string.rounded_corners_margin_pos_text), PROP_ROUNDED_CORNERS_MARGIN_POS,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mRoundedCornersMarginPos,
					mRoundedCornersMarginPosTitles, mRoundedCornersMarginPosAddInfos).
					setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin2, R.drawable.icons8_rounded_corners_margin2));
			listView.add(new ListOption(mOwner, getString(R.string.rounded_corners_margin_mod_text), PROP_ROUNDED_CORNERS_MARGIN_MOD,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mScreenMod,
					mScreenModTitles, mScreenModAddInfos).
					setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin2, R.drawable.icons8_rounded_corners_margin2));
			listView.add(new BoolOption(mOwner, getString(R.string.rounded_corners_margin_fullscreen_only), PROP_ROUNDED_CORNERS_MARGIN_FSCR,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin2, R.drawable.icons8_rounded_corners_margin2));
			listView.add(new ListOption(mOwner, getString(R.string.ext_fullscreen_margin_text), PROP_EXT_FULLSCREEN_MARGIN,
					getString(R.string.ext_fullscreen_margin_add_info), this.lastFilteredValue).add2(mExtFullscreenMargin,
					R.string.ext_fullscreen_margin_2,
					mExtFullscreenMarginPosTitles, mExtFullscreenMarginAddInfos).
					setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_ext_fullscreen, R.drawable.icons8_ext_fullscreen));
			listView.add(new ListOption(mOwner, getString(R.string.ext_fullscreen_margin_mod), PROP_EXT_FULLSCREEN_MOD,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mScreenMod,
					mScreenModTitles, mScreenModAddInfos).
					setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_rounded_corners_margin2, R.drawable.icons8_rounded_corners_margin2));
			dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.options_page_show_titlebar), PROP_STATUS_LOCATION,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_titlebar_font_face), PROP_STATUS_FONT_FACE,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_titlebar_font_size), PROP_STATUS_FONT_SIZE,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_titlebar_font_color), PROP_STATUS_FONT_COLOR,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_show_titlebar_title), PROP_SHOW_TITLE,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_show_titlebar_page_number), PROP_SHOW_PAGE_NUMBER,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_show_titlebar_page_count), PROP_SHOW_PAGE_COUNT,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_show_titlebar_pages_to_chapter), PROP_SHOW_PAGES_TO_CHAPTER,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_show_titlebar_percent), PROP_SHOW_POS_PERCENT,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_show_titlebar_chapter_marks), PROP_STATUS_CHAPTER_MARKS,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_show_titlebar_battery_percent), PROP_SHOW_BATTERY_PERCENT,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.ext_fullscreen_margin_text), PROP_EXT_FULLSCREEN_MARGIN,
					getString(R.string.ext_fullscreen_margin_add_info));
			this.updateFilteredMark(getString(R.string.ext_fullscreen_margin_mod), PROP_EXT_FULLSCREEN_MOD,
					getString(R.string.option_add_info_empty_text));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class ToolbarOption extends SubmenuOption {
		public ToolbarOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_TOOLBAR_TITLE, addInfo, filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("ToolbarDialog", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			listView.add(new ListOption(mOwner, getString(R.string.options_view_toolbar_position), PROP_TOOLBAR_LOCATION,
					getString(R.string.options_view_toolbar_position_add_info), this.lastFilteredValue).add(mToolbarPositions, mToolbarPositionsTitles,
					mToolbarPositionsAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_navigation_toolbar_top, R.drawable.icons8_navigation_toolbar_top));
			listView.add(new BoolOption(mOwner, getString(R.string.options_view_toolbar_hide_in_fullscreen), PROP_TOOLBAR_HIDE_IN_FULLSCREEN,
					getString(R.string.options_view_toolbar_hide_in_fullscreen_add_info), this.lastFilteredValue).setDefaultValue("0").
					setIconIdByAttr(R.attr.cr3_option_fullscreen_drawable, R.drawable.cr3_option_fullscreen));
			listView.add(new ListOption(mOwner, getString(R.string.options_view_toolbar_appearance), PROP_TOOLBAR_APPEARANCE,
					getString(R.string.options_view_toolbar_appearance_add_info), this.lastFilteredValue).
					add(mToolbarApperance, mToolbarApperanceTitles, mToolbarApperanceAddInfos).setDefaultValue("0").setIconIdByAttr(
					R.attr.attr_icons8_navigation_toolbar_top, R.drawable.icons8_navigation_toolbar_top));
			OptionBase rtO = new ReaderToolbarOption(mOwner, getString(R.string.options_reader_toolbar_buttons),
					getString(R.string.options_reader_toolbar_buttons_add_info), this.lastFilteredValue).setIconIdByAttr(R.attr.cr3_option_controls_keys_drawable, R.drawable.cr3_option_controls_keys);
			((ReaderToolbarOption)rtO).updateFilterEnd();
			listView.add(rtO);
			dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.options_view_toolbar_position), PROP_TOOLBAR_LOCATION,
					getString(R.string.options_view_toolbar_position_add_info));
			this.updateFilteredMark(getString(R.string.options_view_toolbar_hide_in_fullscreen), PROP_TOOLBAR_HIDE_IN_FULLSCREEN,
					getString(R.string.options_view_toolbar_hide_in_fullscreen_add_info));
			this.updateFilteredMark(getString(R.string.options_view_toolbar_appearance), PROP_TOOLBAR_APPEARANCE,
					getString(R.string.options_view_toolbar_appearance_add_info));
			this.updateFilteredMark(getString(R.string.options_reader_toolbar_buttons), PROP_TOOLBAR_BUTTONS,
					getString(R.string.options_reader_toolbar_buttons_add_info));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class CloudOption extends SubmenuOption {
		public CloudOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_CLOUD_TITLE, addInfo, filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("CloudDialog", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			listView.add(new CloudSettingsOption(mOwner, getString(R.string.yandex_settings),
					getString(R.string.yandex_settings_v), getString(R.string.option_add_info_empty_text), this.lastFilteredValue,
					YANDEX_SETTINGS_OPTION).
					setIconIdByAttr(R.attr.attr_icons8_yandex, R.drawable.icons8_yandex_logo));
			listView.add(new InfoOption(mOwner, getString(R.string.ynd_home_folder),
					PROP_CLOUD_YND_HOME_FOLDER, getString(R.string.ynd_home_folder_hint), this.lastFilteredValue).
					setDefaultValue("/").
					setIconIdByAttr(R.attr.cr3_browser_folder_root_drawable, R.drawable.cr3_browser_folder_root));
			listView.add(new CloudSettingsOption(mOwner, getString(R.string.dropbox_settings),
					getString(R.string.dropbox_settings_v), getString(R.string.option_add_info_empty_text), this.lastFilteredValue,
					DROPBOX_SETTINGS_OPTION).
					setIconIdByAttr(R.attr.attr_icons8_dropbox_filled, R.drawable.icons8_dropbox_filled));
			listView.add(new ListOption(mOwner, getString(R.string.cloud_sync_variant),
					PROP_CLOUD_SYNC_VARIANT, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mCloudSyncVariants, mCloudSyncVariantsTitles, mCloudSyncVariantsAddInfos).setDefaultValue(Integer.toString(mCloudSyncVariants[0])).
					setIconIdByAttr(R.attr.attr_icons8_cloud_storage, R.drawable.icons8_cloud_storage));
			listView.add(new SaveOptionsToGDOption(mOwner, getString(R.string.save_settings_to_cloud),
					getString(R.string.save_settings_to_cloud_v), getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_settings_to_gd, R.drawable.icons8_settings_to_gd));
			listView.add(new LoadOptionsFromGDOption(mOwner, getString(R.string.load_settings_from_cloud),
					getString(R.string.load_settings_from_cloud_v), getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_settings_from_gd, R.drawable.icons8_settings_from_gd));
			listView.add(new ListOption(mOwner, getString(R.string.save_pos_to_cloud_timeout),
					PROP_SAVE_POS_TO_CLOUD_TIMEOUT, getString(R.string.save_pos_to_cloud_timeout_add_info), this.lastFilteredValue).
					add(mMotionTimeouts, mMotionTimeoutsTitles, mMotionTimeoutsAddInfos).setDefaultValue(Integer.toString(mMotionTimeouts[0])).
					setIconIdByAttr(R.attr.attr_icons8_position_to_gd_interval, R.drawable.icons8_position_to_gd_interval));
			dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.yandex_settings), "",
					getString(R.string.yandex_settings_v));
			this.updateFilteredMark(getString(R.string.dropbox_settings), "",
					getString(R.string.dropbox_settings_v));
			this.updateFilteredMark(getString(R.string.cloud_sync_variant), PROP_CLOUD_SYNC_VARIANT,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.save_settings_to_cloud), "",
					getString(R.string.save_settings_to_cloud_v));
			this.updateFilteredMark(getString(R.string.load_settings_from_cloud), "",
					getString(R.string.load_settings_from_cloud_v));
			this.updateFilteredMark(getString(R.string.save_pos_to_cloud_timeout), PROP_SAVE_POS_TO_CLOUD_TIMEOUT,
					getString(R.string.save_pos_to_cloud_timeout_add_info));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class DictionaryOption extends SubmenuOption {
		public DictionaryOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_DICTIONARY_TITLE, addInfo, filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("DictionaryOption", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			listView.add(new BoolOption(mOwner, getString(R.string.options_selection_keep_selection_after_dictionary), PROP_APP_SELECTION_PERSIST,
					getString(R.string.options_selection_keep_selection_after_dictionary_add_info), this.lastFilteredValue).setDefaultValue("0").
					setIconIdByAttr(R.attr.attr_icons8_document_selection_lock, R.drawable.icons8_document_selection_lock));
			listView.add(new DictOptions(mOwner, getString(R.string.options_app_dictionary), getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_google_translate, R.drawable.icons8_google_translate));
			listView.add(new DictOptions2(mOwner, getString(R.string.options_app_dictionary2),
					getString(R.string.options_app_dictionary2_add_info), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_google_translate_2, R.drawable.icons8_google_translate_2));
			listView.add(new BoolOption(mOwner, getString(R.string.options_app_dict_word_correction),
					PROP_APP_DICT_WORD_CORRECTION, getString(R.string.options_app_dict_word_correction_add_info), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_l_h,R.drawable.icons8_l_h));
			listView.add(new BoolOption(mOwner, getString(R.string.options_app_dict_longtap_change),
					PROP_APP_DICT_LONGTAP_CHANGE, getString(R.string.options_app_dict_longtap_change_add_info), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_single_double_tap, R.drawable.icons8_single_double_tap));
			listView.add(new BoolOption(mOwner, getString(R.string.options_app_show_user_dic_panel), PROP_APP_SHOW_USER_DIC_PANEL,
					getString(R.string.options_app_show_user_dic_panel_add_info), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_google_translate_user,R.drawable.icons8_google_translate_user));
			dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.options_selection_keep_selection_after_dictionary), PROP_APP_SELECTION_PERSIST,
					getString(R.string.options_selection_keep_selection_after_dictionary_add_info));
			this.updateFilteredMark(getString(R.string.options_app_dictionary), "",
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_app_dictionary2), "",
					getString(R.string.options_app_dictionary2_add_info));
			this.updateFilteredMark(getString(R.string.options_app_dict_word_correction), PROP_APP_DICT_WORD_CORRECTION,
					getString(R.string.options_app_dict_word_correction_add_info));
			this.updateFilteredMark(getString(R.string.options_app_dict_longtap_change), PROP_APP_DICT_LONGTAP_CHANGE,
					getString(R.string.options_app_dict_longtap_change_add_info));
			this.updateFilteredMark(getString(R.string.options_app_show_user_dic_panel), PROP_APP_SHOW_USER_DIC_PANEL,
					getString(R.string.options_app_show_user_dic_panel_add_info));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class FilebrowserOption extends SubmenuOption {
		public FilebrowserOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_FILEBROWSER_TITLE, addInfo, filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("FilebrowserOption", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			listView.add(new ListOption(mOwner, getString(R.string.mi_book_sort_order), PROP_APP_BOOK_SORT_ORDER,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue)
					.add(sortOrderValues, sortOrderLabels, sortOrderAddInfos).setDefaultValue(FileInfo.SortOrder.TITLE_AUTHOR.name()).noIcon());
			listView.add(new BoolOption(mOwner, getString(R.string.options_app_show_cover_pages), PROP_APP_SHOW_COVERPAGES,
					getString(R.string.options_app_show_cover_pages_add_info), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_book, R.drawable.icons8_book));
			listView.add(new ListOption(mOwner, getString(R.string.options_app_cover_page_size),
					PROP_APP_COVERPAGE_SIZE, getString(R.string.options_app_cover_page_size_add_info), this.lastFilteredValue).
					add(mCoverPageSizes, mCoverPageSizeTitles, mCoverPageSizeAddInfos).
					setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_book_big_and_small, R.drawable.icons8_book_big_and_small));
			listView.add(new BoolOption(mOwner, getString(R.string.options_app_scan_book_props),
					PROP_APP_BOOK_PROPERTY_SCAN_ENABLED,
					getString(R.string.options_app_scan_book_props_add_info), this.lastFilteredValue).
					setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_book_scan_properties,R.drawable.icons8_book_scan_properties));
			listView.add(new BoolOption(mOwner, getString(R.string.options_app_browser_hide_empty_dirs), PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS,
					getString(R.string.options_app_browser_hide_empty_dirs_add_info), this.lastFilteredValue).setDefaultValue("0").noIcon());
			listView.add(new BoolOption(mOwner, getString(R.string.mi_book_browser_simple_mode), PROP_APP_FILE_BROWSER_SIMPLE_MODE,
					getString(R.string.mi_book_browser_simple_mode_add_info), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_file,R.drawable.icons8_file));
			listView.add(new ListOption(mOwner, getString(R.string.mi_book_browser_max_group_size), PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE,
					getString(R.string.mi_book_browser_max_group_size_add_info), this.lastFilteredValue).
					add(mBrowserMaxGroupItems).setDefaultValue("8").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.browser_tap_option_tap), PROP_APP_FILE_BROWSER_TAP_ACTION,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mBrowserAction, mBrowserActionTitles, mBrowserActionAddInfos).setDefaultValue("0").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.browser_tap_option_longtap), PROP_APP_FILE_BROWSER_LONGTAP_ACTION,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mBrowserAction, mBrowserActionTitles, mBrowserActionAddInfos).setDefaultValue("1").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.sec_group_common), PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mSecGroupCommon, mSecGroupCommonTitles, mSecGroupCommonTitlesAddInfos).setDefaultValue("0").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.sec_group_author), PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.sec_group_series), PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.sec_group_genres), PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.sec_group_rating), PROP_APP_FILE_BROWSER_SEC_GROUP_RATING,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.sec_group_state), PROP_APP_FILE_BROWSER_SEC_GROUP_STATE,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.sec_group_dates), PROP_APP_FILE_BROWSER_SEC_GROUP_DATES,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mSecGroupCommon3, mSecGroupCommonTitles3, mSecGroupCommonTitlesAddInfos3).setDefaultValue("0").noIcon());
			listView.add(new ListOption(mOwner, getString(R.string.sec_group_search), PROP_APP_FILE_BROWSER_SEC_GROUP_SEARCH,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).
					add(mSecGroupCommon2, mSecGroupCommonTitles2, mSecGroupCommonTitlesAddInfos2).setDefaultValue("0").noIcon());
			dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.mi_book_sort_order), PROP_APP_BOOK_SORT_ORDER,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_app_show_cover_pages), PROP_APP_SHOW_COVERPAGES,
					getString(R.string.options_app_show_cover_pages_add_info));
			this.updateFilteredMark(getString(R.string.options_app_cover_page_size), PROP_APP_COVERPAGE_SIZE,
					getString(R.string.options_app_cover_page_size_add_info));
			this.updateFilteredMark(getString(R.string.options_app_scan_book_props), PROP_APP_BOOK_PROPERTY_SCAN_ENABLED,
					getString(R.string.options_app_scan_book_props_add_info));
			this.updateFilteredMark(getString(R.string.options_app_browser_hide_empty_dirs), PROP_APP_FILE_BROWSER_HIDE_EMPTY_FOLDERS,
					getString(R.string.options_app_browser_hide_empty_dirs_add_info));
			this.updateFilteredMark(getString(R.string.mi_book_browser_simple_mode), PROP_APP_FILE_BROWSER_SIMPLE_MODE,
					getString(R.string.mi_book_browser_simple_mode_add_info));
			this.updateFilteredMark(getString(R.string.mi_book_browser_max_group_size), PROP_APP_FILE_BROWSER_MAX_GROUP_SIZE,
					getString(R.string.mi_book_browser_max_group_size_add_info));
			this.updateFilteredMark(getString(R.string.sec_group_common), PROP_APP_FILE_BROWSER_SEC_GROUP_COMMON,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.sec_group_author), PROP_APP_FILE_BROWSER_SEC_GROUP_AUTHOR,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.sec_group_dates), PROP_APP_FILE_BROWSER_SEC_GROUP_DATES,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.sec_group_genres), PROP_APP_FILE_BROWSER_SEC_GROUP_GENRES,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.sec_group_rating), PROP_APP_FILE_BROWSER_SEC_GROUP_RATING,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.sec_group_search), PROP_APP_FILE_BROWSER_SEC_GROUP_SEARCH,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.sec_group_series), PROP_APP_FILE_BROWSER_SEC_GROUP_SERIES,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.sec_group_state), PROP_APP_FILE_BROWSER_SEC_GROUP_STATE,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.browser_tap_option_tap), PROP_APP_FILE_BROWSER_TAP_ACTION,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.browser_tap_option_longtap), PROP_APP_FILE_BROWSER_LONGTAP_ACTION,
					getString(R.string.option_add_info_empty_text));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class RareOption extends SubmenuOption {
		public RareOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_RARE_TITLE, addInfo, filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("RareOption", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			OptionBase srO = new SkippedResOption(mOwner, getString(R.string.skipped_res), getString(R.string.skipped_res_add_info), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_resolution,R.drawable.icons8_resolution);
			((SkippedResOption)srO).updateFilterEnd();
			listView.add(srO);
			listView.add(new ListOption(mOwner, getString(R.string.force_tts_koef),
					PROP_APP_TTS_FORCE_KOEF, getString(R.string.force_tts_koef_add_info), this.lastFilteredValue).
					add(mForceTTS, mForceTTSTitles, mForceTTSAddInfos).
					setDefaultValue("0").
					setIconIdByAttr(R.attr.cr3_button_tts_drawable, R.drawable.icons8_speaker));
			listView.add(new BoolOption(mOwner, getString(R.string.options_app_tapzone_hilite), PROP_APP_TAP_ZONE_HILIGHT,
					getString(R.string.options_app_tapzone_hilite_add_info), this.lastFilteredValue).setDefaultValue("0").
					setIconIdByAttr(R.attr.cr3_option_touch_drawable, R.drawable.cr3_option_touch));
			if ( !DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) )
				listView.add(new BoolOption(mOwner, getString(R.string.options_app_trackball_disable), PROP_APP_TRACKBALL_DISABLED,
						getString(R.string.options_app_trackball_disable_add_info), this.lastFilteredValue).setDefaultValue("0").
						setIconIdByAttr(R.attr.attr_icons8_computer_mouse,R.drawable.icons8_computer_mouse));
			dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.skipped_res), "",
					getString(R.string.skipped_res_add_info));
			this.updateFilteredMark(getString(R.string.force_tts_koef), PROP_APP_TTS_FORCE_KOEF,
					getString(R.string.force_tts_koef_add_info));
			this.updateFilteredMark(getString(R.string.options_app_tapzone_hilite), PROP_APP_TAP_ZONE_HILIGHT,
					getString(R.string.options_app_tapzone_hilite_add_info));
			this.updateFilteredMark(getString(R.string.options_app_trackball_disable), PROP_APP_TRACKBALL_DISABLED,
					getString(R.string.options_app_trackball_disable_add_info));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class PageColorsOption extends SubmenuOption {
		public PageColorsOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_PAGECOLORS_TITLE, addInfo, filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("PageColorsOption", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			listView.add(new NightModeOption(mOwner, getString(R.string.options_inverse_view), PROP_NIGHT_MODE,
					getString(R.string.options_inverse_view_add_info), this.lastFilteredValue).setIconIdByAttr(R.attr.cr3_option_night_drawable, R.drawable.cr3_option_night));
			listView.add(new ColorOption(mOwner, getString(R.string.options_color_text), PROP_FONT_COLOR, 0x000000,
					getString(R.string.options_color_text_add_info), this.lastFilteredValue).setIconIdByAttr(R.attr.attr_icons8_font_color, R.drawable.icons8_font_color));
			listView.add(new ColorOption(mOwner, getString(R.string.options_color_background), PROP_BACKGROUND_COLOR, 0xFFFFFF,
					getString(R.string.options_color_background_add_info), this.lastFilteredValue).
					setIconIdByAttr(R.attr.attr_icons8_paint_palette1, R.drawable.icons8_paint_palette1));
			if ( !DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) ) {
				listView.add(new ColorOption(mOwner, getString(R.string.options_view_color_selection), PROP_HIGHLIGHT_SELECTION_COLOR,
						0xCCCCCC, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).noIcon());
				listView.add(new ColorOption(mOwner, getString(R.string.options_view_color_bookmark_comment), PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT,
						0xFFFF40, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).noIcon());
				listView.add(new ColorOption(mOwner, getString(R.string.options_view_color_bookmark_correction), PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION,
						0xFF8000, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).noIcon());
			}
			dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.options_inverse_view), PROP_NIGHT_MODE,
					getString(R.string.options_inverse_view_add_info));
			this.updateFilteredMark(getString(R.string.options_color_text), PROP_FONT_COLOR,
					getString(R.string.options_color_text_add_info));
			this.updateFilteredMark(getString(R.string.options_color_background), PROP_BACKGROUND_COLOR,
					getString(R.string.options_color_background_add_info));
			this.updateFilteredMark(getString(R.string.options_view_color_selection), PROP_HIGHLIGHT_SELECTION_COLOR,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_view_color_bookmark_comment), PROP_HIGHLIGHT_BOOKMARK_COLOR_COMMENT,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_view_color_bookmark_correction), PROP_HIGHLIGHT_BOOKMARK_COLOR_CORRECTION,
					getString(R.string.option_add_info_empty_text));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class PageMarginsOption extends SubmenuOption {
		public PageMarginsOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_PAGEMARGINS_TITLE, addInfo, filter);
		}

		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("PageMarginsOption", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			listView.add(new ListOption(mOwner, getString(R.string.options_page_margin_left), PROP_PAGE_MARGIN_LEFT,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mMargins).setDefaultValue("5").
					setIconIdByAttr(R.attr.cr3_option_text_margin_left_drawable, R.drawable.cr3_option_text_margin_left));
			listView.add(new ListOption(mOwner, getString(R.string.options_page_margin_right), PROP_PAGE_MARGIN_RIGHT,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mMargins).setDefaultValue("5").
					setIconIdByAttr(R.attr.cr3_option_text_margin_right_drawable, R.drawable.cr3_option_text_margin_right));
			listView.add(new ListOption(mOwner, getString(R.string.options_page_margin_top), PROP_PAGE_MARGIN_TOP,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mMargins).setDefaultValue("5").
					setIconIdByAttr(R.attr.cr3_option_text_margin_top_drawable, R.drawable.cr3_option_text_margin_top));
			listView.add(new ListOption(mOwner, getString(R.string.options_page_margin_bottom), PROP_PAGE_MARGIN_BOTTOM,
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mMargins).setDefaultValue("5").
					setIconIdByAttr(R.attr.cr3_option_text_margin_bottom_drawable, R.drawable.cr3_option_text_margin_bottom));
			dlg.setView(listView);
			dlg.show();
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.options_page_margin_left), PROP_PAGE_MARGIN_LEFT,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_margin_right), PROP_PAGE_MARGIN_RIGHT,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_margin_top), PROP_PAGE_MARGIN_TOP,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_page_margin_bottom), PROP_PAGE_MARGIN_BOTTOM,
					getString(R.string.option_add_info_empty_text));
			return this.lastFiltered;
		}

		public String getValueLabel() { return ">"; }
	}

	class PluginsOption extends SubmenuOption {
		public PluginsOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_APP_PLUGIN_ENABLED, addInfo, filter);
		}
		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("PluginsDialog", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(),this);
			boolean defEnableLitres = activity.getCurrentLanguage().toLowerCase().startsWith("ru") && !DeviceInfo.POCKETBOOK;
			listView.add(new BoolOption(mOwner, "LitRes", PROP_APP_PLUGIN_ENABLED + "." +
					OnlineStorePluginManager.PLUGIN_PKG_LITRES, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).setDefaultValue(defEnableLitres ? "1" : "0"));
			dlg.setView(listView);
			dlg.show();
		}

		public String getValueLabel() { return ">"; }
	}
	
	class ImageScalingOption extends SubmenuOption {
		public ImageScalingOption( OptionOwner owner, String label, String addInfo, String filter ) {
			super(owner, label, PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE, addInfo, filter);
		}
		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("ImageScalingDialog", mActivity, label, false, false);
			OptionsListView listView = new OptionsListView(getContext(), this);
			listView.add(new ListOption(mOwner, getString(R.string.options_format_image_scaling_block_mode), PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE,
				getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mImageScalingModes, mImageScalingModesTitles, mImageScalingModesAddInfos).
					setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_expand,
					R.drawable.icons8_expand));
			listView.add(new ListOption(mOwner, getString(R.string.options_format_image_scaling_block_scale), PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mImageScalingFactors,
					mImageScalingFactorsTitles, mImageScalingFactorsAddInfos).
					setDefaultValue("2").setIconIdByAttr(R.attr.attr_icons8_expand,
					R.drawable.icons8_expand));
			listView.add(new ListOption(mOwner, getString(R.string.options_format_image_scaling_inline_mode), PROP_IMG_SCALING_ZOOMIN_INLINE_MODE, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mImageScalingModes,
					mImageScalingModesTitles, mImageScalingModesAddInfos).
					setDefaultValue("0").setIconIdByAttr(R.attr.attr_icons8_expand,
					R.drawable.icons8_expand));
			listView.add(new ListOption(mOwner, getString(R.string.options_format_image_scaling_inline_scale), PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE, getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(mImageScalingFactors,
					mImageScalingFactorsTitles, mImageScalingFactorsAddInfos).
					setDefaultValue("2").setIconIdByAttr(R.attr.attr_icons8_expand,
					R.drawable.icons8_expand));
			dlg.setView(listView);
			dlg.show();
		}

		private void copyProperty( String to, String from ) {
			mProperties.put(to, mProperties.get(from));
		}

		protected void closed() {
			copyProperty(PROP_IMG_SCALING_ZOOMOUT_BLOCK_MODE, PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE);
			copyProperty(PROP_IMG_SCALING_ZOOMOUT_INLINE_MODE, PROP_IMG_SCALING_ZOOMIN_INLINE_MODE);
			copyProperty(PROP_IMG_SCALING_ZOOMOUT_BLOCK_SCALE, PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE);
			copyProperty(PROP_IMG_SCALING_ZOOMOUT_INLINE_SCALE, PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE);
		}

		public boolean updateFilterEnd() {
			this.updateFilteredMark(getString(R.string.options_format_image_scaling_block_mode), PROP_IMG_SCALING_ZOOMIN_BLOCK_MODE,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_format_image_scaling_block_scale), PROP_IMG_SCALING_ZOOMIN_BLOCK_SCALE,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_format_image_scaling_inline_mode), PROP_IMG_SCALING_ZOOMIN_INLINE_MODE,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(getString(R.string.options_format_image_scaling_inline_scale), PROP_IMG_SCALING_ZOOMIN_INLINE_SCALE,
					getString(R.string.option_add_info_empty_text));
			this.updateFilteredMark(PROP_IMG_SCALING_ZOOMOUT_BLOCK_MODE);
			this.updateFilteredMark(PROP_IMG_SCALING_ZOOMOUT_INLINE_MODE);
			this.updateFilteredMark(PROP_IMG_SCALING_ZOOMOUT_BLOCK_SCALE);
			this.updateFilteredMark(PROP_IMG_SCALING_ZOOMOUT_INLINE_SCALE);
			return this.lastFiltered;
		}
		
		public String getValueLabel() { return ">"; }
	}

	class TapZoneOption extends SubmenuOption {
		public TapZoneOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super( owner, label, property, addInfo, filter);
			ReaderAction[] actions = ReaderAction.AVAILABLE_ACTIONS;
			for ( ReaderAction a : actions )
					this.updateFilteredMark(a.id, getString(a.nameId), getString(a.addInfoR));
		}
		View grid;
		private void initTapZone( View view, final int tapZoneId )
		{
			if ( view==null )
				return;
			final TextView text = (TextView)view.findViewById(R.id.tap_zone_action_text_short);
			final TextView longtext = (TextView)view.findViewById(R.id.tap_zone_action_text_long);
			final  ImageView iv = (ImageView)view.findViewById(R.id.zone_icon);
			final String propName = property + "." + tapZoneId;
			final String longPropName = property + ".long." + tapZoneId;
			final ReaderAction action = ReaderAction.findById( mProperties.getProperty(propName) );
			if ((iv != null)&&(action != null)) {
				int iconId = action.iconId;
				if (iconId == 0) {
					iconId = Utils.resolveResourceIdByAttr(activity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
				}
				Drawable d = activity.getResources().getDrawable(action.getIconIdWithDef(activity));
				iv.setImageDrawable(d);
				mActivity.tintViewIcons(iv,true);
			}
			final ReaderAction longAction = ReaderAction.findById( mProperties.getProperty(longPropName) );
			final ImageView ivl = (ImageView)view.findViewById(R.id.zone_icon_long);
			if ((ivl != null)&&(longAction != null)) {
				int iconId = longAction.iconId;
				if (iconId == 0) {
					iconId = Utils.resolveResourceIdByAttr(activity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
				}
				Drawable d = activity.getResources().getDrawable(longAction.getIconIdWithDef(activity));
				ivl.setImageDrawable(d);
				mActivity.tintViewIcons(ivl, true);
			}
			text.setText(getString(action.nameId));
			longtext.setText(getString(longAction.nameId));

			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();
			text.setTextColor(colorIcon);
			longtext.setTextColor(colorIcon);

			view.setLongClickable(true);
			final String filt = this.lastFilteredValue;
			view.setOnClickListener(new View.OnClickListener () {
				@Override
				public void onClick(View v) {
					// TODO: i18n
					ActionOption option = new ActionOption(mOwner, getString(R.string.options_app_tap_action_short), propName, true,
							false, getString(action.addInfoR), filt);
					option.setIconId(action.getIconId());
					option.setOnChangeHandler(new Runnable() {
						public void run() {
							ReaderAction action = ReaderAction.findById( mProperties.getProperty(propName) );
							text.setText(getString(action.nameId));
							int iconId = action.iconId;
							if (iconId == 0) {
								iconId = Utils.resolveResourceIdByAttr(activity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
							}
							Drawable d = activity.getResources().getDrawable(action.getIconIdWithDef(activity));
							iv.setImageDrawable(d);
							mActivity.tintViewIcons(iv,true);
						}
					});
					option.onSelect();
				}
			});
			view.setOnLongClickListener(new View.OnLongClickListener () {
				@Override
				public boolean onLongClick(View v) {
					// TODO: i18n
					ActionOption option = new ActionOption(mOwner, getString(R.string.options_app_tap_action_long), longPropName, true,
							true, getString(longAction.addInfoR), filt);
					option.setIconId(action.getIconId());
					option.setOnChangeHandler(new Runnable() {
						public void run() {
							ReaderAction longAction = ReaderAction.findById( mProperties.getProperty(longPropName) );
							longtext.setText(getString(longAction.nameId));
							int iconId = longAction.iconId;
							if (iconId == 0) {
								iconId = Utils.resolveResourceIdByAttr(activity, R.attr.cr3_option_other_drawable, R.drawable.cr3_option_other);
							}
							Drawable d = activity.getResources().getDrawable(longAction.getIconIdWithDef(activity));
							ivl.setImageDrawable(d);
							mActivity.tintViewIcons(ivl, true);
						}
					});
					option.onSelect();
					return true;
				}
			});
		}

		public String getValueLabel() { return ">"; }
		public void onSelect() {
			if (!enabled)
				return;
			BaseDialog dlg = new BaseDialog("TapZoneDialog", mActivity, label, false, false);
			grid = (View)mInflater.inflate(R.layout.options_tap_zone_grid, null);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell1), 1);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell2), 2);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell3), 3);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell4), 4);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell5), 5);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell6), 6);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell7), 7);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell8), 8);
			initTapZone(grid.findViewById(R.id.tap_zone_grid_cell9), 9);
			dlg.setView(grid);
			dlg.show();
		}
	}
	
	public static class Pair {
		public String value;
		public String label;
		public Pair(String value, String label) {
			this.value = value;
			this.label = label;
		}
	}

	public static class Three {
		public String value;
		public String label;
		public String addInfo;
		public Three(String value, String label, String addInfo) {
			this.value = value;
			this.label = label;
			this.addInfo = addInfo;
		}
	}

	public static class SubmenuOption extends ListOption {
		public SubmenuOption( OptionOwner owner, String label, String property, String addInfo, String filter) {
			super(owner, label, property, addInfo, filter);
		}
		public int getItemViewType() {
			return OPTION_VIEW_TYPE_SUBMENU;
		}
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				view = mInflater.inflate(R.layout.option_item_submenu, null);
				mActivity.tintViewIcons(view);
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			labelView.setText(label);
			labelView.setEnabled(enabled);
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			return view;
		}
	}
	
	public static class ListOption extends OptionBase {
		protected ArrayList<Three> list = new ArrayList<Three>();
		protected ArrayList<Three> listFiltered = new ArrayList<Three>();
		protected BaseAdapter listAdapter;
		private ListView listView;

		public ListOption( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
		}
		public void add(String value, String label, String addInfo) {
			list.add( new Three(value, label, addInfo) );
			listFiltered.add( new Three(value, label, addInfo) );
			this.updateFilteredMark(value);
			this.updateFilteredMark(label);
			this.updateFilteredMark(addInfo);
		}
		public ListOption add(String[]values) {
			for ( String item : values ) {
				add(item, item, mActivity.getString(R.string.option_add_info_empty_text));
				this.updateFilteredMark(item);
			}
			return this;
		}
		public ListOption add(double[]values) {
			for ( double item : values ) {
				String s = String.valueOf(item); 
				add(s, s, mActivity.getString(R.string.option_add_info_empty_text));
				this.updateFilteredMark(s);
			}
			return this;
		}
		public ListOption add(int[]values) {
			for ( int item : values ) {
				String s = String.valueOf(item); 
				add(s, s, mActivity.getString(R.string.option_add_info_empty_text));
				this.updateFilteredMark(s);
			}
			return this;
		}
		public ListOption add(int[]values, int[]labelIDs, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = String.valueOf(values[i]); 
				String label = mActivity.getString(labelIDs[i]);
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
				this.updateFilteredMark(value);
				this.updateFilteredMark(label);
				this.updateFilteredMark(addInfo);
			}
			return this;
		}
		public ListOption add2(int[]values, int labelID, int[]labelIDs, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = String.valueOf(values[i]);
				String labelConst = mActivity.getString(labelID);
				String sLab = "";
				if (labelIDs[i]<0) sLab = labelConst.trim()+" " + String.valueOf((int)-labelIDs[i]);
					else sLab = mActivity.getString(labelIDs[i]);
				String label = sLab;
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
				this.updateFilteredMark(value);
				this.updateFilteredMark(label);
				this.updateFilteredMark(addInfo);
			}
			return this;
		}
		public ListOption add(String[]values, int[]labelIDs, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = values[i]; 
				String label = mActivity.getString(labelIDs[i]);
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
				this.updateFilteredMark(value);
				this.updateFilteredMark(label);
				this.updateFilteredMark(addInfo);
			}
			return this;
		}
		public ListOption add(String[]values, String[]labels, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = values[i]; 
				String label = labels[i];
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
				this.updateFilteredMark(value);
				this.updateFilteredMark(label);
				this.updateFilteredMark(addInfo);
			}
			return this;
		}
		public ListOption add(int[]values, String[]labels, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = String.valueOf(values[i]); 
				String label = labels[i];
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
				this.updateFilteredMark(value);
				this.updateFilteredMark(label);
				this.updateFilteredMark(addInfo);
			}
			return this;
		}
		public ListOption addPercents(int[]values) {
			for ( int item : values ) {
				String s = String.valueOf(item); 
				add(s, s + "%", mActivity.getString(R.string.option_add_info_empty_text));
				this.updateFilteredMark(s);
			}
			return this;
		}
		public String findValueLabel( String value ) {
			for ( Three three: list ) {
				if ( value!=null && three.value.equals(value) )
					return three.label;
			}
			return null;
		}
		public int findValue( String value ) {
			if ( value==null )
				return -1;
			for ( int i=0; i<list.size(); i++ ) {
				if ( value.equals(list.get(i).value) )
					return i;
			}
			return -1;
		}
		
		public int getSelectedItemIndex() {
			return findValue(mProperties.getProperty(property));
		}

		protected void closed() {
		}
		
		protected int getItemLayoutId(int position, final Three item) {
			return R.layout.option_value; 
		}
		
		protected void updateItemContents( final View layout, final Three item, final ListView listView, final int position ) {
			TextView view;
			RadioButton cb;
			//iv = (ImageView) = layout.findViewById(R.id.option_value_text);
			view = (TextView)layout.findViewById(R.id.option_value_text);
			cb = (RadioButton)layout.findViewById(R.id.option_value_check);
			ImageView btnOptionAddInfo = (ImageView)layout.findViewById(R.id.btn_option_add_info);

			if (item.addInfo.trim().equals("")) {
				if (btnOptionAddInfo!=null)
					btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				if (btnOptionAddInfo!=null) {
					btnOptionAddInfo.setImageDrawable(
							mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
									R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
					mActivity.tintViewIcons(btnOptionAddInfo);
				}
				final View view1 = layout;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							//Toast toast = Toast.makeText(mActivity, item.addInfo, Toast.LENGTH_LONG);
							//toast.show();
							mActivity.showToast(item.addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			view.setText(item.label);
			String currValue = mProperties.getProperty(property);
			boolean isSelected = item.value!=null && currValue!=null && item.value.equals(currValue) ;//getSelectedItemIndex()==position;
			cb.setChecked(isSelected);
			cb.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listView.getOnItemClickListener().onItemClick(listView, listView, position, 0);
//					mProperties.setProperty(property, item.value);
//					dismiss();
//					optionsListView.refresh();
				}
			});
		}
		
		public String getValueLabel() { return findValueLabel(mProperties.getProperty(property)); }

		public void listUpdated(String sText) {
			listFiltered.clear();
			for(int i=0;i<list.size();i++){
				if (
						((list.get(i).label.toLowerCase()).contains(sText.toLowerCase()))||
						((list.get(i).value.toLowerCase()).contains(sText.toLowerCase()))||
						((list.get(i).addInfo.toLowerCase()).contains(sText.toLowerCase()))
					) {
					OptionsDialog.Three item = new OptionsDialog.Three(
							list.get(i).value, list.get(i).label, list.get(i).addInfo);
					listFiltered.add(item);
				}
			}
			listAdapter = new BaseAdapter() {

				public boolean areAllItemsEnabled() {
					return true;
				}

				public boolean isEnabled(int position) {
					return true;
				}

				public int getCount() {
					return listFiltered.size();
				}

				public Object getItem(int position) {
					return listFiltered.get(position);
				}

				public long getItemId(int position) {
					return position;
				}

				public int getItemViewType(int position) {
					return 0;
				}

				public View getView(final int position, View convertView,
									ViewGroup parent) {
					ViewGroup layout;
					final Three item = listFiltered.get(position);
					if ( convertView==null ) {
						layout = (ViewGroup)mInflater.inflate(getItemLayoutId(position, item), null);
						//view = new TextView(getContext());
					} else {
						layout = (ViewGroup)convertView;
					}
					updateItemContents( layout, item, listView, position );
					//cb.setClickable(false);
//					cb.setOnClickListener(new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//
//						}
//					});
					return layout;
				}

				public int getViewTypeCount() {
					return 1;
				}

				public boolean hasStableIds() {
					return true;
				}

				public boolean isEmpty() {
					return listFiltered.size()==0;
				}

				private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

				public void registerDataSetObserver(DataSetObserver observer) {
					observers.add(observer);
				}

				public void unregisterDataSetObserver(DataSetObserver observer) {
					observers.remove(observer);
				}

			};
			int selItem = getSelectedItemIndex();
			if ( selItem<0 )
				selItem = 0;
			listView.setAdapter(listAdapter);
			listAdapter.notifyDataSetChanged();
			listView.setSelection(selItem);
		}

		public void onSelect() {
			if (!enabled)
				return;
			final BaseDialog dlg = new BaseDialog("ListOptionDialog", mActivity, label, false, false);
			View view = mInflater.inflate(R.layout.searchable_listview, null);
			LinearLayout viewList = (LinearLayout)view.findViewById(R.id.lv_list);
			final EditText tvSearchText = (EditText)view.findViewById(R.id.search_text);
			tvSearchText.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
					listUpdated(cs.toString());
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }

				@Override
				public void afterTextChanged(Editable arg0) {}
			});
			ImageButton ibSearch = (ImageButton)view.findViewById(R.id.btn_search);
			listView = new BaseListView(mActivity, false);
			listUpdated("");

			int colorGrayC;
			int colorGray;
			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorThemeGray2Contrast, R.attr.colorThemeGray2, R.attr.colorIcon});
			colorGrayC = a.getColor(0, Color.GRAY);
			colorGray = a.getColor(1, Color.GRAY);
			colorIcon = a.getColor(2, Color.BLACK);
			a.recycle();
			int newTextSize = ((CoolReader)mActivity).settings().getInt(Settings.PROP_STATUS_FONT_SIZE, 16);

			LinearLayout llUL = (LinearLayout)view.findViewById(R.id.ll_useful_links);

			if (usefulLinks.size()>0) {
				for (Map.Entry<String, String> entry : usefulLinks.entrySet()) {
					//System.out.println(entry.getKey() + " = " + entry.getValue());
					TextView ulText = new TextView(mActivity);
					ulText.setText(entry.getKey());
					ulText.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
					ulText.setTextColor(colorIcon);
					ulText.setPaintFlags(ulText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
					ulText.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(entry.getValue()));
							mActivity.startActivity(myIntent);
						}
					});
					llUL.addView(ulText);
				}
			}  else {
				((ViewGroup)llUL.getParent()).removeView(llUL);
			}

			LinearLayout llQF = (LinearLayout)view.findViewById(R.id.ll_quick_filters);

			if (quickFilters.size()>0) {
				for (String s: quickFilters) {
					Button qfButton = new Button(mActivity);
					qfButton.setText(s);
					qfButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, newTextSize);
					qfButton.setTextColor(colorIcon);
					qfButton.setBackgroundColor(Color.argb(150, Color.red(colorGray), Color.green(colorGray), Color.blue(colorGray)));
					qfButton.setPadding(1, 1, 1, 1);
					LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT);
					llp.setMargins(4, 1, 1, 4);
					qfButton.setLayoutParams(llp);
					qfButton.setMaxLines(1);
					qfButton.setEllipsize(TextUtils.TruncateAt.END);
					llQF.addView(qfButton);
					qfButton.setOnClickListener(new View.OnClickListener() {
						public void onClick(View v) {
							tvSearchText.setText(qfButton.getText());
						}
					});
				}
			} else {
				((ViewGroup)llQF.getParent()).removeView(llQF);
			}
			viewList.addView(listView);
			ibSearch.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					tvSearchText.setText("");
					listUpdated("");
				}
			});
			dlg.setView(view);
			ibSearch.requestFocus();
			//final AlertDialog d = dlg.create();
			listView.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> adapter, View listview,
						int position, long id) {
					Three item = listFiltered.get(position);
					onClick(item);
					dlg.dismiss();
					closed();
				}
			});
			dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override

				public void onDismiss(DialogInterface dialog) {
					String sOldProp = StrUtils.getNonEmptyStr(mProperties.getProperty(property),false);
					String sNewProp = StrUtils.getNonEmptyStr(onSelectDismiss(sOldProp),false);
					if (!sNewProp.equals(sOldProp))
						mProperties.setProperty(property, sNewProp);
				}
			});
			dlg.show();
		}

		public String onSelectDismiss(String propValue) {
			return propValue;
		}

		public Three OnPreClick ( Three item ) {
			return item;
		}
		
		public void onClick( Three item ) {
			mProperties.setProperty(property, OnPreClick(item).value);
			refreshList();
			if ( onChangeHandler!=null )
				onChangeHandler.run();
			if ( optionsListView!=null )
				optionsListView.refresh();
		}
	}

	public static class ListOption2Text extends OptionBase {
		private ArrayList<Three> list = new ArrayList<Three>();
		public ListOption2Text( OptionOwner owner, String label, String property, String addInfo, String filter ) {
			super(owner, label, property, addInfo, filter);
		}
		public void add(String value, String label, String addInfo) {
			list.add( new Three(value, label, addInfo) );
		}
		public ListOption2Text add(String[]values) {
			for ( String item : values ) {
				add(item, item, mActivity.getString(R.string.option_add_info_empty_text));
			}
			return this;
		}
		public ListOption2Text add(double[]values) {
			for ( double item : values ) {
				String s = String.valueOf(item);
				add(s, s, mActivity.getString(R.string.option_add_info_empty_text));
			}
			return this;
		}
		public ListOption2Text add(int[]values) {
			for ( int item : values ) {
				String s = String.valueOf(item);
				add(s, s, mActivity.getString(R.string.option_add_info_empty_text));
			}
			return this;
		}
		public ListOption2Text add(int[]values, int[]labelIDs, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = String.valueOf(values[i]);
				String label = mActivity.getString(labelIDs[i]);
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
			}
			return this;
		}
		public ListOption2Text add(String[]values, int[]labelIDs, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = values[i];
				String label = mActivity.getString(labelIDs[i]);
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
			}
			return this;
		}
		public ListOption2Text add(String[]values, String[]labels, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = values[i];
				String label = labels[i];
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
			}
			return this;
		}
		public ListOption2Text add(int[]values, String[]labels, int[]addInfos) {
			for ( int i=0; i<values.length; i++ ) {
				String value = String.valueOf(values[i]);
				String label = labels[i];
				String addInfo = mActivity.getString(addInfos[i]);
				add(value, label, addInfo);
			}
			return this;
		}
		public ListOption2Text addPercents(int[]values) {
			for ( int item : values ) {
				String s = String.valueOf(item);
				add(s, s + "%", mActivity.getString(R.string.option_add_info_empty_text));
			}
			return this;
		}
		public String findValueLabel( String value ) {
			for ( Three three : list ) {
				if ( value!=null && three.value.equals(value) )
					return three.label;
			}
			return null;
		}
		public int findValue( String value ) {
			if ( value==null )
				return -1;
			for ( int i=0; i<list.size(); i++ ) {
				if ( value.equals(list.get(i).value) )
					return i;
			}
			return -1;
		}

		public int getSelectedItemIndex() {
			return findValue(mProperties.getProperty(property));
		}

		protected void closed() {

		}

		protected int getItemLayoutId(int position, final Three item) {
			return R.layout.option_value;
		}

		@Override
		public View getView(View convertView, ViewGroup parent) {
			View view;
			convertView = myView;
			if ( convertView==null ) {
				//view = new TextView(getContext());
				if (label.contains("~"))
					view = mInflater.inflate(R.layout.option_item_2text, null);
				else view = mInflater.inflate(R.layout.option_item, null);
				if (view != null) {
					TextView label = (TextView) view.findViewById(R.id.option_label);
					if (label != null)
						if (mOwner instanceof OptionsDialog) {
							if (!((((OptionsDialog)mOwner).mFilteredProps).contains(property))) {
								if (!StrUtils.isEmptyStr(property))
									label.setTypeface(null, Typeface.ITALIC);
							}
						}
				}
			} else {
				view = (View)convertView;
			}
			myView = view;
			TextView labelView = (TextView)view.findViewById(R.id.option_label);
			TextView labelView2 = null;
			ImageView icon2 = null;
			if (label.contains("~")) {
				labelView2 = (TextView) view.findViewById(R.id.option_label2);
				icon2 = (ImageView)view.findViewById(R.id.option_icon2);
			}
			TextView valueView = (TextView)view.findViewById(R.id.option_value);
			int colorIcon;
			TypedArray a = mActivity.getTheme().obtainStyledAttributes(new int[]
					{R.attr.colorIcon});
			colorIcon = a.getColor(0, Color.GRAY);
			a.recycle();
			valueView.setTextColor(colorIcon);
			ImageView icon1 = (ImageView)view.findViewById(R.id.option_icon);
			setup2IconView(icon1,icon2);
			ImageView btnOptionAddInfo = (ImageView)view.findViewById(R.id.btn_option_add_info);
			if (addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = view;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							//Toast toast = Toast.makeText(mActivity, addInfo, Toast.LENGTH_LONG);
							//toast.show();
							mActivity.showToast(addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			String lab1 = label;
			String lab2 = "";
			if (lab1.contains("~")) {
				lab2 = lab1.split("~")[1];
				lab1 = lab1.split("~")[0];
			}
			labelView.setText(lab1);
			if (label.contains("~")) labelView2.setText(lab2);
			if (valueView != null) {
				String valueLabel = getValueLabel();
				if (valueLabel != null && valueLabel.length() > 0) {
					valueView.setText(valueLabel);
					valueView.setVisibility(View.VISIBLE);
				} else {
					valueView.setText("");
					valueView.setVisibility(View.INVISIBLE);
				}
			}
			//label = label.replace("~", " / ");
			setupIconView((ImageView)view.findViewById(R.id.option_icon));
			return view;
		}

		protected void updateItemContents( final View layout, final Three item, final ListView listView, final int position ) {
			TextView view;
			RadioButton cb;
			view = (TextView)layout.findViewById(R.id.option_value_text);
			cb = (RadioButton)layout.findViewById(R.id.option_value_check);
			ImageView btnOptionAddInfo = (ImageView)layout.findViewById(R.id.btn_option_add_info);

			if (item.addInfo.trim().equals("")) {
				btnOptionAddInfo.setVisibility(View.INVISIBLE);
			} else {
				btnOptionAddInfo.setImageDrawable(
						mActivity.getResources().getDrawable(Utils.resolveResourceIdByAttr(mActivity,
								R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
				mActivity.tintViewIcons(btnOptionAddInfo);
				final View view1 = layout;
				if (btnOptionAddInfo != null)
					btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							//Toast toast = Toast.makeText(mActivity, item.addInfo, Toast.LENGTH_LONG);
							//toast.show();
							mActivity.showToast(item.addInfo, Toast.LENGTH_LONG, view1, true, 0);
						}
					});
			}
			view.setText(item.label);
			String currValue = mProperties.getProperty(property);
			boolean isSelected = item.value!=null && currValue!=null && item.value.equals(currValue) ;//getSelectedItemIndex()==position;
			cb.setChecked(isSelected);
			cb.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					listView.getOnItemClickListener().onItemClick(listView, listView, position, 0);
				}
			});
		}

		public String getValueLabel() { return findValueLabel(mProperties.getProperty(property)); }

		public void onSelect() {
			if (!enabled)
				return;
			final BaseDialog dlg = new BaseDialog("ListOption2TextDialog", mActivity, label, false, false);

			final ListView listView = new BaseListView(mActivity, false);


			ListAdapter listAdapter = new BaseAdapter() {

				public boolean areAllItemsEnabled() {
					return true;
				}

				public boolean isEnabled(int position) {
					return true;
				}

				public int getCount() {
					return list.size();
				}

				public Object getItem(int position) {
					return list.get(position);
				}

				public long getItemId(int position) {
					return position;
				}

				public int getItemViewType(int position) {
					return 0;
				}

				public View getView(final int position, View convertView,
									ViewGroup parent) {
					ViewGroup layout;
					final Three item = list.get(position);
					if ( convertView==null ) {
						layout = (ViewGroup)mInflater.inflate(getItemLayoutId(position, item), null);
						//view = new TextView(getContext());
					} else {
						layout = (ViewGroup)convertView;
					}
					updateItemContents( layout, item, listView, position );
					//cb.setClickable(false);
//					cb.setOnClickListener(new View.OnClickListener() {
//						@Override
//						public void onClick(View v) {
//
//						}
//					});
					return layout;
				}

				public int getViewTypeCount() {
					return 1;
				}

				public boolean hasStableIds() {
					return true;
				}

				public boolean isEmpty() {
					return list.size()==0;
				}

				private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

				public void registerDataSetObserver(DataSetObserver observer) {
					observers.add(observer);
				}

				public void unregisterDataSetObserver(DataSetObserver observer) {
					observers.remove(observer);
				}

			};
			int selItem = getSelectedItemIndex();
			if ( selItem<0 )
				selItem = 0;
			listView.setAdapter(listAdapter);
			listView.setSelection(selItem);
			dlg.setView(listView);
			//final AlertDialog d = dlg.create();
			listView.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> adapter, View listview,
										int position, long id) {
					Three item = list.get(position);
					onClick(item);
					dlg.dismiss();
					closed();
				}
			});
			dlg.show();
		}

		public void onClick( Three item ) {
			mProperties.setProperty(property, item.value);
			refreshList();
			if ( onChangeHandler!=null )
				onChangeHandler.run();
			if ( optionsListView!=null )
				optionsListView.refresh();
		}
	}

	class DictOptions extends ListOption
	{
		public DictOptions(OptionOwner owner, String label, String addInfo, String filter )
		{
			super( owner, label, PROP_APP_DICTIONARY, addInfo, filter );
			List<DictInfo> dicts = Dictionaries.getDictList(mActivity);
			setDefaultValue(dicts.get(0).id);
			for (DictInfo dict : dicts) {
				boolean installed = mActivity.isPackageInstalled(dict.packageName) || StrUtils.isEmptyStr(dict.packageName);
				String sAdd = mActivity.getString(R.string.options_app_dictionary_not_installed);
				if (StrUtils.isEmptyStr(dict.packageName)) sAdd = "";
				if (((dict.internal==1)||(dict.internal==6)) &&
						(dict.packageName.equals("com.socialnmobile.colordict")) && (!installed)) {
					installed = mActivity.isPackageInstalled("mobi.goldendict.android")||
							mActivity.isPackageInstalled("mobi.goldendict.androie")
							|| StrUtils.isEmptyStr(dict.packageName); // changed package name - 4pda version 2.0.1b7
					String sMinicard="";
					if (dict.internal==6) sMinicard=" (minicard)";
					String sInfo = "";
					if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
						sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
					else sInfo = "Link: " + dict.httpLink;
					add(dict.id, (installed ? "GoldenDict" + sMinicard: dict.name + " " + sAdd),
							sInfo);
				} else {
					String sInfo = "";
					if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
						sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
					else sInfo = "Link: " + dict.httpLink;
					add(dict.id, dict.name + (installed ? "" : " " + sAdd),
							sInfo);
				}
			}
		}

		protected int getItemLayoutId(int position, final Three item) {
			return R.layout.option_value;
		}

		protected void updateItemContents( final View layout, final Three item, final ListView listView, final int position ) {
			super.updateItemContents(layout, item, listView, position);
			ImageView img = (ImageView) layout.findViewById(R.id.option_value_icon);
			List<DictInfo> dicts = Dictionaries.getDictList(mActivity);
			for (DictInfo dict : dicts) {
				if (item.value.equals(dict.id)) {
					if (dict.dicIcon !=0)
						img.setImageDrawable(mActivity.getResources().getDrawable(dict.dicIcon));
                    if (dict.icon != null) {
						Drawable fakeIcon = mActivity.getResources().getDrawable(R.drawable.lingvo);
						final Bitmap bmp = Bitmap.createBitmap(dict.icon.getIntrinsicWidth(), dict.icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
						final Canvas canvas = new Canvas(bmp);
						dict.icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
						dict.icon.draw(canvas);
						Bitmap resizedBitmap = Bitmap.createScaledBitmap(
								bmp, fakeIcon.getIntrinsicWidth(), fakeIcon.getIntrinsicHeight(), false);
						img.setImageBitmap(resizedBitmap);
					}
				}
			}
		}
	}

    class DictOptions2 extends ListOption
    {
        public DictOptions2( OptionOwner owner, String label, String addInfo, String filter )
        {
            super( owner, label, PROP_APP_DICTIONARY_2, addInfo, filter );
            List<DictInfo> dicts = Dictionaries.getDictList(mActivity);
            setDefaultValue(dicts.get(0).id);
            for (DictInfo dict : dicts) {
				boolean installed = mActivity.isPackageInstalled(dict.packageName) || StrUtils.isEmptyStr(dict.packageName);
				String sAdd = mActivity.getString(R.string.options_app_dictionary_not_installed);
				if (((dict.internal==1)||(dict.internal==6)) &&
						(dict.packageName.equals("com.socialnmobile.colordict")) && (!installed)) {
					installed = mActivity.isPackageInstalled("mobi.goldendict.android")||
							mActivity.isPackageInstalled("mobi.goldendict.androie"); // changed package name - 4pda version 2.0.1b7
					String sMinicard="";
					if (dict.internal==6) sMinicard=" (minicard)";
					String sInfo = "";
					if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
						sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
					else sInfo = "Link: " + dict.httpLink;
					add(dict.id, (installed ? "GoldenDict" + sMinicard: dict.name + " " + sAdd),
							sInfo);
				} else {
					String sInfo = "";
					if (!((StrUtils.isEmptyStr(dict.packageName))&&(StrUtils.isEmptyStr(dict.className))))
						sInfo = "Package: " + dict.packageName + "; \nclass: " + dict.className;
					else sInfo = "Link: " + dict.httpLink;
					add(dict.id, dict.name + (installed ? "" : " " + sAdd),sInfo);
				}
            }
        }

		protected int getItemLayoutId(int position, final Three item) {
			return R.layout.option_value;
		}

		protected void updateItemContents( final View layout, final Three item, final ListView listView, final int position ) {
			super.updateItemContents(layout, item, listView, position);
			ImageView img = (ImageView) layout.findViewById(R.id.option_value_icon);
			List<DictInfo> dicts = Dictionaries.getDictList(mActivity);
			for (DictInfo dict : dicts) {
				if (item.value.equals(dict.id)) {
					if (dict.dicIcon !=0) {
						img.setVisibility(View.VISIBLE);
						img.setImageDrawable(mActivity.getResources().getDrawable(dict.dicIcon));
					}
					if (dict.icon != null) {
						Drawable fakeIcon = mActivity.getResources().getDrawable(R.drawable.lingvo);
						final Bitmap bmp = Bitmap.createBitmap(dict.icon.getIntrinsicWidth(), dict.icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
						final Canvas canvas = new Canvas(bmp);
						dict.icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
						dict.icon.draw(canvas);
						Bitmap resizedBitmap = Bitmap.createScaledBitmap(
								bmp, fakeIcon.getIntrinsicWidth(), fakeIcon.getIntrinsicHeight(), false);
						img.setImageBitmap(resizedBitmap);
					}
				}
			}
		}
    }
	
	class HyphenationOptions extends ListOption
	{
		public HyphenationOptions( OptionOwner owner, String label, String addInfo, String filter )
		{
			super( owner, label, PROP_HYPHENATION_DICT, addInfo, filter );
			setDefaultValue(Engine.HyphDict.RUSSIAN.name);
			Engine.HyphDict[] dicts = Engine.HyphDict.values();
			for ( Engine.HyphDict dict : dicts ) {
				String ainfo = getString(R.string.option_add_info_empty_text);
				if (dict.file!=null) ainfo = "Language: " + dict.language + "; file: " +
						dict.file;
				if (!dict.hide)
					add(dict.toString(), dict.getName(), ainfo);
			};
		}
	}
	
	class ThemeOptions extends ListOption
	{
		public ThemeOptions( OptionOwner owner, String label, String addInfo, String filter )
		{
			super( owner, label, PROP_APP_THEME, addInfo, filter );
			setDefaultValue(DeviceInfo.isForceHCTheme(BaseActivity.getScreenForceEink()) ? "WHITE" : "GRAY1");
			for (InterfaceTheme theme : InterfaceTheme.allThemes)
				add(theme.getCode(), getString(theme.getDisplayNameResourceId()), getString(R.string.option_add_info_empty_text));
		}
	}
	
	class ThumbnailCache {
		final int maxcount;
		final int dx;
		final int dy;
		class Item {
			Drawable drawable;
			Bitmap bmp;
			String path;
			int id;
			public void clear() {
				if ( bmp!=null ) {
					//bmp.recycle();
					bmp = null;
				}
				if ( drawable!=null )
					drawable = null;
			}
		}
		ArrayList<Item> list = new ArrayList<Item>(); 
		public ThumbnailCache( int dx, int dy, int maxcount ) {
			this.dx = dx;
			this.dy = dy;
			this.maxcount = maxcount;
		}
		private void remove( int maxsize ) {
			while ( list.size()>maxsize ) {
				Item item = list.remove(0);
				item.clear();
			}
		}
		private Drawable createDrawable( String path ) {
			File f = new File(path);
			if ( !f.isFile() || !f.exists() )
				return null;
			try { 
				BitmapDrawable drawable = (BitmapDrawable)BitmapDrawable.createFromPath(path);
				if ( drawable==null )
					return null;
				Bitmap src = drawable.getBitmap();
				Bitmap bmp = Bitmap.createScaledBitmap(src, dx, dy, true);
				//Canvas canvas = new Canvas(bmp);
				BitmapDrawable res = new BitmapDrawable(bmp);
				//src.recycle();
				Item item = new Item();
				item.path = path;
				item.drawable = res; //drawable;
				item.bmp = bmp;
				list.add(item);
				remove(maxcount);
				return drawable;
			} catch ( Exception e ) {
				return null;
			}
		}
		private Drawable createDrawable( int resourceId ) {
			try { 
				//Drawable drawable = mReaderView.getActivity().getResources().getDrawable(resourceId);
				InputStream is = getContext().getResources().openRawResource(resourceId);
				if ( is==null )
					return null;
				BitmapDrawable src = new BitmapDrawable(is);
				Item item = new Item();
				item.id = resourceId;
				Bitmap bmp = Bitmap.createScaledBitmap(src.getBitmap(), dx, dy, true);
				BitmapDrawable res = new BitmapDrawable(bmp);
				item.drawable = res;
				item.bmp = bmp;
				list.add(item);
				remove(maxcount);
				return res;
			} catch ( Exception e ) {
				return null;
			}
		}
		public Drawable getImage( String path ) {
			if ( path==null || !path.startsWith("/"))
				return null;
			// find existing
			for ( int i=0; i<list.size(); i++ ) {
				if ( list.get(i).path!=null && path.equals(list.get(i).path) ) {
					Item item = list.remove(i);
					list.add(item);
					return item.drawable;
				}
			}
			return createDrawable( path ); 
		}
		public Drawable getImage( int resourceId ) {
			if ( resourceId==0 )
				return null;
			// find existing
			for ( int i=0; i<list.size(); i++ ) {
				if ( list.get(i).id == resourceId ) {
					Item item = list.remove(i);
					list.add(item);
					return item.drawable;
				}
			}
			return createDrawable( resourceId ); 
		}
		public void clear() {
			remove(0);
		}
	}
	
	ThumbnailCache textureSampleCache = new ThumbnailCache(64, 64, 100);
	
	class TextureOptions extends ListOption
	{
		public TextureOptions( OptionOwner owner, String label, String addInfo, String filter )
		{
			super( owner, label, PROP_PAGE_BACKGROUND_IMAGE, addInfo, filter );
			setDefaultValue("(NONE)");
			BackgroundTextureInfo[] textures = Services.getEngine().getAvailableTextures();
			for ( BackgroundTextureInfo item : textures )
				add( item.id, item.name, item.id);
		}

		protected void closed() {
			textureSampleCache.clear();
		}

		protected int getItemLayoutId(int position, final Three item) {
			return R.layout.option_value_image; 
		}

		@Override
		public Three OnPreClick ( Three item ) {
			File f = new File(item.value);
			if (f.exists()) return item;
			File f1 = new File(item.value.replace("/textures/", "/backgrounds/"));
			if (f1.exists()) {
				item.value = item.value.replace("/textures/", "/backgrounds/");
				return item;
			}
			File f2 = new File(item.value.replace("/backgrounds/", "/textures/"));
			if (f2.exists()) {
				item.value = item.value.replace("/backgrounds/", "/textures/");
				return item;
			}
			return item;
		}

		@Override
		public String onSelectDismiss(String propValue) {
			File f = new File(propValue);
			if (f.exists()) return propValue;
			File f1 = new File(propValue.replace("/textures/", "/backgrounds/"));
			if (f1.exists()) {
				propValue = propValue.replace("/textures/", "/backgrounds/");
				return propValue;
			}
			File f2 = new File(propValue.replace("/backgrounds/", "/textures/"));
			if (f2.exists()) {
				propValue = propValue.replace("/backgrounds/", "/textures/");
				return propValue;
			}
			return propValue;
		}

		protected void updateItemContents( final View layout, final Three item, final ListView listView, final int position ) {
			super.updateItemContents(layout, item, listView, position);
			ImageView img = (ImageView)layout.findViewById(R.id.option_value_image);
            ImageView imgT = (ImageView)layout.findViewById(R.id.option_value_type);
			ImageView imgAddInfo = (ImageView)layout.findViewById(R.id.btn_option_add_info);
			ImageView imgDel = (ImageView)layout.findViewById(R.id.option_value_del);
			RadioButton cb = (RadioButton)layout.findViewById(R.id.option_value_check);
			if (StrUtils.isEmptyStr(item.addInfo)) {
				imgAddInfo.setVisibility(View.INVISIBLE);
				imgDel.setVisibility(View.INVISIBLE);
			}
			else {
				File f = new File(item.addInfo);
				if (!f.exists()) {
					imgAddInfo.setVisibility(View.INVISIBLE);
					imgDel.setVisibility(View.INVISIBLE);
				}
				else {
					imgAddInfo.setVisibility(View.VISIBLE);
					imgDel.setVisibility(View.VISIBLE);
				}
			}
			if (cb.isChecked()) imgDel.setVisibility(View.INVISIBLE);
			imgDel.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (cb.isChecked()) {
						return;
					}
					String sfile = item.addInfo;
					File f = new File(sfile);
					if (!f.exists()) return;
					mActivity.askConfirmation(R.string.delete_texture, new Runnable() {
						@Override
						public void run() {
							if (f.delete()) {
								mActivity.showToast(R.string.texture_deleted);
								Three forRemove = null;
								for (Three t: list)
									if (t.addInfo.equals(item.addInfo)) forRemove = t;
								if (forRemove != null) list.remove(forRemove);
								forRemove = null;
								for (Three t: listFiltered)
									if (t.addInfo.equals(item.addInfo)) forRemove = t;
								if (forRemove != null) listFiltered.remove(forRemove);
								listUpdated("");
							}
						}
					});
				}
			});
			int cl = mProperties.getColor(PROP_BACKGROUND_COLOR, Color.WHITE);
			final BackgroundTextureInfo texture = Services.getEngine().getTextureInfoById(item.value);
			img.setBackgroundColor(cl);
			if (texture.tiled) imgT.setImageResource(Utils.resolveResourceIdByAttr(activity,
                    R.attr.attr_icons8_texture, R.drawable.icons8_texture));
			else
                imgT.setImageResource(Utils.resolveResourceIdByAttr(activity,
                        R.attr.attr_icons8_fullscreen, R.drawable.icons8_fullscreen));
			activity.tintViewIcons(imgT,true);
			if (imgAddInfo != null) activity.tintViewIcons(imgAddInfo,true);
			if (imgDel != null) activity.tintViewIcons(imgDel,true);
			imgT.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(final View v) {
					final BackgroundTextureInfo texture = Services.getEngine().getTextureInfoById(item.value);
					if ((texture.resourceId == 0)&&(!texture.id.equals(BackgroundTextureInfo.NO_TEXTURE_ID))) {
						activity.askConfirmation(R.string.texture_switch_mode, new Runnable() {
							@Override
							public void run() {
								ArrayList<String> tDirs = Engine.getDataDirsExt(Engine.DataDirType.TexturesDirs, true);
								tDirs = Engine.getDataDirsExt(Engine.DataDirType.BackgroundsDirs, true);
								File f = new File(texture.id);
								String sNewName = texture.id;
								if (texture.id.contains("/textures/"))
									sNewName = sNewName.replace("/textures/", "/backgrounds/");
								else
									sNewName = sNewName.replace("/backgrounds/", "/textures/");
								File fTo = new File(sNewName);
								if (!f.renameTo(fTo))
									activity.showToast(activity.getString(R.string.pic_problem));
								else {
									BackgroundTextureInfo[] textures = Services.getEngine().getAvailableTextures();
									ArrayList<Three> listTemp = new ArrayList<Three>();
									for (Three th : list) {
										if (th.value.equals(texture.id))
											th.value=sNewName;
										listTemp.add(th);
									}
									list=listTemp;
									listTemp = new ArrayList<Three>();
									for (Three th : listFiltered) {
										if (th.value.equals(texture.id))
											th.value=sNewName;
										listTemp.add(th);
									}
									listFiltered = listTemp;
									if (!texture.tiled) ((ImageView)v).setImageResource(Utils.resolveResourceIdByAttr(activity,
											R.attr.attr_icons8_texture, R.drawable.icons8_texture));
									else
										((ImageView)v).setImageResource(Utils.resolveResourceIdByAttr(activity,
												R.attr.attr_icons8_fullscreen, R.drawable.icons8_fullscreen));
									mActivity.tintViewIcons(v);
									listAdapter.notifyDataSetChanged();
									listAdapter.notifyDataSetInvalidated();
								}
							}
						});
					}
				}
			});
			if ( texture.resourceId!=0 ) {
				Drawable drawable = textureSampleCache.getImage(texture.resourceId);
				if ( drawable!=null ) {
					img.setImageResource(0);
					img.setImageDrawable(drawable);
					img.setBackgroundColor(Color.TRANSPARENT);
				} else {
					img.setBackgroundColor(cl);
					img.setImageResource(0);
					img.setImageDrawable(null);
				}
			} else {
				// load image from file
				Drawable drawable = textureSampleCache.getImage(texture.id);
				if ( drawable!=null ) {
					img.setImageResource(0);
					img.setImageDrawable(drawable);
					img.setBackgroundColor(Color.TRANSPARENT);
				} else {
					img.setBackgroundColor(cl);
					img.setImageResource(0);
					img.setImageDrawable(null);
				}
			}
		}
	}

	class FontsOptions extends ListOption
	{
		ArrayList<String> faces = new ArrayList<String>();
		ArrayList<String> faceValues = new ArrayList<String>();

		public FontsOptions( OptionOwner owner, String label, String property, String addInfo, boolean bForCSS, String filter )
		{
			super( owner, label, property, addInfo, filter );
			HashMap<String,ArrayList<String>> fontFiles = new HashMap<String,ArrayList<String>>();
			if (bForCSS) {
				faces.add("-");
				faceValues.add("");
				faces.add(getString(R.string.options_css_font_face_sans_serif));
				faceValues.add("font-family: sans-serif");
				faces.add(getString(R.string.options_css_font_face_serif));
				faceValues.add("font-family: serif");
				faces.add(getString(R.string.options_css_font_face_monospace));
				faceValues.add("font-family: \"Courier New\", \"Courier\", monospace");
			};
			usefulLinks.put("Google Fonts", "https://fonts.google.com/");
			usefulLinks.put("Paratype PT fonts", "http://rus.paratype.ru/pt-sans-pt-serif");
			for (String face : mFontFacesFiles) {
				String sFace = face;
				String sFile = "";
				if (face.contains("~")) {
					sFace = face.split("~")[0];
					sFile = face.split("~")[1];
				}
				if ((!StrUtils.isEmptyStr(sFace))&&(!StrUtils.isEmptyStr(sFile))) {
					if (sFile.contains("/")) {
						String s = sFile.substring(0,sFile.lastIndexOf("/"));
						if (!quickFilters.contains(s)) quickFilters.add(s);
					} else
					if (sFile.contains("\\")) {
						String s = sFile.substring(0,sFile.lastIndexOf("\\"));
						if (!quickFilters.contains(s)) quickFilters.add(s);
					}
					if (fontFiles.get(sFace) == null) {
						ArrayList<String> alFiles = new ArrayList<String>();
						alFiles.add(sFile);
						fontFiles.put(sFace,alFiles);
					} else {
						ArrayList<String> alFiles = fontFiles.get(sFace);
						alFiles.add(sFile);
						fontFiles.put(sFace,alFiles);
					}
				}
				if (!faces.contains(sFace)) {
					faces.add(sFace);
					faceValues.add((bForCSS ? "font-family: " : "") + sFace);
				}
			}
			int i;
			for (i = 0; i < faces.size(); i++) {
				int iFilesExists = -1;
				String addI = "";
				if (fontFiles.get(faces.get(i)) != null) {
					iFilesExists = 0;
					for (String s: fontFiles.get(faces.get(i))) {
						File f = new File(s);
						if (f.exists()) {
							iFilesExists++;
							addI = addI + "~" + s;
						}
					}
				}
				if (!StrUtils.isEmptyStr(addI)) addI = addI.substring(1);
				if (iFilesExists != 0) add(faceValues.get(i), faces.get(i), addI);
			}
			if (faces.size()>0) setDefaultValue(faceValues.get(0));
		}

		protected void closed() {

		}

		protected int getItemLayoutId(int position, final Three item) {
            return R.layout.option_value_fonts;
		}

		private void addItem(TableLayout table, Three item, final String addInfo, final String testPhrase) {
			TableRow tableRow = (TableRow)mInflater.inflate(R.layout.font_option_item, null);
			ImageView btnOptionAddInfo = null;
			btnOptionAddInfo = (ImageView)tableRow.findViewById(R.id.btn_option_add_info1);
			if ((btnOptionAddInfo!=null)&&(StrUtils.isEmptyStr(addInfo))) btnOptionAddInfo.setVisibility(View.INVISIBLE);
			else
				btnOptionAddInfo.setImageDrawable(
					activity.getResources().getDrawable(Utils.resolveResourceIdByAttr(activity,
						R.attr.attr_icons8_option_info, R.drawable.icons8_ask_question)));
			mActivity.tintViewIcons(btnOptionAddInfo);
			if (btnOptionAddInfo != null) {
				btnOptionAddInfo.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mActivity.showToast(addInfo, Toast.LENGTH_LONG, v, true, 0);
					}
				});
			}
			TextView txt1 = (TextView)tableRow.findViewById(R.id.option_value_add_text1);
			txt1.setText(testPhrase);
			Typeface tf = null;
			try {
				if (StrUtils.isEmptyStr(addInfo))
					tf = Typeface.create(item.value.replaceAll("font-family: ", ""), Typeface.NORMAL);
				else
					tf = Typeface.createFromFile(addInfo);
				txt1.setTypeface(tf);
			} catch (Exception e) {

			}
			table.addView(tableRow);
		}

		protected void updateItemContents( final View layout, final Three item, final ListView listView, final int position ) {
			super.updateItemContents(layout, item, listView, position);
			RadioButton cb = (RadioButton)layout.findViewById(R.id.option_value_check);
			ImageView iv = (ImageView)layout.findViewById(R.id.option_value_del);
			if (cb.isChecked()) iv.setVisibility(View.INVISIBLE);
			else {
				String[] arrFonts = item.addInfo.split("~");
				int iFilesCnt = 0;
				for (String s: arrFonts) {
					if ((!s.startsWith("/system"))&&(!StrUtils.isEmptyStr(s))) {
						File f = new File(s);
						if (f.exists()) iFilesCnt++;
					}
				}
				if (iFilesCnt == 0) iv.setVisibility(View.INVISIBLE);
			};
			iv.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (cb.isChecked()) {
						return;
					}
					String[] arrFonts = item.addInfo.split("~");
					int iFilesCnt = 0;
					for (String s: arrFonts) {
						if ((!s.startsWith("/system"))&&(!StrUtils.isEmptyStr(s))) {
							File f = new File(s);
							if (f.exists()) iFilesCnt++;
						}
					}
					if (iFilesCnt>0) {
						final int iFilesCntF = iFilesCnt;
						mActivity.askConfirmation(R.string.delete_font, new Runnable() {
							@Override
							public void run() {
								int iFilesDeleted = 0;
								for (String s: arrFonts) {
									if (!s.startsWith("/system")) {
										File f = new File(s);
										if (f.exists())
											if (f.delete()) iFilesDeleted++;
									}
								}
								if (iFilesDeleted != iFilesCntF) {
									mActivity.showToast(mActivity.getString(R.string.fonts_deleted_partial,
											String.valueOf(iFilesDeleted),String.valueOf(iFilesCntF)));
								} else {
									mActivity.showToast(R.string.fonts_deleted_full);
									Three forRemove = null;
									for (Three t: list)
										if (t.label.equals(item.label)) forRemove = t;
									if (forRemove != null) list.remove(forRemove);
									forRemove = null;
									for (Three t: listFiltered)
										if (t.label.equals(item.label)) forRemove = t;
									if (forRemove != null) listFiltered.remove(forRemove);
									listUpdated("");
								}
							}
						});
					} else {
						mActivity.showToast(R.string.non_system_fonts_not_found);
					}
				}
			});
			mActivity.tintViewIcons(iv,true);
			TableLayout table = (TableLayout)layout.findViewById(R.id.table_add_text1);
			table.removeAllViews();
			String[] sAdd = {""};
			if (!StrUtils.isEmptyStr(item.addInfo)) sAdd = item.addInfo.split("~");
			String sAdd1 = "";
			String sAdd2 = "";
			String sAdd3 = "";
			if (sAdd.length>0) sAdd1=sAdd[0];
			if (sAdd.length>1) sAdd2=sAdd[1];
			if (sAdd.length>2) sAdd3=sAdd[2];
			String s = mActivity.getCurrentLanguage();
			if (s.toUpperCase().startsWith("RU")) {
				int r1 = (int)(Math.random()*((FontsPangramms.RUSSIAN.length-1)+1));
				int r2 = (int)(Math.random()*((FontsPangramms.RUSSIAN.length-1)+1));
				int r3 = (int)(Math.random()*((FontsPangramms.RUSSIAN.length-1)+1));
				addItem(table, item, sAdd1, FontsPangramms.RUSSIAN[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.RUSSIAN[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.RUSSIAN[r3]);
			} else
			if (s.toUpperCase().startsWith("DE")) {
				int r1 = (int)(Math.random()*((FontsPangramms.GERMAN.length-1)+1));
				int r2 = (int)(Math.random()*((FontsPangramms.GERMAN.length-1)+1));
				int r3 = (int)(Math.random()*((FontsPangramms.GERMAN.length-1)+1));
				addItem(table, item, sAdd1, FontsPangramms.GERMAN[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.GERMAN[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.GERMAN[r3]);
			} else
			if (s.toUpperCase().startsWith("EN")) {
				int r1 = (int)(Math.random()*((FontsPangramms.ENGLISH.length-1)+1));
				int r2 = (int)(Math.random()*((FontsPangramms.ENGLISH.length-1)+1));
				int r3 = (int)(Math.random()*((FontsPangramms.ENGLISH.length-1)+1));
				addItem(table, item, sAdd1, FontsPangramms.ENGLISH[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.ENGLISH[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.ENGLISH[r3]);
			} else
			if (s.toUpperCase().startsWith("ES")) {
				int r1 = (int)(Math.random()*((FontsPangramms.SPAIN.length-1)+1));
				int r2 = (int)(Math.random()*((FontsPangramms.SPAIN.length-1)+1));
				int r3 = (int)(Math.random()*((FontsPangramms.SPAIN.length-1)+1));
				addItem(table, item, sAdd1, FontsPangramms.SPAIN[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.SPAIN[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.SPAIN[r3]);
			} else
			if (s.toUpperCase().startsWith("NL")) {
				int r1 = (int)(Math.random()*((FontsPangramms.DUTCH.length-1)+1));
				int r2 = (int)(Math.random()*((FontsPangramms.DUTCH.length-1)+1));
				int r3 = (int)(Math.random()*((FontsPangramms.DUTCH.length-1)+1));
				addItem(table, item, sAdd1, FontsPangramms.DUTCH[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.DUTCH[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.DUTCH[r3]);
			}  else
			if (s.toUpperCase().startsWith("CS")) {
				int r1 = (int)(Math.random()*((FontsPangramms.CH.length-1)+1));
				int r2 = (int)(Math.random()*((FontsPangramms.CH.length-1)+1));
				int r3 = (int)(Math.random()*((FontsPangramms.CH.length-1)+1));
				addItem(table, item, sAdd1, FontsPangramms.CH[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.CH[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.CH[r3]);
			} else
			if (s.toUpperCase().startsWith("FR")) {
				int r1 = (int)(Math.random()*((FontsPangramms.FRENCH.length-1)+1));
				int r2 = (int)(Math.random()*((FontsPangramms.FRENCH.length-1)+1));
				int r3 = (int)(Math.random()*((FontsPangramms.FRENCH.length-1)+1));
				addItem(table, item, sAdd1, FontsPangramms.FRENCH[r1]);
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, FontsPangramms.FRENCH[r2]);
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, FontsPangramms.FRENCH[r3]);
			} else {
				addItem(table, item, sAdd1, getString(R.string.font_test_phrase));
				if (!StrUtils.isEmptyStr(sAdd2))
					addItem(table, item, sAdd2, getString(R.string.font_test_phrase2));
				if (!StrUtils.isEmptyStr(sAdd3))
					addItem(table, item, sAdd3, getString(R.string.font_test_phrase3));
			}
		}
	}
	
	//byte[] fakeLongArrayForDebug;
	
	public enum Mode {
		READER,
		BROWSER,
	}

	public static ArrayList<String> filterProfileSettings(Properties settings) {
		ArrayList<String> props = new ArrayList<String>();
		for (Object k : settings.keySet()) {
			String key = (String)k;
			boolean found = false;
			for (String pattern : Settings.PROFILE_SETTINGS) {
				if (pattern.endsWith("*")) {
					if (key.startsWith(pattern.substring(0, pattern.length()-1))) {
						found = true;
						break;
					}
				} else if (pattern.equalsIgnoreCase(key)) {
					found = true;
					break;
				} else if (key.startsWith("styles.")) {
					found = true;
					break;
				}
			}
			if (found) props.add(key);
		}
		return props;
	}

	public OptionsDialog(BaseActivity activity, ReaderView readerView, String[] fontFaces, String[] fontFacesFiles, Mode mode)
	{
		super("OptionsDialog", activity, null, false, false);
		String filter = "";
		if (activity instanceof CoolReader) filter = ((CoolReader)activity).optionsFilter;
		File f = activity.getSettingsFile(activity.getCurrentProfile());
		String sF = f.getAbsolutePath();
		sF = sF.replace("/storage/","/s/").replace("/emulated/","/e/");
		if (!filter.trim().equals("")) sF = sF + "\n"+getString(R.string.mi_filter_option) + ": "+filter;
		String sprof = activity.getCurrentProfileName();
		if (!StrUtils.isEmptyStr(sprof)) sprof = sprof + " - ";
		this.upperText = sprof + sF;
		mActivity = activity;
		mReaderView = readerView;
		mFontFaces = fontFaces;
		mFontFacesFiles = fontFacesFiles;
		mProperties = new Properties(mActivity.settings()); //  readerView.getSettings();
		mFilteredProps = filterProfileSettings(mProperties);
		mOldProperties = new Properties(mProperties);
		if (mode == Mode.READER) {
			if (mReaderView != null) {
				mProperties.setBool(PROP_TXT_OPTION_PREFORMATTED, mReaderView.isTextAutoformatEnabled());
				mProperties.setBool(PROP_EMBEDDED_STYLES, mReaderView.getDocumentStylesEnabled());
				mProperties.setBool(PROP_EMBEDDED_FONTS, mReaderView.getDocumentFontsEnabled());
				mProperties.setInt(PROP_REQUESTED_DOM_VERSION, mReaderView.getDOMVersion());
				mProperties.setInt(PROP_RENDER_BLOCK_RENDERING_FLAGS, mReaderView.getBlockRenderingFlags());
				isTextFormat = readerView.isTextFormat();
				isEpubFormat = readerView.isFormatWithEmbeddedFonts();
				isHtmlFormat = readerView.isHtmlFormat();
			}
		}
		showIcons = mProperties.getBool(PROP_APP_SETTINGS_SHOW_ICONS, true);
		this.mode = mode;
	}
	
	class OptionsListView extends BaseListView {
		public ArrayList<OptionBase> mOptions = new ArrayList<OptionBase>();
		public ArrayList<OptionBase> mOptionsFiltered = new ArrayList<OptionBase>();
		public ArrayList<OptionBase> mOptionsThis;
		private ListAdapter mAdapter;
		private OptionBase root;
		public void refresh()
		{
			//setAdapter(mAdapter);
			for ( OptionBase item : mOptions ) {
				item.refreshItem();
			}
			invalidate();
		}
		public OptionsListView add( OptionBase option ) {
			if ((option.lastFiltered)||(root != null)) {
				mOptions.add(option);
				option.optionsListView = this;
				if (root != null) root.updateFilteredMark(option.lastFiltered);
			}
			return this;
		}

		public OptionsListView addExt( OptionBase option, String addWords ) {
			if (!addWords.equals("")) {
				for (String s: addWords.split("\\,")) option.updateFilteredMark(s);
			}
			option.updateFilteredMark(addWords);
			if ((option.lastFiltered)||(root != null)) {
				mOptions.add(option);
				option.optionsListView = this;
				if (root != null) root.updateFilteredMark(option.lastFiltered);
			}
			return this;
		}

		public void listUpdated(String sText) {
			mOptionsFiltered.clear();
			for(int i=0;i<mOptions.size();i++){
				if (
						((mOptions.get(i).label.toLowerCase()).contains(sText.toLowerCase()))||
						((mOptions.get(i).property.toLowerCase()).contains(sText.toLowerCase()))||
						((mOptions.get(i).addInfo.toLowerCase()).contains(sText.toLowerCase()))
						) {
					mOptionsFiltered.add(mOptions.get(i));
				}
			}
			mOptionsThis = mOptionsFiltered;
			if ((sText.equals(""))&&(mOptions.size()==0)) mOptionsThis = mOptions;
			mAdapter = new BaseAdapter() {
				public boolean areAllItemsEnabled() {
					return false;
				}

				public boolean isEnabled(int position) {
					return true;
				}

				public int getCount() {
					return mOptionsThis.size();
				}

				public Object getItem(int position) {
					return mOptionsThis.get(position);
				}

				public long getItemId(int position) {
					return position;
				}

				public int getItemViewType(int position) {
//					OptionBase item = mOptions.get(position);
//					return item.getItemViewType();
					return position;
				}


				public View getView(int position, View convertView, ViewGroup parent) {
					OptionBase item = mOptionsThis.get(position);
					return item.getView(convertView, parent);
				}

				public int getViewTypeCount() {
					//return OPTION_VIEW_TYPE_COUNT;
					return mOptionsThis.size() > 0 ? mOptionsThis.size() : 1;
				}

				public boolean hasStableIds() {
					return true;
				}

				public boolean isEmpty() {
					return mOptionsThis.size()==0;
				}

				private ArrayList<DataSetObserver> observers = new ArrayList<DataSetObserver>();

				public void registerDataSetObserver(DataSetObserver observer) {
					observers.add(observer);
				}

				public void unregisterDataSetObserver(DataSetObserver observer) {
					observers.remove(observer);
				}
			};
			setAdapter(mAdapter);
		}

		public OptionsListView( Context context, OptionBase root )
		{
			super(context, false);
			setFocusable(true);
			setFocusableInTouchMode(true);
			this.root = root;
			listUpdated("");
		}
		@Override
		public boolean performItemClick(View view, int position, long id) {
			if (mOptionsThis!=null) {
				mOptionsThis.get(position).onSelect();
			}
			else {
				mOptions.get(position).onSelect();
			}
			return true;
		}
		
	}
	
	public View createTabContent(String tag) {
		if ( "App".equals(tag) )
			return mOptionsApplication;
		else if ( "Styles".equals(tag) )
			return mOptionsStyles;
		else if ( "CSS".equals(tag) )
			return mOptionsCSS;
		else if ( "Controls".equals(tag) )
			return mOptionsControls;
		else if ( "Page".equals(tag))
			return mOptionsPage;
		return null;
	}

	private String getString( int resourceId )
	{
		return getContext().getResources().getString(resourceId); 
	}

	class StyleEditorOption extends SubmenuOption {
		
		private final String prefix;
		
		public StyleEditorOption( OptionOwner owner, String label, String prefix, String addInfo, String filter ) {
			super(owner, label, "dummy.prop", addInfo, filter);
			this.prefix = prefix;
		}

		public void onSelect() {
			SelectOrFilter(true);
		}

		public boolean updateFilterEnd() {
			SelectOrFilter(false);
			return this.lastFiltered;
		}

		public void SelectOrFilter(boolean isSelect) {
			BaseDialog dlg = null;
			if (isSelect) {
				dlg = new BaseDialog("StyleEditorDialog", mActivity, label, false, false);
			}
			OptionsListView listView = new OptionsListView(getContext(), this);
			String[] firstLineOptions = {"", "text-align: justify", "text-align: left", "text-align: center", "text-align: right", };
			int empty = R.string.option_add_info_empty_text;
			int[] addInfos = {empty, empty, empty, empty, empty, };
			int[] firstLineOptionNames = {
					R.string.options_css_inherited,
					R.string.options_css_text_align_justify,
					R.string.options_css_text_align_left,
					R.string.options_css_text_align_center,
					R.string.options_css_text_align_right,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_text_align), prefix + ".align",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(firstLineOptions,
					firstLineOptionNames, addInfos).setIconIdByAttr(R.attr.cr3_option_text_align_drawable, R.drawable.cr3_option_text_align));
			
			String[] identOptions = {"", // inherited
			        "text-indent: 0em",
			        "text-indent: 1.2em",
			        "text-indent: 2em",
			        "text-indent: -1.2em",
			        "text-indent: -2em"};
			int[] identOptionNames = {
					R.string.options_css_inherited,
					R.string.options_css_text_indent_no_indent,
					R.string.options_css_text_indent_small_indent,
					R.string.options_css_text_indent_big_indent,
					R.string.options_css_text_indent_small_outdent,
					R.string.options_css_text_indent_big_outdent};
			int[] addInfosI = {empty, empty, empty, empty, empty, empty};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_text_indent), prefix + ".text-indent",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(identOptions,
					identOptionNames, addInfosI).setIconIdByAttr(R.attr.cr3_option_text_indent_drawable, R.drawable.cr3_option_text_indent));

			listView.add(new FontsOptions(mOwner, getString(R.string.options_css_font_face), prefix + ".font-face",
					getString(R.string.option_add_info_empty_text), true, this.lastFilteredValue).
					setIconIdByAttr(R.attr.cr3_option_font_face_drawable, R.drawable.cr3_option_font_face));
			
		    String[] fontSizeStyles = {
		        "", // inherited
		        "font-size: 110%",
		        "font-size: 120%",
		        "font-size: 150%",
		        "font-size: 90%",
		        "font-size: 80%",
		        "font-size: 70%",
		        "font-size: 60%",
		    };
		    int[] fontSizeStyleNames = {
			    R.string.options_css_inherited,
			    R.string.options_css_font_size_110p,
			    R.string.options_css_font_size_120p,
			    R.string.options_css_font_size_150p,
			    R.string.options_css_font_size_90p,
			    R.string.options_css_font_size_80p,
			    R.string.options_css_font_size_70p,
			    R.string.options_css_font_size_60p,
		    };
			int[] fontSizeStyleAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_font_size), prefix + ".font-size",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(fontSizeStyles, fontSizeStyleNames, fontSizeStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size));

		    String[] fontWeightStyles = {
		        "", // inherited
		        "font-weight: normal",
		        "font-weight: bold",
		        "font-weight: bolder",
		        "font-weight: lighter",
		    };
		    int[] fontWeightStyleNames = {
		        R.string.options_css_inherited,
		        R.string.options_css_font_weight_normal,
		        R.string.options_css_font_weight_bold,
		        R.string.options_css_font_weight_bolder,
		        R.string.options_css_font_weight_lighter,
		    };
			int[] fontWeightStyleAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_font_weight), prefix + ".font-weight",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(fontWeightStyles, fontWeightStyleNames, fontWeightStyleAddInfos).setIconIdByAttr(
							R.attr.cr3_option_text_bold_drawable, R.drawable.cr3_option_text_bold));

		    String[] fontStyleStyles = {
		        "", // inherited
		        "font-style: normal",
		        "font-style: italic",
		    };
		    int[] fontStyleStyleNames = {
		    	R.string.options_css_inherited,
		    	R.string.options_css_font_style_normal,
		    	R.string.options_css_font_style_italic,
		    };
			int[] fontStyleStyleAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_font_style), prefix + ".font-style",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(fontStyleStyles, fontStyleStyleNames, fontStyleStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_text_italic_drawable, R.drawable.cr3_option_text_italic));

		    String[] lineHeightStyles = {
			        "", // inherited
			        "line-height: 75%",
			        "line-height: 80%",
			        "line-height: 85%",
			        "line-height: 90%",
			        "line-height: 95%",
			        "line-height: 100%",
			        "line-height: 110%",
			        "line-height: 120%",
			        "line-height: 130%",
			        "line-height: 140%",
			        "line-height: 150%",
			    };
		    String[] lineHeightStyleNames = {
			        "-",
			        "75%",
			        "80%",
			        "85%",
			        "90%",
			        "95%",
			        "100%",
			        "110%",
			        "120%",
			        "130%",
			        "140%",
			        "150%",
			    };
			int[] lineHeightStyleAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_interline_space), prefix + ".line-height",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(lineHeightStyles, lineHeightStyleNames, lineHeightStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_line_spacing_drawable, R.drawable.cr3_option_line_spacing));

		    String[] textDecorationStyles = {
		    		"", // inherited
		            "text-decoration: none",
		            "text-decoration: underline",
		            "text-decoration: line-through",
		            "text-decoration: overline",
			    };
		    int[] textDecorationStyleNames = {
			    	R.string.options_css_inherited,
			    	R.string.options_css_text_decoration_none,
			    	R.string.options_css_text_decoration_underline,
			    	R.string.options_css_text_decoration_line_through,
			    	R.string.options_css_text_decoration_overlineline,
			    };
			int[] textDecorationStyleAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_font_decoration), prefix + ".text-decoration",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(textDecorationStyles, textDecorationStyleNames, textDecorationStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_text_underline_drawable, R.drawable.cr3_option_text_underline));

		    String[] verticalAlignStyles = {
		    		"", // inherited
		            "vertical-align: baseline",
		            "vertical-align: sub",
		            "vertical-align: super",
			    };
		    int[] verticalAlignStyleNames = {
			    	R.string.options_css_inherited,
			    	R.string.options_css_text_valign_baseline,
			    	R.string.options_css_text_valign_subscript,
			    	R.string.options_css_text_valign_superscript,
			    };
			int[] verticalAlignStyleAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_text_valign), prefix + ".vertical-align",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(verticalAlignStyles, verticalAlignStyleNames, verticalAlignStyleAddInfos).setIconIdByAttr(R.attr.cr3_option_text_superscript_drawable, R.drawable.cr3_option_text_superscript));

		    String[] fontColorStyles = {
		        "", // inherited
		        "color: black",
		        "color: green",
		        "color: silver",
		        "color: lime",
		        "color: gray",
		        "color: olive",
		        "color: white",
		        "color: yellow",
		        "color: maroon",
		        "color: navy",
		        "color: red",
		        "color: blue",
		        "color: purple",
		        "color: teal",
		        "color: fuchsia",
		        "color: aqua",
		    };
		    String[] fontColorStyleNames = {
		        "-",
		        "Black",
		        "Green",
		        "Silver",
		        "Lime",
		        "Gray",
		        "Olive",
		        "White",
		        "Yellow",
		        "Maroon",
		        "Navy",
		        "Red",
		        "Blue",
		        "Purple",
		        "Teal",
		        "Fuchsia",
		        "Aqua",
		    };
			int[] fontColorStyleAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_text_color), prefix + ".color",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(fontColorStyles, fontColorStyleNames, fontColorStyleAddInfos).setIconIdByAttr(R.attr.attr_icons8_font_color, R.drawable.icons8_font_color));
			
			String[] marginTopOptions = {"", // inherited
			        "margin-top: 0em",
			        "margin-top: 0.2em",
			        "margin-top: 0.3em",
			        "margin-top: 0.5em",
			        "margin-top: 1em",
			        "margin-top: 2em"};
			String[] marginBottomOptions = {"", // inherited
			        "margin-bottom: 0em",
			        "margin-bottom: 0.2em",
			        "margin-bottom: 0.3em",
			        "margin-bottom: 0.5em",
			        "margin-bottom: 1em",
			        "margin-bottom: 2em"};
			int[] marginTopBottomOptionNames = {
			    	R.string.options_css_inherited,
			    	R.string.options_css_margin_0,
			    	R.string.options_css_margin_02em,
			    	R.string.options_css_margin_03em,
			    	R.string.options_css_margin_05em,
			    	R.string.options_css_margin_1em,
			    	R.string.options_css_margin_15em,
			        };
			int[] marginTopBottomOptionAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			String[] marginLeftOptions = {
					"", // inherited
			        "margin-left: 0em",
			        "margin-left: 0.5em",
			        "margin-left: 1em",
			        "margin-left: 1.5em",
			        "margin-left: 2em",
			        "margin-left: 4em",
			        "margin-left: 5%",
			        "margin-left: 10%",
			        "margin-left: 15%",
			        "margin-left: 20%",
			        "margin-left: 30%"};
			String[] marginRightOptions = {
					"", // inherited
			        "margin-right: 0em",
			        "margin-right: 0.5em",
			        "margin-right: 1em",
			        "margin-right: 1.5em",
			        "margin-right: 2em",
			        "margin-right: 4em",
			        "margin-right: 5%",
			        "margin-right: 10%",
			        "margin-right: 15%",
			        "margin-right: 20%",
			        "margin-right: 30%"};
			int[] marginLeftRightOptionNames = {
			    	R.string.options_css_inherited,
			    	R.string.options_css_margin_0,
			    	R.string.options_css_margin_05em,
			    	R.string.options_css_margin_1em,
			    	R.string.options_css_margin_15em,
			    	R.string.options_css_margin_2em,
			    	R.string.options_css_margin_4em,
			    	R.string.options_css_margin_5p,
			    	R.string.options_css_margin_10p,
			    	R.string.options_css_margin_15p,
			    	R.string.options_css_margin_20p,
			    	R.string.options_css_margin_30p,
			};
			int[] marginLeftRightOptionAddInfos = {
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
					R.string.option_add_info_empty_text,
			};
			listView.add(new ListOption(mOwner, getString(R.string.options_css_margin_top), prefix + ".margin-top",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(marginTopOptions, marginTopBottomOptionNames, marginTopBottomOptionAddInfos).setIconIdByAttr(R.attr.cr3_option_text_margin_top_drawable, R.drawable.cr3_option_text_margin_top));
			listView.add(new ListOption(mOwner, getString(R.string.options_css_margin_bottom), prefix + ".margin-bottom",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(marginBottomOptions, marginTopBottomOptionNames, marginTopBottomOptionAddInfos).setIconIdByAttr(R.attr.cr3_option_text_margin_bottom_drawable, R.drawable.cr3_option_text_margin_bottom));
			listView.add(new ListOption(mOwner, getString(R.string.options_css_margin_left), prefix + ".margin-left",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(marginLeftOptions, marginLeftRightOptionNames, marginLeftRightOptionAddInfos).setIconIdByAttr(R.attr.cr3_option_text_margin_left_drawable, R.drawable.cr3_option_text_margin_left));
			listView.add(new ListOption(mOwner, getString(R.string.options_css_margin_right), prefix + ".margin-right",
					getString(R.string.option_add_info_empty_text), this.lastFilteredValue).add(marginRightOptions, marginLeftRightOptionNames, marginLeftRightOptionAddInfos).setIconIdByAttr(R.attr.cr3_option_text_margin_right_drawable, R.drawable.cr3_option_text_margin_right));

			if (isSelect) {
				dlg.setTitle(label);
				dlg.setView(listView);
				dlg.show();
			}
		}

		public String getValueLabel() { return ">"; }
	}
	
	
	private ListOption createStyleEditor(String styleCode, int titleId, int addInfo, String filter) {
		ListOption res = new StyleEditorOption(this, getString(titleId), "styles." + styleCode,
				getString(addInfo), filter);
		res.noIcon();
		return res;
	}

	final static private String[] styleCodes = {
		"def",
		"title",
		"subtitle",
		"pre",
		"link",
		"cite",
		"epigraph",
		"poem",
		"text-author",
		"footnote",
		"footnote-link",
		"footnote-title",
		"annotation",
	};
	
	final static private int[] styleTitles = {
		R.string.options_css_def,
		R.string.options_css_title,
		R.string.options_css_subtitle,
		R.string.options_css_pre,
		R.string.options_css_link,
		R.string.options_css_cite,
		R.string.options_css_epigraph,
		R.string.options_css_poem,
		R.string.options_css_textauthor,
		R.string.options_css_footnote,
		R.string.options_css_footnotelink,
		R.string.options_css_footnotetitle,
		R.string.options_css_annotation,
	};

	final static private int[] styleAddInfos = {
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
			R.string.option_add_info_empty_text,
	};
	
	private void fillStyleEditorOptions(String filter) {
		mOptionsCSS = new OptionsListView(getContext(),null);
		//mProperties.setBool(PROP_TXT_OPTION_PREFORMATTED, mReaderView.isTextAutoformatEnabled());
		//mProperties.setBool(PROP_EMBEDDED_STYLES, mReaderView.getDocumentStylesEnabled());
		if (mReaderView != null) {
			mOptionsCSS.add(new BoolOption(this, getString(R.string.mi_book_styles_enable), PROP_EMBEDDED_STYLES,
					getString(R.string.option_add_info_empty_text), filter).setDefaultValue("0").noIcon().setOnChangeHandler(new Runnable() {
				@Override
				public void run() {
					boolean value = mProperties.getBool(PROP_EMBEDDED_STYLES, false);
					mEmbedFontsOptions.enabled = isEpubFormat && value;
				}
			}) );

			mEmbedFontsOptions = new BoolOption(this, getString(R.string.options_font_embedded_document_font_enabled), PROP_EMBEDDED_FONTS,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("1").noIcon();
			boolean value = mProperties.getBool(PROP_EMBEDDED_STYLES, false);
			mEmbedFontsOptions.enabled = isEpubFormat && value;
			mOptionsCSS.add(mEmbedFontsOptions);

			if (isTextFormat) {
				mOptionsCSS.add(new BoolOption(this, getString(R.string.mi_text_autoformat_enable), PROP_TXT_OPTION_PREFORMATTED,
						getString(R.string.option_add_info_empty_text), filter).setDefaultValue("1").noIcon());
			}
			if (isHtmlFormat) {
				mOptionsCSS.add(new ListOption(this, getString(R.string.options_rendering_preset), PROP_RENDER_BLOCK_RENDERING_FLAGS,
						getString(R.string.option_add_info_empty_text), filter).
						add(mRenderingPresets, mRenderingPresetsTitles, mRenderingPresetsAddInfos).
						setDefaultValue(Integer.valueOf(Engine.BLOCK_RENDERING_FLAGS_WEB).toString()).noIcon());
				mOptionsCSS.add(new ListOption(this, getString(R.string.options_requested_dom_level), PROP_REQUESTED_DOM_VERSION,
						getString(R.string.option_add_info_empty_text), filter).
						add(mDOMVersionPresets, mDOMVersionPresetTitles, mRenderingPresetsAddInfos).
						setDefaultValue(Integer.valueOf(Engine.DOM_VERSION_CURRENT).toString()).noIcon());
			}
		}
		for (int i=0; i<styleCodes.length; i++) {
			StyleEditorOption seO = (StyleEditorOption) createStyleEditor(styleCodes[i], styleTitles[i], styleAddInfos[i], filter);
			seO.updateFilterEnd();
			mOptionsCSS.add(seO);
		}
	}
	
	private void setupBrowserOptions(String filter)
	{
        mInflater = LayoutInflater.from(getContext());
        ViewGroup view = (ViewGroup)mInflater.inflate(R.layout.options_browser, null);
        ViewGroup body = (ViewGroup)view.findViewById(R.id.body);
        
        
        mOptionsBrowser = new OptionsListView(getContext(), null);

		mOptionsBrowser.add(new ListOption(this, getString(R.string.mi_book_sort_order), PROP_APP_BOOK_SORT_ORDER,
				getString(R.string.option_add_info_empty_text), filter).add(sortOrderValues, sortOrderLabels, sortOrderAddInfos).setDefaultValue(FileInfo.SortOrder.TITLE_AUTHOR.name()).noIcon());
		OptionBase sbO5 = new FilebrowserOption(this, getString(R.string.filebrowser_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_file, R.drawable.icons8_file);
		((FilebrowserOption)sbO5).updateFilterEnd();
		mOptionsBrowser.add(sbO5);
		mOptionsBrowser.add(new ListOption(this, getString(R.string.options_app_backlight_screen), PROP_APP_SCREEN_BACKLIGHT,
				getString(R.string.options_app_backlight_screen_add_info), filter).add(mBacklightLevels, mBacklightLevelsTitles, mBacklightLevelsAddInfos).setDefaultValue("-1").noIcon());
		mOptionsBrowser.add(new LangOption(this, filter).noIcon());
		//mOptionsBrowser.add(new PluginsOption(this, getString(R.string.options_app_plugins), getString(R.string.option_add_info_empty_text), filter).noIcon());
		mOptionsBrowser.add(new BoolOption(this, getString(R.string.options_app_fullscreen), PROP_APP_FULLSCREEN,
				getString(R.string.options_app_fullscreen_add_info), filter).setIconIdByAttr(R.attr.cr3_option_fullscreen_drawable, R.drawable.cr3_option_fullscreen));
		if ( !DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) ) {
			mOptionsBrowser.add(new NightModeOption(this, getString(R.string.options_inverse_view), PROP_NIGHT_MODE,
					getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_night_drawable, R.drawable.cr3_option_night));
		}
		if ( !DeviceInfo.isForceHCTheme(false)) {
		//plotn - when setting EINK manually, hc doesnt work ... still dont know why
			mOptionsBrowser.add(new ThemeOptions(this, getString(R.string.options_app_ui_theme),
					getString(R.string.options_app_ui_theme_add_info), filter).setIconIdByAttr(R.attr.attr_icons8_change_theme_1,
                    R.drawable.icons8_change_theme_1));
		}
		mOptionsBrowser.refresh();
		
		body.addView(mOptionsBrowser);
		setView(view);
	}
	private void setupReaderOptions(String filter)
	{
        mInflater = LayoutInflater.from(getContext());
        mTabs = (TabHost)mInflater.inflate(R.layout.options, null);
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast});
		final int colorGray = a.getColor(0, Color.GRAY);
		final int colorGrayC = a.getColor(1, Color.GRAY);

		mTabs.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				for (int i = 0; i < mTabs.getTabWidget().getChildCount(); i++) {
					mTabs.getTabWidget().getChildAt(i)
							.setBackgroundColor(colorGrayC); // unselected
				}

				mTabs.getTabWidget().getChildAt(mTabs.getCurrentTab())
						.setBackgroundColor(colorGray); // selected

			}
		});
		// setup tabs
		//setView(R.layout.options);
		//setContentView(R.layout.options);
		//mTabs = (TabHost)findViewById(android.R.id.tabhost); 
		mTabs.setup();
		
		TabWidget tabWidget = (TabWidget)mTabs.findViewById(android.R.id.tabs);
		//tabWidget.
		//new TabHost(getContext());
		
		mOptionsStyles = new OptionsListView(getContext(), null);
		mOptionsStyles.add(new FontsOptions(this, getString(R.string.options_font_face), PROP_FONT_FACE,
				getString(R.string.option_add_info_empty_text), false, filter).setIconIdByAttr(R.attr.cr3_option_font_face_drawable, R.drawable.cr3_option_font_face));
		mOptionsStyles.add(new ListOption(this, getString(R.string.options_font_size), PROP_FONT_SIZE,
				getString(R.string.option_add_info_empty_text), filter).add(filterFontSizes(mFontSizes)).setDefaultValue("24").setIconIdByAttr(R.attr.cr3_option_font_size_drawable, R.drawable.cr3_option_font_size));
		mOptionsStyles.add(new BoolOption(this, getString(R.string.options_font_embolden), PROP_FONT_WEIGHT_EMBOLDEN,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_text_bold_drawable, R.drawable.cr3_option_text_bold));
		//mOptionsStyles.add(new BoolOption(getString(R.string.options_font_antialias), PROP_FONT_ANTIALIASING).setInverse().setDefaultValue("0"));
		mOptionsStyles.add(new ListOption(this, getString(R.string.options_font_antialias), PROP_FONT_ANTIALIASING,
				getString(R.string.option_add_info_empty_text), filter).add(mAntialias, mAntialiasTitles, mAntialiasAddInfos).setDefaultValue("2").setIconIdByAttr(R.attr.cr3_option_text_antialias_drawable, R.drawable.cr3_option_text_antialias));
		mOptionsStyles.add(new ListOption(this, getString(R.string.options_interline_space), PROP_INTERLINE_SPACE,
				getString(R.string.option_add_info_empty_text), filter).addPercents(mInterlineSpaces).setDefaultValue("100").setIconIdByAttr(R.attr.cr3_option_line_spacing_drawable, R.drawable.cr3_option_line_spacing));
		mOptionsStyles.add(new BoolOption(this, getString(R.string.options_style_multilang), PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_text_multilang_drawable,
				R.drawable.cr3_option_text_multilang).setOnChangeHandler(new Runnable() {
			@Override
			public void run() {
				boolean value = mProperties.getBool(PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false);
				mHyphDictOption.enabled = !value;
				mEnableHyphOption.enabled = value;
			}
		}));
		mEnableHyphOption = new BoolOption(this, getString(R.string.options_style_enable_hyphenation), PROP_TEXTLANG_HYPHENATION_ENABLED,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("0").
				setIconIdByAttr(R.attr.cr3_option_text_hyphenation_drawable, R.drawable.cr3_option_text_hyphenation);
		mEnableHyphOption.enabled = mProperties.getBool(PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false);
		mOptionsStyles.add(mEnableHyphOption);
		mHyphDictOption = new HyphenationOptions(this, getString(R.string.options_hyphenation_dictionary),getString(R.string.option_add_info_empty_text), filter).
				setIconIdByAttr(R.attr.cr3_option_text_hyphenation_drawable, R.drawable.cr3_option_text_hyphenation);
		mHyphDictOption.enabled = !mProperties.getBool(PROP_TEXTLANG_EMBEDDED_LANGS_ENABLED, false);
		mOptionsStyles.add(mHyphDictOption);

		mOptionsStyles.add(new BoolOption(this, getString(R.string.options_style_floating_punctuation), PROP_FLOATING_PUNCTUATION,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("1").setIconIdByAttr(R.attr.cr3_option_text_floating_punct_drawable, R.drawable.cr3_option_text_other));
		mOptionsStyles.add(new ListOption(this, getString(R.string.options_text_shaping), PROP_FONT_SHAPING,
				getString(R.string.option_add_info_empty_text), filter).add(mShaping, mShapingTitles, mShapingTitlesAddInfos).setDefaultValue("1").
				setIconIdByAttr(R.attr.cr3_option_text_ligatures_drawable, R.drawable.cr3_option_text_ligatures));
		mOptionsStyles.add(new BoolOption(this, getString(R.string.options_font_kerning), PROP_FONT_KERNING_ENABLED,
				getString(R.string.option_add_info_empty_text), filter).setDefaultValue("1").setIconIdByAttr(R.attr.cr3_option_text_kerning_drawable, R.drawable.cr3_option_text_kerning));
		OptionBase isO = new ImageScalingOption(this, getString(R.string.options_format_image_scaling), getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_images_drawable, R.drawable.cr3_option_images);
		((ImageScalingOption)isO).updateFilterEnd();
		mOptionsStyles.add(isO);
		mOptionsStyles.add(new ListOption(this, getString(R.string.options_render_font_gamma), PROP_FONT_GAMMA, getString(R.string.option_add_info_empty_text), filter).add(mGammas).setDefaultValue("1.0").setIconIdByAttr(R.attr.cr3_option_font_gamma_drawable, R.drawable.cr3_option_font_gamma));
		mOptionsStyles.add(new ListOption(this, getString(R.string.options_format_min_space_width_percent), PROP_FORMAT_MIN_SPACE_CONDENSING_PERCENT, getString(R.string.option_add_info_empty_text), filter).addPercents(mMinSpaceWidths).setDefaultValue("50").setIconIdByAttr(R.attr.cr3_option_text_width_drawable, R.drawable.cr3_option_text_width));
		mOptionsStyles.add(new ListOption(this, getString(R.string.options_font_hinting), PROP_FONT_HINTING, getString(R.string.option_add_info_empty_text), filter).add(mHinting, mHintingTitles, mHintingTitlesAddInfos).setDefaultValue("2").noIcon());
		mOptionsStyles.add(new FontsOptions(this, getString(R.string.options_font_fallback_face), PROP_FALLBACK_FONT_FACE, getString(R.string.option_add_info_empty_text), false, filter).setIconIdByAttr(R.attr.cr3_option_font_face_drawable, R.drawable.cr3_option_font_face));
		mOptionsPage = new OptionsListView(getContext(), null);
		mOptionsPage.add(new BoolOption(this, getString(R.string.options_app_fullscreen), PROP_APP_FULLSCREEN,
				getString(R.string.options_app_fullscreen_add_info), filter).setIconIdByAttr(R.attr.cr3_option_fullscreen_drawable, R.drawable.cr3_option_fullscreen));
		mOptionsPage.add(new ListOption(this, getString(R.string.global_margin),
				PROP_GLOBAL_MARGIN, getString(R.string.global_margin_add_info), filter)
				.add(mScreenMargins).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_text_margin_left_drawable, R.drawable.cr3_option_text_margins));
		OptionBase sbO = new ToolbarOption(this, getString(R.string.toolbar),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_navigation_toolbar_top, R.drawable.icons8_navigation_toolbar_top);
		((ToolbarOption)sbO).updateFilterEnd();
		mOptionsPage.add(sbO);
		mOptionsPage.add(new ListOption(this, getString(R.string.options_view_mode), PROP_PAGE_VIEW_MODE,
				getString(R.string.options_view_mode_add_info), filter).add(mViewModes, mViewModeTitles, mViewModeAddInfos).
				setDefaultValue("1").setIconIdByAttr(R.attr.cr3_option_view_mode_scroll_drawable, R.drawable.cr3_option_view_mode_scroll).setOnChangeHandler(new Runnable() {
			@Override
			public void run() {
				int value = mProperties.getInt(PROP_PAGE_VIEW_MODE, 1);
				mFootNotesOption.enabled = value == 1;
			}
		}));
		//mOptionsPage.add(new ListOption(getString(R.string.options_page_orientation), PROP_ROTATE_ANGLE).add(mOrientations, mOrientationsTitles).setDefaultValue("0"));
		if (DeviceInfo.getSDKLevel() >= 9) {
			mOptionsPage.add(new ListOption(this, getString(R.string.options_page_orientation), PROP_APP_SCREEN_ORIENTATION,
					getString(R.string.options_page_orientation_add_info), filter).add(mOrientations_API9, mOrientationsTitles_API9, mOrientationsAddInfos_API9).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_page_orientation_landscape_drawable, R.drawable.cr3_option_page_orientation_landscape));
			mOptionsPage.add(new ListOption(this, getString(R.string.orientation_popup_toolbar_duration),
						PROP_APP_SCREEN_ORIENTATION_POPUP_DURATION,
					getString(R.string.orient_add_info), filter).
					add(mOrient, mOrientTitles, mOrientAddInfos).setDefaultValue("10").noIcon());
		}
		else
			mOptionsPage.add(new ListOption(this, getString(R.string.options_page_orientation), PROP_APP_SCREEN_ORIENTATION,
					getString(R.string.options_page_orientation_add_info), filter).add(mOrientations, mOrientationsTitles, mOrientationsAddInfos).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_page_orientation_landscape_drawable, R.drawable.cr3_option_page_orientation_landscape));
		mOptionsPage.add(new ListOption(this, getString(R.string.options_page_landscape_pages), PROP_LANDSCAPE_PAGES,
				getString(R.string.options_page_landscape_pages_add_info), filter).add(mLandscapePages, mLandscapePagesTitles, mLandscapePagesAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.cr3_option_pages_two_drawable, R.drawable.cr3_option_pages_two));
		if ( !DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) )
			mOptionsPage.add(new TextureOptions(this, getString(R.string.options_background_texture),
					getString(R.string.options_background_texture_add_info), filter).
					setIconIdByAttr(R.attr.attr_icons8_texture, R.drawable.icons8_texture));
		if ( DeviceInfo.EINK_SCREEN_UPDATE_MODES_SUPPORTED ) {
			mOptionsPage.add(new ListOption(this, getString(R.string.options_screen_update_mode), PROP_APP_SCREEN_UPDATE_MODE, getString(R.string.option_add_info_empty_text), filter).add(mScreenUpdateModes, mScreenUpdateModesTitles, mScreenUpdateModesAddInfos).setDefaultValue("0"));
			mOptionsPage.add(new ListOption(this, getString(R.string.options_screen_update_interval), PROP_APP_SCREEN_UPDATE_INTERVAL, getString(R.string.option_add_info_empty_text), filter).add(mScreenFullUpdateInterval).setDefaultValue("10"));
		}
		if (DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink())) {
			mOptionsPage.addExt(new ListOption(this, getString(R.string.options_screen_blackpage_interval), PROP_APP_SCREEN_BLACKPAGE_INTERVAL,
					getString(R.string.options_screen_blackpage_interval_add_info), filter).
					add(mScreenFullUpdateInterval).setIconIdByAttr(R.attr.attr_icons8_blackpage_interval, R.drawable.icons8_blackpage_interval).
					setDefaultValue("0"),"eink");
			mOptionsPage.addExt(new ListOption(this, getString(R.string.options_screen_blackpage_duration), PROP_APP_SCREEN_BLACKPAGE_DURATION,
					getString(R.string.options_screen_blackpage_duration_add_info), filter).
					add(mScreenBlackPageDuration).
					setIconIdByAttr(R.attr.attr_icons8_blackpage_duration, R.drawable.icons8_blackpage_duration).
					setDefaultValue("300"),"eink");
		}
		OptionBase sbO2 = new StatusBarOption(this, getString(R.string.options_page_titlebar_new), PROP_APP_TITLEBAR_NEW,
				getString(R.string.options_page_titlebar_add_info), filter).setIconIdByAttr(R.attr.attr_icons8_document_r_title, R.drawable.icons8_document_r_title);
		((StatusBarOption)sbO2).updateFilterEnd();
		mOptionsPage.add(sbO2);
		mFootNotesOption = new BoolOption(this, getString(R.string.options_page_footnotes), PROP_FOOTNOTES,
				getString(R.string.options_page_footnotes_add_info), filter).setDefaultValue("1");
		int value = mProperties.getInt(PROP_PAGE_VIEW_MODE, 1);
		mFootNotesOption.enabled = value == 1;
		mOptionsPage.add(mFootNotesOption);

		if ( !DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) )
			mOptionsPage.add(new ListOption(this, getString(R.string.options_page_animation), PROP_PAGE_ANIMATION,
					getString(R.string.options_page_animation_add_info), filter).
					add(mAnimation, mAnimationTitles, mAnimationAddInfos).setDefaultValue("1").noIcon());
		mOptionsPage.add(new ListOption(this, getString(R.string.options_view_bookmarks_highlight), PROP_APP_HIGHLIGHT_BOOKMARKS,
				getString(R.string.options_view_bookmarks_highlight_add_info), filter).add(mHighlightMode, mHighlightModeTitles, mHighlightModeAddInfos).setDefaultValue("1").
			setIconIdByAttr(R.attr.attr_icons8_bookmark_simple_color,R.drawable.icons8_bookmark_simple_color));

		OptionBase sbO7 = new PageColorsOption(this, getString(R.string.page_color_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_paint_palette1, R.drawable.icons8_paint_palette1);
		((PageColorsOption)sbO7).updateFilterEnd();
		mOptionsPage.add(sbO7);
		OptionBase sbO8 = new PageMarginsOption(this, getString(R.string.page_margins_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_text_margin_left_drawable, R.drawable.cr3_option_text_margin_left);
		((PageMarginsOption)sbO8).updateFilterEnd();
		mOptionsPage.add(sbO8);
		mOptionsPage.add(new GeoOption(this, filter).
                setIconIdByAttr(R.attr.attr_icons8_train_headphones, R.drawable.train_headphones));
        mOptionsControls = new OptionsListView(getContext(), null);
		OptionBase kmO = new KeyMapOption(this, getString(R.string.options_app_key_actions),
				getString(R.string.options_app_key_actions_add_info), filter).setIconIdByAttr(R.attr.cr3_option_controls_keys_drawable, R.drawable.cr3_option_controls_keys);
		((KeyMapOption)kmO).updateFilterEnd();
		mOptionsControls.add(kmO);
		mOptionsControls.add(new TapZoneOption(this, getString(R.string.options_app_tapzones_normal), PROP_APP_TAP_ZONE_ACTIONS_TAP,
				getString(R.string.options_app_tapzones_normal_add_info), filter).setIconIdByAttr(R.attr.cr3_option_controls_tapzones_drawable, R.drawable.cr3_option_controls_tapzones));
		mOptionsControls.add(new ListOption(this, getString(R.string.options_controls_tap_secondary_action_type), PROP_APP_SECONDARY_TAP_ACTION_TYPE,
				getString(R.string.options_controls_tap_secondary_action_type_add_info), filter).add(mTapSecondaryActionType, mTapSecondaryActionTypeTitles, mTapSecondaryActionTypeAddInfos).setDefaultValue(String.valueOf(TAP_ACTION_TYPE_LONGPRESS)).
				setIconIdByAttr(R.attr.attr_icons8_double_tap, R.drawable.icons8_double_tap));
		mOptionsControls.add(new BoolOption(this, getString(R.string.options_app_double_tap_selection), PROP_APP_DOUBLE_TAP_SELECTION,
				getString(R.string.options_app_double_tap_selection_add_info), filter).setDefaultValue("0").setIconIdByAttr(R.attr.cr3_option_touch_drawable, R.drawable.cr3_option_touch));
		if ( !DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) )
			mOptionsControls.add(new BoolOption(this, getString(R.string.options_controls_enable_volume_keys), PROP_CONTROLS_ENABLE_VOLUME_KEYS,
					getString(R.string.options_controls_enable_volume_keys_add_info), filter).setDefaultValue("1").
				setIconIdByAttr(R.attr.attr_icons8_speaker_buttons,R.drawable.icons8_speaker_buttons));
		//if ( !DeviceInfo.EINK_SCREEN ) // nook glowlight has this option
		mOptionsControls.add(new ListOption(this, getString(R.string.options_controls_flick_brightness), PROP_APP_FLICK_BACKLIGHT_CONTROL,
				getString(R.string.options_controls_flick_brightness_add_info), filter).
					add(mFlickBrightness, mFlickBrightnessTitles, mFlickBrightnessAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_sunrise,R.drawable.icons8_sunrise));
		mOptionsControls.add(new ListOption(this, getString(R.string.option_controls_gesture_page_flipping_enabled), 
			    PROP_APP_GESTURE_PAGE_FLIPPING, getString(R.string.option_controls_gesture_page_flipping_enabled_add_info), filter).add(
				mPagesPerFullSwipe, mPagesPerFullSwipeTitles, mPagesPerFullSwipeAddInfos).setDefaultValue("1").setIconIdByAttr(R.attr.attr_icons8_gesture, R.drawable.icons8_gesture));
		mOptionsControls.add(new BoolOption(this, getString(R.string.disable_two_pointer_gestures), PROP_APP_DISABLE_TWO_POINTER_GESTURES,
				getString(R.string.two_pointer_gestures_add_info), filter).setDefaultValue("0").noIcon());
		mOptionsControls.add(new ListOption(this, getString(R.string.options_selection_action), PROP_APP_SELECTION_ACTION,
				getString(R.string.options_selection_action_add_info), filter).add(mSelectionAction, mSelectionActionTitles, mSelectionActionAddInfos).setDefaultValue("0").
            setIconIdByAttr(R.attr.attr_icons8_document_selection1, R.drawable.icons8_document_selection1));
		mOptionsControls.add(new ListOption(this, getString(R.string.options_multi_selection_action), PROP_APP_MULTI_SELECTION_ACTION,
				getString(R.string.options_multi_selection_action_add_info), filter).add(mMultiSelectionAction, mMultiSelectionActionTitles, mMultiSelectionActionAddInfo).setDefaultValue("0").
					setIconIdByAttr(R.attr.attr_icons8_document_selection2, R.drawable.icons8_document_selection2));
		mOptionsControls.add(new ListOption(this, getString(R.string.options_selection_action_long),
				PROP_APP_SELECTION_ACTION_LONG, getString(R.string.options_selection_action_long_add_info), filter).add(mSelectionAction, mSelectionActionTitles, mSelectionActionAddInfos).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_document_selection1_long, R.drawable.icons8_document_selection1_long));
		mOptionsApplication = new OptionsListView(getContext(), null);
		mOptionsApplication.add(new LangOption(this, filter).noIcon());
		if ( !DeviceInfo.isForceHCTheme(false)) {
		//plotn - when setting EINK manually, hc doesnt work ... still dont know why
			mOptionsApplication.add(new ThemeOptions(this, getString(R.string.options_app_ui_theme), getString(R.string.options_app_ui_theme_add_info), filter).setIconIdByAttr(R.attr.attr_icons8_change_theme_1,
					R.drawable.icons8_change_theme_1));
		}
		OptionBase sbO4 = new DictionaryOption(this, getString(R.string.dictionary_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_google_translate, R.drawable.icons8_google_translate);
		((DictionaryOption)sbO4).updateFilterEnd();
		mOptionsApplication.add(sbO4);
		mOptionsApplication.add(new BoolOption(this, getString(R.string.options_screen_force_eink), PROP_APP_SCREEN_FORCE_EINK,
				getString(R.string.options_screen_force_eink_add_info), filter).
				setIconIdByAttr(R.attr.attr_icons8_eink, R.drawable.icons8_eink).
				setDefaultValue("0"));
		if ( !DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) ) {
			mOptionsApplication.add(new ListOption(this, getString(R.string.options_app_backlight_timeout), PROP_APP_SCREEN_BACKLIGHT_LOCK,
					getString(R.string.options_app_backlight_timeout_add_info), filter).
					add(mBacklightTimeout, mBacklightTimeoutTitles, mBacklightLevelsAddInfos).setDefaultValue("3").setIconIdByAttr(R.attr.attr_icons8_sun_1, R.drawable.icons8_sun_1));
			mBacklightLevelsTitles[0] = getString(R.string.options_app_backlight_screen_default);
			mOptionsApplication.add(new ListOption(this, getString(R.string.options_app_backlight_screen), PROP_APP_SCREEN_BACKLIGHT,
					getString(R.string.options_app_backlight_screen_add_info), filter).add(mBacklightLevels, mBacklightLevelsTitles, mBacklightLevelsAddInfos).
					setDefaultValue("-1").
                    setIconIdByAttr(R.attr.attr_icons8_sun, R.drawable.icons8_sun));
		}
		mOptionsApplication.add(new ListOption(this, getString(R.string.options_app_tts_stop_motion_timeout), PROP_APP_MOTION_TIMEOUT,
				getString(R.string.options_app_tts_stop_motion_timeout_add_info), filter).add(mMotionTimeouts, mMotionTimeoutsTitles, mMotionTimeoutsAddInfos).setDefaultValue(Integer.toString(mMotionTimeouts[0])).
				setIconIdByAttr(R.attr.attr_icons8_moving_sensor,R.drawable.icons8_moving_sensor));
		mOptionsApplication.add(new BoolOption(this, getString(R.string.options_app_key_backlight_off), PROP_APP_KEY_BACKLIGHT_OFF,
				getString(R.string.options_app_key_backlight_off_add_info), filter).setDefaultValue("1").noIcon());
		mOptionsApplication.add(new IconsBoolOption(this, getString(R.string.options_app_settings_icons), PROP_APP_SETTINGS_SHOW_ICONS,
				getString(R.string.options_app_settings_icons_add_info), filter).setDefaultValue("1").noIcon());
		if ( !DeviceInfo.isEinkScreen(BaseActivity.getScreenForceEink()) ) {
			mOptionsApplication.add(new BoolOption(this, getString(R.string.options_app_settings_icons_is_custom_color), PROP_APP_ICONS_IS_CUSTOM_COLOR,
					getString(R.string.option_add_info_empty_text), filter).setDefaultValue("0").noIcon());
			mOptionsApplication.add(new ColorOption(this, getString(R.string.options_app_settings_icons_custom_color), PROP_APP_ICONS_CUSTOM_COLOR, 0x000000,
					getString(R.string.option_add_info_empty_text), filter).noIcon());
		}
		OptionBase sbO5 = new FilebrowserOption(this, getString(R.string.filebrowser_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_file, R.drawable.icons8_file);
		((FilebrowserOption)sbO5).updateFilterEnd();
		mOptionsApplication.add(sbO5);
		mOptionsApplication.add(new ListOption(this, getString(R.string.save_pos_timeout),
				PROP_SAVE_POS_TIMEOUT, getString(R.string.save_pos_timeout_add_info), filter).add(mMotionTimeouts1, mMotionTimeoutsTitles1, mMotionTimeoutsAddInfos1).setDefaultValue(Integer.toString(mMotionTimeouts1[2])).
				setIconIdByAttr(R.attr.attr_icons8_position_to_disk_interval, R.drawable.icons8_position_to_disk_interval));
		mOptionsApplication.add(new ListOption(this, getString(R.string.save_pos_timeout_speak),
				PROP_SAVE_POS_SPEAK_TIMEOUT, getString(R.string.option_add_info_empty_text), filter).
				add(mMotionTimeoutsSec, mMotionTimeoutsTitlesSec, mMotionTimeoutsAddInfosSec).setDefaultValue("0").
				setIconIdByAttr(R.attr.attr_icons8_position_to_disk_interval, R.drawable.icons8_position_to_disk_interval));
		//mOptionsApplication.add(new PluginsOption(this, getString(R.string.options_app_plugins), getString(R.string.option_add_info_empty_text), filter).noIcon());
		OptionBase sbO3 = new CloudOption(this, getString(R.string.cloud_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.attr_icons8_cloud_storage, R.drawable.icons8_cloud_storage);
		((CloudOption)sbO3).updateFilterEnd();
		mOptionsApplication.add(sbO3);
		OptionBase sbO6 = new RareOption(this, getString(R.string.rare_settings),
				getString(R.string.option_add_info_empty_text), filter).setIconIdByAttr(R.attr.cr3_option_other_drawable, R.drawable.icons8_more);
		((RareOption)sbO6).updateFilterEnd();
		mOptionsApplication.add(sbO6);
		fillStyleEditorOptions(filter);

		mOptionsStyles.refresh();
		mOptionsCSS.refresh();
		mOptionsPage.refresh();
		mOptionsApplication.refresh();
		
		addTab("Styles",
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_type_filled, R.drawable.icons8_type_filled),
				//R.drawable.icons8_type_filled,
				R.string.tab_options_styles);
		addTab("CSS",
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_css, R.drawable.icons8_css),
				//R.drawable.icons8_css,
				R.string.tab_options_css);
		addTab("Page",
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_page, R.drawable.icons8_page),
				//R.drawable.icons8_page,
				R.string.tab_options_page);
		addTab("Controls",
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_cursor, R.drawable.icons8_cursor),
				//R.drawable.icons8_cursor,
				R.string.tab_options_controls);
		addTab("App",
				Utils.resolveResourceIdByAttr(activity, R.attr.attr_icons8_settings, R.drawable.icons8_settings),
				//R.drawable.icons8_settings,
				R.string.tab_options_app);
		setView(mTabs);
		((CoolReader)activity).tintViewIcons(mTabs);
		mTabs.invalidate();
		mTabs.setCurrentTab(4);
		if (mOptionsApplication.mOptions.size()>0) mTabs.setCurrentTab(4); else
		if (mOptionsStyles.mOptions.size()>0) mTabs.setCurrentTab(0); else
		if (mOptionsCSS.mOptions.size()>0) mTabs.setCurrentTab(1); else
		if (mOptionsPage.mOptions.size()>0) mTabs.setCurrentTab(2); else
		if (mOptionsControls.mOptions.size()>0) mTabs.setCurrentTab(3); else {
			mTabs.setCurrentTab(4);
			activity.showToast(getString(R.string.mi_no_options) +" "+filter);
		}

	}
	
	private void addTab(String name, int imageDrawable, int contentDescription) {
		TabHost.TabSpec ts = mTabs.newTabSpec(name);
		Drawable icon = getContext().getResources().getDrawable(imageDrawable);
		activity.tintViewIcons(icon,true);
		// temporary rollback ImageButton tabs: no highlight for current tab in this implementation
//		if (true) {
			ts.setIndicator("", icon);
//		} else {
//			// ACCESSIBILITY: we need to specify contentDescription
//			ImageButton ib = new ImageButton(getContext());
//			ib.setImageDrawable(icon);
//			ib.setBackgroundResource(R.drawable.cr3_toolbar_button_background);
//			Utils.setContentDescription(ib, getContext().getResources().getString(contentDescription));
//			ts.setIndicator(ib);
//		}
		
		ts.setContent(this);
		mTabs.addTab(ts);
	}

	@Override
	protected void onStart() {
		if (!StrUtils.isEmptyStr(selectedOption)) {
			BackgroundThread.instance().postGUI(new Runnable() {
				@Override
				public void run() {
					OptionBase selOpt = null;
					if (mOptionsStyles != null)
						for (OptionBase opt : mOptionsStyles.mOptions) {
							if (opt.property.equals(selectedOption)) selOpt = opt;
						}
					if (mOptionsCSS != null)
						for (OptionBase opt : mOptionsCSS.mOptions) {
							if (opt.property.equals(selectedOption)) selOpt = opt;
						}
					if (mOptionsPage != null)
						for (OptionBase opt : mOptionsPage.mOptions) {
							if (opt.property.equals(selectedOption)) selOpt = opt;
						}
					if (mOptionsApplication != null)
						for (OptionBase opt : mOptionsApplication.mOptions) {
							if (opt.property.equals(selectedOption)) selOpt = opt;
						}
					if (mOptionsControls != null)
						for (OptionBase opt : mOptionsControls.mOptions) {
							if (opt.property.equals(selectedOption)) selOpt = opt;
						}
					if (mOptionsBrowser != null)
						for (OptionBase opt : mOptionsBrowser.mOptions) {
							if (opt.property.equals(selectedOption)) selOpt = opt;
						}
					if (selOpt != null) {
						selOpt.onSelect();
						if (selOpt.optionsListView != null) {
							if (selOpt.optionsListView == mOptionsStyles) mTabs.setCurrentTab(0);
							if (selOpt.optionsListView == mOptionsCSS) mTabs.setCurrentTab(1);
							if (selOpt.optionsListView == mOptionsPage) mTabs.setCurrentTab(2);
							if (selOpt.optionsListView == mOptionsControls) mTabs.setCurrentTab(3);
							if (selOpt.optionsListView == mOptionsApplication)
								mTabs.setCurrentTab(4);
						}
					}
					selectedOption = "";
				}
			}, 100);
		} else {
			if (selectedTab != -1) {
				BackgroundThread.instance().postGUI(new Runnable() {
					@Override
					public void run() {
						mTabs.setCurrentTab(selectedTab);
						selectedTab = -1;
					}
				}, 100);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		L.v("creating OptionsDialog");
		CoolReader.dumpHeapAllocation();
		L.v("calling gc");
		System.gc();
		CoolReader.dumpHeapAllocation();
		L.v("creating options dialog");
        setCancelable(true);
        setCanceledOnTouchOutside(true);
 
		mMotionTimeoutsTitles = activity.getResources().getStringArray(R.array.motion_timeout_titles);
        mMotionTimeouts = activity.getResources().getIntArray(R.array.motion_timeout_values);
		mMotionTimeoutsAddInfos = activity.getResources().getIntArray(R.array.motion_timeout_add_infos);
		mMotionTimeoutsTitlesSec = activity.getResources().getStringArray(R.array.motion_timeout_sec_titles);
		mMotionTimeoutsSec = activity.getResources().getIntArray(R.array.motion_timeout_sec_values);
		mMotionTimeoutsAddInfosSec = activity.getResources().getIntArray(R.array.motion_timeout_sec_add_infos);
		for (int i=0;i<mMotionTimeouts.length; i++) mMotionTimeoutsAddInfos[i] = R.string.option_add_info_empty_text;
		for (int i=0;i<mMotionTimeoutsSec.length; i++) mMotionTimeoutsAddInfosSec[i] = R.string.option_add_info_empty_text;
		mMotionTimeoutsTitles1 = Arrays.copyOfRange(mMotionTimeoutsTitles,1,mMotionTimeoutsTitles.length-1);
		mMotionTimeouts1 = Arrays.copyOfRange(mMotionTimeouts,1,mMotionTimeouts.length-1);
		mMotionTimeoutsAddInfos1 = Arrays.copyOfRange(mMotionTimeoutsAddInfos,1,mMotionTimeoutsAddInfos.length-1);

		mPagesPerFullSwipeTitles = activity.getResources().getStringArray(R.array.pages_per_full_swipe_titles);
		mPagesPerFullSwipe = activity.getResources().getIntArray(R.array.pages_per_full_swipe_values);
		mPagesPerFullSwipeAddInfos = activity.getResources().getIntArray(R.array.pages_per_full_swipe_add_infos);
		for (int i=0;i<mPagesPerFullSwipeAddInfos.length; i++) mPagesPerFullSwipeAddInfos[i] = R.string.option_add_info_empty_text;

		mForceTTSTitles = activity.getResources().getStringArray(R.array.force_tts_titles);
		mForceTTS = activity.getResources().getIntArray(R.array.force_tts_values);
		mForceTTSAddInfos = activity.getResources().getIntArray(R.array.force_tts_add_infos);
		for (int i=0;i<mForceTTSAddInfos.length; i++) mForceTTSAddInfos[i] = R.string.option_add_info_empty_text;

		mBrowserMaxGroupItems = activity.getResources().getIntArray(R.array.browser_max_group_items);

		mOrientTitles = activity.getResources().getStringArray(R.array.orient_titles);
		mOrient = activity.getResources().getIntArray(R.array.orient_values);
		mOrientAddInfos = activity.getResources().getIntArray(R.array.orient_add_infos);
		for (int i=0;i<mOrientAddInfos.length; i++) mOrientAddInfos[i] = R.string.option_add_info_empty_text;

		String filter = "";
		if (activity instanceof CoolReader) filter = ((CoolReader)activity).optionsFilter;
		this.optionFilter = filter;

		if (mode == Mode.READER)
        	setupReaderOptions(filter);
        else if (mode == Mode.BROWSER)
        	setupBrowserOptions("");
        
		setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface dialog) {
				onPositiveButtonClick();
			}
		});

		ImageButton positiveButton = (ImageButton)view.findViewById(R.id.options_btn_back);
		positiveButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				onPositiveButtonClick();
			}
		});
		
//		ImageButton negativeButton = (ImageButton)mTabs.findViewById(R.id.options_btn_cancel);
//		negativeButton.setOnClickListener(new View.OnClickListener() {
//			public void onClick(View v) {
//				onNegativeButtonClick();
//			}
//		});
		String filterOptions = "";
		if (activity instanceof CoolReader) {
			CoolReader cr = ((CoolReader) activity);
			filterOptions = cr.optionsFilter;
			if (StrUtils.isEmptyStr(filterOptions)) {
				if (upperTextLayout != null) {
					TextView lbl = (TextView) upperTextLayout.findViewById(R.id.base_dlg_upper_text);
					if (lbl != null) {
						if (cr.getReaderView()!=null)
							lbl.setPaintFlags(lbl.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
						lbl.setOnClickListener(new View.OnClickListener() {

							@Override
							public void onClick(View v) {
								if (cr.getReaderView()!=null) {
									SwitchProfileDialog dlg = new SwitchProfileDialog(cr, cr.getReaderView(), OptionsDialog.this);
									dlg.show();
								} else {
									cr.showToast(cr.getString(R.string.switch_profile_note));
								}
							}
						});
					}
				}
			}
		}
		super.onCreate(savedInstanceState);
		L.v("OptionsDialog is created");
	}

//	private void askApply()
//	{
//		Properties diff = mProperties.diff(mOldProperties);
//		if ( diff.size()>0 ) {
//			L.d("Some properties were changed, ask user whether to apply");
//			AlertDialog.Builder dlg = new AlertDialog.Builder(getContext());
//			dlg.setTitle(R.string.win_title_options_apply);
//			dlg.setPositiveButton(R.string.dlg_button_ok, new OnClickListener() {
//				public void onClick(DialogInterface arg0, int arg1) {
//					onPositiveButtonClick();
//				}
//			});
//			dlg.setNegativeButton(R.string.dlg_button_cancel, new OnClickListener() {
//				public void onClick(DialogInterface arg0, int arg1) {
//					onNegativeButtonClick();
//				}
//			});
//			dlg.show();
//		}
//	}
	
	protected void apply() {
		if (mode == Mode.READER) {
			int domVersion = mProperties.getInt(PROP_REQUESTED_DOM_VERSION, Engine.DOM_VERSION_CURRENT);
			int rendFlags = mProperties.getInt(PROP_RENDER_BLOCK_RENDERING_FLAGS, Engine.BLOCK_RENDERING_FLAGS_WEB);
			if (mReaderView != null) {
				if (mProperties.getBool(PROP_TXT_OPTION_PREFORMATTED, true) != mReaderView.isTextAutoformatEnabled()) {
					mReaderView.toggleTextFormat();
				}
				if (mProperties.getBool(PROP_EMBEDDED_STYLES, true) != mReaderView.getDocumentStylesEnabled()) {
					mReaderView.toggleDocumentStyles();
				}
				if (mProperties.getBool(PROP_EMBEDDED_FONTS, true) != mReaderView.getDocumentFontsEnabled()) {
					mReaderView.toggleEmbeddedFonts();
				}
				if (domVersion != mReaderView.getDOMVersion()) {
					mReaderView.setDOMVersion(domVersion);
				}
				if (rendFlags != mReaderView.getBlockRenderingFlags()) {
					mReaderView.setBlockRenderingFlags(rendFlags);
				}
			}
		}
		mActivity.setSettings(mProperties, 0, true);
		try {
			if (mActivity instanceof CoolReader) {
				if (((CoolReader) mActivity).getmReaderFrame() != null)
					((CoolReader) mActivity).getmReaderFrame().updateCRToolbar(((CoolReader) mActivity));
				mActivity.setCutoutMode(mActivity.settings().getInt(PROP_EXT_FULLSCREEN_MARGIN, 0));
			}
		} catch (Exception e) {
		}
        //mReaderView.setSettings(mProperties, mOldProperties);
	}
	
	@Override
	protected void onPositiveButtonClick() {
		apply();
        dismiss();
	}

	@Override
	protected void onNegativeButtonClick() {
		onPositiveButtonClick();
	}

	@Override
	protected void onStop() {
		//L.d("OptionsDialog.onStop() : calling gc()");
		//System.gc();
		super.onStop();
	}

	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (mode == Mode.READER) {
	        if (((OptionsListView)mTabs.getCurrentView()).onKeyDown(keyCode, event))
	        	return true;
		} else {
	        if (view.onKeyDown(keyCode, event))
	        	return true;
		}
        return super.onKeyDown(keyCode, event);
    }	
}
