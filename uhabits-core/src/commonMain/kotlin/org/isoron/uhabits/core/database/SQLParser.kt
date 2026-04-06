/*
 * Copyright (C) 2014 Markus Pfeiffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.isoron.uhabits.core.database

object SQLParser {
    private const val STATE_NONE = 0
    private const val STATE_STRING = 1
    private const val STATE_COMMENT = 2
    private const val STATE_COMMENT_BLOCK = 3

    fun parse(input: String): List<String> {
        val commands = mutableListOf<String>()
        val sb = StringBuilder()
        var state = STATE_NONE
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (state == STATE_COMMENT_BLOCK) {
                if (c == '*' && i + 1 < input.length && input[i + 1] == '/') {
                    state = STATE_NONE
                    i += 2
                    continue
                }
                i++
                continue
            } else if (state == STATE_COMMENT) {
                if (c == '\r' || c == '\n') {
                    state = STATE_NONE
                }
                i++
                continue
            } else if (state == STATE_NONE && c == '/' && i + 1 < input.length && input[i + 1] == '*') {
                state = STATE_COMMENT_BLOCK
                i += 2
                continue
            } else if (state == STATE_NONE && c == '-' && i + 1 < input.length && input[i + 1] == '-') {
                state = STATE_COMMENT
                i += 2
                continue
            } else if (state == STATE_NONE && c == ';') {
                val command = sb.toString().trim()
                if (command.isNotEmpty()) {
                    commands.add(command)
                }
                sb.clear()
                i++
                continue
            } else if (state == STATE_NONE && c == '\'') {
                state = STATE_STRING
            } else if (state == STATE_STRING && c == '\'') {
                state = STATE_NONE
            }
            if (state == STATE_NONE || state == STATE_STRING) {
                if (state == STATE_NONE && (c == '\r' || c == '\n' || c == '\t' || c == ' ')) {
                    if (sb.isNotEmpty() && sb[sb.length - 1] != ' ') {
                        sb.append(' ')
                    }
                } else {
                    sb.append(c)
                }
            }
            i++
        }
        val remaining = sb.toString().trim()
        if (remaining.isNotEmpty()) {
            commands.add(remaining)
        }
        return commands
    }
}
