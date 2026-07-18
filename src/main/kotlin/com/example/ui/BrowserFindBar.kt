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
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
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
) : JBPanel<BrowserFindBar>(BorderLayout()), Disposable {

    private val textField = JBTextField()
    private val matchLabel = JBLabel()
    private val matchCaseCheckBox = JCheckBox("Aa")
    private var lastSearchText: String? = null
    private val jsQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)

    init {
        Disposer.register(this, jsQuery)
        jsQuery.addHandler { countStr ->
            println("BrowserFindBar: received match count = $countStr")
            val count = countStr.toIntOrNull() ?: 0
            updateMatchLabel(0, count)
            null
        }

        border = JBUI.Borders.empty(4)

        matchLabel.horizontalAlignment = SwingConstants.LEFT

        val searchPanel = JPanel(GridBagLayout()).apply {
            val gbc = GridBagConstraints()
            gbc.insets = JBUI.insets(0, 3)
            gbc.anchor = GridBagConstraints.WEST
            gbc.fill = GridBagConstraints.NONE

            add(JBLabel("Find:"), gbc)

            textField.columns = 15
            add(textField, gbc)

            gbc.weightx = 1.0
            gbc.fill = GridBagConstraints.HORIZONTAL
            add(matchLabel, gbc)

            gbc.weightx = 0.0
            gbc.fill = GridBagConstraints.NONE
            matchCaseCheckBox.setToolTipText(HtmlChunk.text("Match Case"))
            add(matchCaseCheckBox, gbc)

            // for all icons see https://intellij-icons.jetbrains.design/
            val prevBtn = JButton(AllIcons.General.ChevronUp) // see action id="MyChat.FindInBrowserPrevious"
            prevBtn.setToolTipText(HtmlChunk.text("Find previous match"))
            val nextBtn = JButton(AllIcons.General.ChevronDown)
            nextBtn.setToolTipText(HtmlChunk.text("Find Next match"))
            val closeBtn = JButton(AllIcons.Actions.Close)
            closeBtn.setToolTipText(HtmlChunk.text("Close find"))
            
            fun configureIconButton(button: JButton) {
                val size = Dimension(24, 24)
                button.preferredSize = size
                button.minimumSize = size
                button.maximumSize = size
                button.margin = JBUI.emptyInsets()
                button.isFocusable = false
                button.isContentAreaFilled = false
                button.border = null
            }
            configureIconButton(prevBtn)
            configureIconButton(nextBtn)
            configureIconButton(closeBtn)

            add(prevBtn, gbc)
            add(nextBtn, gbc)
            add(closeBtn, gbc)

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

        add(searchPanel, BorderLayout.CENTER)

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

        textField.registerKeyboardAction(
            {
                browser.openDevtools()
            },
            KeyStroke.getKeyStroke("control shift I"),
            WHEN_FOCUSED
        )

        textField.registerKeyboardAction(
            {
                browser.openDevtools()
            },
            KeyStroke.getKeyStroke("meta shift I"),
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
        if (text.isBlank()) {
            lastSearchText = ""
            updateMatchLabel(0, 0)
            browser.cefBrowser.stopFinding(true)
            return
        }
        lastSearchText = text

        browser.cefBrowser.find(
            text,
            forward,
            matchCaseCheckBox.isSelected,
            findNext
        )

        // Alternative: Use JS to count matches since CefFindHandler is still missing in 2026.2
        val escapedText = text.replace("\\", "\\\\").replace("'", "\\'")
        val flags = if (matchCaseCheckBox.isSelected) "g" else "gi"
        val js = """
            (function() {
                var searchText = '$escapedText';
                var flags = '$flags';
                if (!searchText) return;
                try {
                    var pattern = searchText.replace(/[-\/\\^${'$'}*+?.()|[\]{}]/g, '\\${'$'}&');
                    var regex = new RegExp(pattern, flags);
                    var visited = new Set();
                    
                    function getDeepText(node) {
                        if (!node) return "";
                        if (node.nodeType === Node.TEXT_NODE) {
                            if (visited.has(node)) return "";
                            visited.add(node);
                            return node.textContent;
                        }
                        var name = node.nodeName;
                        if (name === 'SCRIPT' || name === 'STYLE' || name === 'NOSCRIPT' || name === 'TEMPLATE') return "";
                        
                        var t = "";
                        if (node.shadowRoot) {
                            var shadowChildren = node.shadowRoot.childNodes;
                            for (var i = 0; i < shadowChildren.length; i++) {
                                t += getDeepText(shadowChildren[i]);
                            }
                        }
                        if (node.childNodes) {
                            var children = node.childNodes;
                            for (var i = 0; i < children.length; i++) {
                                t += getDeepText(children[i]);
                            }
                        }
                        return t;
                    }
                    
                    var allText = getDeepText(document.body);
                    var matches = allText.match(regex);
                    var count = matches ? matches.length : 0;
                    console.log('Find: pattern="' + pattern + '" flags="' + flags + '" found ' + count + ' matches');
                    ${jsQuery.inject("count.toString()")}
                } catch (e) {
                    console.error('Find count error:', e);
                }
            })();
        """.trimIndent()
        browser.cefBrowser.executeJavaScript(js, "", 0)

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


    private fun updateMatchLabel(
        activeMatchOrdinal: Int,
        count: Int
    ) {
        matchLabel.text = when {
            textField.text.isBlank() -> ""
            activeMatchOrdinal > 0 -> "$activeMatchOrdinal/$count"
            else -> "Matches: $count"
        }
    }

    override fun dispose() {
        // JBCefJSQuery is disposed via Disposer.register in init
    }
}
