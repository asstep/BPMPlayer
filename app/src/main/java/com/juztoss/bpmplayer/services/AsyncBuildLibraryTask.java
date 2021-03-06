package com.juztoss.bpmplayer.services;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;

import com.juztoss.bpmplayer.DatabaseHelper;
import com.juztoss.bpmplayer.audio.BpmDetector;
import com.juztoss.bpmplayer.presenters.BPMPlayerApp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by JuzTosS on 5/27/2016.
 */

public class AsyncBuildLibraryTask extends AsyncTask<String, String, Void>
{
    private final String UPDATE_COMPLETE = "UpdateComplete";
    private final int MAX_PROGRESS_VALUE = 1000000;
    private Context mContext;
    private BPMPlayerApp mApp;
    public ArrayList<OnBuildLibraryProgressUpdate> mBuildLibraryProgressUpdate;

    private int mOverallProgress = 0;

    private PowerManager.WakeLock wakeLock;

    public AsyncBuildLibraryTask(Context context)
    {
        mContext = context;
        mApp = (BPMPlayerApp) mContext;
        mBuildLibraryProgressUpdate = new ArrayList<>();
    }

    /**
     * Provides callback methods that expose this
     * AsyncTask's progress.
     *
     * @author Saravan Pantham
     */
    public interface OnBuildLibraryProgressUpdate
    {

        /**
         * Called when this AsyncTask begins executing
         * its doInBackground() method.
         */
        void onStartBuildingLibrary();

        /**
         * Called whenever mOverall Progress has been updated.
         */
        void onProgressUpdate(AsyncBuildLibraryTask task,
                              int overallProgress, int maxProgress,
                              boolean mediaStoreTransferDone);

        /**
         * Called when this AsyncTask finishes executing
         * its onPostExecute() method.
         */
        void onFinishBuildingLibrary(AsyncBuildLibraryTask task);

    }

    @Override
    protected void onPreExecute()
    {
        super.onPreExecute();

        mApp.setIsBuildingLibrary(true);
        mApp.setIsScanFinished(false);

        if (mBuildLibraryProgressUpdate != null)
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onStartBuildingLibrary();

        // Acquire a wakelock to prevent the CPU from sleeping while the process is running.
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getName());
        wakeLock.acquire();

    }

    @Override
    protected Void doInBackground(String... params)
    {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        Cursor mediaStoreCursor = getSongsFromMediaStore();
        try
        {
            if (mediaStoreCursor != null)
            {
                saveMediaStoreDataToDB(mediaStoreCursor);
                detectSongsBpm(mediaStoreCursor);
            }
        }
        finally
        {
            mediaStoreCursor.close();
        }
        publishProgress(UPDATE_COMPLETE);
        return null;
    }

    private void detectSongsBpm(Cursor mediaStoreCursor)
    {
        final int subprogress = (MAX_PROGRESS_VALUE - mOverallProgress) / mediaStoreCursor.getCount();
//        int numOfCores = SystemHelper.getNumberOfCores() - 1;
//        if (numOfCores <= 0)///Set numOfCores bigger value while playing causes lags
        int numOfCores = 1;
        final ExecutorService es = Executors.newFixedThreadPool(numOfCores);
        final int filePathColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        final int idColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media._ID);

        class SongBpmDetector implements Runnable
        {
            private final String mFullPath;
            private final String mediaId;

            public SongBpmDetector(String songMediaId, String fullPath)
            {
                mFullPath = fullPath;
                mediaId = songMediaId;
            }

            @Override
            public void run()
            {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                double bpm = BpmDetector.detect(mFullPath);
                int bpmX10 = (int) (bpm * 10);
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.MUSIC_LIBRARY_BPMX10, bpmX10);
                DatabaseHelper.db().update(DatabaseHelper.TABLE_MUSIC_LIBRARY, values, DatabaseHelper.MUSIC_LIBRARY_MEDIA_ID + "= ?", new String[]{mediaId});

                Log.e("DEBUG", mFullPath + " : " + bpmX10);
                mOverallProgress += subprogress;
                publishProgress();
            }
        }

        for (int i = 0; i < mediaStoreCursor.getCount(); i++)
        {
            mediaStoreCursor.moveToPosition(i);
            String songFileFullPath = mediaStoreCursor.getString(filePathColIndex);
            String songId = mediaStoreCursor.getString(idColIndex);
            es.execute(new SongBpmDetector(songId, songFileFullPath));
        }

        es.shutdown();
        try
        {
            es.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves a cursor of songs from MediaStore. The cursor
     * is limited to songs that are within the folders that the user
     * selected.
     */
    private Cursor getSongsFromMediaStore()
    {
        //Get a cursor of all active music folders.
//        Cursor musicFoldersCursor = mApp.getDBAccessHelper().getAllMusicFolderPaths();

        //Build the appropriate selection statement.
        Cursor mediaStoreCursor = null;
        String sortOrder = null;
        String projection[] = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA};

//        //Grab the cursor of MediaStore entries.
//        if (musicFoldersCursor == null || musicFoldersCursor.getCount() < 1)
//        {
        //No folders were selected by the user. Grab all songs in MediaStore.
//            mediaStoreCursor = MediaStoreAccessHelper.getAllSongs(mContext, projection, sortOrder);
        ContentResolver contentResolver = mContext.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

        mediaStoreCursor = contentResolver.query(uri, projection, selection, null, sortOrder);
//        }
//        else
//        {
//            mediaStoreCursor = MediaStoreAccessHelper.getAllSongsWithSelection(mContext,
//                    buildMusicFoldersSelection(musicFoldersCursor),
//                    projection,
//                    sortOrder);
//
//            //Close the music folders cursor.
//            musicFoldersCursor.close();
//        }

        return mediaStoreCursor;
    }

    /**
     * Iterates through mediaStoreCursor and transfers its mId
     * over to Jams' private database.
     */
    private void saveMediaStoreDataToDB(Cursor mediaStoreCursor)
    {
        try
        {
            DatabaseHelper.db().beginTransaction();

            DatabaseHelper.db().delete(DatabaseHelper.TABLE_MUSIC_LIBRARY, null, null);
            DatabaseHelper.db().delete(DatabaseHelper.TABLE_FOLDERS, null, null);

            //Tracks the progress of this method.
            int subProgress;
            if (mediaStoreCursor.getCount() != 0)
                subProgress = mOverallProgress / 4 / (mediaStoreCursor.getCount());
            else
                subProgress = mOverallProgress / 4;

            class Node
            {
                public Node(long id)
                {
                    mId = id;
                }

                public void add(Node node, String name)
                {
                    mChildren.add(node);
                    mChildrenNames.add(name);
                    node.mParent = this;
                }

                public Long mId;
                Node mParent;
                public List<Node> mChildren = new ArrayList<>();
                public List<String> mChildrenNames = new ArrayList<>();

                public Node get(String folder)
                {
                    return mChildren.get(mChildrenNames.indexOf(folder));
                }
            }

            Node folderIds = new Node(-1);

            //Prefetch each column's index.
            final int filePathColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            final int idColIndex = mediaStoreCursor.getColumnIndex(MediaStore.Audio.Media._ID);

            for (int i = 0; i < mediaStoreCursor.getCount(); i++)
            {
                mediaStoreCursor.moveToPosition(i);
                mOverallProgress += subProgress;
                publishProgress();

                String songFileFullPath = mediaStoreCursor.getString(filePathColIndex);
                String songId = mediaStoreCursor.getString(idColIndex);

                String[] folders = songFileFullPath.split("/");
                StringBuilder b = new StringBuilder(songFileFullPath);
                String songFileName = folders[folders.length - 1];

                String songNameWithSlash = "/" + songFileName;
                b.replace(songFileFullPath.lastIndexOf(songNameWithSlash), songFileFullPath.lastIndexOf(songNameWithSlash) + songNameWithSlash.length(), "");
                String songFileFolder = b.toString();

                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.MUSIC_LIBRARY_PATH, songFileFolder);
                values.put(DatabaseHelper.MUSIC_LIBRARY_NAME, songFileName);
                values.put(DatabaseHelper.MUSIC_LIBRARY_MEDIA_ID, songId);

                //Add all the entries to the database to build the songs library.
                DatabaseHelper.db().insert(DatabaseHelper.TABLE_MUSIC_LIBRARY,
                        null,
                        values);

                Node parentNode = folderIds;
                for (int j = 1; j < (folders.length - 1); j++)
                {
                    String folder = folders[j];
                    if (parentNode.mChildrenNames.contains(folder))
                    {
                        parentNode = parentNode.get(folder);
                        continue;
                    }

                    ContentValues folderValues = new ContentValues();
                    folderValues.put(DatabaseHelper.FOLDERS_NAME, folder);

                    folderValues.put(DatabaseHelper.FOLDERS_PARENT_ID, parentNode.mId);
                    if (j == (folders.length - 2))//This is the last segment
                        folderValues.put(DatabaseHelper.FOLDERS_HAS_SONGS, true);

                    long id = DatabaseHelper.db().insert(DatabaseHelper.TABLE_FOLDERS, null, folderValues);
                    Node newNode = new Node(id);
                    parentNode.add(newNode, folder);
                    parentNode = newNode;
                }


            }

        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        finally
        {
            //Close the transaction.
            DatabaseHelper.db().setTransactionSuccessful();
            DatabaseHelper.db().endTransaction();
        }

    }
//
//    /**
//     * Constructs the selection string for limiting the MediaStore
//     * query to specific music folders.
//     */
//    private String buildMusicFoldersSelection(Cursor musicFoldersCursor)
//    {
//        String mediaStoreSelection = MediaStore.Audio.Media.IS_MUSIC + "!=0 AND (";
//        int folderPathColIndex = musicFoldersCursor.getColumnIndex(DBAccessHelper.FOLDER_PATH);
//        int includeColIndex = musicFoldersCursor.getColumnIndex(DBAccessHelper.INCLUDE);
//
//        for (int i = 0; i < musicFoldersCursor.getCount(); i++)
//        {
//            musicFoldersCursor.moveToPosition(i);
//            boolean include = musicFoldersCursor.getInt(includeColIndex) > 0;
//
//            //Set the correct LIKE clause.
//            String likeClause;
//            if (include)
//                likeClause = " LIKE ";
//            else
//                likeClause = " NOT LIKE ";
//
//            //The first " AND " clause was already appended to mediaStoreSelection.
//            if (i != 0 && !include)
//                mediaStoreSelection += " AND ";
//            else if (i != 0 && include)
//                mediaStoreSelection += " OR ";
//
//            mediaStoreSelection += MediaStore.Audio.Media.DATA + likeClause
//                    + "'%" + musicFoldersCursor.getString(folderPathColIndex)
//                    + "/%'";
//
//        }
//
//        //Append the closing parentheses.
//        mediaStoreSelection += ")";
//        return mediaStoreSelection;
//    }

    @Override
    protected void onProgressUpdate(String... progressParams)
    {
        super.onProgressUpdate(progressParams);

        if (progressParams.length > 0 && progressParams[0].equals(UPDATE_COMPLETE))
        {
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onProgressUpdate(this, mOverallProgress,
                            MAX_PROGRESS_VALUE, true);

            return;
        }

        if (mBuildLibraryProgressUpdate != null)
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onProgressUpdate(this, mOverallProgress, MAX_PROGRESS_VALUE, false);

    }

    @Override
    protected void onPostExecute(Void arg0)
    {
        //Release the wakelock.
        wakeLock.release();
        mApp.setIsBuildingLibrary(false);
        mApp.setIsScanFinished(true);

        if (mBuildLibraryProgressUpdate != null)
            for (int i = 0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i) != null)
                    mBuildLibraryProgressUpdate.get(i).onFinishBuildingLibrary(this);

    }

    /**
     * Setter methods.
     */
    public void setOnBuildLibraryProgressUpdate(OnBuildLibraryProgressUpdate
                                                        buildLibraryProgressUpdate)
    {
        if (buildLibraryProgressUpdate != null)
            mBuildLibraryProgressUpdate.add(buildLibraryProgressUpdate);
    }
}
