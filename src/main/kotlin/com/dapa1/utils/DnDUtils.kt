package com.dapa1.utils

import com.intellij.ide.dnd.DnDEvent
import com.intellij.ide.dnd.DnDNativeTarget
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTargetDragEvent
import java.io.File

// Drag & drop utilities
private val LOG = Logger.getInstance("com.dapa1.utils.DnDUtils")

/**
 * Accepts a drag if the transferable carries a file list; rejects it otherwise.
 *
 * dragEnter and dragOver require identical handling for file drops, so this
 * extension eliminates the duplicated if/else at every call site.
 */
fun DropTargetDragEvent.acceptOrRejectFileDrop() {
    if (isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        acceptDrag(DnDConstants.ACTION_COPY)
    } else {
        rejectDrag()
    }
}

/**
 * Extracts VirtualFiles from an IntelliJ [DnDEvent].
 *
 * IntelliJ's DnD system attaches different object types depending on the drag source:
 *
 *  - Project tree drag   → TransferableWrapper (reflection) or VirtualFile / VirtualFile[]
 *  - OS file manager     → DnDNativeTarget.EventInfo carrying a javaFileListFlavor transferable
 *
 * The function tries each case in order and returns the first non-empty result.
 * All reflection is guarded so an unexpected structure produces an empty list rather
 * than a crash.
 *
 * Extracted from MyChatWindow so it can be independently tested and reused by
 * any other IntelliJ DnD target in the plugin.
 */
fun extractVirtualFiles(event: DnDEvent): List<VirtualFile> {
    LOG.info("extractVirtualFiles: attachedObject type=${event.attachedObject?.javaClass?.name}")
    val attached = event.attachedObject ?: return emptyList()

    // ── 1. Direct VirtualFile ────────────────────────────────────────────────
    if (attached is VirtualFile) return listOf(attached)

    // ── 2. Array of VirtualFiles ─────────────────────────────────────────────
    if (attached is Array<*> && attached.isArrayOf<VirtualFile>()) {
        @Suppress("UNCHECKED_CAST")
        return (attached as Array<VirtualFile>).toList()
    }

    // ── 3. TransferableWrapper (Project tree drag) ───────────────────────────
    // Use reflection to avoid a compile-time dependency on the internal interface.
    try {
        val twClass = Class.forName("com.intellij.ide.dnd.TransferableWrapper")
        if (!twClass.isInstance(attached)) throw NoSuchMethodException("Not a TransferableWrapper")

        val getPsiElementsMethod = twClass.getMethod("getPsiElements")
        getPsiElementsMethod.isAccessible = true
        val psiElements = getPsiElementsMethod.invoke(attached)

        if (psiElements is Array<*> && psiElements.isNotEmpty()) {
            val result = mutableListOf<VirtualFile>()
            val psiElementClass = Class.forName("com.intellij.psi.PsiElement")
            val psiFileClass = Class.forName("com.intellij.psi.PsiFile")
            val psiFileSystemItemClass = Class.forName("com.intellij.psi.PsiFileSystemItem")

            for (element in psiElements) {
                if (element == null) continue

                // PsiElement → getContainingFile() → getVirtualFile()
                try {
                    val containingFile = psiElementClass.getMethod("getContainingFile").invoke(element)
                    if (containingFile != null) {
                        val vf = psiFileClass.getMethod("getVirtualFile").invoke(containingFile) as? VirtualFile
                        if (vf != null) { result.add(vf); continue }
                    }
                } catch (_: Exception) { }

                // PsiDirectory / PsiFileSystemItem → getVirtualFile()
                try {
                    if (psiFileSystemItemClass.isInstance(element)) {
                        val vf = psiFileSystemItemClass.getMethod("getVirtualFile").invoke(element) as? VirtualFile
                        if (vf != null) { result.add(vf); continue }
                    }
                } catch (_: Exception) { }
            }

            LOG.info("TransferableWrapper: extracted ${result.size} VirtualFile(s) from ${psiElements.size} PsiElement(s)")
            if (result.isNotEmpty()) return result
        }
    } catch (_: NoSuchMethodException) {
        // Not a TransferableWrapper — fall through to next strategy
    } catch (e: Exception) {
        LOG.warn("TransferableWrapper extraction error: ${e.message}")
    }

    // ── 4. OS file manager drag via DnDNativeTarget.EventInfo ────────────────
    if (attached is DnDNativeTarget.EventInfo) {
        val transferable = attached.transferable
        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            @Suppress("UNCHECKED_CAST")
            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
            val lfs = LocalFileSystem.getInstance()
            return files?.mapNotNull { lfs.findFileByIoFile(it) } ?: emptyList()
        }
    }

    return emptyList()
}
