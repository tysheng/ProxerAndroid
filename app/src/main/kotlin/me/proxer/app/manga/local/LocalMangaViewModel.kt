package me.proxer.app.manga.local

import android.app.Application
import android.arch.lifecycle.MutableLiveData
import com.gojuno.koptional.toOptional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import me.proxer.app.MainApplication.Companion.bus
import me.proxer.app.MainApplication.Companion.globalContext
import me.proxer.app.MainApplication.Companion.mangaDao
import me.proxer.app.MainApplication.Companion.mangaDatabase
import me.proxer.app.R
import me.proxer.app.base.BaseViewModel
import me.proxer.app.manga.MangaLocks
import me.proxer.app.util.ErrorUtils
import me.proxer.app.util.Validators
import me.proxer.app.util.extension.CompleteLocalMangaEntry
import me.proxer.app.util.extension.getQuantityString
import me.proxer.app.util.extension.plus
import java.io.File
import kotlin.concurrent.write

/**
 * @author Ruben Gees
 */
class LocalMangaViewModel(application: Application) : BaseViewModel<List<CompleteLocalMangaEntry>>(application) {

    override val isLoginRequired = true

    val jobInfo = MutableLiveData<String>()

    private var searchQuery: String? = null

    private var disposable: Disposable? = null
    private var deletionDisposable: Disposable? = null
    private val jobInfoDisposables = CompositeDisposable()

    init {
        updateJobInfo()

        jobInfoDisposables + Observable
                .merge(
                        bus.register(LocalMangaJob.StartedEvent::class.java),
                        bus.register(LocalMangaJob.FinishedEvent::class.java)
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    updateJobInfo()
                    reload()
                }

        jobInfoDisposables + bus.register(LocalMangaJob.FailedEvent::class.java)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { updateJobInfo() }
    }

    override fun onCleared() {
        disposable?.dispose()
        deletionDisposable?.dispose()
        jobInfoDisposables.dispose()

        disposable = null
        deletionDisposable = null

        super.onCleared()
    }

    override fun load() {
        disposable?.dispose()
        disposable = Single.fromCallable { Validators.validateLogin() }
                .map {
                    mangaDao.getEntries()
                            .associate { it.toNonLocalEntryCore() to mangaDao.getChaptersForEntry(it.id) }
                            .filter { (_, chapters) -> chapters.isNotEmpty() }
                            .filter { (entry, _) ->
                                searchQuery.let {
                                    when {
                                        it != null && it.isNotBlank() -> entry.name.contains(it, true)
                                        else -> true
                                    }
                                }
                            }
                            .toList()
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    error.value = null
                    data.value = null
                    isLoading.value = true
                }
                .doAfterTerminate { isLoading.value = false }
                .subscribe({
                    error.value = null
                    data.value = it
                }, {
                    data.value = null
                    error.value = ErrorUtils.handle(it)
                })
    }

    fun setSearchQuery(value: String?, trigger: Boolean = true) {
        if (searchQuery != value) {
            searchQuery = value

            if (trigger) reload()
        }
    }

    fun deleteChapter(chapter: LocalMangaChapter) {
        deletionDisposable?.dispose()
        deletionDisposable = Completable
                .fromAction {
                    mangaDatabase.deleteChapterAndEntryIfEmpty(chapter)

                    MangaLocks.localLock.write {
                        File("${globalContext.filesDir}/manga/${chapter.entryId}/${chapter.id}").deleteRecursively()
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { reload() }
    }

    fun updateJobInfo() = jobInfoDisposables + Single
            .fromCallable {
                val runningJobs = LocalMangaJob.countRunningJobs()
                val scheduledJobs = LocalMangaJob.countScheduledJobs()
                var message = ""

                message += when (runningJobs > 0) {
                    true -> globalContext.getQuantityString(R.plurals.fragment_local_manga_chapters_downloading,
                            runningJobs)
                    false -> ""
                }

                message += when (runningJobs > 0 && scheduledJobs > 0) {
                    true -> "\n"
                    false -> ""
                }

                message += when (scheduledJobs > 0) {
                    true -> globalContext.getQuantityString(R.plurals.fragment_local_manga_chapters_scheduled,
                            scheduledJobs)
                    false -> ""
                }

                (if (message.isBlank()) null else message).toOptional()
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { value -> jobInfo.value = value.toNullable() }
}
