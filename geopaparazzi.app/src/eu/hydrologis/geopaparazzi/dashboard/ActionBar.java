/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.geopaparazzi.dashboard;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.sensors.SensorsManager;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.actionbar.ActionItem;
import eu.hydrologis.geopaparazzi.dashboard.quickaction.actionbar.QuickAction;

/**
 * The action bar utilities class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class ActionBar {
    private static final boolean LOG_HOW = GPLog.LOG_ABSURD;
    private static DecimalFormat formatter = new DecimalFormat("0.00000"); //$NON-NLS-1$
    private final View actionBarView;
    private ActionItem infoQuickaction;

    private String nodataString;
    private String timeString;
    private String lonString;
    private String latString;
    private String altimString;
    private String azimString;
    private String loggingString;
    private String acquirefixString;
    private String gpsonString;
    private String gpsStatusString;
    private final SensorsManager sensorsManager;
    private String satellitesString;
    private ImageButton menuButton;
    private String projectName;
    private String projectString;
    private String indent = "  ";
    private GpsServiceStatus lastGpsServiceStatus;
    private double[] lastGpsPosition;
    private int[] lastGpsStatusExtras;
    private long lastGpsPositiontime;

    private ActionBar( View actionBarView, SensorsManager sensorsManager ) {
        this.actionBarView = actionBarView;
        this.sensorsManager = sensorsManager;

        try {
            ResourcesManager resourcesManager = ResourcesManager.getInstance(actionBarView.getContext());
            File applicationDir = resourcesManager.getApplicationDir();
            projectName = applicationDir.getName();
        } catch (Exception e) {
            e.printStackTrace();
        }

        initVars();
        createQuickActions();

        final int gpsInfoButtonId = R.id.action_bar_info;
        ImageButton gpsInfoButton = (ImageButton) actionBarView.findViewById(gpsInfoButtonId);
        gpsInfoButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(gpsInfoButtonId, v);
            }
        });

        final int compassButtonId = R.id.action_bar_compass;
        ImageButton compassButton = (ImageButton) actionBarView.findViewById(compassButtonId);
        compassButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                push(compassButtonId, v);
            }
        });

        final int menuButtonId = R.id.action_bar_menu;
        menuButton = (ImageButton) actionBarView.findViewById(menuButtonId);

        checkLogging();
    }

    /**
     * @return the menu button.
     */
    public ImageButton getMenuButton() {
        return menuButton;
    }

    /**
     * @return the action bar view.
     */
    public View getActionBarView() {
        return actionBarView;
    }

    private void initVars() {
        Context context = actionBarView.getContext();
        timeString = context.getString(R.string.utctime);
        lonString = context.getString(R.string.lon);
        latString = context.getString(R.string.lat);
        altimString = context.getString(R.string.altim);
        azimString = context.getString(R.string.azimuth);
        nodataString = context.getString(R.string.nogps_data);
        loggingString = context.getString(R.string.text_logging);
        acquirefixString = context.getString(R.string.gps_searching_fix);
        gpsonString = context.getString(R.string.text_gpson);
        satellitesString = context.getString(R.string.satellites);
        projectString = context.getString(R.string.project);
        gpsStatusString = context.getString(R.string.gps_status);
    }

    /**
     * Build the action bar.
     * 
     * @param activity the parent activity.
     * @param activityId the activity id.
     * @param sensorsManager teh sensor manager.
     * @return the actionbar.
     */
    public static ActionBar getActionBar( Activity activity, int activityId, SensorsManager sensorsManager ) {
        View view = activity.findViewById(activityId);
        return new ActionBar(view, sensorsManager);
    }

    /**
     * Set the title.
     * 
     * @param titleResourceId the id of the text to set. 
     * @param titleViewId the view id.
     */
    public void setTitle( int titleResourceId, int titleViewId ) {
        TextView textView = (TextView) actionBarView.findViewById(titleViewId);
        if (textView != null) {
            textView.setText(titleResourceId);
        }
    }

    /**
     * Push action.
     * 
     * @param id id.
     * @param v view.
     */
    public void push( int id, View v ) {
        switch( id ) {
        case R.id.action_bar_info: {
            QuickAction qa = new QuickAction(v);
            infoQuickaction.setTitle(createGpsInfo());
            qa.addActionItem(infoQuickaction);
            qa.setAnimStyle(QuickAction.ANIM_AUTO);
            qa.show();

            break;
        }
        case R.id.action_bar_compass: {
            Context context = actionBarView.getContext();
            openCompass(context);
            break;
        }
        default:
            break;
        }
    }

    /**
     * Opens the comapss app.
     * 
     * @param context the context to use.
     */
    public static void openCompass( final Context context ) {
        String gpsStatusAction = "com.eclipsim.gpsstatus.VIEW";
        String gpsStatusPackage = "com.eclipsim.gpsstatus";
        boolean hasGpsStatus = false;
        List<PackageInfo> installedPackages = new ArrayList<PackageInfo>();

        { // try to get the installed packages list. Seems to have troubles over different
          // versions, so trying them all
            try {
                installedPackages = context.getPackageManager().getInstalledPackages(0);
            } catch (Exception e) {
                // ignore
            }
            if (installedPackages.size() == 0)
                try {
                    installedPackages = context.getPackageManager().getInstalledPackages(PackageManager.GET_ACTIVITIES);
                } catch (Exception e) {
                    // ignore
                }
        }

        if (installedPackages.size() > 0) {
            // if a list is available, check if the status gps is installed
            for( PackageInfo packageInfo : installedPackages ) {
                String packageName = packageInfo.packageName;
                if (LOG_HOW)
                    GPLog.addLogEntry("ACTIONBAR", packageName);
                if (packageName.startsWith(gpsStatusPackage)) {
                    hasGpsStatus = true;
                    if (LOG_HOW)
                        GPLog.addLogEntry("ACTIONBAR", "Found package: " + packageName);
                    break;
                }
            }
        } else {
            /*
             * if no package list is available, for now try to fire it up anyways.
             * This has been a problem for a user on droidx with android 2.2.1.
             */
            hasGpsStatus = true;
        }

        if (hasGpsStatus) {
            Intent intent = new Intent(gpsStatusAction);
            context.startActivity(intent);
        } else {
            new AlertDialog.Builder(context).setTitle(context.getString(R.string.installgpsstatus_title))
                    .setMessage(context.getString(R.string.installgpsstatus_message)).setIcon(android.R.drawable.ic_dialog_info)
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int whichButton ) {
                            // ignore
                        }
                    }).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                        public void onClick( DialogInterface dialog, int whichButton ) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse("market://search?q=com.eclipsim.gpsstatus2"));
                            context.startActivity(intent);
                        }
                    }).show();
        }
    }

    private void createQuickActions() {
        /*
         * LOG QUICKACTIONS
         */
        infoQuickaction = new ActionItem();
        infoQuickaction.setTitle("Info:\ninfo1\ninfo2");
        // Context context = actionBarView.getContext();
        // infoQuickaction.setIcon(context.getResources().getDrawable(R.drawable.action_bar_info));
        // infoQuickaction.setOnClickListener(new OnClickListener(){
        // public void onClick( View v ) {
        // }
        // });

    }

    private String createGpsInfo() {
        double azimuth = sensorsManager.getNormalAzimuth();

        StringBuilder sb = new StringBuilder();
        if (projectName != null && projectName.length() != 0) {

            sb.append(projectString).append(":\n");
            sb.append(indent).append(projectName).append("\n\n");
        }
        sb.append(gpsStatusString).append(":\n");
        if (lastGpsPosition == null || lastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
            // Logger.d("COMPASSVIEW", "Location from gps is null!");
            sb.append(indent).append(nodataString).append("\n");
            if (lastGpsServiceStatus == GpsServiceStatus.GPS_ON__NO_LISTENING) {
                if (lastGpsServiceStatus != GpsServiceStatus.GPS_FIX) {
                    sb.append(indent).append(acquirefixString);
                } else {
                    sb.append(indent).append(gpsonString);
                    sb.append(": ").append(lastGpsServiceStatus == GpsServiceStatus.GPS_OFF); //$NON-NLS-1$
                }
            }
            sb.append("\n");
            addGpsStatusInfo(sb);

        } else {
            sb.append(indent).append(timeString);
            sb.append(" ").append(TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(lastGpsPositiontime))); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(latString);
            sb.append(" ").append(formatter.format(lastGpsPosition[1])); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(lonString);
            sb.append(" ").append(formatter.format(lastGpsPosition[0])); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(altimString);
            sb.append(" ").append((int) lastGpsPosition[2]); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(azimString);
            sb.append(" ").append((int) (360 - azimuth)); //$NON-NLS-1$
            sb.append("\n");
            sb.append(indent).append(loggingString);
            sb.append(": ").append(lastGpsServiceStatus == GpsServiceStatus.GPS_DATABASELOGGING); //$NON-NLS-1$
            sb.append("\n");
            addGpsStatusInfo(sb);
        }
        return sb.toString();
    }

    private void addGpsStatusInfo( StringBuilder sb ) {
        if (lastGpsStatusExtras != null) {
            int satCount = lastGpsStatusExtras[1];
            // int satForFixCount = info.getSatUsedInFixCount();
            sb.append(indent).append(satellitesString).append(": ").append(satCount).append("\n");
            // sb.append("used for fix: ").append(satForFixCount).append("\n");
        }
    }

    /**
     * Checks gps logging status.
     */
    public void checkLogging() {
        Button gpsOnOffView = (Button) actionBarView.findViewById(R.id.gpsOnOff);
        gpsOnOffView.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                if (lastGpsServiceStatus == GpsServiceStatus.GPS_OFF) {
                    final Context context = v.getContext();
                    String prompt = context.getResources().getString(R.string.prompt_gpsenable);
                    Utilities.yesNoMessageDialog(context, prompt, new Runnable(){
                        public void run() {
                            Intent gpsOptionsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            context.startActivity(gpsOptionsIntent);
                        }
                    }, null);
                }
            }
        });

        if (LOG_HOW && lastGpsStatusExtras != null) {
            int satCount = lastGpsStatusExtras[1];
            int satForFixCount = lastGpsStatusExtras[2];
            StringBuilder sb = new StringBuilder();
            sb.append("satellites: ").append(satCount);
            sb.append(" of which for fix: ").append(satForFixCount);
            GPLog.addLogEntry(this, sb.toString());
        }

        Resources resources = gpsOnOffView.getResources();

        if (lastGpsServiceStatus != GpsServiceStatus.GPS_OFF) {// gpsManager.isEnabled()) {
            if (LOG_HOW)
                GPLog.addLogEntry(this, "GPS seems to be on");
            if (lastGpsServiceStatus == GpsServiceStatus.GPS_DATABASELOGGING) {
                if (LOG_HOW)
                    GPLog.addLogEntry(this, "GPS seems to be also logging");
                gpsOnOffView.setBackgroundDrawable(resources.getDrawable(R.drawable.gps_background_logging));
            } else {
                if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
                    if (LOG_HOW)
                        GPLog.addLogEntry(this, "GPS has fix");
                    gpsOnOffView.setBackgroundDrawable(resources.getDrawable(R.drawable.gps_background_hasfix_notlogging));
                } else {
                    if (LOG_HOW)
                        GPLog.addLogEntry(this, "GPS doesn't have a fix");
                    gpsOnOffView.setBackgroundDrawable(resources.getDrawable(R.drawable.gps_background_notlogging));
                }
            }
        } else {
            if (LOG_HOW)
                GPLog.addLogEntry(this, "GPS seems to be off");
            gpsOnOffView.setBackgroundDrawable(resources.getDrawable(R.drawable.gps_background_off));
        }
    }

    /**
     * Set the current status.
     * 
     * @param gpsServiceStatus gps status.
     * @param lastGpsPosition gps position.
     * @param lastGpsPositionExtras position extras.
     * @param lastGpsStatusExtras status extras.
     * @param lastPositiontime position time.
     */
    public void setStatus( GpsServiceStatus gpsServiceStatus, double[] lastGpsPosition, float[] lastGpsPositionExtras,
            int[] lastGpsStatusExtras, long lastPositiontime ) {
        this.lastGpsServiceStatus = gpsServiceStatus;
        this.lastGpsPosition = lastGpsPosition;
        this.lastGpsStatusExtras = lastGpsStatusExtras;
        this.lastGpsPositiontime = lastPositiontime;

        checkLogging();
    }
}
