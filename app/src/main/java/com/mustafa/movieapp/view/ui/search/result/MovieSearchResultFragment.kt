package com.mustafa.movieapp.view.ui.search.result

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mustafa.movieapp.R
import com.mustafa.movieapp.binding.FragmentDataBindingComponent
import com.mustafa.movieapp.databinding.FragmentMovieSearchResultBinding
import com.mustafa.movieapp.di.Injectable
import com.mustafa.movieapp.extension.hideKeyboard
import com.mustafa.movieapp.models.Status
import com.mustafa.movieapp.utils.autoCleared
import com.mustafa.movieapp.view.adapter.MovieSearchListAdapter
import com.mustafa.movieapp.view.ui.common.AppExecutors
import com.mustafa.movieapp.view.ui.common.RetryCallback
import com.mustafa.movieapp.view.ui.search.MovieSearchViewModel
import kotlinx.android.synthetic.main.fragment_movie_search_result.*
import kotlinx.android.synthetic.main.fragment_movie_search_result.view.*
import kotlinx.android.synthetic.main.toolbar_search_result.*
import timber.log.Timber
import javax.inject.Inject

class MovieSearchResultFragment : Fragment(), Injectable {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var appExecutors: AppExecutors

    private val viewModel by viewModels<MovieSearchViewModel> { viewModelFactory }
    var dataBindingComponent = FragmentDataBindingComponent(this)
    var binding by autoCleared<FragmentMovieSearchResultBinding>()
    var adapter by autoCleared<MovieSearchListAdapter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_movie_search_result,
            container,
            false
        )

        Timber.d("Hell..Yeahh...onCreateView()")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeUI()
        Timber.d("Hell..Yeahh...onViewCreated()")

        subscribers()

        viewModel.setSearchMovieQueryAndPage(getQuerySafeArgs(), 1)

        with(binding) {
            lifecycleOwner = this@MovieSearchResultFragment
            searchResult = viewModel.searchMovieListLiveData
            query = viewModel.queryMovieLiveData
            callback = object : RetryCallback {
                override fun retry() {
                    viewModel.refresh()
                }
            }
        }
    }

    private fun subscribers() {
        viewModel.searchMovieListLiveData.observe(viewLifecycleOwner, Observer {
            if (it.data != null && it.data.isNotEmpty()) {
                adapter.submitList(it.data)
            }
        })
    }


    private fun getQuerySafeArgs(): String? {
        val params =
            MovieSearchResultFragmentArgs.fromBundle(
                requireArguments()
            )
        return params.query
    }

    private fun initializeUI() {
        adapter = MovieSearchListAdapter(
            appExecutors,
            dataBindingComponent
        ) {
            navController().navigate(
                MovieSearchResultFragmentDirections.actionMovieSearchFragmentResultToMovieDetail(
                    it
                )
            )
        }

        hideKeyboard()
        binding.root.recyclerView_search_result_movies.adapter = adapter

        recyclerView_search_result_movies.layoutManager = LinearLayoutManager(context)

        recyclerView_search_result_movies.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastPosition = layoutManager.findLastVisibleItemPosition()
                if (lastPosition == adapter.itemCount - 1
                    && viewModel.searchMovieListLiveData.value?.status != Status.LOADING
                    && dy > 0
                ) {
                    if (viewModel.searchMovieListLiveData.value?.hasNextPage!!) {
                        viewModel.loadMore()
                    }
                }
            }
        })

        search_view.setOnSearchClickListener {
            navController().navigate(MovieSearchResultFragmentDirections.actionMovieSearchFragmentResultToMovieSearchFragment())
        }

        arrow_back.setOnClickListener {
            navController().navigate(MovieSearchResultFragmentDirections.actionMovieSearchFragmentResultToMovieSearchFragment())
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Hell..Yeahh...onDestroy()")
    }

    override fun onStop() {
        super.onStop()
        Timber.d("Hell..Yeahh...onStop()")
    }

    override fun onResume() {
        super.onResume()
        Timber.d("Hell..Yeahh...onResume()")
    }

    override fun onPause() {
        super.onPause()
        Timber.d("Hell..Yeahh...onPause()")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("Hell..Yeahh...onDestroyView()")
    }

    override fun onDetach() {
        super.onDetach()
        Timber.d("Hell..Yeahh...onDetach()")
    }

    override fun onStart() {
        super.onStart()
        Timber.d("Hell..Yeahh...onStart()")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("Hell..Yeahh...onCreate()")
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        Timber.d("Hell..Yeahh...onAttachFragment()")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Timber.d("Hell..Yeahh...onAttach()")
    }


    /**
     * Created to be able to override in tests
     */
    fun navController() = findNavController()
}