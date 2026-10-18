package pl.hexmind.mindshaper.activities.workshop

// Workshop tabs on Material TabLayout: folder-like tabs joined with the card below them.
// To restore: paste into WorkshopActivity, the TabLayout from WorkshopTabsLegacy_layout.xml into
// workshop_activity.xml, and the WorkshopTabsLegacy_*.xml resources back into res/ under their names:
//   drawable/tab_background_selector.xml, drawable/tab_background_active.xml,
//   drawable/tab_background_inactive.xml, drawable/shape_card_tab_joined.xml (card_goals / card_paths body),
//   color/tab_text_color.xml, layout/tab_section_header.xml

//    private lateinit var tabsWorkshop: TabLayout

//    // both sections stay inflated; tabs only swap visibility
//    private fun setupTabs() {
//        tabsWorkshop = findViewById(R.id.tabs_workshop)
//        cardGoals    = findViewById(R.id.card_goals)
//        cardPaths    = findViewById(R.id.card_paths)
//
//        tabsWorkshop.addTab(newSectionTab(R.string.workshop_tab_goals))
//        tabsWorkshop.addTab(newSectionTab(R.string.workshop_tab_paths))
//
//        tabsWorkshop.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
//            override fun onTabSelected(tab: TabLayout.Tab) = showTab(tab.position)
//            override fun onTabUnselected(tab: TabLayout.Tab) {}
//            override fun onTabReselected(tab: TabLayout.Tab) {}
//        })
//
//        showTab(TAB_GOALS)
//    }
//
//    private fun newSectionTab(titleRes: Int): TabLayout.Tab {
//        val title = layoutInflater.inflate(R.layout.tab_section_header, tabsWorkshop, false) as TextView
//        title.setText(titleRes)
//        return tabsWorkshop.newTab().apply { customView = title }
//    }
