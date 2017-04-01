package me.proxer.app.activity.base

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.design.widget.CollapsingToolbarLayout
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.Toolbar
import android.transition.Transition
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.GlideDrawable
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget
import com.h6ah4i.android.tablayouthelper.TabLayoutHelper
import me.proxer.app.R
import me.proxer.app.activity.ImageDetailActivity
import me.proxer.app.util.ActivityUtils
import me.proxer.app.util.extension.bindView
import me.proxer.app.util.listener.TransitionListenerWrapper
import okhttp3.HttpUrl
import org.jetbrains.anko.applyRecursively

/**
 * @author Ruben Gees
 */
abstract class ImageTabsActivity : MainActivity() {

    abstract val headerImageUrl: HttpUrl?
    abstract val sectionsPagerAdapter: PagerAdapter

    open protected val contentView
        get() = R.layout.activity_image_tabs

    open protected val itemToDisplay
        get() = 0

    private var isHeaderImageVisible = true

    open protected val toolbar: Toolbar by bindView(R.id.toolbar)
    open protected val appbar: AppBarLayout by bindView(R.id.appbar)
    open protected val collapsingToolbar: CollapsingToolbarLayout by bindView(R.id.collapsingToolbar)
    open protected val viewPager: ViewPager by bindView(R.id.viewPager)
    open protected val headerImage: ImageView by bindView(R.id.image)
    open protected val tabs: TabLayout by bindView(R.id.tabs)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(contentView)
        setSupportActionBar(toolbar)
        supportPostponeEnterTransition()

        setupToolbar()
        setupImage()
        loadImage()

        if (isEnterTransitionPossible(savedInstanceState) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.sharedElementEnterTransition.addListener(object : TransitionListenerWrapper {

                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                override fun onTransitionEnd(transition: Transition?) {
                    window.sharedElementEnterTransition.removeListener(this)

                    setupContent(savedInstanceState)

                    // We need to invalidate the ImageView once the transition is finished, as either a bug in Glide
                    // or the Android framework is causing the image to be moved a bit sometimes.
                    headerImage.post { headerImage.invalidate() }
                }
            })
        } else {
            setupContent(savedInstanceState)
        }
    }

    override fun onBackPressed() {
        when (isHeaderImageVisible) {
            true -> super.onBackPressed()
            false -> finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                when (isHeaderImageVisible) {
                    true -> supportFinishAfterTransition()
                    false -> finish()
                }

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    open protected fun setupImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            headerImage.transitionName = ActivityUtils.getTransitionName(this)
        }

        headerImage.setOnClickListener {
            val safeHeaderImageUrl = headerImageUrl

            if (headerImage.drawable != null && safeHeaderImageUrl != null) {
                ImageDetailActivity.navigateTo(this@ImageTabsActivity, safeHeaderImageUrl, it as ImageView)
            }
        }
    }

    open protected fun loadImage() {
        if (headerImageUrl == null) {
            loadEmptyImage()
            supportStartPostponedEnterTransition()
        } else {
            Glide.with(this)
                    .load(headerImageUrl.toString())
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(object : GlideDrawableImageViewTarget(headerImage) {
                        override fun onResourceReady(resource: GlideDrawable?,
                                                     animation: GlideAnimation<in GlideDrawable>?) {
                            super.onResourceReady(resource, animation)

                            supportStartPostponedEnterTransition()
                        }
                    })
        }
    }

    open protected fun setupToolbar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        collapsingToolbar.isTitleEnabled = false

        appbar.addOnOffsetChangedListener { _, verticalOffset ->
            isHeaderImageVisible = collapsingToolbar.height + verticalOffset >
                    collapsingToolbar.scrimVisibleHeightTrigger

            listOf(tabs, toolbar).forEach {
                it.applyRecursively {
                    if (it is TextView) {
                        when (isHeaderImageVisible) {
                            true -> it.setShadowLayer(3f, 0f, 0f, ContextCompat.getColor(this, android.R.color.black))
                            false -> it.setShadowLayer(0f, 0f, 0f, 0)
                        }
                    }
                }
            }
        }
    }

    open protected fun setupContent(savedInstanceState: Bundle?) {
        viewPager.adapter = sectionsPagerAdapter

        if (savedInstanceState == null) {
            viewPager.currentItem = itemToDisplay
        }

        TabLayoutHelper(tabs, viewPager).apply { isAutoAdjustTabModeEnabled = true }
    }

    open protected fun loadEmptyImage() {}

    private fun isEnterTransitionPossible(savedInstanceState: Bundle?): Boolean {
        return savedInstanceState == null && ActivityUtils.getTransitionName(this) != null
    }
}