package com.juztoss.bpmplayer.models;

import java.util.List;

/**
 * Created by JuzTosS on 4/25/2016.
 */
public class ParentLink extends BaseExplorerElement
{
    private BaseExplorerElement mSource;

    public ParentLink(BaseExplorerElement element)
    {
        mSource = element;
    }

    @Override
    public String name()
    {
        return "..";
    }

    @Override
    public ExplorerPriority priority()
    {
        return ExplorerPriority.PARENT_LINK;
    }

    @Override
    public List<BaseExplorerElement> getChildren()
    {
        return mSource.getChildren();
    }

    @Override
    public List<Composition> getCompositions()
    {
        return mSource.getCompositions();
    }
}
