package com.android.aman.newsapp

import android.app.SearchManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.newsapp.Adapter
import com.example.newsapp.Networking.ApiClient
import com.example.newsapp.Networking.ApiInterface
import com.example.newsapp.Utils
import com.example.newsapp.model.Article
import com.example.newsapp.model.News
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.error_handling.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() , SwipeRefreshLayout.OnRefreshListener{

    private val apiKey: String = "fe1386d861f343f2bbb08659b49bb6cb"
    var articles: ArrayList<Article> = ArrayList()


    private lateinit var adapter: Adapter
    private lateinit var viewManger: RecyclerView.LayoutManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        swipeRefresh.setOnRefreshListener(this)
        swipeRefresh.setColorSchemeResources(R.color.colorAccent)

        viewManger = LinearLayoutManager(this)
        recyclerView.layoutManager = viewManger
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.isNestedScrollingEnabled = false

        loadJson("")

        // onLoadRefresh("")
    }

    private fun loadJson(keyword: String) {

        errorLayout.visibility = View.GONE
        swipeRefresh.isRefreshing = true

        val apiInterface: ApiInterface? = ApiClient.getApiClient?.create(ApiInterface::class.java)

        val utils = Utils()

        val country: String = utils.getCountry()
        val language: String = utils.getLanguage()

        val call: Call<News>?

        call = if (keyword.length < 0) {
            apiInterface?.getNewsSearch(keyword, language, "publishedAt", apiKey)
        } else {
            apiInterface?.getNews(country, apiKey)
        }


        call?.enqueue(object : Callback<News> {
            override fun onFailure(call: Call<News>?, t: Throwable?) {
                headlines.visibility = View.INVISIBLE
                swipeRefresh.isRefreshing = false
                Toast.makeText(this@MainActivity, "No Result", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<News>?, response: Response<News>?) {
                if (response!!.isSuccessful && response.body().article != null) {
                    if (articles.isNotEmpty()) {
                    }

                    articles = (response.body().article as ArrayList<Article>?)!!
                    adapter = Adapter(this@MainActivity, articles)
                    recyclerView.adapter = adapter
                    adapter.notifyDataSetChanged()

                    headlines.visibility = View.VISIBLE
                    swipeRefresh.isRefreshing = false

                } else {
                    headlines.visibility = View.INVISIBLE
                    swipeRefresh.isRefreshing = false


                    val errorCode: String = when {
                        response.code() == 404 -> "404 not found"
                        response.code() == 500 -> "500 server broken"
                        else -> "unknown error"
                    }

                    showErrorMessage("No Result", "Try Again!\n$errorCode")
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        val searchManger: SearchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView: SearchView = menu?.findItem(R.id.search_bar)?.actionView as SearchView
        val menuItem: MenuItem = menu.findItem(R.id.search_bar)

        searchView.setSearchableInfo(searchManger.getSearchableInfo(componentName))
        searchView.queryHint = "Search News..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query?.length!! > 2) {
                    loadJson(query)
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Type more than two letters",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })

        menuItem.icon.setVisible(false, false)

        return true
    }

    override fun onRefresh() {
        loadJson("")
    }

    private fun onLoadRefresh(keyword: String) {
        swipeRefresh.post {
            Runnable {
                loadJson(keyword)
            }
        }
    }

    fun showErrorMessage(title: String, message: String) {
        if (errorLayout.visibility == View.GONE) {
            errorLayout.visibility = View.VISIBLE
        }

        errorTitle.text = title
        errorMessage.text = message

        btnRetry.setOnClickListener {
            onLoadRefresh("")
        }

    }

}



