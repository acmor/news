package feeds

import api.*
import db.Feed
import db.FeedQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedsRepository(
    private val api: NewsApi,
    private val db: FeedQueries,
) {

    fun insertOrReplace(feed: Feed) {
        db.insertOrReplace(feed)
    }

    suspend fun insertByUrl(url: String) = withContext(Dispatchers.IO) {
        db.insertOrReplace(api.addFeed(url))
    }

    suspend fun selectAll() = withContext(Dispatchers.IO) {
        db.selectAll().executeAsList()
    }

    fun selectById(id: String): Feed? {
        return db.selectById(id).executeAsOneOrNull()
    }

    suspend fun updateTitle(feedId: String, newTitle: String) = withContext(Dispatchers.IO) {
        val feed = selectById(feedId) ?: throw Exception("Cannot find feed $feedId in cache")
        val trimmedNewTitle = newTitle.trim()
        api.updateFeedTitle(feedId, trimmedNewTitle)
        db.insertOrReplace(feed.copy(title = trimmedNewTitle))
    }

    suspend fun deleteById(id: String) = withContext(Dispatchers.IO) {
        api.deleteFeed(id)
        db.deleteById(id)
    }

    suspend fun sync() = withContext(Dispatchers.IO) {
        val newFeeds = api.getFeeds().sortedBy { it.id }
        val cachedFeeds = selectAll().sortedBy { it.id }

        if (newFeeds.map { it.id } != cachedFeeds.map { it.id }) {
            db.transaction {
                db.deleteAll()

                newFeeds.forEach { feed ->
                    val cachedFeed = cachedFeeds.find { it.id == feed.id }

                    db.insertOrReplace(
                        feed.copy(
                            openEntriesInBrowser = cachedFeed?.openEntriesInBrowser ?: false,
                            blockedWords = cachedFeed?.blockedWords ?: "",
                        )
                    )
                }
            }
        }
    }
}