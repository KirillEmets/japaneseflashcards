package com.kirillemets.flashcards.addWord

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kirillemets.flashcards.database.CardDatabaseDao
import com.kirillemets.flashcards.database.DatabaseRepository
import com.kirillemets.flashcards.database.FlashCard
import com.kirillemets.flashcards.network.JishoApi
import com.kirillemets.flashcards.network.QueryData
import kotlinx.coroutines.*
import java.lang.Exception

class AddWordFragmentViewModel(repository: DatabaseRepository): ViewModel() {
    val database: CardDatabaseDao = repository.cardDatabaseDao

    private val job = Job()
    private var coroutineScope = CoroutineScope(job + Dispatchers.Main)

    private var searchJob: Job = Job()

    private val _flashCards: MutableLiveData<List<SearchResultCard>> = MutableLiveData(listOf())
    val flashCards: LiveData<List<SearchResultCard>>
        get() = _flashCards

    private val _insertionResult: MutableLiveData<Boolean> = MutableLiveData()
    val insertionResult: LiveData<Boolean>
        get() = _insertionResult

    fun startSearch(word: String, withDelay: Boolean = true) {
        searchJob.cancel(CancellationException())
        searchJob = coroutineScope.launch {
            if(withDelay)
                delay(500)
            try {
                val queryData = getSearchQuery(word)
                _flashCards.value = createFlashCards(queryData)
            }
            catch (e: CancellationException) {

            }
            catch (e: Exception) {
                // TODO
                _flashCards.value = listOf(SearchResultCard(e.message?:"", "error", listOf()))
            }
        }
    }

    fun onAddButtonClicked(resultCard: SearchResultCard, id: Int) {
        coroutineScope.launch {
            insert(resultCard.flashCard(id))
        }
    }

    private suspend fun insert(flashCard: FlashCard) {
        withContext(Dispatchers.IO) {
            if(database.find(flashCard.english, flashCard.japanese, flashCard.reading).isNotEmpty()) {
                _insertionResult.postValue(false)
                return@withContext
            }
            database.insert(flashCard)
            _insertionResult.postValue(true)
        }
    }

    private suspend fun getSearchQuery(word: String): QueryData {
        return JishoApi.retrofitService.getDataObjectAsync(word).await()
    }

    private fun createFlashCards(data: QueryData): List<SearchResultCard> {
        val list: MutableList<SearchResultCard> = mutableListOf()
        var japanese: String
        var reading: String
        var englishMeanings: List<String>

        data.data.forEach {word ->
            reading = word.japanese?.get(0)?.reading ?: ""
            japanese = word.japanese?.get(0)?.word ?: reading
            if(japanese == reading)
                reading = ""

            englishMeanings = word.senses?.map {
                    sense -> sense.english_definitions.joinToString()
            } ?: listOf()

            list.add(
                SearchResultCard(
                japanese,
                reading,
                englishMeanings
                )
            )
        }
        if(list.size > 10)
            return list.subList(0, 10)
        return list
    }

    override fun onCleared() {
        super.onCleared()
        job.cancel(CancellationException("Cancelled on onCleared"))
    }
}

class AddWordFragmentViewModelFactory (private val repository: DatabaseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return modelClass.getConstructor(DatabaseRepository::class.java).newInstance(repository)
    }
}