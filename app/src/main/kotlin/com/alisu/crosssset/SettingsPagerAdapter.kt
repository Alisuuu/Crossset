package com.alisu.crosssset

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class SettingsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val tables = SettingsTable.entries

    override fun getItemCount(): Int = tables.size

    override fun createFragment(position: Int): Fragment {
        return SettingsFragment.newInstance(tables[position])
    }
}
