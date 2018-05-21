package org.coolreader.crengine;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.google.android.gms.drive.widget.DataBufferAdapter;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;

import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.coolreader.CoolReader;
import org.coolreader.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

public class GoogleDriveTools {

    private CoolReader mCoolReader = null;
    private  Object mActionObject = null;

    public static final int REQUEST_CODE_NO_ACTION = 0;
    public static final int REQUEST_CODE_SIGN_IN = 10000;
    public static final int REQUEST_CODE_SAVE_SETTINGS = 100001;
    //    public static final int REQUEST_CODE_CREATE_DEF_FOLDER = 100002;
//    public static final int REQUEST_CODE_CREATE_SETT_FOLDER = 100003;
//    public static final int REQUEST_CODE_SEARCH_DEF_FOLDER = 100004;
//    public static final int REQUEST_CODE_SEARCH_SETT_FOLDER = 100005;
    public static final int REQUEST_CODE_LOAD_SETTINGS_LIST = 100006;
    public static final int REQUEST_CODE_LOAD_SETTINGS = 100007;
    public static final int REQUEST_CODE_SAVE_READING_POS = 100008;
    public static final int REQUEST_CODE_LOAD_READING_POS_LIST = 100009;
    public static final int REQUEST_CODE_LOAD_READING_POS = 100010;
    public static final int REQUEST_CODE_SAVE_READING_POS_QUIET = 100011;
    public static final int REQUEST_CODE_SAVE_BOOKMARKS = 100012;
    public static final int REQUEST_CODE_LOAD_BOOKMARKS_LIST = 100013;
    public static final int REQUEST_CODE_LOAD_BOOKMARKS = 100014;
    public static final int REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD = 100015;

    private DriveClient mDriveClient = null;
    private DriveResourceClient mDriveResourceClient = null;
    private DriveFolder folderCR = null;
    private DriveFolder folderSettings = null;

    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount mSignInAccount;
    //private DriveClient mDriveClient;
    //private DriveResourceClient mDriveResourceClient;

    public GoogleDriveTools(CoolReader mCR) {
        this.mCoolReader = mCR;
    }

    private static final String TAG = "GoogleDriveTools";

    public void signInAndDoAnAction(int act, Object obj) {
        Log.i(TAG, "Start sign in");
        mActionObject = obj;
        GoogleSignInClient GoogleSignInClient = buildGoogleSignInClient();
        Intent signInIntent = GoogleSignInClient.getSignInIntent();
        mCoolReader.startActivityForResult(signInIntent, act);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(Drive.SCOPE_FILE)
                        .build();
        return GoogleSignIn.getClient(mCoolReader, signInOptions);
    }

    public boolean CheckSingIn() {
        if ((mDriveClient == null) || (mDriveResourceClient == null)) {
            // Use the last signed in account here since it already have a Drive scope.
            mDriveClient = Drive.getDriveClient(mCoolReader, GoogleSignIn.getLastSignedInAccount(mCoolReader));
            // Build a drive resource client.
            mDriveResourceClient =
                    Drive.getDriveResourceClient(mCoolReader, GoogleSignIn.getLastSignedInAccount(mCoolReader));
        }
        return ((mDriveClient != null) && (mDriveResourceClient != null));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                //		Log.i(TAG, "Sign in request code");
                if (resultCode == Activity.RESULT_OK) {
                    CheckSingIn();
                }
                break;
            case REQUEST_CODE_SAVE_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (save cr3.ini)");
                    if (CheckSingIn())
                        syncReq(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_LOAD_SETTINGS_LIST:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (load saved cr3.ini list)");
                    if (CheckSingIn())
                        syncReq(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_LOAD_SETTINGS:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (load chosen cr3.ini)");
                    if (CheckSingIn())
                        searchCoolReaderFolder(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_SAVE_READING_POS:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (save reading pos)");
                    if (CheckSingIn())
                        syncReq(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_SAVE_READING_POS_QUIET:
                if (resultCode == Activity.RESULT_OK) {
                    if (CheckSingIn()) syncReq(requestCode, mActionObject, true);
                }
                break;
            case REQUEST_CODE_LOAD_READING_POS_LIST:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (load reading pos list)");
                    if (CheckSingIn())
                        syncReq(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_LOAD_READING_POS:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (load chosen reading pos)");
                    if (CheckSingIn())
                        searchCoolReaderFolder(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_SAVE_BOOKMARKS:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (save bookmarks)");
                    if (CheckSingIn())
                        syncReq(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_LOAD_BOOKMARKS_LIST:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (load bookmarks list)");
                    if (CheckSingIn())
                        syncReq(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_LOAD_BOOKMARKS:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (load chosen bookmarks)");
                    if (CheckSingIn())
                        searchCoolReaderFolder(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
            case REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD:
                if (resultCode == Activity.RESULT_OK) {
                    mCoolReader.showToast(mCoolReader.getString(R.string.gd_begin) + " (save current book)");
                    if (CheckSingIn())
                        syncReq(requestCode, mActionObject, false);
                    else
                        mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + " : sing in");
                }
                break;
        }
    }

    private void syncReq(final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "syncReq ...");
        Task<Void> tskReq = mDriveClient.requestSync();
        tskReq.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        if ((nextAction == REQUEST_CODE_SAVE_SETTINGS) ||
                            (nextAction == REQUEST_CODE_LOAD_SETTINGS_LIST) ||
                            (nextAction == REQUEST_CODE_LOAD_SETTINGS) ||
                            (nextAction == REQUEST_CODE_SAVE_READING_POS) ||
                            (nextAction == REQUEST_CODE_LOAD_READING_POS_LIST) ||
                            (nextAction == REQUEST_CODE_LOAD_READING_POS) ||
                            (nextAction == REQUEST_CODE_SAVE_BOOKMARKS) ||
                            (nextAction == REQUEST_CODE_LOAD_BOOKMARKS_LIST) ||
                            (nextAction == REQUEST_CODE_LOAD_BOOKMARKS) ||
                            (nextAction == REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD)
                           ) {
                                searchCoolReaderFolder(nextAction, actionObject, bQuiet);
                           }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error syncReq", e);
                        if (!bQuiet)
                            mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search CoolReader folder");
                    }
                });

    }

    private void searchCoolReaderFolder(final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Searching for google drive CoolReader folder ...");
        getDriveResourceClient().getRootFolder().continueWithTask(new Continuation<DriveFolder, Task<MetadataBuffer>>() {
            @Override
            public Task<MetadataBuffer> then(@NonNull Task<DriveFolder> task) throws Exception {
                DriveFolder parentFolder = task.getResult();
                final Query query = new Query.Builder()
                        .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, "CoolReader"),
                                Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"),
                                Filters.eq(SearchableField.TRASHED, false),
                                Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                        ))
                        .build();
                return getDriveResourceClient().queryChildren(parentFolder, query);
            }
        }).addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (
                                    (nextAction == REQUEST_CODE_SAVE_SETTINGS) ||
                                    (nextAction == REQUEST_CODE_SAVE_READING_POS) ||
                                    (nextAction == REQUEST_CODE_SAVE_BOOKMARKS) ||
                                    (nextAction == REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD)
                               )
                                createCoolReaderFolder(nextAction, actionObject, bQuiet);
                            if (
                                    (nextAction == REQUEST_CODE_LOAD_SETTINGS_LIST) ||
                                    (nextAction == REQUEST_CODE_LOAD_SETTINGS) ||
                                    (nextAction == REQUEST_CODE_LOAD_READING_POS_LIST) ||
                                    (nextAction == REQUEST_CODE_LOAD_READING_POS) ||
                                    (nextAction == REQUEST_CODE_LOAD_BOOKMARKS_LIST) ||
                                    (nextAction == REQUEST_CODE_LOAD_BOOKMARKS)
                               ) {
                                Log.e(TAG, "Error search CoolReader folder");
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": CoolReader folder not found");
                            }
                        } else {
                            Metadata m = metadataBuffer.get(0);
                            if (
                                    (nextAction == REQUEST_CODE_SAVE_SETTINGS) ||
                                    (nextAction == REQUEST_CODE_LOAD_SETTINGS_LIST) ||
                                    (nextAction == REQUEST_CODE_LOAD_SETTINGS)
                               ) {
                                searchSettingsFolder(m.getDriveId().asDriveFolder(), nextAction, actionObject, bQuiet);
                            }
                            if (
                                 (nextAction == REQUEST_CODE_SAVE_READING_POS) ||
                                 (nextAction == REQUEST_CODE_LOAD_READING_POS_LIST) ||
                                 (nextAction == REQUEST_CODE_LOAD_READING_POS)
                               ) {
                                searchReadingPosFolder(m.getDriveId().asDriveFolder(), nextAction, actionObject, bQuiet);
                            }
                            if (
                                 (nextAction == REQUEST_CODE_SAVE_BOOKMARKS) ||
                                 (nextAction == REQUEST_CODE_LOAD_BOOKMARKS_LIST) ||
                                 (nextAction == REQUEST_CODE_LOAD_BOOKMARKS)
                               ) {
                                searchBookmarksFolder(m.getDriveId().asDriveFolder(), nextAction, actionObject, bQuiet);
                            }
                            if (
                                    (nextAction == REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD)
                               ) {
                                searchBooksFolder(m.getDriveId().asDriveFolder(), nextAction, actionObject, bQuiet);
                            }
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search CoolReader folder", e);
                        if (!bQuiet)
                            mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search CoolReader folder");
                    }
                });
    }

    private void searchSettingsFolder(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Searching for google drive settings folder ...");
        final Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, "settings"),
                        Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                ))
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_SAVE_SETTINGS)
                                createSettingsFolder(parentFolder, nextAction, bQuiet);
                            if (
                                    (nextAction == REQUEST_CODE_LOAD_SETTINGS_LIST) ||
                                            (nextAction == REQUEST_CODE_LOAD_SETTINGS)
                                    ) {
                                Log.e(TAG, "Error search settings folder");
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": settings folder not found");
                            }
                        } else {
                            Metadata m = metadataBuffer.get(0);
                            if (nextAction == REQUEST_CODE_SAVE_SETTINGS) {
                                DriveFolder df = m.getDriveId().asDriveFolder();
                                saveSettingsFile(df, bQuiet);
                            }
                            if (nextAction == REQUEST_CODE_LOAD_SETTINGS_LIST) {
                                DriveFolder df = m.getDriveId().asDriveFolder();
                                loadSettingList(df, nextAction, actionObject, bQuiet);
                            }
                            if (nextAction == REQUEST_CODE_LOAD_SETTINGS) {
                                if (actionObject!=null) {
                                    Metadata md = ((Metadata) actionObject);
                                    DriveFile dfl = md.getDriveId().asDriveFile();
                                    loadSettingFile(dfl, nextAction, bQuiet);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search settings folder", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search settings folder");
                        //showMessage(getString(R.string.query_failed));
                        //finish();
                    }
                });
    }

    private void searchReadingPosFolder(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Searching for google drive reading_pos folder ...");
        final Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, "reading_pos"),
                        Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                ))
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_SAVE_READING_POS)
                                createReadingPosFolder(parentFolder, nextAction, actionObject, bQuiet);
                            if (
                                    (nextAction == REQUEST_CODE_LOAD_READING_POS_LIST)
                                    ) {
                                Log.e(TAG, "Error search reading pos folder");
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": reading pos folder not found");
                            }
                        } else {
                            Metadata m = metadataBuffer.get(0);
                            if (nextAction == REQUEST_CODE_SAVE_READING_POS) {
                                DriveFolder df = m.getDriveId().asDriveFolder();
                                searchReadingPosFile(df, nextAction, actionObject, bQuiet);
                            }
                            if (nextAction == REQUEST_CODE_LOAD_READING_POS_LIST) {
                                DriveFolder df = m.getDriveId().asDriveFolder();
                                loadReadingPosList(df, nextAction, actionObject, bQuiet);
                            }
                            if (nextAction == REQUEST_CODE_LOAD_READING_POS) {
                                if (actionObject!=null) {
                                    Metadata md = ((Metadata) actionObject);
                                    DriveFile dfl = md.getDriveId().asDriveFile();
                                    loadReadingPosFile(dfl, nextAction, actionObject, bQuiet);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search reading_pos folder", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search reading_pos folder");
                        //showMessage(getString(R.string.query_failed));
                        //finish();
                    }
                });
    }

    private void searchBookmarksFolder(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Searching for google drive bookmarks folder ...");
        final Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, "bookmarks"),
                        Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                ))
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_SAVE_BOOKMARKS)
                                createBookmarksFolder(parentFolder, nextAction, actionObject, bQuiet);
                            if (
                                    (nextAction == REQUEST_CODE_LOAD_BOOKMARKS_LIST)
                               ) {
                                Log.e(TAG, "Error search bookmarks folder");
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": bookmarks folder not found");
                            }
                        } else {
                            Metadata m = metadataBuffer.get(0);
                            if (nextAction == REQUEST_CODE_SAVE_BOOKMARKS) {
                                DriveFolder df = m.getDriveId().asDriveFolder();
                                searchBookmarksFile(df, nextAction, actionObject, bQuiet);
                            }
                            if (nextAction == REQUEST_CODE_LOAD_BOOKMARKS_LIST) {
                                DriveFolder df = m.getDriveId().asDriveFolder();
                                loadBookmarksList(df, nextAction, actionObject, bQuiet);
                            }
                            if (nextAction == REQUEST_CODE_LOAD_BOOKMARKS) {
                                if (actionObject!=null) {
                                    Metadata md = ((Metadata) actionObject);
                                    DriveFile dfl = md.getDriveId().asDriveFile();
                                    loadBookmarksFile(dfl, nextAction, actionObject, bQuiet);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search bookmarks folder", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search bookmarks folder");
                        //showMessage(getString(R.string.query_failed));
                        //finish();
                    }
                });
    }

    private void searchBooksFolder(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Searching for google drive Books folder ...");
        final Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, "Books"),
                        Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                ))
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD)
                                createBooksFolder(parentFolder, nextAction, actionObject, bQuiet);
                        } else {
                            Metadata m = metadataBuffer.get(0);
                            if (nextAction == REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD) {
                                DriveFolder df = m.getDriveId().asDriveFolder();
                                searchBookFile(df, nextAction, actionObject, bQuiet);
                            }
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search books folder", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search Books folder");
                        //showMessage(getString(R.string.query_failed));
                        //finish();
                    }
                });
    }

    private void loadSettingList(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Load settings list ...");

        final SortOrder sortOrder = new SortOrder.Builder().addSortDescending(SortableField.CREATED_DATE).build();
        final Query query = new Query.Builder()
                .addFilter(Filters.and(
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                )).setSortOrder(sortOrder)
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_LOAD_SETTINGS_LIST) {
                                Log.e(TAG, "Error search settings files");
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": settings files were not found");
                            }
                        } else {
                            if (nextAction == REQUEST_CODE_LOAD_SETTINGS_LIST) {
                                ChooseConfFileDlg dlg = new ChooseConfFileDlg(mCoolReader, metadataBuffer);
                                dlg.show();
                            }
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search files in settings folder", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search files in settings folder");
                    }
                });
    }

    private void loadReadingPosList(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Load reading pos list ...");

        final ReaderView rv = (ReaderView)actionObject;
        if (rv == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": no reading book");
            return;
        }
        final String sBookFName = rv.getBookInfo().getFileInfo().filename;
        CRC32 crc = new CRC32();
        crc.update(sBookFName.getBytes());

        final SortOrder sortOrder = new SortOrder.Builder().addSortDescending(SortableField.CREATED_DATE).build();
        final Query query = new Query.Builder()
                .addFilter(Filters.and(
                        Filters.contains(SearchableField.TITLE, String.valueOf(crc.getValue()) + "_"),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                )).setSortOrder(sortOrder)
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_LOAD_READING_POS_LIST) {
                                Log.e(TAG, "Error search reading pos files");
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": reading pos files were not found");
                            }
                        } else {
                            if (nextAction == REQUEST_CODE_LOAD_READING_POS_LIST) {
                                ChooseReadingPosDlg dlg = new ChooseReadingPosDlg(mCoolReader, metadataBuffer);
                                dlg.show();
                            }
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search files in reading pos folder", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search files in reading pos folder");
                    }
                });
    }

    private void loadBookmarksList(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Load bookmarks list ...");

        final ReaderView rv = (ReaderView)actionObject;
        if (rv == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": no reading book");
            return;
        }
        final String sBookFName = rv.getBookInfo().getFileInfo().filename;
        CRC32 crc = new CRC32();
        crc.update(sBookFName.getBytes());

        final SortOrder sortOrder = new SortOrder.Builder().addSortDescending(SortableField.CREATED_DATE).build();
        final Query query = new Query.Builder()
                .addFilter(Filters.and(
                        Filters.contains(SearchableField.TITLE, String.valueOf(crc.getValue()) + "_"),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                )).setSortOrder(sortOrder)
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_LOAD_BOOKMARKS_LIST) {
                                Log.e(TAG, "Error search bookmarks files");
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": bookmarks files were not found");
                            }
                        } else {
                            if (nextAction == REQUEST_CODE_LOAD_BOOKMARKS_LIST) {
                                ChooseBookmarksDlg dlg = new ChooseBookmarksDlg(mCoolReader, metadataBuffer);
                                dlg.show();
                            }
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search files in bookmarks folder", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search files in bookmarks folder");
                    }
                });
    }

    private void copyFile(String sFin, String sFout) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;

        try {
            in = new FileInputStream(sFin);
            out = new FileOutputStream(sFout);
            int c;
            while ((c = in.read()) != -1) {
                out.write(c);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    private void loadSettingFile(final DriveFile dfl, final int nextAction, final boolean bQuiet) {
        Log.d(TAG, "Load settings file ...");
        Task<DriveContents> openFileTask =
                getDriveResourceClient().openFile(dfl, DriveFile.MODE_READ_ONLY);
        openFileTask
                .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                        DriveContents contents = task.getResult();
                        try {
                            final File fSett = mCoolReader.getSettingsFile(0);
                            String sFName = fSett.getPath();
                            String newName = sFName + ".fromGoogleDrive";
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(contents.getInputStream()));
                            BufferedWriter bw = null;
                            FileWriter fw = null;
                            char[] bytesArray = new char[1000];
                            int bytesRead = 1000;
                            fw = new FileWriter(newName);
                            bw = new BufferedWriter(fw);
                            while (bytesRead != -1) {
                                bytesRead = reader.read(bytesArray, 0, 1000);
                                if (bytesRead != -1) bw.write(bytesArray,0,bytesRead);
                            }
                            bw.close();
                            fw.close();
                            // first delete bk file if exists
                            File file = new File(sFName + ".bk");
                            boolean bWasErr = false;
                            if (file.exists()){
                                if (!file.delete()) {
                                    if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": delete .bk file");
                                    bWasErr = true;
                                };
                            }
                            if (!bWasErr) {
                                try {
                                    copyFile(sFName, sFName + ".bk");
                                } catch (Exception e) {
                                    bWasErr = true;
                                    if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": copy current file to .bk");
                                }
                            }
                            if (!bWasErr) {
                                if (fSett.exists()) {
                                    if (!fSett.delete()) {
                                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": delete cr3.ini file");
                                        bWasErr = true;
                                    };
                                }
                            }
                            if (!bWasErr) {
                                try {
                                    copyFile(newName, sFName);
                                } catch (Exception e) {
                                    bWasErr = true;
                                    if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": copy new file to current");
                                }
                            }
                            if (!bWasErr) {
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_ok) + ": " + newName + " created. Closing app");
                                mCoolReader.finish();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading settings file", e);
                            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": reading settings file");
                        }
                        Task<Void> discardTask = getDriveResourceClient().discardContents(contents);
                        return discardTask;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error reading settings file", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": reading settings file");
                    }
                });
    }

    private void loadReadingPosFile(final DriveFile dfl, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Load reading pos file ...");

        Task<DriveContents> openFileTask =
                getDriveResourceClient().openFile(dfl, DriveFile.MODE_READ_ONLY);
        openFileTask
                .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                        DriveContents contents = task.getResult();
                        try {
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(contents.getInputStream()));

                            String line = null;
                            String sFile = new String();
                            while ((line = reader.readLine()) != null) {
                                sFile += line;
                            }

                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            Bookmark bmk = gson.fromJson(sFile, Bookmark.class);
                            if (bmk != null) {
                                bmk.setTimeStamp(System.currentTimeMillis());
                                if (mCoolReader.getReaderView() != null) {
                                    mCoolReader.getReaderView().savePositionBookmark(bmk);
                                    if ( mCoolReader.getReaderView().getBookInfo()!=null )
                                        mCoolReader.getReaderView().getBookInfo().setLastPosition(bmk);
                                        mCoolReader.getReaderView().goToBookmark(bmk);
                                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_ok) + ": reading pos updated");
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading settings file", e);
                            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": reading settings file");
                        }
                        Task<Void> discardTask = getDriveResourceClient().discardContents(contents);
                        return discardTask;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error reading settings file", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": reading settings file");
                    }
                });
    }

    public static <T> List<T> stringToArray(String s, Class<T[]> clazz) {
        T[] arr = new Gson().fromJson(s, clazz);
        return Arrays.asList(arr); //or return Arrays.asList(new Gson().fromJson(s, clazz)); for a one-liner
    }

    private void loadBookmarksFile(final DriveFile dfl, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Load bookmarks file ...");

        final ReaderView rv = mCoolReader.getReaderView();
        if (rv == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": no reading book");
            return;
        }
        if (rv.getBookInfo() == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": no reading book");
            return;
        }

        Task<DriveContents> openFileTask =
                getDriveResourceClient().openFile(dfl, DriveFile.MODE_READ_ONLY);
        openFileTask
                .continueWithTask(new Continuation<DriveContents, Task<Void>>() {
                    @Override
                    public Task<Void> then(@NonNull Task<DriveContents> task) throws Exception {
                        DriveContents contents = task.getResult();
                        try {
                            BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(contents.getInputStream()));

                            String line = null;
                            String sFile = new String();
                            while ((line = reader.readLine()) != null) {
                                sFile += line;
                            }
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            ArrayList<Bookmark> abmkThis = rv.getBookInfo().getAllBookmarks();
                            ArrayList<Bookmark> abmk = new ArrayList<Bookmark>(stringToArray(sFile, Bookmark[].class));
                            int iCreated = 0;
                            for (Bookmark bmk: abmk) {
                                boolean bFound = false;
                                for (Bookmark bm: abmkThis) {
                                    if (bm.getStartPos().equals(bmk.getStartPos())) bFound = true;
                                }
                                if ((!bFound)&(bmk.getType()!=Bookmark.TYPE_LAST_POSITION)) {
                                    rv.getBookInfo().addBookmark(bmk);
                                    iCreated++;
                                }
                            }
                            rv.highlightBookmarks();
                            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_ok) + ": " + iCreated + " bookmark(s) created");
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading bookmarks file", e);
                            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": reading bookmarks file");
                        }
                        Task<Void> discardTask = getDriveResourceClient().discardContents(contents);
                        return discardTask;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error reading settings file", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": reading settings file");
                    }
                });
    }

    private void createCoolReaderFolder(final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Creating google drive CoolReader folder ...");
        getDriveResourceClient().getRootFolder().continueWithTask(new Continuation<DriveFolder, Task<DriveFolder>>() {
                    @Override
                    public Task<DriveFolder> then(@NonNull Task<DriveFolder> task) throws Exception {
                        DriveFolder parentFolder = task.getResult();
                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle("CoolReader")
                                .setMimeType(DriveFolder.MIME_TYPE)
                                .setStarred(false)
                                .build();
                        return getDriveResourceClient().createFolder(parentFolder, changeSet);
                    }
                })
                .addOnSuccessListener(mCoolReader,
                        new OnSuccessListener<DriveFolder>() {
                            @Override
                            public void onSuccess(DriveFolder driveFolder) {
                                folderCR = driveFolder;
                                if (nextAction == REQUEST_CODE_SAVE_SETTINGS)
                                    createSettingsFolder(driveFolder, nextAction, bQuiet);
                                if (nextAction == REQUEST_CODE_SAVE_READING_POS)
                                    createReadingPosFolder(driveFolder, nextAction, actionObject, bQuiet);
                            }
                        })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error creating CoolReader folder ...");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": create CoolReader folder");
                    }
                });
    }

    private void createSettingsFolder(final DriveFolder parentFolder, final int nextAction, final boolean bQuiet) {
        Log.d(TAG, "Creating google drive settings folders ...");

        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("settings")
                .setMimeType(DriveFolder.MIME_TYPE)
                .setStarred(false)
                .build();
        Task<DriveFolder> tsk = getDriveResourceClient().createFolder(parentFolder, changeSet);
        tsk.addOnSuccessListener(mCoolReader,
                        new OnSuccessListener<DriveFolder>() {
                            @Override
                            public void onSuccess(DriveFolder driveFolder) {
                                folderSettings = driveFolder;
                                if (nextAction == REQUEST_CODE_SAVE_SETTINGS) {
                                    saveSettingsFile(folderSettings, bQuiet);
                                }
                            }
                        })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error create settings folder");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": create settings folder");
                    }
                });
    }

    private void createReadingPosFolder(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Creating google drive reading_pos folders ...");

        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("reading_pos")
                .setMimeType(DriveFolder.MIME_TYPE)
                .setStarred(false)
                .build();
        Task<DriveFolder> tsk = getDriveResourceClient().createFolder(parentFolder, changeSet);
        tsk.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<DriveFolder>() {
                    @Override
                    public void onSuccess(DriveFolder driveFolder) {
                        folderSettings = driveFolder;
                        if (nextAction == REQUEST_CODE_SAVE_READING_POS) {
                            saveReadingPosFile(folderSettings, actionObject, bQuiet);
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error create reading_pos folder");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": create reading_pos folder");
                    }
                });
    }

    private void createBookmarksFolder(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Creating google drive reading_pos folders ...");

        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("bookmarks")
                .setMimeType(DriveFolder.MIME_TYPE)
                .setStarred(false)
                .build();
        Task<DriveFolder> tsk = getDriveResourceClient().createFolder(parentFolder, changeSet);
        tsk.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<DriveFolder>() {
                    @Override
                    public void onSuccess(DriveFolder driveFolder) {
                        folderSettings = driveFolder;
                        if (nextAction == REQUEST_CODE_SAVE_BOOKMARKS) {
                            saveBookmarksFile(folderSettings, actionObject, bQuiet);
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error create bookmarks folder");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": create bookmarks folder");
                    }
                });
    }

    private void createBooksFolder(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Creating google drive Books folders ...");

        final MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle("Books")
                .setMimeType(DriveFolder.MIME_TYPE)
                .setStarred(false)
                .build();
        Task<DriveFolder> tsk = getDriveResourceClient().createFolder(parentFolder, changeSet);
        tsk.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<DriveFolder>() {
                    @Override
                    public void onSuccess(DriveFolder driveFolder) {
                        folderSettings = driveFolder;
                        if (nextAction == REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD) {
                            saveBookFile(folderSettings, actionObject, bQuiet);
                        }
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error create Books folder");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": create Books folder");
                    }
                });
    }

    public void saveSettingsFile(final DriveFolder df, final boolean bQuiet) {
        final File fSett = mCoolReader.getSettingsFile(0);
        Log.d(TAG, "Starting save cr3.ini to drive...");

        final android.text.format.DateFormat dfmt = new android.text.format.DateFormat();
        final CharSequence sFName0 = dfmt.format("yyyy-MM-dd_kkmmss", new java.util.Date());

        final String sFName = sFName0.toString() + "_" + fSett.getName() + "_" + mCoolReader.getAndroid_id();

        // Create content from file
        final Task<DriveFolder> rootFolderTask = getDriveResourceClient().getRootFolder();
        final Task<DriveContents> createContentsTask = getDriveResourceClient().createContents();
        Tasks.whenAll(rootFolderTask, createContentsTask)
                .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                        DriveFolder parent = df; // rootFolderTask.getResult();
                        DriveContents contents = createContentsTask.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        boolean bWasErr = false;
                        try {
                            FileInputStream fin=new FileInputStream(fSett);
                            byte[] buffer = new byte[fin.available()];
                            fin.read(buffer, 0, buffer.length);
                            outputStream.write(buffer, 0, buffer.length);
                        } catch (Exception e) {
                            bWasErr = true;
                            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": Error saving file ("+e.getClass().getSimpleName()+")");
                        }

                        if (!bWasErr) {
                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle(sFName)
                                    .setMimeType("application/octet-stream")
                                    .setStarred(false)
                                    .setDescription("from " + mCoolReader.getModel())
                                    .build();

                            return getDriveResourceClient().createFile(parent, changeSet, contents);
                        }
                        return null;
                    }
                })
                .addOnSuccessListener(mCoolReader,
                        new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_ok)+": "+sFName+" created");
                            }
                        })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error save cr3.ini to drive");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": save cr3.ini file");
                    }
                });
    }

    private void searchReadingPosFile(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Searching for google drive reading_pos file ...");

        final ReaderView rv = (ReaderView)actionObject;
        if (rv == null) {
            mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": no reading book");
            return;
        }
        final String sBookFName = rv.getBookInfo().getFileInfo().filename;
        CRC32 crc = new CRC32();
        crc.update(sBookFName.getBytes());

        final String sFName = String.valueOf(crc.getValue()) + "_" + mCoolReader.getAndroid_id()+".json";

        final Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, sFName),
                        Filters.eq(SearchableField.MIME_TYPE, "text/plain"),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                ))
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_SAVE_READING_POS)
                                saveReadingPosFile(parentFolder, actionObject, bQuiet);
                        } else {
                            Metadata m = metadataBuffer.get(0);
                            if (nextAction == REQUEST_CODE_SAVE_READING_POS) {
                                DriveFile df = m.getDriveId().asDriveFile();
                                getDriveResourceClient().delete(df).
                                        addOnSuccessListener(
                                            new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void v) {
                                                    saveReadingPosFile(parentFolder, actionObject, bQuiet);
                                                }
                                            })
                                        .addOnFailureListener(mCoolReader, new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.e(TAG, "Error delete reading_pos file", e);
                                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": delete reading pos file");
                                    }
                                });
                            }
                        } //else - if file was
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search reading_pos file", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search reading_pos file");
                        //showMessage(getString(R.string.query_failed));
                        //finish();
                    }
                });
    }

    private void searchBookmarksFile(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Searching for google drive bookmarks file ...");

        final ReaderView rv = (ReaderView)actionObject;
        if (rv == null) {
            mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": no reading book");
            return;
        }
        final String sBookFName = rv.getBookInfo().getFileInfo().filename;
        CRC32 crc = new CRC32();
        crc.update(sBookFName.getBytes());

        final String sFName = String.valueOf(crc.getValue()) + "_" + mCoolReader.getAndroid_id()+".json";

        final Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, sFName),
                        Filters.eq(SearchableField.MIME_TYPE, "text/plain"),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                ))
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_SAVE_BOOKMARKS)
                                saveBookmarksFile(parentFolder, actionObject, bQuiet);
                        } else {
                            Metadata m = metadataBuffer.get(0);
                            if (nextAction == REQUEST_CODE_SAVE_BOOKMARKS) {
                                DriveFile df = m.getDriveId().asDriveFile();
                                getDriveResourceClient().delete(df).
                                        addOnSuccessListener(
                                                new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void v) {
                                                        saveBookmarksFile(parentFolder, actionObject, bQuiet);
                                                    }
                                                })
                                        .addOnFailureListener(mCoolReader, new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.e(TAG, "Error delete bookmarks file", e);
                                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": delete bookmarks file");
                                            }
                                        });
                            }
                        } //else - if file was
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search bookmarks file", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search bookmarks file");
                        //showMessage(getString(R.string.query_failed));
                        //finish();
                    }
                });
    }

    private void searchBookFile(final DriveFolder parentFolder, final int nextAction, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Searching for google drive book file ...");

        final ReaderView rv = (ReaderView)actionObject;
        if (rv == null) {
            mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": no reading book");
            return;
        }
        if (rv.getBookInfo() == null) {
            mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": no reading book");
            return;
        }

        FileInfo item = rv.getBookInfo().getFileInfo();
        final String sBookFName = item.filename;
        final String sPathName = rv.getBookInfo().getFileInfo().pathname;
        final String sBookFName2 = item.isArchive && item.arcname != null
                ? new File(item.arcname).getName() : null;
        final String sPathName2 = item.isArchive && item.arcname != null
                ? new File(item.arcname).getPath() : null;

        final String sBookFNameF = sBookFName2 == null ? sBookFName : sBookFName2;
        final String sPathNameF = sPathName2 == null ? sPathName : sPathName2;

        final Query query = new Query.Builder()
                .addFilter(Filters.and(Filters.eq(SearchableField.TITLE, sBookFNameF),
                        Filters.eq(SearchableField.TRASHED, false),
                        Filters.in(SearchableField.PARENTS, parentFolder.getDriveId())
                ))
                .build();
        Task<MetadataBuffer> queryTask = getDriveResourceClient().queryChildren(parentFolder, query);
        queryTask.addOnSuccessListener(mCoolReader,
                new OnSuccessListener<MetadataBuffer>() {
                    @Override
                    public void onSuccess(MetadataBuffer metadataBuffer) {
                        if (metadataBuffer.getCount() == 0) {
                            if (nextAction == REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD)
                                saveBookFile(parentFolder, actionObject, bQuiet);
                        } else {
                            Metadata m = metadataBuffer.get(0);
                            if (nextAction == REQUEST_CODE_SAVE_CURRENT_BOOK_TO_GD) {
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": book has been already saved");
                            }
                        } //else - if file was
                    }
                })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error search bookmarks file", e);
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error) + ": search book file");
                        //showMessage(getString(R.string.query_failed));
                        //finish();
                    }
                });
    }

    public void saveReadingPosFile(final DriveFolder df, final Object actionObject, final boolean bQuiet) {
        final File fSett = mCoolReader.getSettingsFile(0);
        Log.d(TAG, "Starting save reading pos to drive...");

        final ReaderView rv = (ReaderView)actionObject;
        if (rv == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": book was not found");
            return;
        }
        final Bookmark bmk = rv.getCurrentPositionBookmark();

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String prettyJson = gson.toJson(bmk);

        if (bmk == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": pos was not get");
            return;
        }
        //FileInfo fi = rv.getOpenedFileInfo();
        final String sBookFName = rv.getBookInfo().getFileInfo().filename;
        double dPerc = bmk.getPercent();
        dPerc = dPerc / 100;
        final String sPerc = String.format("%5.2f" , dPerc)+"% of ";
        CRC32 crc = new CRC32();
        crc.update(sBookFName.getBytes());

        final String sFName = String.valueOf(crc.getValue()) + "_" + mCoolReader.getAndroid_id()+".json";

        // Create content from file
        final Task<DriveFolder> rootFolderTask = getDriveResourceClient().getRootFolder();
        final Task<DriveContents> createContentsTask = getDriveResourceClient().createContents();
        Tasks.whenAll(rootFolderTask, createContentsTask)
                .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                        DriveFolder parent = df; // rootFolderTask.getResult();
                        DriveContents contents = createContentsTask.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        try {
                            outputStream.write(prettyJson.getBytes(), 0, prettyJson.getBytes().length);
                        } catch (Exception e) {
                        }

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(sFName)
                                .setMimeType("text/plain")
                                .setStarred(false)
                                .setDescription(sPerc+sBookFName+" ~from "+mCoolReader.getModel())
                                .build();

                        return getDriveResourceClient().createFile(parent, changeSet, contents);
                    }
                })
                .addOnSuccessListener(mCoolReader,
                        new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_ok)+": "+sFName+" created");
                            }
                        })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error save reading pos to drive");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": save reading pos file");
                    }
                });
    }

    public void saveBookmarksFile(final DriveFolder df, final Object actionObject, final boolean bQuiet) {
        final File fSett = mCoolReader.getSettingsFile(0);
        Log.d(TAG, "Starting save bookmarks to drive...");

        final ReaderView rv = (ReaderView)actionObject;
        if (rv == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": book was not found");
            return;
        }
        if (rv.getBookInfo() == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": book was not found");
            return;
        }
        final ArrayList<Bookmark> abmk = rv.getBookInfo().getAllBookmarks();

        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final String prettyJson = gson.toJson(abmk);

        //FileInfo fi = rv.getOpenedFileInfo();
        final String sBookFName = rv.getBookInfo().getFileInfo().filename;
        CRC32 crc = new CRC32();
        crc.update(sBookFName.getBytes());

        final String sFName = String.valueOf(crc.getValue()) + "_" + mCoolReader.getAndroid_id()+".json";

        // Create content from file
        final Task<DriveFolder> rootFolderTask = getDriveResourceClient().getRootFolder();
        final Task<DriveContents> createContentsTask = getDriveResourceClient().createContents();
        Tasks.whenAll(rootFolderTask, createContentsTask)
                .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                        DriveFolder parent = df; // rootFolderTask.getResult();
                        DriveContents contents = createContentsTask.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        try {
                            outputStream.write(prettyJson.getBytes(), 0, prettyJson.getBytes().length);
                        } catch (Exception e) {
                        }

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(sFName)
                                .setMimeType("text/plain")
                                .setStarred(false)
                                .setDescription(String.valueOf(abmk.size())+" bookmark(s) of "+sBookFName+" ~from "+mCoolReader.getModel())
                                .build();

                        return getDriveResourceClient().createFile(parent, changeSet, contents);
                    }
                })
                .addOnSuccessListener(mCoolReader,
                        new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_ok)+": "+sFName+" created");
                            }
                        })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error save bookmarks to drive");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": save bookmarks file");
                    }
                });
    }

    public void saveBookFile(final DriveFolder df, final Object actionObject, final boolean bQuiet) {
        Log.d(TAG, "Starting save book to drive...");

        final ReaderView rv = (ReaderView)actionObject;
        if (rv == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": book was not found");
            return;
        }
        if (rv.getBookInfo() == null) {
            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": book was not found");
            return;
        }
        FileInfo item = rv.getBookInfo().getFileInfo();
        final String sBookFName = item.filename;
        final String sPathName = rv.getBookInfo().getFileInfo().pathname;
        final String sBookFName2 = item.isArchive && item.arcname != null
                ? new File(item.arcname).getName() : null;
        final String sPathName2 = item.isArchive && item.arcname != null
                ? new File(item.arcname).getPath() : null;

        final String sBookFNameF = sBookFName2 == null ? sBookFName : sBookFName2;
        final String sPathNameF = sPathName2 == null ? sPathName : sPathName2;

        // Create content from file
        final Task<DriveFolder> rootFolderTask = getDriveResourceClient().getRootFolder();
        final Task<DriveContents> createContentsTask = getDriveResourceClient().createContents();
        Tasks.whenAll(rootFolderTask, createContentsTask)
                .continueWithTask(new Continuation<Void, Task<DriveFile>>() {
                    @Override
                    public Task<DriveFile> then(@NonNull Task<Void> task) throws Exception {
                        DriveFolder parent = df; // rootFolderTask.getResult();
                        DriveContents contents = createContentsTask.getResult();
                        OutputStream outputStream = contents.getOutputStream();
                        boolean bWasErr = false;
                        try {
                            FileInputStream fin=new FileInputStream(sPathNameF);
                            byte[] buffer = new byte[fin.available()];
                            fin.read(buffer, 0, buffer.length);
                            outputStream.write(buffer, 0, buffer.length);
                        } catch (Exception e) {
                            bWasErr = true;
                            if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": Error saving file ("+e.getClass().getSimpleName()+")");
                        }

                        if (!bWasErr) {
                            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                    .setTitle(sBookFNameF)
                                    .setMimeType("application/octet-stream")
                                    .setStarred(false)
                                    .setDescription("Saved from " + mCoolReader.getModel()+" ("+mCoolReader.getAndroid_id()+")")
                                    .build();
                            return getDriveResourceClient().createFile(parent, changeSet, contents);
                        }
                        return null;
                    }
                })
                .addOnSuccessListener(mCoolReader,
                        new OnSuccessListener<DriveFile>() {
                            @Override
                            public void onSuccess(DriveFile driveFile) {
                                if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_ok)+": "+sBookFNameF+" created");
                            }
                        })
                .addOnFailureListener(mCoolReader, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error save book to drive");
                        if (!bQuiet) mCoolReader.showToast(mCoolReader.getString(R.string.gd_error)+": save book file");
                    }
                });
    }

    public DriveResourceClient getDriveResourceClient() {
        return mDriveResourceClient;
    }

}
