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
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import com.mainstreetcode.teammate.R
import com.mainstreetcode.teammate.adapters.JoinRequestAdapter
import com.mainstreetcode.teammate.adapters.viewholders.input.InputViewHolder
import com.mainstreetcode.teammate.baseclasses.HeaderedFragment
import com.mainstreetcode.teammate.model.JoinRequest
import com.mainstreetcode.teammate.model.Team
import com.mainstreetcode.teammate.model.User
import com.mainstreetcode.teammate.util.ScrollManager
import com.mainstreetcode.teammate.util.yes
import com.mainstreetcode.teammate.viewmodel.gofers.Gofer
import com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer
import com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.ACCEPTING
import com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.APPROVING
import com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.INVITING
import com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.JOINING
import com.mainstreetcode.teammate.viewmodel.gofers.JoinRequestGofer.WAITING
import com.mainstreetcode.teammate.viewmodel.gofers.TeamHostingGofer
import com.tunjid.androidbootstrap.view.util.InsetFlags

/**
 * Invites a Team member
 */

class JoinRequestFragment : HeaderedFragment<JoinRequest>(), JoinRequestAdapter.AdapterListener {

    override lateinit var headeredModel: JoinRequest
        private set

    private lateinit var gofer: JoinRequestGofer

    override val fabStringResource: Int
        @StringRes
        get() = gofer.fabTitle

    override val fabIconResource: Int
        @DrawableRes
        get() = R.drawable.ic_check_white_24dp

    override val toolbarMenu: Int
        get() = R.menu.fragment_user_edit

    override val toolbarTitle: CharSequence
        get() = gofer.getToolbarTitle(this)

    override fun getStableTag(): String =
            Gofer.tag(super.getStableTag(), arguments!!.getParcelable(ARG_JOIN_REQUEST))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headeredModel = arguments!!.getParcelable(ARG_JOIN_REQUEST)!!
        gofer = teamMemberViewModel.gofer(headeredModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_headered, container, false)

        scrollManager = ScrollManager.with<InputViewHolder<*>>(rootView.findViewById(R.id.model_list))
                .withRefreshLayout(rootView.findViewById(R.id.refresh_layout)) { this.refresh() }
                .withAdapter(JoinRequestAdapter(gofer.items, this))
                .addScrollListener { _, dy -> updateFabForScrollState(dy) }
                .withInconsistencyHandler(this::onInconsistencyDetected)
                .withRecycledViewPool(inputRecycledViewPool())
                .withLinearLayoutManager()
                .build()

        scrollManager.recyclerView.requestFocus()
        return rootView
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val isEmpty = headeredModel.isEmpty
        val canBlockUser = gofer.hasPrivilegedRole()
        val canDeleteRequest = canBlockUser || gofer.isRequestOwner

        val blockItem = menu.findItem(R.id.action_block)
        val deleteItem = menu.findItem(R.id.action_kick)

        blockItem?.isVisible = !isEmpty && canBlockUser
        deleteItem?.isVisible = !isEmpty && canDeleteRequest
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_kick -> showDeletePrompt().yes
            R.id.action_block -> blockUser(headeredModel.user, headeredModel.team).yes
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun insetFlags(): InsetFlags = NO_TOP

    override fun showsFab(): Boolean = gofer.showsFab()

    override fun onImageClick() {}

    override fun gofer(): TeamHostingGofer<JoinRequest> = gofer

    override fun onPrepComplete() {
        requireActivity().invalidateOptionsMenu()
        super.onPrepComplete()
    }

    override fun onModelUpdated(result: DiffUtil.DiffResult) {
        viewHolder.bind(headeredModel)
        scrollManager.onDiff(result)
        toggleProgress(false)
    }

    override fun canEditFields(): Boolean = gofer.canEditFields()

    override fun canEditRole(): Boolean = gofer.canEditRole()

    override fun onClick(view: View) {
        if (view.id != R.id.fab) return

        @JoinRequestGofer.JoinRequestState
        val state = gofer.state

        if (state == WAITING) return

        when {
            state == APPROVING || state == ACCEPTING -> saveRequest()
            headeredModel.position.isInvalid -> showSnackbar(getString(R.string.select_role))
            state == JOINING || state == INVITING -> createJoinRequest()
        }
    }

    private fun createJoinRequest() {
        toggleProgress(true)
        disposables.add(gofer.save().subscribe(this::onJoinRequestSent, defaultErrorHandler::accept))
    }

    private fun saveRequest() {
        toggleProgress(true)
        disposables.add(gofer.save().subscribe({ onRequestSaved() }, defaultErrorHandler::accept))
    }

    private fun deleteRequest() {
        toggleProgress(true)
        disposables.add(gofer.remove().subscribe(this::onRequestDeleted, defaultErrorHandler::accept))
    }

    private fun onJoinRequestSent(result: DiffUtil.DiffResult) {
        scrollManager.onDiff(result)
        hideBottomSheet()
        toggleProgress(false)
        togglePersistentUi()
        showSnackbar(getString(
                if (headeredModel.isTeamApproved) R.string.user_invite_sent
                else R.string.team_submitted_join_request))
    }

    private fun onRequestDeleted() {
        val name = headeredModel.user.firstName
        if (!gofer.isRequestOwner) showSnackbar(getString(R.string.removed_user, name))
        requireActivity().onBackPressed()
    }

    private fun onRequestSaved() {
        val name = headeredModel.user.firstName
        if (!gofer.isRequestOwner) showSnackbar(getString(R.string.added_user, name))
        requireActivity().onBackPressed()
    }

    private fun showDeletePrompt() {
        val requestUser = headeredModel.user
        val prompt =
                if (gofer.isRequestOwner) getString(R.string.confirm_request_leave, headeredModel.team.name)
                else getString(R.string.confirm_request_drop, requestUser.firstName)

        AlertDialog.Builder(requireActivity()).setTitle(prompt)
                .setPositiveButton(R.string.yes) { _, _ -> deleteRequest() }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                .show()
    }

    companion object {

        internal const val ARG_JOIN_REQUEST = "join-request"

        internal fun inviteInstance(team: Team): JoinRequestFragment {
            val fragment = newInstance(JoinRequest.invite(team))
            fragment.setEnterExitTransitions()

            return fragment
        }

        fun joinInstance(team: Team, user: User): JoinRequestFragment {
            val fragment = newInstance(JoinRequest.join(team, user))
            fragment.setEnterExitTransitions()

            return fragment
        }

        internal fun viewInstance(request: JoinRequest): JoinRequestFragment {
            val fragment = newInstance(request)
            fragment.setEnterExitTransitions()

            return fragment
        }

        private fun newInstance(joinRequest: JoinRequest): JoinRequestFragment {
            val fragment = JoinRequestFragment()
            val args = Bundle()

            args.putParcelable(ARG_JOIN_REQUEST, joinRequest)
            fragment.arguments = args
            fragment.setEnterExitTransitions()

            return fragment
        }
    }
}
