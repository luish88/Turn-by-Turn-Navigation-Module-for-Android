package com.dev.mytbt.ToolsUI;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

/**
 * Created by Luís Henriques for MyTbt.
 * 14-09-2018, 23:14
 */
public class MainPagerAdapter extends FragmentStatePagerAdapter {

    private ArrayList<Fragment> fragments;

    public MainPagerAdapter(FragmentManager fragmentManager) {
        super(fragmentManager);
        fragments = new ArrayList<>();
    }

    /**
     * Adds a fragment to this adapter's list
     * @param f the fragment to be added
     */
    public void addFragment(Fragment f) {
        fragments.add(f);
    }

    /**
     * @return Returns total number of pages
     */
    @Override
    public int getCount() {
        return fragments.size();
    }

    /**
     *
     * @param position the index of the fragment to be returned
     * @return Returns the fragment to display for that page
     */
    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }
}