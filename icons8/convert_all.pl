#!/usr/bin/perl -w

$TARGET_DIR = "../android/res/";

#                      dpi: 120       160       240        320         480            640
my %ic_actions_sizes  = (ldpi=>24, mdpi=>32, hdpi=>48, xhdpi=>64, xxhdpi=>96,  xxxhdpi=>128);
my %ic_menu_sizes     = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);
my %ic_launcher_sizes = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);
my %ic_bigicons_sizes = (ldpi=>36, mdpi=>48, hdpi=>72, xhdpi=>96, xxhdpi=>144, xxxhdpi=>192);

my %ic_actions_list=(
        'icons8_me_smb.svg' => 'icons8_me_smb.png',
        'icons8_search_history.svg' => 'icons8_search_history.png',
        'icons8_scissors.svg' => 'icons8_scissors.png',
        'icons8_back_key.svg' => 'icons8_back_key.png',
        'icons8_back_key_double.svg' => 'icons8_back_key_double.png',
        'icons8_back_key_long.svg' => 'icons8_back_key_long.png',
        'icons8_forward_key.svg' => 'icons8_forward_key.png',
        'icons8_forward_key_double.svg' => 'icons8_forward_key_double.png',
        'icons8_forward_key_long.svg' => 'icons8_forward_key_long.png',
        'icons8_left_key.svg' => 'icons8_left_key.png',
        'icons8_left_key_double.svg' => 'icons8_left_key_double.png',
        'icons8_left_key_long.svg' => 'icons8_left_key_long.png',
        'icons8_right_key.svg' => 'icons8_right_key.png',
        'icons8_right_key_double.svg' => 'icons8_right_key_double.png',
        'icons8_right_key_long.svg' => 'icons8_right_key_long.png',
        'icons8_menu_key.svg' => 'icons8_menu_key.png',
        'icons8_menu_key_double.svg' => 'icons8_menu_key_double.png',
        'icons8_menu_key_long.svg' => 'icons8_menu_key_long.png',
        'icons8_quote_2.svg' => 'icons8_quote_2.png',
        'icons8_ok_from_clipboard.svg' => 'icons8_ok_from_clipboard.png',
        'icons8_odt.svg' => 'icons8_odt.png',
        'icons8_bookmark_simple_color.svg' => 'icons8_bookmark_simple_color.png',
        'icons8_bookmark_link.svg' => 'icons8_bookmark_link.png',
        'icons8_book_big_and_small.svg' => 'icons8_book_big_and_small.png',
        'icons8_book_scan_properties.svg' => 'icons8_book_scan_properties.png',
        'icons8_computer_mouse.svg' => 'icons8_computer_mouse.png',
        'icons8_document_footnote.svg' => 'icons8_document_footnote.png',
        'icons8_document_r_title.svg' => 'icons8_document_r_title.png',
        'icons8_l_h.svg' => 'icons8_l_h.png',
        'icons8_moving_sensor.svg' => 'icons8_moving_sensor.png',
        'icons8_paint_palette1.svg' => 'icons8_paint_palette1.png',
        'icons8_resolution.svg' => 'icons8_resolution.png',
        'icons8_single_double_tap.svg' => 'icons8_single_double_tap.png',
        'icons8_speaker_buttons.svg' => 'icons8_speaker_buttons.png',
        'icons8_texture.svg' => 'icons8_texture.png',
        'icons8_minus.svg' => 'icons8_minus.png',
        'icons8_edit_row_2.svg' => 'icons8_edit_row_2.png',
        'icons8_position.svg' => 'icons8_position.png',
        'icons8_bookmark_simple.svg' => 'icons8_bookmark_simple.png',
        'icons8_doc.svg' => 'icons8_doc.png',
        'icons8_docx.svg' => 'icons8_docx.png',
        'icons8_epub_1.svg' => 'icons8_epub_1.png',
        'icons8_fb2.svg' => 'icons8_fb2.png',
        'icons8_file.svg' => 'icons8_file.png',
        'icons8_html_filetype_2.svg' => 'icons8_html_filetype_2.png',
        'icons8_image_file.svg' => 'icons8_image_file.png',
        'icons8_mobi.svg' => 'icons8_mobi.png',
        'icons8_smooth_background_color.svg' => 'icons8_smooth_background_color.png',
        'icons8_txt_2.svg' => 'icons8_txt_2.png',
        'icons8_folder_scan.svg' => 'icons8_folder_scan.png',
        'icons8_position_info.svg' => 'icons8_position_info.png',
        'cr3_option_text_kerning_gray.svg' => 'cr3_option_text_kerning_gray.png',
        'cr3_option_text_antialias_gray.svg' => 'cr3_option_text_antialias_gray.png',
        'cr3_option_images_gray.svg' => 'cr3_option_images_gray.png',
	'icons8_search.svg' => 'icons8_search.png',
	'icons8_home.svg' => 'icons8_home.png',
        'icons8_bookmark.svg' => 'icons8_bookmark.png',
        'icons8_documents_folder_2.svg' => 'icons8_documents_folder_2.png',
        'icons8_internet_folder_2.svg' => 'icons8_internet_folder_2.png',
        'icons8_exit.svg' => 'icons8_exit.png',
        'icons8_folder.svg' => 'icons8_folder.png',
        'icons8_add_link_1.svg' => 'icons8_add_link_1.png',
        'icons8_administrative_tools.svg' => 'icons8_administrative_tools.png',
        'icons8_manual_2.svg' => 'icons8_manual_2.png',
        'icons8_google_drive_2.svg' => 'icons8_google_drive_2.png',
        'icons8_type_filled.svg' => 'icons8_type_filled_2.png',
        'icons8_google_translate.svg' => 'icons8_google_translate.png',
        'icons8_info.svg' => 'icons8_info.png',
        'icons8_increase_font_2.svg' => 'icons8_increase_font_2.png',
        'icons8_decrease_font_1.svg' => 'icons8_decrease_font_1.png',
        'icons8_back.svg' => 'icons8_back.png',
        'icons8_forward.svg' => 'icons8_forward.png',
        'icons8_fast_forward_number.svg' => 'icons8_fast_forward_number.png',
        'icons8_fast_forward_percent.svg' => 'icons8_fast_forward_percent.png',
        'icons8_folder_star.svg' => 'icons8_folder_star.png',
        'icons8_speaker.svg' => 'icons8_speaker.png',
        'icons8_natural_user_interface_2.svg' => 'icons8_natural_user_interface_2.png',
        'icons8_books_switch.svg' => 'icons8_books_switch.png',
        'icons8_ok.svg' => 'icons8_ok.png',
        'icons8_cancel.svg' => 'icons8_cancel.png',
        'icons8_book_link.svg' => 'icons8_book_link.png',
        'icons8_micro_sd_2.svg' => 'icons8_micro_sd_2.png',
        'icons8_favorite_folder_2.svg' => 'icons8_favorite_folder_2.png',
        'icons8_google_drive_2_plus.svg' => 'icons8_google_drive_2_plus.png',
        'icons8_google_translate_save.svg' => 'icons8_google_translate_save.png',
        'icons8_google_translate_switch.svg' => 'icons8_google_translate_switch.png',
        'icons8_google_translate_2.svg' => 'icons8_google_translate_2.png',
        'icons8_google_translate_user.svg' => 'icons8_google_translate_user.png',
        'icons8_bookmark_plus.svg' => 'icons8_bookmark_plus.png',
        'icons8_book_minus.svg' => 'icons8_book_minus.png',
        'icons8_plus.svg' => 'icons8_plus.png',
        'icons8_book.svg' => 'icons8_book.png',
        'icons8_css.svg' => 'icons8_css_2.png',
        'icons8_alphabetical_sorting.svg' => 'icons8_alphabetical_sorting.png',
        'icons8_menu.svg' => 'icons8_menu.png',
        'icons8_touchscreen.svg' => 'icons8_touchscreen.png',
        'icons8_night_landscape_2.svg' => 'icons8_night_landscape_2.png',
        'icons8_drop_down.svg' => 'icons8_drop_down.png',
        'icons8_more.svg' => 'icons8_more.png',
        'icons8_about.svg' => 'icons8_about.png',
        'icons8_gamma.svg' => 'icons8_gamma.png',
        'icons8_bold.svg' => 'icons8_bold.png',
        'icons8_italic.svg' => 'icons8_italic.png',
        'icons8_underline_2.svg' => 'icons8_underline_2.png',
        'icons8_repeat.svg' => 'icons8_repeat.png',
        'icons8_document_1.svg' => 'icons8_document_1.png',
        'icons8_document_z.svg' => 'icons8_document_z.png',
        'icons8_document_down2.svg' => 'icons8_document_down2.png',
        'icons8_document_down.svg' => 'icons8_document_down.png',
        'icons8_document_up2.svg' => 'icons8_document_up2.png',
        'icons8_document_up.svg' => 'icons8_document_up.png',
        'icons8_page.svg' => 'icons8_page_2.png',
        'icons8_change_theme_1.svg' => 'icons8_change_theme_1.png',
        'icons8_data_grid_1.svg' => 'icons8_data_grid_1.png',
        'icons8_document_align.svg' => 'icons8_document_align.png',
        'icons8_document_bottom_margin.svg' => 'icons8_document_bottom_margin.png',
        'icons8_document_first_line_margin.svg' => 'icons8_document_first_line_margin.png',
        'icons8_document_left_margin.svg' => 'icons8_document_left_margin.png',
        'icons8_document_line_space.svg' => 'icons8_document_line_space.png',
        'icons8_document_right_margin.svg' => 'icons8_document_right_margin.png',
        'icons8_document_space_width.svg' => 'icons8_document_space_width.png',
        'icons8_document_top_margin.svg' => 'icons8_document_top_margin.png',
        'icons8_folder_arrow.svg' => 'icons8_folder_arrow.png',
        'icons8_font_color.svg' => 'icons8_font_color.png',
        'icons8_font_down.svg' => 'icons8_font_down.png',
        'icons8_font_face.svg' => 'icons8_font_face.png',
        'icons8_font_size.svg' => 'icons8_font_size.png',
        'icons8_font_superscript.svg' => 'icons8_font_superscript.png',
        'icons8_font_up.svg' => 'icons8_font_up.png',
        'icons8_fullscreen.svg' => 'icons8_fullscreen.png',
        'icons8_gesture.svg' => 'icons8_gesture.png',
        'icons8_keyboard.svg' => 'icons8_keyboard.png',
        'icons8_orientation.svg' => 'icons8_orientation.png',
        'icons8_sunrise.svg' => 'icons8_sunrise.png',
        'icons8_switch_profile.svg' => 'icons8_switch_profile.png',
        'icons8_document_2pages.svg' => 'icons8_document_2pages.png',
        'icons8_document_down_ch.svg' => 'icons8_document_down_ch.png',
        'icons8_document_up_ch.svg' => 'icons8_document_up_ch.png',

        'icons8_book_from_gd.svg' => 'icons8_book_from_gd.png',
        'icons8_bookmarks_from_gd.svg' => 'icons8_bookmarks_from_gd.png',
        'icons8_bookmarks_to_gd.svg' => 'icons8_bookmarks_to_gd.png',
        'icons8_book_to_gd.svg' => 'icons8_book_to_gd.png',
        'icons8_document_hang_punct.svg' => 'icons8_document_hang_punct.png',
        'icons8_document_hyp_dic.svg' => 'icons8_document_hyp_dic.png',
        'icons8_document_selection1.svg' => 'icons8_document_selection1.png',
        'icons8_document_selection2.svg' => 'icons8_document_selection2.png',
        'icons8_document_selection_lock.svg' => 'icons8_document_selection_lock.png',
        'icons8_double_tap.svg' => 'icons8_double_tap.png',
        'icons8_folder_star_arrow.svg' => 'icons8_folder_star_arrow.png',
        'icons8_lock_portrait_2.svg' => 'icons8_lock_portrait_2.png',
        'icons8_navigation_toolbar_top.svg' => 'icons8_navigation_toolbar_top.png',
        'icons8_openbook.svg' => 'icons8_openbook.png',
        'icons8_position_from_gd.svg' => 'icons8_position_from_gd.png',
        'icons8_position_to_gd.svg' => 'icons8_position_to_gd.png',
        'icons8_position_to_gd_interval.svg' => 'icons8_position_to_gd_interval.png',
        'icons8_reading.svg' => 'icons8_reading.png',
        'icons8_scroll.svg' => 'icons8_scroll.png',
        'icons8_settings_from_gd.svg' => 'icons8_settings_from_gd.png',
        'icons8_settings_to_gd.svg' => 'icons8_settings_to_gd.png',
        'icons8_sun.svg' => 'icons8_sun.png',
        'icons8_sun_1.svg' => 'icons8_sun_1.png',
        'icons8_folder_az.svg' => 'icons8_folder_az.png'
);

my %ic_menu_list=(
	'icons8_css.svg' => 'icons8_css.png',
        'icons8_page.svg' => 'icons8_page.png',
        'icons8_one_finger.svg' => 'icons8_one_finger.png',
        'icons8_settings.svg' => 'icons8_settings.png',
        'icons8_cursor.svg' => 'icons8_cursor.png',
        'icons8_type_filled.svg' => 'icons8_type_filled.png',
        'icons8_book.svg' => 'icons8_book_2.png',
        'icons8_happy.svg' => 'icons8_happy.png',
        'icons8_link.svg' => 'icons8_link.png',
        'icons8_folder.svg' => 'icons8_folder_2.png',
        'icons8_star.svg' => 'icons8_star.png'

);

my %ic_launcher_list=(
);

my %ic_bigicons_list=(
);

my ($srcfile, $dstfile);
my ($dpi, $size);
my $folder;
my $resfile;
my $cmd;
my $ret;

# action icons
while (($srcfile, $dstfile) = each(%ic_actions_list))
{
	while (($dpi, $size) = each(%ic_actions_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# menu icons
while (($srcfile, $dstfile) = each(%ic_menu_list))
{
	while (($dpi, $size) = each(%ic_menu_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# launcher icons
while (($srcfile, $dstfile) = each(%ic_launcher_list))
{
	while (($dpi, $size) = each(%ic_launcher_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}

# bigicons
while (($srcfile, $dstfile) = each(%ic_bigicons_list))
{
	while (($dpi, $size) = each(%ic_bigicons_sizes))
	{
		$folder = "${TARGET_DIR}/drawable-${dpi}/";
		if (-d $folder)
		{
			$resfile = "${folder}/${dstfile}";
			$cmd = "inkscape -z -e ${resfile} -w ${size} -h ${size} ${srcfile}";
			print "$cmd\n";
			$ret = system($cmd);
			print "Failed!\n" if $ret != 0;
		}
	}
}
