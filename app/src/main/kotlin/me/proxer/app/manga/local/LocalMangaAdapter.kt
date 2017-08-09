package me.proxer.app.manga.local

import android.os.Bundle
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import io.reactivex.subjects.PublishSubject
import kotterknife.bindView
import me.proxer.app.GlideRequests
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.manga.local.LocalMangaAdapter.ViewHolder
import me.proxer.app.manga.local.LocalMangaChapterAdapter.LocalMangaChapterAdapterCallback
import me.proxer.app.util.data.ParcelableStringBooleanMap
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.defaultLoad
import me.proxer.app.view.PaddingDividerItemDecoration
import me.proxer.library.entitiy.info.EntryCore
import me.proxer.library.util.ProxerUrls

/**
 * @author Ruben Gees
 */
class LocalMangaAdapter(savedInstanceState: Bundle?, private val glide: GlideRequests)
    : BaseAdapter<CompleteLocalMangaEntry, ViewHolder>() {

    private companion object {
        private const val EXPANDED_STATE = "local_manga_expanded"
    }

    val clickSubject: PublishSubject<Pair<EntryCore, LocalMangaChapter>> = PublishSubject.create()
    val longClickSubject: PublishSubject<Pair<ImageView, EntryCore>> = PublishSubject.create()
    val deleteClickSubject: PublishSubject<Pair<EntryCore, LocalMangaChapter>> = PublishSubject.create()

    private val expanded: ParcelableStringBooleanMap

    init {
        expanded = when (savedInstanceState) {
            null -> ParcelableStringBooleanMap()
            else -> savedInstanceState.getParcelable(EXPANDED_STATE)
        }

        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_local_manga, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(data[position])
    override fun getItemId(position: Int) = data[position].first.id.toLong()

    override fun areItemsTheSame(old: CompleteLocalMangaEntry, new: CompleteLocalMangaEntry): Boolean {
        return old.first.id == new.first.id
    }

    override fun onViewRecycled(holder: ViewHolder) = glide.clear(holder.image)

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)

        holder.adapter.callback = object : LocalMangaChapterAdapterCallback {
            override fun onChapterClick(chapter: LocalMangaChapter) = withSafeAdapterPosition(holder) {
                clickSubject.onNext(data[it].first to chapter)
            }

            override fun onDeleteClick(chapter: LocalMangaChapter) = withSafeAdapterPosition(holder) {
                deleteClickSubject.onNext(data[it].first to chapter)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)

        holder.adapter.callback = null
    }

    override fun saveInstanceState(outState: Bundle) = outState.putParcelable(EXPANDED_STATE, expanded)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val adapter: LocalMangaChapterAdapter
            get() = chapters.adapter as LocalMangaChapterAdapter

        internal val image: ImageView by bindView(R.id.image)
        internal val title: TextView by bindView(R.id.title)
        internal val chapters: RecyclerView by bindView(R.id.chapters)

        init {
            itemView.setOnClickListener {
                withSafeAdapterPosition(this) {
                    val id = data[it].first.id

                    when (expanded[id]) {
                        true -> expanded.remove(id)
                        else -> expanded.put(id, true)
                    }

                    notifyItemChanged(it)
                }
            }

            itemView.setOnLongClickListener {
                withSafeAdapterPosition(this) {
                    longClickSubject.onNext(image to data[it].first)
                }

                true
            }

            chapters.isNestedScrollingEnabled = false
            chapters.layoutManager = LinearLayoutManager(itemView.context)
            chapters.adapter = LocalMangaChapterAdapter()
            chapters.addItemDecoration(PaddingDividerItemDecoration(chapters.context, 4f))
        }

        fun bind(item: CompleteLocalMangaEntry) {
            ViewCompat.setTransitionName(image, "local_manga_${item.first.id}")

            title.text = item.first.name

            if (expanded[item.first.id] == true) {
                chapters.visibility = View.VISIBLE

                adapter.swapData(item.second)
                adapter.notifyDataSetChanged()
            } else {
                chapters.visibility = View.GONE

                adapter.clear()
            }

            glide.defaultLoad(image, ProxerUrls.entryImage(item.first.id))
        }
    }
}
