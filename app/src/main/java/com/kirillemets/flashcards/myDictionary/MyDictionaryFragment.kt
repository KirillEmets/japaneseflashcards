package com.kirillemets.flashcards.myDictionary

import android.os.Bundle
import android.view.*
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kirillemets.flashcards.R
import com.kirillemets.flashcards.database.CardDatabase
import com.kirillemets.flashcards.databinding.FragmentMyDictionaryBinding


class MyDictionaryFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private lateinit var viewModel: MyDictionaryFragmentViewModel
    lateinit var binding: FragmentMyDictionaryBinding
    private val adapter: MyDictionaryFragmentAdapter = MyDictionaryFragmentAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val database = CardDatabase.getInstance(requireContext()).flashCardsDao()
        binding = FragmentMyDictionaryBinding.inflate(layoutInflater)

        viewModel = ViewModelProvider(
            this,
            MyDictionaryFragmentViewModelFactory(database)
        ).get(MyDictionaryFragmentViewModel::class.java)

        binding.recyclerView.adapter = adapter
        viewModel.allCards.observe(this, {
            adapter.cards = it
        })

        binding.lifecycleOwner = this
        return binding.root
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.my_dictionaty_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.item_remove -> {
                viewModel.deleteWords(adapter.checkedCards.toSet())
                uncheckAllWords()
                true
            }
            R.id.item_reset -> {
                viewModel.resetWords(adapter.checkedCards.toSet())
                uncheckAllWords()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun uncheckAllWords() {
        adapter.checkedCards.clear()

        binding.recyclerView.children.forEach { view ->
            (binding.recyclerView.getChildViewHolder(view)
                    as MyDictionaryFragmentAdapter.MyDictionaryFragmentViewHolder)
                .binding.card.isChecked = false
        }

    }

}