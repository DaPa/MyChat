package com.example.ui

import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBLabel
import com.intellij.icons.AllIcons
import com.intellij.ide.setToolTipText
import com.intellij.openapi.util.text.HtmlChunk
import javax.swing.JButton
import com.intellij.util.ui.JBUI
import com.intellij.ui.jcef.JBCefBrowser
//import org.cef.browser.CefBrowser
//import org.cef.handler.CefFindHandlerAdapter
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
//import java.awt.Rectangle
import javax.swing.Box
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.SwingConstants

/**
 * Inline Find bar for JBCefBrowser.
 *
 * Supports:
 * - Live search
 * - Next / previous navigation
 * - Closing / clearing highlights
 */
class BrowserFindBar(
    private val browser: JBCefBrowser
) : JBPanel<BrowserFindBar>(BorderLayout()) {

    private val textField = JBTextField()
    private val matchLabel = JBLabel()  // "0/0"
    // CRITICAL: CefFindHandler/CefFindHandlerAdapter and addFindHandler/onFindResult are NOT available
    //  in the current JCEF version used by this project. Do NOT attempt to implement real-time
    //  match counts (e.g., "1/5") until the JCEF version is upgraded to one that supports these APIs.
    private val matchCaseCheckBox = JCheckBox("Aa")
    private var lastSearchText: String? = null

    init {

        // Note: Real-time match counting is disabled due to JCEF API limitations (no CefFindHandler).
        /*
        browser.jbCefClient.addFindHandler(object : CefFindHandlerAdapter() {
            override fun onFindResult(
                browser: CefBrowser?,
                identifier: Int,
                count: Int,
                selectionRect: Rectangle?,
                activeMatchOrdinal: Int,
                finalUpdate: Boolean
            ) {
                updateMatchLabel(activeMatchOrdinal, count)
            }
        }, browser.cefBrowser)
        */

        border = JBUI.Borders.empty(4)

        matchLabel.preferredSize = Dimension(45, matchLabel.preferredSize.height)
        matchLabel.horizontalAlignment = SwingConstants.RIGHT

        val searchPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            add(JBLabel("Find:"))

            textField.columns = 20
            add(textField)
            add(Box.createHorizontalStrut(2))
            add(matchLabel)
            matchCaseCheckBox.setToolTipText(HtmlChunk.text("Match Case"))
            add(matchCaseCheckBox)

            // for all icons see https://intellij-icons.jetbrains.design/
            val prevBtn = JButton(AllIcons.General.ChevronUp) // see action id="MyChat.FindInBrowserPrevious"
            prevBtn.setToolTipText(HtmlChunk.text("Find previous match"))
            val nextBtn = JButton(AllIcons.General.ChevronDown)
            nextBtn.setToolTipText(HtmlChunk.text("Find Next match"))
            val closeBtn = JButton(AllIcons.Actions.Close)
            closeBtn.setToolTipText(HtmlChunk.text("Close find"))
            prevBtn.isFocusable = false
            nextBtn.isFocusable = false
            closeBtn.isFocusable = false
            prevBtn.border = null
            nextBtn.border = null
            closeBtn.border = null

            fun configureIconButton(button: JButton) {
                val size = Dimension(24, 24)

                button.preferredSize = size
                button.minimumSize = size
                button.maximumSize = size

                button.margin = JBUI.emptyInsets()

                button.isFocusable = false
                button.isContentAreaFilled = false
            }
            configureIconButton(prevBtn)
            configureIconButton(nextBtn)
            configureIconButton(closeBtn)

            add(Box.createHorizontalStrut(4))
            add(prevBtn)
            add(nextBtn)
            add(Box.createHorizontalStrut(4))
            add(closeBtn)

            // --- Button logic ---
            prevBtn.addActionListener {
                performFind(forward = false, findNext = true)
            }

            nextBtn.addActionListener {
                performFind(forward = true, findNext = true)
            }

            closeBtn.addActionListener {
                browser.cefBrowser.stopFinding(true)
                //textField.text = ""
                matchLabel.text = ""
                this@BrowserFindBar.isVisible = false
            }
        }

        add(searchPanel, BorderLayout.WEST)

        // --- Live search while typing ---
        textField.addActionListener {
            performFind(forward = true, findNext = false)
        }

        textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = onChange()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = onChange()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = onChange()

            private fun onChange() {
                val text = textField.text
                if (text.isNotEmpty()) {
                    performFind(forward = true, findNext = false)
                }
            }
        })

        textField.registerKeyboardAction(
            {
                browser.cefBrowser.stopFinding(true)
                isVisible = false
            },
            KeyStroke.getKeyStroke("ESCAPE"),
            WHEN_FOCUSED
        )

        matchCaseCheckBox.addActionListener {
            val text = textField.text
            if (text.isNotBlank()) {
                performFind(forward = true, findNext = false) // start a fresh search
            }
        }

        isVisible = false
    }

    /**
     * Executes find in the browser.
     */
    fun performFind(forward: Boolean, findNext: Boolean) {
        val text = textField.text
        lastSearchText = text
        if (text.isBlank()) return

        browser.cefBrowser.find(
            text,
            forward,
            matchCaseCheckBox.isSelected,
            findNext
        )
        textField.requestFocus()
    }

    /**
     * Opens the find bar and focuses input.
     */
    fun open() {
        if (isVisible) {
            isVisible = false
            return
        }
        textField.text = lastSearchText
        isVisible = true
        textField.requestFocus()
        textField.selectAll()
    }


    // This method currently remains unused due to JCEF API limitations (no CefFindHandler).
    private fun updateMatchLabel(
        activeMatchOrdinal: Int,
        count: Int
    ) {
        matchLabel.text =
            if (count <= 0) ""
            else "$activeMatchOrdinal/$count"
    }

}
