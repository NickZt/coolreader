package org.coolreader.db;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.dropbox.core.v1.DbxEntry;

import org.coolreader.CoolReader;
import org.coolreader.crengine.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class CRDBService extends Service {
	public static final Logger log = L.create("db");
	public static final Logger vlog = L.create("db", Log.ASSERT);

	public MainDB getMainDB() {
		return mainDB;
	}

	public CoverDB getCoverDB() {
		return coverDB;
	}

	private MainDB mainDB = new MainDB();
    private CoverDB coverDB = new CoverDB();
	
    @Override
    public void onCreate() {
    	log.i("onCreate()");
    	mThread = new ServiceThread("crdb");
    	mThread.start();
    	execTask(new OpenDatabaseTask());
    }

    public void reopenDatabase() {
		execTask(new ReOpenDatabaseTask());
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log.i("Received start id " + startId + ": " + intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	log.i("onDestroy()");
    	execTask(new CloseDatabaseTask());
    	mThread.stop(5000);
    }

    private File getDatabaseDir() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				return getFilesDir();
			}
		}
    	//File storage = Environment.getExternalStorageDirectory();
    	File storage = DeviceInfo.EINK_NOOK ? new File("/media/") : Environment.getExternalStorageDirectory();
    	//File cr3dir = new File(storage, ".cr3");
		File cr3dir = new File(storage, "cr3e");
		if (cr3dir.isDirectory())
    		cr3dir.mkdirs();
    	if (!cr3dir.isDirectory() || !cr3dir.canWrite()) {
	    	log.w("Cannot use " + cr3dir + " for writing database, will use data directory instead");
	    	log.w("getFilesDir=" + getFilesDir() + " getDataDirectory=" + Environment.getDataDirectory());
    		cr3dir = getFilesDir(); //Environment.getDataDirectory();
    	}
    	log.i("DB directory: " + cr3dir);
    	return cr3dir;
    }
    
    private class OpenDatabaseTask extends Task {
    	public OpenDatabaseTask() {
    		super("OpenDatabaseTask");
    	}
    	
		@Override
		public void work() {
	    	open();
		}

		private boolean open() {
	    	File dir = getDatabaseDir();
	    	boolean res = mainDB.open(dir);
	    	res = coverDB.open(dir) && res;
	    	if (!res) {
	    		mainDB.close();
	    		coverDB.close();
	    	}
	    	return res;
	    }
	    
    }

    private class CloseDatabaseTask extends Task {
    	public CloseDatabaseTask() {
    		super("CloseDatabaseTask");
    	}

    	@Override
		public void work() {
	    	close();
		}

		private void close() {
			clearCaches();
    		mainDB.close();
    		coverDB.close();
	    }
    }

	private class ReOpenDatabaseTask extends Task {
		public ReOpenDatabaseTask() {
			super("ReOpenDatabaseTask");
		}

		@Override
		public void work() {
			close();
			open();
		}

		private boolean open() {
			File dir = getDatabaseDir();
			boolean res = mainDB.open(dir);
			res = coverDB.open(dir) && res;
			if (!res) {
				mainDB.close();
				coverDB.close();
			}
			return res;
		}

		private void close() {
			clearCaches();
			mainDB.close();
			coverDB.close();
		}
	}

	private FlushDatabaseTask lastFlushTask;
    private class FlushDatabaseTask extends Task {
    	private boolean force;
    	public FlushDatabaseTask(boolean force) {
    		super("FlushDatabaseTask");
    		this.force = force;
    		lastFlushTask = this;
    	}
		@Override
		public void work() {
			long elapsed = Utils.timeInterval(lastFlushTime);
			if (force || (lastFlushTask == this && elapsed > MIN_FLUSH_INTERVAL)) {
		    	mainDB.flush();
		    	coverDB.flush();
		    	if (!force)
		    		lastFlushTime = Utils.timeStamp();
			}
		}
    }
    
    private void clearCaches() {
		mainDB.clearCaches();
		coverDB.clearCaches();
    }

    private static final long MIN_FLUSH_INTERVAL = 30000; // 30 seconds
    private long lastFlushTime;
    
    /**
     * Schedule flush.
     */
    private void flush() {
   		execTask(new FlushDatabaseTask(false), MIN_FLUSH_INTERVAL);
    }

    /**
     * Flush ASAP.
     */
    private void forceFlush() {
   		execTask(new FlushDatabaseTask(true));
    }

    public static class FileInfoCache {
    	private ArrayList<FileInfo> list = new ArrayList<FileInfo>();
    	public void add(FileInfo item) {
    		list.add(item);
    	}
    	public void clear() {
    		list.clear();
    	}
    }

	public interface SearchHistoryLoadingCallback {
		void onSearchHistoryLoaded(ArrayList<String> searches);
	}

	public interface UserDicLoadingCallback {
		void onUserDicLoaded(HashMap<String, UserDicEntry> dic);
	}

	public interface DicSearchHistoryLoadingCallback {
		void onDicSearchHistoryLoaded(List<DicSearchHistoryEntry> dic);
	}

	//=======================================================================================
    // OPDS catalogs access code
    //=======================================================================================
    public interface OPDSCatalogsLoadingCallback {
    	void onOPDSCatalogsLoaded(ArrayList<FileInfo> catalogs);
    }
    
	public void saveOPDSCatalog(final Long id, final String url, final String name,
								final String username, final String password,
								final String proxy_addr, final String proxy_port,
								final String proxy_uname, final String proxy_passw,
								final int onion_def_proxy
								) {
		execTask(new Task("saveOPDSCatalog") {
			@Override
			public void work() {
				mainDB.saveOPDSCatalog(id, url, name, username, password,
						proxy_addr, proxy_port,
						proxy_uname, proxy_passw,
				  		onion_def_proxy);
			}
		});
	}

	public void saveSearchHistory(final BookInfo book, final String sHist) {
		execTask(new Task("saveSearchHistory") {
			@Override
			public void work() {
				mainDB.saveSearchHistory(book, sHist);
			}
		});
	}

	public void saveUserDic(final UserDicEntry ude, final int action) {
		execTask(new Task("saveUserDic") {
			@Override
			public void work() {
				mainDB.saveUserDic(ude, action);
			}
		});
	}

	public void updateDicSearchHistory(final DicSearchHistoryEntry dshe, final int action) {
		execTask(new Task("updateDicSearchHistory") {
			@Override
			public void work() {
				mainDB.updateDicSearchHistory(dshe, action);
			}
		});
	}

	public void clearSearchHistory(final BookInfo book) {
		execTask(new Task("clearSearchHistory") {
			@Override
			public void work() {
				mainDB.clearSearchHistory(book);
			}
		});
	}
	
	public void updateOPDSCatalog(final String url, final String field, final String value) {
		execTask(new Task("saveOPDSCatalog") {
			@Override
			public void work() {
				mainDB.updateOPDSCatalog(url, field, value);
			}
		});
	}	

	public void loadOPDSCatalogs(final OPDSCatalogsLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadOPDSCatalogs") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<FileInfo>(); 
				mainDB.loadOPDSCatalogs(list);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onOPDSCatalogsLoaded(list);
					}
				});
			}
		});
	}

	public void loadSearchHistory(final BookInfo book, final SearchHistoryLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadSearchHistory") {
			@Override
			public void work() {
				final ArrayList<String> list = mainDB.loadSearchHistory(book);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onSearchHistoryLoaded(list);
					}
				});
			}
		});
	}

	public void loadUserDic(final UserDicLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadUserDic") {
			@Override
			public void work() {
				final HashMap<String, UserDicEntry> list = mainDB.loadUserDic();
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onUserDicLoaded(list);
					}
				});
			}
		});
	}

	public void loadDicSearchHistory(final DicSearchHistoryLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadDicSearchHistory") {
			@Override
			public void work() {
				final List<DicSearchHistoryEntry> list = mainDB.loadDicSearchHistory();
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onDicSearchHistoryLoaded(list);
					}
				});
			}
		});
	}

	public void removeOPDSCatalog(final Long id) {
		execTask(new Task("removeOPDSCatalog") {
			@Override
			public void work() {
				mainDB.removeOPDSCatalog(id);
			}
		});
	}

	//=======================================================================================
    // coverpage DB access code
    //=======================================================================================
    public interface CoverpageLoadingCallback {
    	void onCoverpageLoaded(FileInfo fileInfo, byte[] data);
    }

	public void saveBookCoverpage(final FileInfo fileInfo, final byte[] data) {
		if (data == null)
			return;
		execTask(new Task("saveBookCoverpage") {
			@Override
			public void work() {
				coverDB.saveBookCoverpage(fileInfo.getPathName(), data);
			}
		});
		flush();
	}
	
	public void loadBookCoverpage(final FileInfo fileInfo, final CoverpageLoadingCallback callback, final Handler handler) 
	{
		execTask(new Task("loadBookCoverpage") {
			@Override
			public void work() {
				final byte[] data = coverDB.loadBookCoverpage(fileInfo.getPathName());
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onCoverpageLoaded(fileInfo, data);
					}
				});
			}
		});
	}
	
	public void deleteCoverpage(final String bookId) {
		execTask(new Task("deleteCoverpage") {
			@Override
			public void work() {
				coverDB.deleteCoverpage(bookId);
			}
		});
		flush();
	}

	//=======================================================================================
    // Item groups access code
    //=======================================================================================
    public interface ItemGroupsLoadingCallback {
    	void onItemGroupsLoaded(FileInfo parent);
    }

    public interface FileInfoLoadingCallback {
		void onFileInfoListLoadBegin(String prefix);
    	void onFileInfoListLoaded(ArrayList<FileInfo> list, String prefix);
    }

	public interface FileInfo1LoadingCallback {
		void onFileInfoListLoadBegin();
		void onFileInfoLoaded(FileInfo fi);
	}
    
    public interface RecentBooksLoadingCallback {
		void onRecentBooksListLoadBegin();
    	void onRecentBooksListLoaded(ArrayList<BookInfo> bookList);
    }
    
    public interface BookInfoLoadingCallback {
    	void onBooksInfoLoaded(BookInfo bookInfo);
    }
    
    public interface BookSearchCallback {
		void onBooksSearchBegin();
    	void onBooksFound(ArrayList<FileInfo> fileList);
    }
    
	public void loadAuthorsList(FileInfo parent, String filterSeries, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent); 
		execTask(new Task("loadAuthorsList") {
			@Override
			public void work() {
				mainDB.loadAuthorsList(p, filterSeries);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onItemGroupsLoaded(p);
					}
				});
			}
		});
	}

	public void loadGenresList(FileInfo parent, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent);
		execTask(new Task("loadGenresList") {
			@Override
			public void work() {
				mainDB.loadGenresList(p);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onItemGroupsLoaded(p);
					}
				});
			}
		});
	}

	public void loadSeriesList(FileInfo parent, String filterAuthor, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent); 
		execTask(new Task("loadSeriesList") {
			@Override
			public void work() {
				mainDB.loadSeriesList(p, filterAuthor);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onItemGroupsLoaded(p);
					}
				});
			}
		});
	}

    public void loadByDateList(FileInfo parent, final String field, final ItemGroupsLoadingCallback callback, final Handler handler) {
        final FileInfo p = new FileInfo(parent);
        execTask(new Task("loadByDateList") {
            @Override
            public void work() {
                mainDB.loadByDateList(p, field);
                sendTask(handler, new Runnable() {
                    @Override
                    public void run() {
                        callback.onItemGroupsLoaded(p);
                    }
                });
            }
        });
    }
	
	public void loadTitleList(FileInfo parent, final ItemGroupsLoadingCallback callback, final Handler handler) {
		final FileInfo p = new FileInfo(parent); 
		execTask(new Task("loadTitleList") {
			@Override
			public void work() {
				mainDB.loadTitleList(p);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onItemGroupsLoaded(p);
					}
				});
			}
		});
	}

	public void findAuthorBooks(final long authorId, final String addFilter, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findAuthorBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<FileInfo>();
				mainDB.findAuthorBooks(list, authorId, addFilter);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onFileInfoListLoaded(list, FileInfo.AUTHOR_GROUP_PREFIX);
					}
				});
			}
		});
	}

	public void findGenreBooks(final long genreId, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findGenreBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<FileInfo>();
				mainDB.findGenreBooks(list, genreId);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onFileInfoListLoaded(list, FileInfo.GENRE_GROUP_PREFIX);
					}
				});
			}
		});
	}
	
	public void findSeriesBooks(final long seriesId, final String addFilter, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findSeriesBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<FileInfo>();
				mainDB.findSeriesBooks(list, seriesId, addFilter);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onFileInfoListLoaded(list, FileInfo.SERIES_GROUP_PREFIX);
					}
				});
			}
		});
	}

	public void findByDateBooks(final long bookdateId, final String field, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findByDateBooks") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<FileInfo>();
				mainDB.findByDateBooks(list, field, bookdateId);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						String fld = "";
						if (field.equals("book_date_n")) fld = FileInfo.BOOK_DATE_GROUP_PREFIX;
						if (field.equals("doc_date_n")) fld = FileInfo.DOC_DATE_GROUP_PREFIX;
						if (field.equals("publ_year_n")) fld = FileInfo.PUBL_YEAR_GROUP_PREFIX;
						if (field.equals("file_create_time")) fld = FileInfo.FILE_DATE_GROUP_PREFIX;
						callback.onFileInfoListLoaded(list, fld);
					}
				});
			}
		});
	}

	public void findBooksByRating(final int minRate, final int maxRate, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findBooksByRating") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<FileInfo>();
				mainDB.findBooksByRating(list, minRate, maxRate);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onFileInfoListLoaded(list, FileInfo.RATING_TAG);
					}
				});
			}
		});
	}

	public void findBooksByState(final int state, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("findBooksByState") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = new ArrayList<FileInfo>();
				mainDB.findBooksByState(list, state);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onFileInfoListLoaded(list, FileInfo.STATE_TAG);
					}
				});
			}
		});
	}

	public void loadRecentBooks(final int maxCount, final RecentBooksLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadRecentBooks") {
			@Override
			public void work() {
				final ArrayList<BookInfo> list = mainDB.loadRecentBooks(maxCount);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onRecentBooksListLoaded(list);
					}
				});
			}
		});
	}
	
	public void sync(final Runnable callback, final Handler handler) {
		execTask(new Task("sync") {
			@Override
			public void work() {
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.run();
					}
				});
			}
		});
	}
	
	public void findByPatterns(final int maxCount, final String author, final String title, final String series, final String filename, final BookSearchCallback callback, final Handler handler) {
		execTask(new Task("findByPatterns") {
			@Override
			public void work() {
				callback.onBooksSearchBegin();
				final ArrayList<FileInfo> list = mainDB.findByPatterns(maxCount, author, title, series, filename);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onBooksFound(list);
					}
				});
			}
		});
	}

	private ArrayList<FileInfo> deepCopyFileInfos(final Collection<FileInfo> src) {
		final ArrayList<FileInfo> list = new ArrayList<FileInfo>(src.size());
		for (FileInfo fi : src)
			list.add(new FileInfo(fi));
		return list;
	}
	
	public void saveFileInfos(final Collection<FileInfo> list) {
		execTask(new Task("saveFileInfos") {
			@Override
			public void work() {
				mainDB.saveFileInfos(list);
			}
		});
		flush();
	}

	public void loadBookInfo(final FileInfo fileInfo, final BookInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadBookInfo") {
			@Override
			public void work() {
				final BookInfo bookInfo = mainDB.loadBookInfo(fileInfo);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onBooksInfoLoaded(bookInfo);
					}
				});
			}
		});
	}

	public void loadFileInfoByOPDSLink(final String opdsLink, final FileInfo1LoadingCallback callback, final Handler handler) {
		execTask(new Task("loadFileInfoByOPDSLink") {
			@Override
			public void work() {
				if (StrUtils.isEmptyStr(opdsLink)) {
					sendTask(handler, new Runnable() {
						@Override
						public void run() {
							callback.onFileInfoLoaded(null);
						}
					});
				} else {
					final FileInfo fileInfo = mainDB.loadFileInfoByOPDSLink(opdsLink);
					sendTask(handler, new Runnable() {
						@Override
						public void run() {
							callback.onFileInfoLoaded(fileInfo);
						}
					});
				}
			}
		});
	}

	public void loadFileInfos(final ArrayList<String> pathNames, final FileInfoLoadingCallback callback, final Handler handler) {
		execTask(new Task("loadFileInfos") {
			@Override
			public void work() {
				final ArrayList<FileInfo> list = mainDB.loadFileInfos(pathNames);
				sendTask(handler, new Runnable() {
					@Override
					public void run() {
						callback.onFileInfoListLoaded(list, "");
					}
				});
			}
		});
	}
	
	public void saveBookInfo(final BookInfo bookInfo) {
		execTask(new Task("saveBookInfo") {
			@Override
			public void work() {
				mainDB.saveBookInfo(bookInfo);
			}
		});
		flush();
	}
	
	public void deleteBook(final FileInfo fileInfo)	{
		execTask(new Task("deleteBook") {
			@Override
			public void work() {
				mainDB.deleteBook(fileInfo);
				coverDB.deleteCoverpage(fileInfo.getPathName());
			}
		});
		flush();
	}
	
	public void deleteBookmark(final Bookmark bm) {
		execTask(new Task("deleteBookmark") {
			@Override
			public void work() {
				mainDB.deleteBookmark(bm);
			}
		});
		flush();
	}
	
	public void setPathCorrector(final MountPathCorrector corrector) {
		execTask(new Task("setPathCorrector") {
			@Override
			public void work() {
				mainDB.setPathCorrector(corrector);
			}
		});
	}

	public void deleteRecentPosition(final FileInfo fileInfo) {
		execTask(new Task("deleteRecentPosition") {
			@Override
			public void work() {
				mainDB.deleteRecentPosition(fileInfo);
			}
		});
		flush();
	}

    //=======================================================================================
    // Favorite folders access code
    //=======================================================================================
    public void createFavoriteFolder(final FileInfo folder) {
   		execTask(new Task("createFavoriteFolder") {
   			@Override
   			public void work() {
   				mainDB.createFavoritesFolder(folder);
   			}
   		});
        flush();
   	}

    public void loadFavoriteFolders(final FileInfoLoadingCallback callback, final Handler handler) {
   		execTask(new Task("loadFavoriteFolders") {
            @Override
            public void work() {
                final ArrayList<FileInfo> favorites = mainDB.loadFavoriteFolders();
                sendTask(handler, new Runnable() {
                    @Override
                    public void run() {
                        callback.onFileInfoListLoaded(favorites, "");
                    }
                });
            }
        });
   	}

    public void updateFavoriteFolder(final FileInfo folder) {
   		execTask(new Task("updateFavoriteFolder") {
   			@Override
   			public void work() {
   				mainDB.updateFavoriteFolder(folder);
   			}
   		});
        flush();
   	}

    public void deleteFavoriteFolder(final FileInfo folder) {
   		execTask(new Task("deleteFavoriteFolder") {
   			@Override
   			public void work() {
   				mainDB.deleteFavoriteFolder(folder);
   			}
   		});
        flush();
   	}

	private abstract class Task implements Runnable {
		private final String name;
		public Task(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return "Task[" + name + "]";
		}

		@Override
		public void run() {
			long ts = Utils.timeStamp();
			vlog.v(toString() + " started");
			try {
				work();
			} catch (Exception e) {
				log.e("Exception while running DB task in background", e);
			}
			vlog.v(toString() + " finished in " + Utils.timeInterval(ts) + " ms");
		}
		
		public abstract void work();
	}
	
	/**
	 * Execute runnable in CDRDBService background thread.
	 * Exceptions will be ignored, just dumped into log.
	 * @param task is Runnable to execute
	 */
	private void execTask(final Task task) {
		vlog.v("Posting task " + task);
		mThread.post(task);
	}
	
	/**
	 * Execute runnable in CDRDBService background thread, delayed.
	 * Exceptions will be ignored, just dumped into log.
	 * @param task is Runnable to execute
	 */
	private void execTask(final Task task, long delay) {
		vlog.v("Posting task " + task + " with delay " + delay);
		mThread.postDelayed(task, delay);
	}
	
	/**
	 * Send task to handler, if specified, otherwise run immediately.
	 * Exceptions will be ignored, just dumped into log.
	 * @param handler is handler to send task to, null to run immediately
	 * @param task is Runnable to execute
	 */
	private void sendTask(Handler handler, Runnable task) {
		try {
			if (handler != null) {
				vlog.v("Senging task to " + handler.toString());
				handler.post(task);
			} else {
				vlog.v("No Handler provided: executing task in current thread");
				task.run();
			}
		} catch (Exception e) {
			log.e("Exception in DB callback", e);
		}
	}

	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     * Provides interface for asynchronous operations with database.
     */
    public class LocalBinder extends Binder {
        public CRDBService getService() {
            return CRDBService.this;
        }
        
    	public void saveBookCoverpage(final FileInfo fileInfo, byte[] data) {
    		getService().saveBookCoverpage(fileInfo, data);
    	}
    	
    	public void loadBookCoverpage(final FileInfo fileInfo, final CoverpageLoadingCallback callback) {
    		getService().loadBookCoverpage(new FileInfo(fileInfo), callback, new Handler());
    	}
    	
    	public void loadOPDSCatalogs(final OPDSCatalogsLoadingCallback callback) {
    		getService().loadOPDSCatalogs(callback, new Handler());
    	}

    	public void saveOPDSCatalog(final Long id, final String url, final String name,
									final String username, final String password,
									final String proxy_addr, final String proxy_port,
									final String proxy_uname, final String proxy_passw,
									final int onion_def_proxy
									) {
    		getService().saveOPDSCatalog(id, url, name, username, password,
					proxy_addr,proxy_port,
					proxy_uname, proxy_passw,
					onion_def_proxy);
    	}

    	public void updateOPDSCatalog(final String url, final String field, final String value) {
    		getService().updateOPDSCatalog(url, field, value);
    	}

    	public void removeOPDSCatalog(final Long id) {
    		getService().removeOPDSCatalog(id);
    	}

    	public void loadAuthorsList(FileInfo parent, String filterSeries, final ItemGroupsLoadingCallback callback) {
    		getService().loadAuthorsList(parent, filterSeries, callback, new Handler());
    	}

		public void loadGenresList(FileInfo parent, final ItemGroupsLoadingCallback callback) {
			getService().loadGenresList(parent, callback, new Handler());
		}

    	public void loadSeriesList(FileInfo parent, String filterAuthor, final ItemGroupsLoadingCallback callback) {
    		getService().loadSeriesList(parent, filterAuthor, callback, new Handler());
    	}

        public void loadByDateList(FileInfo parent, final String field, final ItemGroupsLoadingCallback callback) {
            getService().loadByDateList(parent, field, callback, new Handler());
        }
    	
    	public void loadTitleList(FileInfo parent, final ItemGroupsLoadingCallback callback) {
    		getService().loadTitleList(parent, callback, new Handler());
    	}

    	public void loadAuthorBooks(long authorId, String addFilter, FileInfoLoadingCallback callback) {
    		getService().findAuthorBooks(authorId, addFilter, callback, new Handler());
    	}

		public void loadGenreBooks(long genreId, FileInfoLoadingCallback callback) {
			getService().findGenreBooks(genreId, callback, new Handler());
		}
    	
    	public void loadSeriesBooks(long seriesId, String addFilter, FileInfoLoadingCallback callback) {
    		getService().findSeriesBooks(seriesId, addFilter, callback, new Handler());
    	}

		public void loadByDateBooks(long bookdateId, final String field, FileInfoLoadingCallback callback) {
			getService().findByDateBooks(bookdateId, field, callback, new Handler());
		}

		public void loadSearchHistory(BookInfo book, SearchHistoryLoadingCallback callback) {
			getService().loadSearchHistory(book, callback, new Handler());
		}

		public void loadUserDic(UserDicLoadingCallback callback) {
			getService().loadUserDic(callback, new Handler());
		}

		public void loadDicSearchHistory(DicSearchHistoryLoadingCallback callback) {
			getService().loadDicSearchHistory(callback, new Handler());
		}

    	public void loadBooksByRating(int minRating, int maxRating, FileInfoLoadingCallback callback) {
    		getService().findBooksByRating(minRating, maxRating, callback, new Handler());
    	}

    	public void loadBooksByState(int state, FileInfoLoadingCallback callback) {
    		getService().findBooksByState(state, callback, new Handler());
    	}

    	public void loadRecentBooks(final int maxCount, final RecentBooksLoadingCallback callback) {
    		getService().loadRecentBooks(maxCount, callback, new Handler());
    	}

    	public void sync(final Runnable callback) {
    		getService().sync(callback, new Handler());
    	}

    	public void saveFileInfos(final Collection<FileInfo> list) {
    		getService().saveFileInfos(deepCopyFileInfos(list));
    	}
    	
    	public void findByPatterns(final int maxCount, final String author, final String title, final String series, final String filename, final BookSearchCallback callback) {
    		getService().findByPatterns(maxCount, author, title, series, filename, callback, new Handler());
    	}
    	
    	public void loadFileInfos(final ArrayList<String> pathNames, final FileInfoLoadingCallback callback) {
    		getService().loadFileInfos(pathNames, callback, new Handler());
    	}

    	public void deleteBook(final FileInfo fileInfo)	{
    		getService().deleteBook(new FileInfo(fileInfo));
    	}

    	public void saveBookInfo(final BookInfo bookInfo) {
    		getService().saveBookInfo(new BookInfo(bookInfo));
    	}

		public void saveSearchHistory(final BookInfo book, String sHist) {
			getService().saveSearchHistory(new BookInfo(book), sHist);
		}

		public void saveUserDic(final UserDicEntry ude, final int action) {
			getService().saveUserDic(ude, action);
		}

		public void updateDicSearchHistory(final DicSearchHistoryEntry dshe, final int action, CoolReader act) {
        	UserDicDlg.mDicSearchHistoryAll.add(0,dshe);
        	int idx = -1;
        	if (UserDicDlg.mDicSearchHistoryAll.size()>0)
        		for (int i=1; i < UserDicDlg.mDicSearchHistoryAll.size(); i++) {
					if (StrUtils.getNonEmptyStr(UserDicDlg.mDicSearchHistoryAll.get(i).getSearch_text(),true).equals(dshe.getSearch_text()))
						idx = i;
				}
        	if (idx > 0) UserDicDlg.mDicSearchHistoryAll.remove(idx);
        	if (act.getmReaderFrame() != null)
        		if (act.getmReaderFrame().getUserDicPanel()!=null)
					act.getmReaderFrame().getUserDicPanel().updateUserDicWords();
			getService().updateDicSearchHistory(dshe, action);
		}

		public void clearSearchHistory(final BookInfo book) {
			getService().clearSearchHistory(new BookInfo(book));
		}

    	public void deleteRecentPosition(final FileInfo fileInfo)	{
    		getService().deleteRecentPosition(new FileInfo(fileInfo));
    	}
    	
    	public void deleteBookmark(final Bookmark bm) {
    		getService().deleteBookmark(new Bookmark(bm));
    	}

    	public void loadBookInfo(final FileInfo fileInfo, final BookInfoLoadingCallback callback) {
    		getService().loadBookInfo(new FileInfo(fileInfo), callback, new Handler());
    	}

		public void loadFileInfoByOPDSLink(final String opdsLink, final FileInfo1LoadingCallback callback) {
			getService().loadFileInfoByOPDSLink(opdsLink, callback, new Handler());
		}

		public void createFavoriteFolder(final FileInfo folder) {
            getService().createFavoriteFolder(folder);
       	}

        public void loadFavoriteFolders(FileInfoLoadingCallback callback) {
            getService().loadFavoriteFolders(callback, new Handler());
       	}

        public void updateFavoriteFolder(final FileInfo folder) {
            getService().updateFavoriteFolder(folder);
       	}

        public void deleteFavoriteFolder(final FileInfo folder) {
            getService().deleteFavoriteFolder(folder);
       	}


    	public void setPathCorrector(MountPathCorrector corrector) {
    		getService().setPathCorrector(corrector);
    	}
    	
    	public void flush() {
    		getService().forceFlush();
    	}

    	public void reopenDatabase() {
        	getService().reopenDatabase();
		}
    }

    @Override
    public IBinder onBind(Intent intent) {
	    log.i("onBind(): " + intent);
        return mBinder;
    }

    @Override
    public void onRebind (Intent intent) {
        log.i("onRebind(): " + intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        log.i("onUnbind(): intent=" + intent);
        return true;
    }

    private ServiceThread mThread;
    private final IBinder mBinder = new LocalBinder();
    
}
