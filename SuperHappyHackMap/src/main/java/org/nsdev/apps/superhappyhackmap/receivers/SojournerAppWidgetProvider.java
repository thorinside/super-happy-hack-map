package org.nsdev.apps.superhappyhackmap.receivers;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import org.nsdev.apps.superhappyhackmap.R;
import org.nsdev.apps.superhappyhackmap.model.DatabaseManager;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by neal on 15-06-15.
 */
public class SojournerAppWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        final int N = appWidgetIds.length;

        for (int i = 0; i < N; i++) {
            int appWidgetId = appWidgetIds[i];
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Intent intent = new Intent(HackReceiver.ACTION_RESET_LAST_HACK, null, context, HackReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sojourner_appwidget);
        views.setOnClickPendingIntent(R.id.reset_button, pendingIntent);

        views.setTextViewText(R.id.reset_date, getSojourerResetDateText(context));

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
        updateAppWidget(context, appWidgetManager, appWidgetId);
    }

    private String getSojourerResetDateText(Context context) {
        Date mostRecentHackTime = DatabaseManager.getInstance().getMostRecentHackTime();
        if (mostRecentHackTime == null) return "--";

        Calendar c = Calendar.getInstance();
        c.setTime(mostRecentHackTime);

        c.add(Calendar.HOUR, 24);

        return DateUtils.getRelativeDateTimeString(context, c.getTime().getTime(), DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, DateUtils.FORMAT_NO_YEAR).toString();
    }
}
