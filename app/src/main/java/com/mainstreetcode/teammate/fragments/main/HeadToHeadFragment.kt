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
import android.text.SpannableStringBuilder
import android.view.View
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.adapters.HeadToHeadAdapterListener
import com.mainstreetcode.teammate.adapters.Shell
import com.mainstreetcode.teammate.adapters.gameAdapter
import com.mainstreetcode.teammate.adapters.headToHeadRequestAdapter
import com.mainstreetcode.teammate.adapters.viewholders.EmptyViewHolder
import com.mainstreetcode.teammate.baseclasses.TeammatesBaseFragment
import com.mainstreetcode.teammate.databinding.FragmentHeadToHeadBinding
import com.mainstreetcode.teammate.model.Competitive
import com.mainstreetcode.teammate.model.Competitor
import com.mainstreetcode.teammate.model.HeadToHead
import com.mainstreetcode.teammate.model.Team
import com.mainstreetcode.teammate.model.User
import com.mainstreetcode.teammate.util.ErrorHandler
import com.mainstreetcode.teammate.util.ExpandingToolbar
import com.mainstreetcode.teammate.util.ScrollManager
import com.tunjid.androidx.core.text.appendNewLine
import com.tunjid.androidx.core.text.bold
import com.tunjid.androidx.core.text.scale
import com.tunjid.androidx.recyclerview.diff.Differentiable

class HeadToHeadFragment : TeammatesBaseFragment(R.layout.fragment_head_to_head),
        Shell.UserAdapterListener,
        Shell.TeamAdapterListener,
        HeadToHeadAdapterListener {

    private var isHome = true
    private lateinit var request: HeadToHead.Request

    private var expandingToolbar: ExpandingToolbar? = null
    private var searchScrollManager: ScrollManager<*>? = null
    private var binding: FragmentHeadToHeadBinding? = null

    private lateinit var matchUps: List<Differentiable>

    override val showsFab: Boolean get() = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        request = HeadToHead.Request.empty()
        matchUps = gameViewModel.headToHeadMatchUps
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) = FragmentHeadToHeadBinding.bind(view).run {
        super.onViewCreated(view, savedInstanceState)
        defaultUi(
                toolbarShows = false,
                fabShows = showsFab
        )

        searchScrollManager = ScrollManager.with<RecyclerView.ViewHolder>(view.findViewById(R.id.search_options))
                .withAdapter(headToHeadRequestAdapter(request, this@HeadToHeadFragment))
                .withInconsistencyHandler(this@HeadToHeadFragment::onInconsistencyDetected)
                .withRecycledViewPool(inputRecycledViewPool())
                .withLinearLayoutManager()
                .build()

        scrollManager = ScrollManager.with<RecyclerView.ViewHolder>(view.findViewById(R.id.list_layout))
                .withPlaceholder(EmptyViewHolder(view, R.drawable.ic_head_to_head_24dp, R.string.game_head_to_head_prompt))
                .withAdapter(gameAdapter(::matchUps) { game -> navigator.push(GameFragment.newInstance(game)) })
                .withRefreshLayout(view.findViewById(R.id.refresh_layout)) { this@HeadToHeadFragment.fetchMatchUps() }
                .withInconsistencyHandler(this@HeadToHeadFragment::onInconsistencyDetected)
                .withLinearLayoutManager()
                .build()

        expandingToolbar = ExpandingToolbar.create(cardViewWrapper.cardViewWrapper) { this@HeadToHeadFragment.fetchMatchUps() }
        expandingToolbar?.setTitleIcon(false)
        expandingToolbar?.setTitle(R.string.game_head_to_head_params)

        scrollManager.notifyDataSetChanged()

        updateHeadToHead(0, 0, 0)
        if (!restoredFromBackStack) expandingToolbar?.changeVisibility(false)

        binding = this
    }

    override fun onDestroyView() {
        expandingToolbar = null
        searchScrollManager = null
        binding = null
        super.onDestroyView()
    }

    override fun onKeyBoardChanged(appeared: Boolean) {
        super.onKeyBoardChanged(appeared)
        if (!appeared && bottomSheetDriver.isBottomSheetShowing) bottomSheetDriver.hideBottomSheet()
    }

    override fun onUserClicked(item: User) = updateCompetitor(item)

    override fun onTeamClicked(item: Team) = updateCompetitor(item)

    override fun onHomeClicked(home: Competitor) {
        isHome = true
        findCompetitor()
    }

    override fun onAwayClicked(away: Competitor) {
        isHome = false
        findCompetitor()
    }

    private fun fetchMatchUps() {
        transientBarDriver.toggleProgress(true)
        disposables.add(gameViewModel.headToHead(request).subscribe({ summary -> updateHeadToHead(summary.wins, summary.draws, summary.losses) }, ErrorHandler.EMPTY::invoke))
        disposables.add(gameViewModel.getMatchUps(request).subscribe({ diffResult ->
            transientBarDriver.toggleProgress(false)
            scrollManager.onDiff(diffResult)
        }, defaultErrorHandler::invoke))
    }

    private fun updateHeadToHead(numWins: Int, numDraws: Int, numLosses: Int) = binding?.apply {
        wins.text = getText(R.string.game_wins, numWins)
        draws.text = getText(R.string.game_draws, numDraws)
        losses.text = getText(R.string.game_losses, numLosses)
    }

    private fun updateCompetitor(item: Competitive) {
        if (isHome) request.updateHome(item)
        else request.updateAway(item)
        searchScrollManager?.notifyDataSetChanged()
        bottomSheetDriver.hideBottomSheet()
        hideKeyboard()
    }

    private fun findCompetitor() =
            if (request.hasInvalidType()) transientBarDriver.showSnackBar(getString(R.string.game_select_tournament_type))
            else bottomSheetDriver.showBottomSheet(
                    requestCode = R.id.request_competitor_pick,
                    fragment = when {
                        User.COMPETITOR_TYPE == request.refPath -> UserSearchFragment.newInstance()
                        Team.COMPETITOR_TYPE == request.refPath -> TeamSearchFragment.newInstance(request.sport)
                        else -> null
                    }
            )

    private fun getText(@StringRes stringRes: Int, count: Int): CharSequence =
            SpannableStringBuilder()
                    .append(count.toString().scale(1.4f).bold())
                    .appendNewLine()
                    .append(getString(stringRes))

    companion object {

        fun newInstance(): HeadToHeadFragment = HeadToHeadFragment().apply {
            arguments = Bundle()
        }
    }
}
