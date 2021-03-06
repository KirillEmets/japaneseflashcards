package com.kirillemets.flashcards.review

import androidx.lifecycle.*
import com.kirillemets.flashcards.TimeUtil
import com.kirillemets.flashcards.database.CardDatabaseDao
import com.kirillemets.flashcards.database.DatabaseRepository
import com.kirillemets.flashcards.database.FlashCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.joda.time.LocalDate
import kotlin.math.roundToInt

class ReviewFragmentViewModel(repository: DatabaseRepository): ViewModel() {
    val database: CardDatabaseDao = repository.cardDatabaseDao
    var delayEasyMultiplier = 1f
    var delayHardMultiplier = 1f

    val reviewCards: MutableLiveData<List<ReviewCard>> = MutableLiveData(listOf())

    private var wordCounter = MutableLiveData<Int>()
    val counterText = Transformations.map(wordCounter) {
        "${it + 1} / ${reviewCards.value?.size ?: 0}"
    }

    val currentCard: LiveData<ReviewCard> = Transformations.map(wordCounter) {
        reviewCards.value!![it]
    }

    val answerShown = MutableLiveData(false)
    var reviewGoing = false

    val fontSizeBig = MutableLiveData(30)
    val fontSizeSmall = MutableLiveData(30)

    val buttonReviewClickable = Transformations.map(reviewCards) {
        it.isNotEmpty()
    }

    val onRunOutOfWords = MutableLiveData(false)

    init {
        loadCardsToReview()
    }

    private fun loadCardsToReview() {
        viewModelScope.launch {
            val currentTime = LocalDate.now().toDateTimeAtStartOfDay().millis
            val cards = getRelevantCardsFromDatabase(currentTime)
            val newList = mutableListOf<ReviewCard>()
            cards.forEach { card ->
                if (card.nextReviewTime <= currentTime)
                    newList.add(ReviewCard.fromDataBaseFlashCardDefault(card))
                if (card.nextReviewTimeReversed <= currentTime)
                    newList.add(ReviewCard.fromDataBaseFlashCardReversed(card))
            }
            reviewCards.value = newList.shuffled()
            if (newList.isNotEmpty())
                wordCounter.value = 0
        }
    }

    private suspend fun getRelevantCardsFromDatabase(time: Long): List<FlashCard> =
        withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            database.getRelevantCards(time)
        }

    fun onButtonShowAnswerClick() {
        answerShown.value = true
    }

    fun onButtonAnswerClick(buttonType: Int) {
        val card: ReviewCard = currentCard.value!!
        if (buttonType == 0) {
            viewModelScope.launch(Dispatchers.IO) {
                if (!card.reversed)
                    database.resetDelayByIds(setOf(card.cardId), TimeUtil.todayMillis)
                else
                    database.resetDelayByIdsReversed(setOf(card.cardId), TimeUtil.todayMillis)

            }
        } else {
            val newDelay: Int = getNewDelay(card.lastDelay, buttonType)

            val nextRepeatTime: Long =
                LocalDate.now().toDateTimeAtStartOfDay().plusDays(newDelay).millis

            viewModelScope.launch(Dispatchers.IO) {
                if (!card.reversed)
                    database.updateRegularDelayAndTime(card.cardId, newDelay, nextRepeatTime)
                else
                    database.updateReversedDelayAndTime(card.cardId, newDelay, nextRepeatTime)
            }
        }

        if (wordCounter.value!! + 1 == reviewCards.value!!.size)
            return onRunOutOfWords()

        wordCounter.value = wordCounter.value?.plus(1)
        answerShown.value = false
    }

    // 1 - easy, 2 - hard
    fun getNewDelay(lastDelay: Int, difficulty: Int): Int =
        (lastDelay * when (difficulty) {
            1 -> delayEasyMultiplier
            2 -> delayHardMultiplier
            else -> 1f
        }).roundToInt()

    fun deleteCurrentCard() {
        val deleteId = currentCard.value!!.cardId
        val new = reviewCards.value!!.toMutableList()
        var wc = wordCounter.value!!

        if (new.indexOfFirst { card -> card.cardId == deleteId } < wc)
            wc -= 1

        new.removeAll { card -> card.cardId == deleteId }
        reviewCards.postValue(new)

        if (new.size <= wc) {
            runBlocking(Dispatchers.IO) {
                database.deleteByIndexes(setOf(deleteId))
            }
            return onRunOutOfWords()
        }

        wordCounter.postValue(wc)
        answerShown.postValue(false)

        viewModelScope.launch(Dispatchers.IO) {
            database.deleteByIndexes(setOf(deleteId))
        }
    }

    fun startReview() {
        reviewGoing = true
    }

    fun restart() {
        answerShown.value = false
        reviewGoing = false
        loadCardsToReview()
    }

    private fun onRunOutOfWords() {
        restart()
        onRunOutOfWords.value = true
    }
}