package me.proxer.app.anime.schedule.widget

import android.content.Intent
import android.widget.RemoteViewsService

/**
 * @author Ruben Gees
 */
class ScheduleWidgetService : RemoteViewsService() {

    companion object {
        const val ARGUMENT_CALENDAR_ENTRIES = "calendar_entries"
        const val ARGUMENT_CALENDAR_ENTRIES_WRAPPER = "calendar_entries_wrapper" /* Hack for making it possible to share
                                                                                    between processes. */
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val calendarEntriesWrapper = intent.getBundleExtra(ARGUMENT_CALENDAR_ENTRIES_WRAPPER)
        val calendarEntries = calendarEntriesWrapper.getParcelableArray(ARGUMENT_CALENDAR_ENTRIES)
            .map { it as SimpleCalendarEntry }

        return ScheduleWidgetViewsFactory(applicationContext, false, calendarEntries)
    }
}
