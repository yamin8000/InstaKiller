package io.github.yamin8000.modules

import com.github.ajalt.mordant.rendering.BorderType
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.table
import com.github.instagram4j.instagram4j.IGClient
import com.github.instagram4j.instagram4j.models.direct.IGThread
import com.github.instagram4j.instagram4j.models.direct.item.ThreadTextItem
import io.github.yamin8000.console.ConsoleHelper.readMultipleStrings
import io.github.yamin8000.console.ConsoleHelper.readSingleString
import io.github.yamin8000.console.ConsoleHelper.pressEnterToContinue
import io.github.yamin8000.helpers.DirectMessageHelper
import io.github.yamin8000.helpers.LoggerHelper.loading
import io.github.yamin8000.helpers.LoggerHelper.progress
import io.github.yamin8000.helpers.UserHelper
import io.github.yamin8000.utils.Constants.askStyle
import io.github.yamin8000.utils.Constants.errorStyle
import io.github.yamin8000.utils.Constants.menuStyle
import io.github.yamin8000.utils.Constants.resultStyle
import io.github.yamin8000.utils.Constants.ter
import io.github.yamin8000.utils.Constants.warningStyle
import io.github.yamin8000.utils.Menus.directMessageMenu
import io.github.yamin8000.utils.Utility.solo

class DirectMessageModule(private val igClient: IGClient) : BaseModule(directMessageMenu) {

    private val helper: DirectMessageHelper by lazy(LazyThreadSafetyMode.NONE) {
        DirectMessageHelper(
            igClient
        )
    }

    override fun run(): Int {
        when (super.run()) {
            0 -> return 0
            1 -> showMenu()
            2 -> getDirectMessages()
            3 -> sendDirectMessageToUsers()
            else -> {
                ter.println(errorStyle("Invalid input, try again!"))
                run()
            }
        }

        run()
        return 0
    }

    private fun sendDirectMessageToUsers() {
        val usernames = readMultipleStrings("username")
        ter.println(TextColors.blue("Enter message you want to send:"))
        val message = readSingleString("message")
        val userHelper = UserHelper(igClient)
        usernames.forEach { username ->
            loading {
                userHelper.getPk(username).solo({
                    it()
                    sendSingleDirectMessage(message, it, username)
                }, { it() })
            }
        }
    }

    private fun sendSingleDirectMessage(message: String, pk: Long, username: String) {
        progress { stopLoading ->
            helper.sendDirectMessageByPks(message, pk).solo({ isSent ->
                stopLoading()
                if (isSent)
                    ter.println(resultStyle("Message successfully sent to ${askStyle(username)}"))
            }, { stopLoading() })
        }
    }

    private fun getDirectMessages() {
        loading { stopLoading ->
            helper.getInbox().solo({ inbox ->
                stopLoading()
                ter.println(resultStyle("Unread count: ${inbox.unseen_count}"))
                val threads = inbox.threads
                if (threads != null) {
                    if (threads.isNotEmpty()) printThreads(threads)
                    else ter.println(warningStyle("No direct messages found!"))
                } else ter.println(errorStyle("Failed to get threads!"))
            }, { stopLoading() })
        }
    }

    private fun printThreads(threads: List<IGThread>) {
        threads.forEach {
            printSingleThread(it)
            pressEnterToContinue("see next direct message")
        }
        ter.println(resultStyle("All direct messages shown!"))
    }

    private fun printSingleThread(thread: IGThread) {
        ter.println(
            table {
                borderStyle = TextColors.brightBlue
                borderType = BorderType.ROUNDED
                body {
                    style = menuStyle
                    row("Thread title (sender name or group name)", thread.thread_title)
                    row("User/Users (usernames)", thread.users.joinToString(", ") { user -> user.username })
                    val lastItem = thread.last_permanent_item
                    row("Item", if (lastItem is ThreadTextItem) lastItem.text else "Unknown")
                }
            }
        )
    }
}