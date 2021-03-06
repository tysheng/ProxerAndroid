package me.proxer.app.chat.pub.room

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseContentFragment
import me.proxer.app.chat.pub.message.ChatActivity
import me.proxer.app.util.extension.autoDispose
import me.proxer.app.util.extension.unsafeLazy
import me.proxer.library.entity.chat.ChatRoom
import org.jetbrains.anko.bundleOf
import kotlin.properties.Delegates

/**
 * @author Ruben Gees
 */
class ChatRoomFragment : BaseContentFragment<List<ChatRoom>>() {

    companion object {
        fun newInstance() = ChatRoomFragment().apply {
            arguments = bundleOf()
        }
    }

    override val viewModel by unsafeLazy { ChatRoomViewModelProvider.get(this) }

    private var adapter by Delegates.notNull<ChatRoomAdapter>()

    override val contentContainer: ViewGroup
        get() = recyclerView

    private val recyclerView by bindView<RecyclerView>(R.id.recyclerView)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter = ChatRoomAdapter()

        adapter.clickSubject
            .autoDispose(this)
            .subscribe { item -> ChatActivity.navigateTo(requireActivity(), item.id, item.name, item.isReadOnly) }

        adapter.linkClickSubject
            .autoDispose(this)
            .subscribe { showPage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat_room, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onDestroyView() {
        recyclerView.layoutManager = null
        recyclerView.adapter = null

        super.onDestroyView()
    }

    override fun showData(data: List<ChatRoom>) {
        super.showData(data)

        adapter.swapDataAndNotifyWithDiffing(data.toList())
    }

    override fun hideData() {
        adapter.swapDataAndNotifyWithDiffing(emptyList())

        super.hideData()
    }
}
