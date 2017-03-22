package me.proxer.app.adapter.base

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.View
import me.proxer.library.entitiy.ProxerIdItem
import java.util.*

/**
 * @author Ruben Gees
 */
abstract class PagingAdapter<T> : RecyclerView.Adapter<PagingAdapter.PagingViewHolder<T>>() {

    var list = ArrayList<T>()
        protected set

    var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView?) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView?) {
        this.recyclerView = null

        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onBindViewHolder(holder: PagingViewHolder<T>, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            return (list[position] as ProxerIdItem).id.toLong()
        } else {
            return super.getItemId(position)
        }
    }

    override fun getItemCount() = list.size

    fun isEmpty() = list.isEmpty()

    open fun insert(items: Iterable<T>) {
        doUpdates(items.plus(list.filter { oldItem ->
            items.find { areItemsTheSame(oldItem, it) } == null
        }))
    }

    open fun append(items: Iterable<T>) {
        doUpdates(list.filter { oldItem ->
            items.find { areItemsTheSame(oldItem, it) } == null
        }.plus(items))
    }

    open fun replace(items: Iterable<T>) {
        doUpdates(items.toList())
    }

    open fun remove(item: T) {
        doUpdates(list.minus(item))
    }

    open fun clear() {
        doUpdates(ArrayList<T>(0))
    }

    open protected fun areItemsTheSame(oldItem: T, newItem: T) = when {
        hasStableIds() -> (oldItem as ProxerIdItem).id == (newItem as ProxerIdItem).id
        else -> oldItem == newItem
    }

    open protected fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem

    open fun destroy() {}

    protected fun doUpdates(newList: List<T>) {
        val previousFirstItem = list.firstOrNull()
        val wasAtTop = (recyclerView?.layoutManager as StaggeredGridLayoutManager?)?.findFirstVisibleItemPositions(null)
                ?.contains(0) ?: false
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = areItemsTheSame(list[oldItemPosition], newList[newItemPosition])

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int)
                    = areContentsTheSame(list[oldItemPosition], newList[newItemPosition])

            override fun getOldListSize() = list.size
            override fun getNewListSize() = newList.size
        })

        list.clear()
        list.addAll(newList)

        result.dispatchUpdatesTo(this)

        if (wasAtTop && previousFirstItem != list.firstOrNull()) {
            recyclerView?.postDelayed({
                recyclerView?.smoothScrollToPosition(0)
            }, 50)
        }

        recyclerView?.postDelayed({
            recyclerView?.invalidateItemDecorations()
        }, 500)
    }

    abstract class PagingViewHolder<in T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

        open fun bind(item: T) {

        }

        protected fun withSafeAdapterPosition(action: (Int) -> Unit) {
            if (adapterPosition != RecyclerView.NO_POSITION) {
                action.invoke(adapterPosition)
            }
        }
    }
}
