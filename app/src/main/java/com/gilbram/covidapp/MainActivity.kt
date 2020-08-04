package com.gilbram.covidapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.gilbram.covidapp.Model.AllNegara
import com.gilbram.covidapp.Model.Negara
import com.gilbram.covidapp.Network.ApiService
import com.gilbram.covidapp.Network.RetrofitBuilder.retrofit
import com.gilbram.covidapp.adapter.CountryAdapter
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {
    private var progressBar:ProgressBar? = null
    private var ascending = true

    companion object{
        lateinit var adapters: CountryAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.progress_bar)

        search_view.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(newText: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                adapters.filter.filter(newText)
                return false
            }
        })
        swipe_refresh.setOnRefreshListener {
            getCountry()
            swipe_refresh.isRefreshing = false
        }
        getCountry()
        initializeView()
    }

    private fun initializeView() {
        sequence.setOnClickListener{
            sequenceWitoutInternet(ascending)
            ascending = !ascending
        }
    }

    private fun sequenceWitoutInternet(asc: Boolean) {
        rv_country.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@MainActivity)
            if (asc) {
                (layoutManager as LinearLayoutManager).reverseLayout = true
                (layoutManager as LinearLayoutManager).stackFromEnd = true
                Toast.makeText(this@MainActivity,"Z-A",Toast.LENGTH_SHORT).show()
            }else{
                (layoutManager as LinearLayoutManager).reverseLayout = false
                (layoutManager as LinearLayoutManager).stackFromEnd = false
                Toast.makeText(this@MainActivity,"A-Z",Toast.LENGTH_SHORT).show()
            }
            adapter = adapters

        }

    }
    private fun getCountry() {
        val api = retrofit.create(ApiService::class.java)
        api.getAllNegara().enqueue(object : Callback<AllNegara>{
            override fun onResponse(call: Call<AllNegara>, response: Response<AllNegara>) {
                if (response.isSuccessful){
                    val getListDataCorona= response.body()!!.Global
                    val formatter:NumberFormat= DecimalFormat("#,###")
                    txt_recovered_globe.text = formatter.format(getListDataCorona?.TotalRecovered?.toDouble())
                    txt_confirmed_globe.text = formatter.format(getListDataCorona?.TotalConfirmed?.toDouble())
                    txt_deaths_glone.text = formatter.format(getListDataCorona?.TotalDeaths?.toDouble())
                    rv_country.apply {
                        setHasFixedSize(true)
                        layoutManager= LinearLayoutManager(this@MainActivity)
                        progressBar?.visibility = View.GONE
                        adapters= CountryAdapter(response.body()!!.Countries as ArrayList<Negara>)
                        {negara ->  itemClicked(negara)}

                        adapter = adapters
                    }

                }else{
                    progressBar?.visibility = View.GONE
                    errorLoading(this@MainActivity)

                }
            }

            override fun onFailure(call: Call<AllNegara>, t: Throwable) {
                progressBar?.visibility = View.GONE
                errorLoading(this@MainActivity)
            }
        })
    }

    private fun itemClicked(negara: Negara) {
        val moveWitData= Intent(this@MainActivity,ChartCountryActivity::class.java)
        moveWitData.putExtra(ChartCountryActivity.EXTRA_COUNTRY,negara)
        startActivity(moveWitData)
    }

    private fun errorLoading(context: MainActivity) {
        val builder= AlertDialog.Builder(context)
        with(builder){
            setTitle("Network server!")
            setCancelable(false)
            setPositiveButton("Refresh"){ _,_->
                super.onRestart()
                val refresh = Intent(this@MainActivity,MainActivity::class.java)
                startActivity(refresh)
                finish()
            }
            setNegativeButton("EXIT"){_,_->
                finish()
            }
            create()
            show()
        }
    }
}