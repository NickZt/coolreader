package org.coolreader.crengine;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.coolreader.CoolReader;
import org.coolreader.R;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class BookInfoDialog extends BaseDialog {
	private final BaseActivity mCoolReader;
	private final LayoutInflater mInflater; 
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
		mLabelMap.put("section.position", R.string.book_info_section_current_position);
		mLabelMap.put("position.percent", R.string.book_info_position_percent);
		mLabelMap.put("position.page", R.string.book_info_position_page);
		mLabelMap.put("position.chapter", R.string.book_info_position_chapter);
		mLabelMap.put("section.book", R.string.book_info_section_book_properties);
		mLabelMap.put("book.authors", R.string.book_info_book_authors);
		mLabelMap.put("book.title", R.string.book_info_book_title);
		mLabelMap.put("book.series", R.string.book_info_book_series_name);
		mLabelMap.put("book.language", R.string.book_info_book_language);
		mLabelMap.put("book.translation", R.string.book_info_book_translation);
	}
	
	private void addItem(TableLayout table, String item) {
		int p = item.indexOf("=");
		if ( p<0 )
			return;
		String name = item.substring(0, p).trim();
		String value = item.substring(p+1).trim();
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
		TextView nameView = (TextView)tableRow.findViewById(R.id.name);
		TextView valueView = (TextView)tableRow.findViewById(R.id.value);
		nameView.setText(name);
		valueView.setText(value);
		table.addView(tableRow);
	}
	
	public BookInfoDialog(final BaseActivity activity, Collection<String> items)
	{
		super(activity);
		mCoolReader = activity;
		setTitle(mCoolReader.getString(R.string.dlg_book_info));
		fillMap();
		mInflater = LayoutInflater.from(getContext());
		View view = mInflater.inflate(R.layout.book_info_dialog, null);
		TableLayout table = (TableLayout)view.findViewById(R.id.table);
		Button btnInfo = (Button) view.findViewById(R.id.btn_edit_info);
		btnInfo.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				CoolReader cr = (CoolReader)mCoolReader;
				FileInfo fi = cr.getReaderView().getBookInfo().getFileInfo();
				FileInfo dfi = fi.parent;
				if (dfi == null) {
					dfi = Services.getScanner().findParent(fi, Services.getScanner().getRoot());
				}
				if (dfi!=null) {
					cr.editBookInfo(dfi, fi);
					dismiss();
				}
			}
		});
		Button btnShortcut = (Button) view.findViewById(R.id.btn_shortcut);

		int colorGray;
		int colorGrayC;
		TypedArray a = mCoolReader.getTheme().obtainStyledAttributes(new int[]
				{R.attr.colorThemeGray2, R.attr.colorThemeGray2Contrast});
		colorGray = a.getColor(0, Color.GRAY);
		colorGrayC = a.getColor(1, Color.GRAY);
		btnInfo.setBackgroundColor(colorGrayC);
		btnShortcut.setBackgroundColor(colorGrayC);

		btnShortcut.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				CoolReader cr = (CoolReader)mCoolReader;
				FileInfo fi = cr.getReaderView().getBookInfo().getFileInfo();
				cr.createBookShortcut(fi);
			}
		});
		for ( String item : items ) {
			addItem(table, item);
		}
		setView( view );
	}

}
