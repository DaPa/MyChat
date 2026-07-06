package com.example.settings

import com.intellij.icons.AllIcons
import com.intellij.ide.setToolTipText
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

/**
 * Settings UI for MyChat plugin: Settings → Other Settings → MyChat
 *
 * Displays a table with columns: Icon | Name | URL | Button
 *
 * Icon column behavior:
 *  - Renders the current icon (loaded from iconPath)
 *  - Click opens a file chooser to pick a new SVG/PNG/etc from disk
 *  - The chosen absolute path is stored in the model; the icon is reloaded immediately
 *
 * Persistence:
 *  - Only iconPath (String) is stored in MyChatSettings.xml — Icon objects are transient
 *  - apply() reads iconPath from the model and saves it to MyChatSettings
 *  - reset() reads iconPath from MyChatSettings and populates the model
 */
class MyChatConfigurable : Configurable {

    companion object {
        // Column indices — single source of truth to avoid off-by-one errors
        private const val COL_ICON = 0
        private const val COL_NAME = 1
        private const val COL_URL = 2
        private const val COL_BUTTON = 3

        // Icon column is narrow — just wide enough for a 16×16 icon + padding
        private const val ICON_COL_WIDTH = 32

        // Row height — give icons a little breathing room
        private const val ROW_HEIGHT = 24
    }

    /**
     * Table model with 4 columns: Icon (String path), Name, URL, Button.
     *
     * The Icon column stores a String (the iconPath), not an Icon object.
     * The custom renderer paints the actual icon; the editor opens a file chooser.
     *
     * getColumnClass() returning String for all columns avoids DefaultTableModel
     * rendering booleans as checkboxes or Icons as labels unexpectedly.
     */
    private val model = object : DefaultTableModel(
        arrayOf("Icon", "Name", "URL", "Button"), 0
    ) {
        // All columns are editable:
        //  - COL_ICON   → IconCellEditor opens a file chooser on single click
        //  - COL_NAME, COL_URL, COL_BUTTON → default inline text editor
        // ❗ `column != COL_ICON` was wrong — it accidentally disabled the icon editor
        //    entirely, so clicks on the icon column did nothing.
        override fun isCellEditable(row: Int, column: Int): Boolean = true

        override fun getColumnClass(columnIndex: Int): Class<*> = String::class.java
    }

    private val table = JBTable(model).apply {
        rowHeight = ROW_HEIGHT

        // Icon column: fixed narrow width, not resizable
        columnModel.getColumn(COL_ICON).apply {
            minWidth = ICON_COL_WIDTH
            maxWidth = ICON_COL_WIDTH
            preferredWidth = ICON_COL_WIDTH
            cellRenderer = IconCellRenderer()
            cellEditor = IconCellEditor()
        }
    }

    override fun getDisplayName(): String = "MyChat"

    // --- Toolbar: "Restore Defaults" button above the table ---

    private fun createDefaultsToolbar(): JComponent {
        val action = object : AnAction("Restore Defaults", "Restore default sites", AllIcons.General.Reset) {
            override fun actionPerformed(e: AnActionEvent) {
                restoreDefaults()
            }
        }
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("MyChatSettingsToolbar", DefaultActionGroup(action), true)
        toolbar.targetComponent = table
        return toolbar.component
    }

    // --- Configurable interface ---

    override fun createComponent(): JComponent {
        val tablePanel = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                // New row: no icon by default (empty path → generic icon shown by renderer)
                model.addRow(arrayOf("", "", "", ""))
            }
            .setRemoveAction {
                val row = table.selectedRow
                if (row >= 0) model.removeRow(row)
            }
            .createPanel()

        return JPanel(BorderLayout()).apply {
            add(createDefaultsToolbar(), BorderLayout.NORTH)
            add(tablePanel, BorderLayout.CENTER)
        }
    }

    /**
     * Returns true if the table contents differ from saved settings.
     * Compares all four fields including iconPath.
     */
    override fun isModified(): Boolean {
        val sites = MyChatSettings.getInstance().state.sites
        if (model.rowCount != sites.size) return true

        for (i in 0 until model.rowCount) {
            val site = sites[i]
            if (model.getValueAt(i, COL_ICON) as String != site.iconPath) return true
            if (model.getValueAt(i, COL_NAME) as String != site.name) return true
            if (model.getValueAt(i, COL_URL) as String != site.url) return true
            if (model.getValueAt(i, COL_BUTTON) as String != site.button) return true
        }
        return false
    }

    /**
     * Saves table contents to MyChatSettings.
     * Icon is set to null — it will be reloaded lazily via resolveIcon() on next access.
     */
    override fun apply() {
        val state = MyChatSettings.getInstance().state
        val newList = mutableListOf<MyChatSettings.SiteConfig>()

        for (i in 0 until model.rowCount) {
            val name = model.getValueAt(i, COL_NAME) as String
            val url = model.getValueAt(i, COL_URL) as String
            val btn = model.getValueAt(i, COL_BUTTON) as String
            val path = model.getValueAt(i, COL_ICON) as String

            require(name.isNotBlank()) { "Name cannot be empty (row ${i + 1})" }
            require(url.isNotBlank()) { "URL cannot be empty (row ${i + 1})" }

            // icon = null: transient cache, will be re-resolved from iconPath on next resolveIcon()
            newList.add(MyChatSettings.SiteConfig(iconPath = path, name = name, url = url, button = btn))
        }

        state.sites = newList
    }

    /**
     * Loads settings into the table.
     * Populates iconPath (String) — the renderer will load the actual icon for display.
     */
    override fun reset() {
        model.rowCount = 0
        MyChatSettings.getInstance().state.sites.forEach { site ->
            model.addRow(arrayOf(site.iconPath, site.name, site.url, site.button))
        }
    }

    private fun restoreDefaults() {
        val result = Messages.showYesNoDialog(
            "Restore default site configuration?",
            "Confirm",
            Messages.getQuestionIcon()
        )
        if (result != Messages.YES) return

        model.rowCount = 0
        MyChatSettings.defaultSites().forEach { site ->
            model.addRow(arrayOf(site.iconPath, site.name, site.url, site.button))
        }
    }

    // --- Icon column: Renderer ---

    /**
     * Renders the icon column by loading the icon from the stored path (String).
     *
     * Shows:
     *  - The actual icon if the path resolves successfully
     *  - The generic fallback icon if the path is blank or loading fails
     *
     * Uses a JLabel (icon centered) as the rendered cell component.
     */
    private inner class IconCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean,
            row: Int, column: Int
        ): Component {
            super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column)
            val path = value as? String ?: ""
            icon = MyChatSettings.loadIcon(path) ?: MyChatIcons.generic
            text = null
            horizontalAlignment = CENTER
            setToolTipText(HtmlChunk.text(path.ifBlank { "Click to choose an icon" }))
            return this
        }
    }

    // --- Icon column: Editor ---

    /**
     * Custom cell editor for the icon column.
     * Clicking the cell immediately opens a file chooser (no double-click, no inline text field).
     *
     * Flow:
     *  1. Table calls getTableCellEditorComponent() to start editing
     *  2. We immediately open a file chooser dialog
     *  3. If the user picks a file: store the path, fire editingStopped
     *  4. If canceled: fire editingCancelled — cell reverts visually
     *
     * Extends AbstractCellEditor to satisfy TableCellEditor without implementing
     * all methods of CellEditor manually.
     */
    private class IconCellEditor : AbstractCellEditor(), TableCellEditor {

        // Currently selected icon path — returned by getCellEditorValue()
        private var currentPath: String = ""

        override fun getCellEditorValue(): Any = currentPath

        override fun getTableCellEditorComponent(
            table: JTable, value: Any?,
            isSelected: Boolean, row: Int, column: Int
        ): Component {
            currentPath = value as? String ?: ""

            // Why we use a plain Swing JFileChooser instead of IntelliJ's FileChooser API:
            //
            // IntelliJ's FileChooser.chooseFile() uses the OS-native file dialog on Windows.
            // When the native dialog closes, the IDE window regains focus, which triggers
            // DynamicPluginsFrameStateListener.applicationActivated() → VirtualFile.refresh().
            // That VFS refresh requires a write-safe context, but it fires inside the Settings
            // dialog modality which is write-unsafe — causing TransactionGuard to log:
            //   "Write-unsafe context! Model changes are allowed from write-safe contexts only."
            //
            // This is an IntelliJ platform bug (the listener fires in the wrong context).
            // Our workaround: use javax.swing.JFileChooser which is a pure Swing dialog.
            // It never triggers applicationActivated() because it doesn't leave the JVM process,
            // so the VFS refresh never fires, and the TransactionGuard error never occurs.
            //
            // Downside: no IDE bookmarks/recent files integration. Acceptable for an icon picker.
            //
            // AbstractCellEditor API:
            //   fireEditingStopped() — protected — commits the new value to the table model
            //   cancelCellEditing()  — public    — discards the edit, reverts the cell
            ApplicationManager.getApplication().invokeLater({
                val fileChooser = JFileChooser().apply {
                    dialogTitle = "Choose Icon"
                    fileSelectionMode = JFileChooser.FILES_ONLY
                    fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                        "Image files (SVG, PNG, GIF, ICO, JPG)",
                        "svg", "png", "gif", "ico", "jpg", "jpeg"
                    )
                }
                val result = fileChooser.showOpenDialog(table)
                if (result == JFileChooser.APPROVE_OPTION) {
                    currentPath = fileChooser.selectedFile.absolutePath
                    fireEditingStopped()
                } else {
                    cancelCellEditing()         // user canceled — revert cell
                }
            }, ModalityState.any())

            // Return a placeholder label while the dialog is opening.
            // The dialog appears almost immediately, so this is rarely visible.
            return JLabel(MyChatSettings.loadIcon(currentPath) ?: MyChatIcons.generic).apply {
                horizontalAlignment = SwingConstants.CENTER
            }
        }
    }

}  // class MyChatConfigurable
