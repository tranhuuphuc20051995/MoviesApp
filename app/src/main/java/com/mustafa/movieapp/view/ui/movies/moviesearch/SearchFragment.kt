package com.mustafa.movieapp.view.ui.movies.moviesearch

import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.view.get
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mustafa.movieapp.R
import com.mustafa.movieapp.binding.FragmentDataBindingComponent
import com.mustafa.movieapp.databinding.FragmentMoviesSearchBinding
import com.mustafa.movieapp.di.Injectable
import com.mustafa.movieapp.extension.gone
import com.mustafa.movieapp.extension.inVisible
import com.mustafa.movieapp.extension.visible
import com.mustafa.movieapp.utils.autoCleared
import com.mustafa.movieapp.view.adapter.MovieSearchListAdapter
import com.mustafa.movieapp.view.adapter.filterSelectableAdapter.FilterMultiSelectableAdapter
import com.mustafa.movieapp.view.adapter.filterSelectableAdapter.SelectableItem
import com.mustafa.movieapp.view.ui.common.AppExecutors
import com.mustafa.movieapp.view.ui.common.RetryCallback
import kotlinx.android.synthetic.main.fragment_movies.recyclerView_movies
import kotlinx.android.synthetic.main.fragment_movies.view.*
import kotlinx.android.synthetic.main.fragment_movies_search.*
import kotlinx.android.synthetic.main.fragment_movies_search_filter.*
import kotlinx.android.synthetic.main.toolbar_search_iconfied.*
import org.jetbrains.anko.textColor
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class SearchFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appExecutors: AppExecutors

    private val viewModel by viewModels<MovieSearchViewModel> { viewModelFactory }
    var dataBindingComponent = FragmentDataBindingComponent(this)
    var binding by autoCleared<FragmentMoviesSearchBinding>()
    var adapter by autoCleared<MovieSearchListAdapter>()
    //    var adapter2 by autoCleared<MovieSearchListAdapter>()
//    val columns =
//        arrayOf("_id", "title", "poster_path", "vote_average", "genre_ids", "release_date")

    private val searchTypes = listOf("Movies", "TV Shows")

    private val ratings = listOf("+9", "+8", "+7", "+6", "+5", "+4")

    private val languages = listOf(
        "English", "French", "German",
        "Italian", "Japanese", "Spanish", "Choose..."
    )

    private val genres = listOf(
        "Adventure",
        "Crime",
        "History",
        "Drama",
        "History",
        "Thriller",
        "Romance",
        "Comedy",
        "Family",
        "War",
        "Horror",
        "Western",
        "Science Fiction",
        "Fantasy",
        "Documentary",
        "Animation"
    )

    private val countries = listOf(
        "United State", "Canada", "Germany", "France", "United Kingdom",
        "Spain", "Italy", "India", "Japan", "Choose..."
    )

    private val years = listOf(
        "2020", "2019", "2018", "2017", "2016",
        "2015", "2014", "2013", "2012", "2011", "2010", "Choose..."
    )

    private val hasAnyFilterBeenSelected = MutableLiveData<Boolean>()

    private val mapFilterTypeToSelectedFilters =
        HashMap<FilterMultiSelectableAdapter, ArrayList<String>>()

//    private lateinit var mSearchViewAdapter: SuggestionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_movies_search,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeUI()
        subscribers()

        with(binding) {
            lifecycleOwner = this@SearchFragment
            searchResult = viewModel.searchMovieListLiveData
            query = viewModel.queryLiveData
            callback = object : RetryCallback {
                override fun retry() {
                    viewModel.refresh()
                }
            }
        }
    }

    private fun initializeUI() {

        initToolbar()

        initTabLayout()

        setFilterButtons()

        initClearAndSeeResultBar()

        setupRecentQueries()


        // Adapter
        adapter = MovieSearchListAdapter(
            appExecutors,
            dataBindingComponent
        ) {
            navController().navigate(
                SearchFragmentDirections.actionMovieSearchFragmentToMovieDetail(
                    it
                )
            )
        }
//        recyclerView_movies.setHasFixedSize(true)
//        recyclerView_movies.addItemDecoration(
//            DividerItemDecoration(
//                recyclerView_movies.context,
//                DividerItemDecoration.HORIZONTAL
//            )
//        )
//        adapter.setHasStableIds(true) // prevent blinking .. in Case notifyDataSetChanged()
        // To have a nice animation and avoid blinking in the RecyclerView:
        /**
         * 1-  adapter.setHasStableIds(true)
         * 2-  Use notifyItemRangeInserted(start, count)
         */
        binding.root.recyclerView_movies.adapter = adapter

        recyclerView_movies.layoutManager = LinearLayoutManager(
            context,
            LinearLayoutManager.VERTICAL,
            false
        )

    }

    private fun subscribers() {

        hasAnyFilterBeenSelected.observe(viewLifecycleOwner, Observer {
            if (it != null && it == true) {
                if (!checkIfAllFiltersEmpty()) {
                    clear_filter.isEnabled = true
                    clear_filter.textColor =
                        context?.resources?.getColor(R.color.colorAccent, context?.theme)!!
                } else {
                    clear_filter.isEnabled = false // to NOT let ripple effect work
                    clear_filter.textColor =
                        context?.resources?.getColor(R.color.clearFilterColor, context?.theme)!!
                }

            }
        })
    }

    /**
     * Init the toolbar
     */
    private fun initToolbar() {

        search_view.onActionViewExpanded()

        voiceSearch.setOnClickListener {
            val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            voiceIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            if (activity?.packageManager?.let { it1 -> voiceIntent.resolveActivity(it1) } != null) {
                startActivityForResult(voiceIntent, 10)
            } else {
                Toast.makeText(
                    context,
                    "your device does not support Speech Input",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

//        val searchViewEditText =
//            search_view.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)

        arrow_back.setOnClickListener {
            search_view.clearFocus()
            navController().navigate(SearchFragmentDirections.actionMovieSearchFragmentToMoviesFragment())
        }


        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener,
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                navController().navigate(
                    SearchFragmentDirections.actionSearchFragmentToSearchFragmentResult(query!!))
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSuggestionsQuery(newText!!)
                viewModel.suggestions.observe(viewLifecycleOwner, Observer {
                    recyclerView_movies.visible()
                    hideRecentQueries()
                    hideRecentSearchesBar()
                    if (!it.isNullOrEmpty()) {
                        adapter.submitList(it)
                    }

                    if (newText.isEmpty() || newText.isBlank()) {
                        showRecentSearchesBar()
                        showRecentQueries()
                        adapter.submitList(null)
                        recyclerView_movies.inVisible()
                    }
//                    Log.d("Loading Suggestions", viewModel.suggestions.value?.map { it.title }.toString())
//                    if (!it.isNullOrEmpty()) {
//                        val cursor : Cursor = convertToCursor(it.take(5)) as Cursor
//                        mSearchViewAdapter.changeCursor(cursor)
//                    }

                })
                return true
            }
        })
    }

    /**
     * Created to be able to override in tests
     */
    fun navController() = findNavController()

    /**
     * dismiss Keyboard
     *
     * @param windowToken The token of the window that is making the request, as returned by View.getWindowToken().
     */
    private fun dismissKeyboard(windowToken: IBinder) {
        val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(windowToken, 0)
    }


    private fun setupRecentQueries() {
        viewModel.getRecentQueries().observe(viewLifecycleOwner, Observer { it ->
            if (!it.isNullOrEmpty()) {
                val queries = it.map { it.query }
                setListViewOfRecentQueries(queries)
            }
        })
    }

    private fun setListViewOfRecentQueries(queries: List<String?>) {
        val arrayAdapter = ArrayAdapter<String>(context!!, R.layout.recent_query_item, queries)
        showRecentQueries()
        list_recent_queries.setHeaderDividersEnabled(true)
        list_recent_queries.setFooterDividersEnabled(true)
        list_recent_queries.adapter = arrayAdapter
        list_recent_queries.setOnItemClickListener { parent, _, position, _ ->
            val query = parent.getItemAtPosition(position) as String
            navController().navigate(
                SearchFragmentDirections.actionSearchFragmentToSearchFragmentResult(query)
            )
        }

        clear_recent_queries.setOnClickListener {
            val builder = AlertDialog.Builder(context!!, R.style.AlertDialogTheme)
            builder.setMessage(R.string.dialog_message)
                .setPositiveButton(R.string.clear) { _, _ ->
                    viewModel.deleteAllRecentQueries()
                    arrayAdapter.clear()
                    hideRecentQueries()
                }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }

            builder.create().show()
        }
    }

    private fun initTabLayout() {

        val layoutTab1 = ((tabs.getChildAt(0)) as LinearLayout).getChildAt(0) as LinearLayout
        val layoutTab2 = ((tabs.getChildAt(0)) as LinearLayout).getChildAt(1) as LinearLayout

        val layoutParams1 = layoutTab1.layoutParams as LinearLayout.LayoutParams
        val layoutParams2 = layoutTab2.layoutParams as LinearLayout.LayoutParams

        layoutParams1.marginStart = 125
        layoutParams1.width = ActionBar.LayoutParams.WRAP_CONTENT
        layoutTab1.layoutParams = layoutParams1

        layoutParams2.marginEnd = 125
        layoutParams2.width = ActionBar.LayoutParams.WRAP_CONTENT
        layoutParams2.weight = 0.5f
        layoutTab2.layoutParams = layoutParams2

        val tabLayout = tabs[0] as ViewGroup

        tabLayout.getChildAt(0).setOnClickListener {
            showRecentSearchesBar()
            hideFiltersLayout()
            showListViewAndRecyclerView()
            search_view.requestFocus()
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.visible()

        }
        tabLayout.getChildAt(1).setOnClickListener {
            hideListViewAndRecyclerView()
            showFiltersLayout()
            hideRecentSearchesBar()
            dismissKeyboard(search_view.windowToken)
            activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.gone()
        }
    }

    private fun setFilterButtons() {

        val searchTypeRecyclerView =
            activity?.findViewById<RecyclerView>(R.id.recycler_view_search_type)
        setFilterAdapter(searchTypeRecyclerView, searchTypes)

        val ratingRecyclerView = activity?.findViewById<RecyclerView>(R.id.recycler_view_ratings)
        setFilterAdapter(ratingRecyclerView, ratings)

        val genreAdapter = activity?.findViewById<RecyclerView>(R.id.recycler_view_genres)
        setFilterAdapter(genreAdapter, genres)

        val yearAdapter = activity?.findViewById<RecyclerView>(R.id.recycler_view_years)
        setFilterAdapter(yearAdapter, years)

        val countryAdapter = activity?.findViewById<RecyclerView>(R.id.recycler_view_countries)
        setFilterAdapter(countryAdapter, countries)

        val languageAdapter = activity?.findViewById<RecyclerView>(R.id.recycler_view_languages)
        setFilterAdapter(languageAdapter, languages)

    }


    private fun setFilterAdapter(
        recyclerView: RecyclerView?,
        listOfButtonFiltersTitles: List<String>
    ) {
        val filters = ArrayList<String>()

        val selectableItemList = ArrayList<SelectableItem>()
        for (item in listOfButtonFiltersTitles) {
            val selectableItem = SelectableItem(item, false)
            selectableItemList.add(selectableItem)
        }
        val filterAdapter =
            FilterMultiSelectableAdapter(selectableItemList, context, dataBindingComponent, {
                filters.add(it)
                hasAnyFilterBeenSelected.value = true
            }, {
                if (filters.size > 0) filters.remove(it)
                hasAnyFilterBeenSelected.value = true
            }
            )
        mapFilterTypeToSelectedFilters[filterAdapter] = filters
        recyclerView?.layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
        recyclerView?.adapter = filterAdapter
        recyclerView?.setHasFixedSize(true)
    }


    private fun initClearAndSeeResultBar() {
        clear_filter.setOnClickListener {
            for (adapter in mapFilterTypeToSelectedFilters.keys) {
                adapter.clearSelection()
            }
            mapFilterTypeToSelectedFilters.forEach{
                it.value.clear()
            }
            hasAnyFilterBeenSelected.value = true
        }

    }

    private fun checkIfAllFiltersEmpty(): Boolean {
        mapFilterTypeToSelectedFilters.forEach {
            if (it.value.isNotEmpty()) {
                return false
            }
        }
        return true
    }

    private fun hideFiltersLayout() {
        filters.inVisible()
    }

    private fun showFiltersLayout() {
        filters.visible()
    }


    private fun hideRecentSearchesBar() {
        recent_queries_bar.gone()
    }

    private fun showRecentSearchesBar() {
        recent_queries_bar.visible()
    }


    private fun hideListViewAndRecyclerView() {
        list_recent_queries.inVisible()
        recyclerView_movies.inVisible()

    }

    private fun showListViewAndRecyclerView() {
        list_recent_queries.visible()
        recyclerView_movies.visible()
    }


    private fun hideRecentQueries() {
        list_recent_queries.inVisible()
    }

    private fun showRecentQueries() {
        list_recent_queries.visible()
    }

    /**
     * Receiving Voice Query
     * @param requestCode
     * @param resultCode
     * @param data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            10 -> if (resultCode == Activity.RESULT_OK && data != null) {
                val voiceQuery = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                dismissKeyboard(search_view.windowToken)
                search_view.setQuery(voiceQuery?.let { it[0] }, true)
            }
        }
    }

//    fun setSelectableItemBackground(view: View) {
//        val typedValue = TypedValue()
//
//        // I used getActivity() as if you were calling from a fragment.
//        // You just want to call getTheme() on the current activity, however you can get it
//        activity?.theme?.resolveAttribute(
//            android.R.attr.selectableItemBackground,
//            typedValue,
//            true
//        )
//
//        // it's probably a good idea to check if the color wasn't specified as a resource
//        if (typedValue.resourceId != 0) {
//            view.setBackgroundResource(typedValue.resourceId)
//        } else {
//            // this should work whether there was a resource id or not
//            view.setBackgroundColor(typedValue.data)
//        }
//
//    }




//    override fun onSuggestionSelect(position: Int): Boolean {
//        val cursor = search_view.suggestionsAdapter.getItem(position) as Cursor
//        val query = cursor.getString(1)
//        search_view.setQuery(query, false)
//        search_view.clearFocus()
//        return true
//    }
//
//    override fun onSuggestionClick(position: Int): Boolean {
//        val cursor = search_view.suggestionsAdapter.getItem(position) as Cursor
//        val query = cursor.getString(1)
//        search_view.setQuery(query, false)
//        search_view.clearFocus()
//        return true
//    }

//
//    private fun initSearchViewForSuggestions() {
//        val searchManager = activity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager
//        val searchableInfo = searchManager.getSearchableInfo(activity!!.componentName)
//        search_view.setSearchableInfo(searchableInfo)
//
//        mSearchViewAdapter = SuggestionsAdapter(
//            activity,
//            dataBindingComponent,
//            R.layout.suggestion_search_item,
//            null,
//            columns,
//            null,
//            -1000
//        )
//        search_view.suggestionsAdapter = mSearchViewAdapter
//
//    }
//
//
//    private fun convertToCursor(movies: List<Movie>): MatrixCursor? {
//
//        val cursor = MatrixCursor(columns)
//        var i = 0
//        for (movie in movies) {
//            val temp = ArrayList<String?>()
//
//            i = i + 1
//
//            temp.add(i.toString())
//
//            temp.add(movie.title)
//
//            if (movie.poster_path == null) {
//                temp.add("")
//            } else {
//                temp.add(movie.poster_path)
//            }
//
//            temp.add(movie.vote_average.toString())
//            temp.add(movie.genre_ids.toString())
//
//            if (movie.release_date == null) {
//                temp.add("")
//            } else {
//                temp.add(StringUtils.formatReleaseDate(movie.release_date))
//            }
//
//            cursor.addRow(temp)
//        }
//        return cursor
//    }

}