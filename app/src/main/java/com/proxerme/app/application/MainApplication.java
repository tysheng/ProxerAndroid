package com.proxerme.app.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ImageView;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.orhanobut.hawk.Hawk;
import com.orhanobut.hawk.HawkBuilder;
import com.proxer.app.EventBusIndex;
import com.proxerme.app.event.SectionChangedEvent;
import com.proxerme.app.manager.BadgeManager;
import com.proxerme.app.manager.NotificationManager;
import com.proxerme.app.manager.UserManager;
import com.proxerme.app.util.EventBusBuffer;
import com.proxerme.app.util.Section;
import com.proxerme.library.connection.ProxerConnection;
import com.proxerme.library.util.PersistentCookieStore;

import net.danlew.android.joda.JodaTimeAndroid;

import org.greenrobot.eventbus.EventBus;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

/**
 * The {@link Application}, which is used by this App. It does some configuration at start.
 *
 * @author Ruben Gees
 */
public class MainApplication extends Application {

    private JobManager jobManager;
    private BadgeManager badgeManager;
    private UserManager userManager;
    private NotificationManager notificationManager;

    private EventBusBuffer eventBusBuffer = new EventBusBuffer();

    private int createdActivities = 0;
    private int startedActivities = 0;

    @Section.SectionId
    private int currentSection = Section.NONE;

    private boolean changingOrientation = false;

    @Override
    public void onCreate() {
        super.onCreate();

        initLibs();

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (changingOrientation) {
                    changingOrientation = false;
                } else {
                    createdActivities++;

                    if (createdActivities == 1) {
                        initManagers();
                    }
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivities++;

                userManager.reLogin();
            }

            @Override
            public void onActivityResumed(Activity activity) {
                badgeManager.startListeningForEvents();
            }

            @Override
            public void onActivityPaused(Activity activity) {
                badgeManager.stopListeningForEvents();
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivities--;

                if (startedActivities <= 0) {
                    setCurrentSection(Section.NONE);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (activity.isChangingConfigurations()) {
                    changingOrientation = true;
                } else {
                    createdActivities--;

                    if (createdActivities <= 0) {
                        destroyManagers();

                        setCurrentSection(Section.NONE);
                    }
                }
            }
        });
    }

    private void initLibs() {
        JodaTimeAndroid.init(this);
        Hawk.init(this).setEncryptionMethod(HawkBuilder.EncryptionMethod.MEDIUM)
                .setStorage(HawkBuilder.newSharedPrefStorage(this)).build();
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
        DrawerImageLoader.init(new DrawerImageLoader.IDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Glide.with(imageView.getContext()).load(uri)
                        .placeholder(new IconicsDrawable(imageView.getContext(),
                                CommunityMaterial.Icon.cmd_account).colorRes(android.R.color.white))
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE).centerCrop().into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Glide.clear(imageView);
            }

            @Override
            public Drawable placeholder(Context ctx) {
                return null;
            }

            @Override
            public Drawable placeholder(Context ctx, String tag) {
                return null;
            }
        });

        CookieManager cookieManager = new CookieManager(new PersistentCookieStore(this),
                CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);
    }

    private void initManagers() {
        jobManager = new JobManager(new Configuration.Builder(this).build());
        badgeManager = new BadgeManager();
        userManager = new UserManager();
        notificationManager = new NotificationManager(this);
    }

    private void destroyManagers() {
        badgeManager.destroy();
        userManager.destroy();
        notificationManager.destroy();

        ProxerConnection.cleanup();
    }

    public int getStartedActivities() {
        return startedActivities;
    }

    @NonNull
    public JobManager getJobManager() {
        return jobManager;
    }

    @NonNull
    public BadgeManager getBadgeManager() {
        return badgeManager;
    }

    @NonNull
    public UserManager getUserManager() {
        return userManager;
    }

    @NonNull
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    @Section.SectionId
    public int getCurrentSection() {
        return currentSection;
    }

    public void setCurrentSection(@Section.SectionId int newSection) {
        this.currentSection = newSection;

        EventBus.getDefault().post(new SectionChangedEvent(newSection));
    }
}
