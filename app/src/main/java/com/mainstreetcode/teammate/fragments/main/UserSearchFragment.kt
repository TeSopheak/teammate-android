/*
 * MIT License
 *
 * Copyright (c) 2019 Adetunji Dahunsi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.mainstreetcode.teammate.fragments.main

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.adapters.UserAdapter
import com.mainstreetcode.teammate.baseclasses.MainActivityFragment
import com.mainstreetcode.teammate.model.Team
import com.mainstreetcode.teammate.model.User
import com.mainstreetcode.teammate.util.InstantSearch
import com.mainstreetcode.teammate.util.ScrollManager
import com.tunjid.androidbootstrap.recyclerview.InteractiveViewHolder
import com.tunjid.androidbootstrap.recyclerview.diff.Differentiable

/**
 * Searches for users
 */

class UserSearchFragment : MainActivityFragment(), View.OnClickListener, SearchView.OnQueryTextListener, UserAdapter.AdapterListener {

    private var searchView: SearchView? = null
    private lateinit var instantSearch: InstantSearch<String, Differentiable>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instantSearch = userViewModel.instantSearch()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_user_search, container, false)
        searchView = root.findViewById(R.id.searchView)

        scrollManager = ScrollManager.with<InteractiveViewHolder<*>>(root.findViewById(R.id.list_layout))
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withAdapter(UserAdapter(instantSearch.currentItems, this))
                .withGridLayoutManager(2)
                .build()

        searchView?.setOnQueryTextListener(this)
        searchView?.setIconifiedByDefault(false)
        searchView?.isIconified = false

        if (targetRequestCode != 0) {
            val items = instantSearch.currentItems
            items.clear()
            items.apply {
                add(userViewModel.currentUser)
                addAll(teamViewModel.getModelList(Team::class.java).distinct())
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        subScribeToSearch()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchView?.clearFocus()
        searchView = null
    }

    override fun staticViews(): IntArray = EXCLUDED_VIEWS

    override fun showsFab(): Boolean = false

    override fun showsToolBar(): Boolean = false

    override fun onUserClicked(item: User) {
        val target = targetFragment
        val canPick = target is UserAdapter.AdapterListener

        if (canPick) (target as UserAdapter.AdapterListener).onUserClicked(item)
        else showFragment(UserEditFragment.newInstance(item))
    }

    override fun onQueryTextSubmit(s: String): Boolean = false

    override fun onQueryTextChange(queryText: String): Boolean {
        if (view == null || TextUtils.isEmpty(queryText)) return true
        instantSearch.postSearch(queryText)
        return true
    }

    private fun subScribeToSearch() {
        disposables.add(instantSearch.subscribe()
                .subscribe(scrollManager::onDiff, defaultErrorHandler::accept))
    }

    companion object {

        private val EXCLUDED_VIEWS = intArrayOf(R.id.list_layout)

        fun newInstance(): UserSearchFragment {
            val fragment = UserSearchFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
