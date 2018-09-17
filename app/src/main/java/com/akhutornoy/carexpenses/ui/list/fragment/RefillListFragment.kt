package com.akhutornoy.carexpenses.ui.list.fragment

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.builders.DatePickerBuilder
import com.applandeo.materialcalendarview.listeners.OnSelectDateListener
import com.akhutornoy.carexpenses.R
import com.akhutornoy.carexpenses.base.*
import com.akhutornoy.carexpenses.ui.list.viewmodel.RefillListViewModel
import com.akhutornoy.carexpenses.ui.list.model.FilterDateRange
import com.akhutornoy.carexpenses.ui.list.model.FuelType
import com.akhutornoy.carexpenses.ui.list.model.RefillItem
import com.akhutornoy.carexpenses.ui.list.model.RefillResult
import com.akhutornoy.carexpenses.ui.list.recyclerview.RefillListAdapter
import com.akhutornoy.carexpenses.utils.DATE_FORMAT
import kotlinx.android.synthetic.main.fragment_refill_list.*
import kotlinx.android.synthetic.main.toolbar.*
import org.joda.time.LocalDate
import java.util.*


abstract class RefillListFragment : BaseDaggerFragment() {

    abstract val viewModel: RefillListViewModel

    private val fuelType: FuelType by lazy { FuelType.valueOf(arguments?.getString(FUEL_TYPE_ARG)!!) }

    private lateinit var navigationCallback: Navigation
    protected lateinit var toolbar: IToolbar

    protected open val addFabVisibility = View.VISIBLE
    protected open val fuelTypeVisibility = View.GONE

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is Navigation) {
            navigationCallback = context
        } else {
            IllegalArgumentException("Calling Activity='${context!!::class.java.simpleName}' should implement '${Navigation::class.java.simpleName}' interface")
        }
    }

    override fun fragmentLayoutId(): Int {
        return R.layout.fragment_refill_list
    }

    override fun getBaseViewModel(): BaseViewModel? {
        return viewModel
    }

    override fun getProgressBar(): View? = progress_bar

    override fun init() {
        initToolbar()
        add_fab.visibility = addFabVisibility
        fuel_type_text_vew.visibility = fuelTypeVisibility
        initListeners()
        loadFromDb()
    }

    protected open fun initToolbar() {
        toolbar = BaseToolbar(activity as BaseActivity)
        setHasOptionsMenu(true)
        toolbar.setToolbar(toolbar_view, false)
        toolbar.setToolbarTitle(R.string.refill_list_refills_title)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        activity?.menuInflater?.inflate(R.menu.menu_refill_list, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_filter -> onFilterClicked()
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onFilterClicked(): Boolean {
        DatePickerBuilder(activity, onSelectedDateListener())
                .pickerType(CalendarView.RANGE_PICKER)
                .date(Calendar.getInstance())
                .build()
                .show()
        return true
    }

    private fun onSelectedDateListener() = OnSelectDateListener { calendars ->
        if (calendars.size > 1) {
            val dateFrom = LocalDate(calendars[0].time)
            val dateTo = LocalDate(calendars[calendars.size - 1].time)
            val filterDateRange = FilterDateRange(dateFrom, dateTo)
            showFilterView(filterDateRange)

            loadFromDb(filterDateRange)
        }
    }

    private fun showFilterView(filterDateRange: FilterDateRange) {
        filter_view_group.visibility = View.VISIBLE
        val sFrom = filterDateRange.from.toString(DATE_FORMAT)
        val sTo = filterDateRange.to.toString(DATE_FORMAT)
        val range = "$sFrom - $sTo"
        filter_from_text_view.text = range
    }

    private fun initListeners() {
        add_fab.setOnClickListener { showAddNewItemScreen() }
        filter_clear_image_view.setOnClickListener { onClearFilterClicked() }
    }

    //TODO maybe should be replaced with Navigation pack library
    private fun showAddNewItemScreen() {
        navigationCallback.navigateToCreateNewRefill(fuelType)
    }

    private fun onClearFilterClicked() {
        filter_from_text_view.text = ""
        filter_view_group.visibility = View.GONE
        loadFromDb(FilterDateRange())
        Toast.makeText(activity, getString(R.string.refill_list_filter_cleared), Toast.LENGTH_LONG).show()
    }

    private fun loadFromDb() {
        val refills = viewModel.getRefills(fuelType)
        observeRefillsList(refills)
    }

    private fun loadFromDb(filterDateRange: FilterDateRange) {
        observeRefillsList(viewModel.getRefills(fuelType, filterDateRange))
    }

    private fun observeRefillsList(liveData: LiveData<RefillResult>) {
        liveData.observe(this,
                Observer { items -> showResult(items!!) })
    }

    private fun showResult(result: RefillResult) {
        //TODO investigate: why the method is called many times on LpgFragment.Done button clicked. Maybe because of observable.
        showList(result.refills)
        val summaryText = getString(R.string.refill_list_summary_text,
                result.summary.liters, result.summary.money)
        summary_results_text_view.text = summaryText
    }

    private fun showList(refills: List<RefillItem>) {
        val listener = object : RefillListAdapter.OnItemSelected<RefillItem> {
            override fun onItemSelected(item: RefillItem) {
                if (fuelType != FuelType.ALL) {
                    navigationCallback.navigateToEditRefill(fuelType, item.dbId)
                }
            }
        }

        val adapter = RefillListAdapter(refills, listener)
        adapter.fuelTypeVisibility = fuelTypeVisibility
        recycler_view.apply {
            layoutManager = LinearLayoutManager(activity)
            this.adapter = adapter
        }
    }

    protected companion object {
        const val FUEL_TYPE_ARG = "FUEL_TYPE_ARG"

        fun newInstance(refillFragment: RefillListFragment, fuelType: FuelType): BaseFragment {
            val args = Bundle()
            args.putString(FUEL_TYPE_ARG, fuelType.name)
            refillFragment.arguments = args
            return refillFragment
        }
    }

    interface Navigation {
        fun navigateToCreateNewRefill(fuelType: FuelType)
        fun navigateToEditRefill(fuelType: FuelType, refillId: Long)
    }
}