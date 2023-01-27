package com.evaluation.fetch.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.evaluation.fetch.R
import com.evaluation.fetch.api.ListApiService
import com.evaluation.fetch.api.ListFetcher
import com.evaluation.fetch.databinding.ActivityMainBinding
import com.evaluation.fetch.databinding.ListItemBinding
import com.evaluation.fetch.model.ListableItem
import com.evaluation.fetch.viewmodel.ListViewModel
import com.evaluation.fetch.viewmodel.ListViewModelFactory

class MainActivity : AppCompatActivity() {

    lateinit var viewModel : ListViewModel
    lateinit var binding: ActivityMainBinding
    private var itemAdapter : ItemAdapter? = ItemAdapter(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate and set view
        binding = ActivityMainBinding.inflate(layoutInflater,)
        setContentView(binding.root)

        // Prepare repository
        val listApiService = ListApiService.getInstance()
        val listFetcher = ListFetcher(listApiService)

        // Prepare recycler view for display
        binding.listRecyclerView.apply{
            layoutManager = LinearLayoutManager(context)
            adapter = itemAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        // Set up the view model with repository
        viewModel = ViewModelProvider(
            this, ListViewModelFactory(listFetcher))[ListViewModel::class.java]

        // Set up toolbar
        binding.listToolbar.apply{

            // Get themed color attribute for Toolbar's title
            val typedValue = TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimary,
                typedValue, true)
            @ColorInt
            val colorOnPrimary = typedValue.data

            // Inflate toolbar and set properties
            inflateMenu(R.menu.toolbar_menu)
            title = context.getString(R.string.toolbar_title)
            setTitleTextColor(colorOnPrimary)
            setOnMenuItemClickListener { item ->

                when (item.itemId) {

                    R.id.refetch_action -> {
                        viewModel.fetchData()
                        true
                    }

                    else -> false
                }
            }
        }

        // Set LiveData observers
        viewModel.apply{

            itemListLiveData.observe(this@MainActivity) { newList ->

                if (newList.isNotEmpty()) {

                    itemAdapter = ItemAdapter(newList)

                    binding.listRecyclerView.apply{
                        visibility = View.VISIBLE
                        adapter = itemAdapter
                    }

                } else {

                    viewModel.errorLiveData.value = getString(R.string.empty_list_error_message)
                }
            }

            errorLiveData.observe(this@MainActivity) { newError ->

                if (newError.isNullOrBlank()) {

                    binding.apply{

                        listRecyclerView.visibility = View.VISIBLE
                        errorGroup.visibility = View.GONE
                    }

                } else {

                    binding.apply{

                        listRecyclerView.visibility = View.GONE
                        errorGroup.visibility = View.VISIBLE
                        errorLabel.text = newError
                    }
                }
            }

            isRunningAsyncLiveData.observe(this@MainActivity) { isRunning ->

                binding.apply {

                    // Show "loading" card only while process is running
                    progressCard.visibility = if (isRunning) View.VISIBLE else View.GONE

                    // Enable Re-fetch action only when there is not a process running
                    listToolbar.menu.findItem(R.id.refetch_action).apply{
                        isEnabled = !isRunning
                        icon?.alpha = if (isRunning) 155 else 255
                    }
                }
            }
        }

        // Perform initial fetch attempt
        viewModel.fetchData()
    }

    // region [ RecyclerView adapter classes ]

    private class ItemViewHolder(val binding: ListItemBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ListableItem) {

            binding.apply {

                itemNameLabel.text = item.name ?: "null"

                listIdIcon.setImageResource(
                    when(item.listId) {
                        1   -> R.drawable.group_1_icon
                        2   -> R.drawable.group_2_icon
                        3   -> R.drawable.group_3_icon
                        4   -> R.drawable.group_4_icon
                        else-> R.drawable.no_group_icon
                    }
                )
            }
        }
    }

    private class ItemAdapter(val items: List<ListableItem>) : RecyclerView.Adapter<ItemViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {

            val binding = ListItemBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return ItemViewHolder(binding)
        }

        override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {

            val item = items[position]

            viewHolder.bind(item)
        }

        override fun getItemCount(): Int = items.size
    }

    // endregion

}