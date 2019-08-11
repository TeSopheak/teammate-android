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

package com.mainstreetcode.teammate.model

import java.util.*

class TeamSearchRequest private constructor(name: String, screenName: String, sport: String?) {

    var name: String = name
        private set

    var screenName: String = screenName
        private set

    var sport: String? = sport
        private set

    fun query(query: String): TeamSearchRequest = TeamSearchRequest(
            if (query.startsWith("@")) "" else query,
            if (query.startsWith("@")) query.replaceFirst("@".toRegex(), "") else "",
            sport = this.sport
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TeamSearchRequest) return false
        val that = other as TeamSearchRequest?
        return (name == that!!.name
                && screenName == that.screenName
                && sport == that.sport)
    }

    override fun hashCode(): Int = Objects.hash(name, screenName, sport)

    companion object {

        fun from(sportCode: String?): TeamSearchRequest =
                TeamSearchRequest("", "", if(sportCode.isNullOrBlank()) null else sportCode)
    }
}
