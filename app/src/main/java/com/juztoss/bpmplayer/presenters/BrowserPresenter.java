package com.juztoss.bpmplayer.presenters;

import android.app.LoaderManager;
import android.content.AsyncTaskLoader;
import android.content.Loader;
import android.os.Bundle;

import com.juztoss.bpmplayer.models.BaseExplorerElement;
import com.juztoss.bpmplayer.models.Composition;
import com.juztoss.bpmplayer.models.CustomExplorerElement;
import com.juztoss.bpmplayer.models.ExplorerPriority;
import com.juztoss.bpmplayer.models.FileSystemFolder;
import com.juztoss.bpmplayer.models.MediaFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by JuzTosS on 4/20/2016.
 */
public class BrowserPresenter extends BasePresenter implements LoaderManager.LoaderCallbacks<List<BaseExplorerElement>>
{
    BaseExplorerElement mCurrent;
    private OnDataChangedListener mListener;

    private List<BaseExplorerElement> mData;

    public BrowserPresenter(BPMPlayerApp app)
    {
        super(app);
        CustomExplorerElement root = new CustomExplorerElement("", new ArrayList<BaseExplorerElement>(), ExplorerPriority.HIGHEST);
        root.add(new FileSystemFolder(new File("/"), "File system", root));
        root.add(new MediaFolder(-1, "Media", false, root, false));

        mCurrent = root;

        mData = new ArrayList<>();
    }

    public void listItemClicked(BaseExplorerElement element)
    {
        mCurrent = element;
    }

    @Override
    public Loader<List<BaseExplorerElement>> onCreateLoader(int id, Bundle args)
    {
        AsyncTaskLoader<List<BaseExplorerElement>> fileLoader = new AsyncTaskLoader<List<BaseExplorerElement>>(getApp())
        {
            @Override
            public List<BaseExplorerElement> loadInBackground()
            {
                return mCurrent.getChildren();
            }
        };
        fileLoader.forceLoad();
        return fileLoader;
    }

    @Override
    public void onLoadFinished(Loader<List<BaseExplorerElement>> loader, List<BaseExplorerElement> data)
    {
        mData = data;
        if (mListener != null)
            mListener.onDataChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<BaseExplorerElement>> loader)
    {

    }

    public List<BaseExplorerElement> getFileList()
    {
        return mData;
    }

    public void switchMode()
    {

    }

    public void setOnDataChangedListener(OnDataChangedListener listener)
    {
        mListener = listener;
    }

    public List<Composition> getCurrentElementCompositions()
    {
        return mCurrent.getCompositions();
    }

    public interface OnDataChangedListener
    {
        void onDataChanged();
    }
}