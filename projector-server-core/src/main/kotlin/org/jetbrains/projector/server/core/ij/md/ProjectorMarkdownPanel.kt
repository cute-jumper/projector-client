/*
 * MIT License
 *
 * Copyright (c) 2019-2021 JetBrains s.r.o.
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
package org.jetbrains.projector.server.core.ij.md

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import org.intellij.plugins.markdown.ui.preview.MarkdownHtmlPanel
import org.intellij.plugins.markdown.ui.preview.PreviewStaticServer
import org.intellij.plugins.markdown.ui.preview.jcef.MarkdownJCEFHtmlPanel
import org.jetbrains.projector.util.logging.Logger
import javax.swing.JComponent

/**
 * Accessed in transformed MdHtmlPanelProvider.
 * See [org.jetbrains.projector.agent.ijInjector.IjMdTransformer]
 */
@Suppress("unused")
public class ProjectorMarkdownPanel(private val isAgent: Boolean, agentDelegateClass: String) : MarkdownHtmlPanel {

  // we need to create instance even in headless mode as in new versions it starts server which hosts styles
  private val agentDelegate = Class.forName(agentDelegateClass).getDeclaredConstructor().newInstance() as MarkdownHtmlPanel
  private val clientDelegate = PanelDelegate(if (isAgent) agentDelegate.component else null)

  override fun dispose() {
    agentDelegate.dispose()
    clientDelegate.dispose()
  }

  override fun getComponent(): JComponent {
    return if (isAgent)
      agentDelegate.component
    else
      clientDelegate.getComponent()
  }

  @Suppress("unused") // deprecated and removed in 2020.3, but is used in previous versions
  public fun setHtml(html: String) {
    agentDelegate.setHtml(html)
    clientDelegate.setHtml(html)
  }

  @Suppress("unused") // deprecated and removed in 2021.1, but is used in previous versions
  public fun setCSS(inlineCss: String?, vararg fileUris: String?) {
    agentDelegate.setCSS(inlineCss, *fileUris)
    clientDelegate.setCSS(inlineCss, *fileUris)
  }

  @Suppress("unused") // deprecated and removed in 2021.1, but is used in previous versions
  public fun render() {
    agentDelegate.render()
    clientDelegate.render()
  }

  override fun setHtml(html: String, initialScrollOffset: Int) {
    agentDelegate.setHtml(html, initialScrollOffset)
    clientDelegate.setHtml(html, initialScrollOffset)

    if (isRenderMethodRemoved) {
      clientDelegate.setCSS(null, styleUrls)
      clientDelegate.render()
    }
  }

  override fun reloadWithOffset(offset: Int) {
    agentDelegate.reloadWithOffset(offset)
    // TODO implement for PanelDelegate
  }

  override fun scrollToMarkdownSrcOffset(offset: Int) {
    agentDelegate.scrollToMarkdownSrcOffset(offset)
    clientDelegate.scrollToMarkdownSrcOffset(offset)
  }

  override fun addScrollListener(listener: MarkdownHtmlPanel.ScrollListener?) {
    agentDelegate.addScrollListener(listener)
    // TODO implement for PanelDelegate
  }

  override fun removeScrollListener(listener: MarkdownHtmlPanel.ScrollListener?) {
    agentDelegate.removeScrollListener(listener)
    // TODO implement for PanelDelegate
  }

  private companion object {
    val logger = Logger<ProjectorMarkdownPanel>()

    private val styleUrls get() = MarkdownJCEFHtmlPanel.styles.map { PreviewStaticServer.getStaticUrl(it) }

    private val isRenderMethodRemoved by lazy {
      val buildNumber = BuildNumber.fromString("211.0")!!
      val appBuild = ApplicationInfo.getInstance().build
      appBuild >= buildNumber
    }

    private val setHtmlMethod by lazy {
      MarkdownHtmlPanel::class.java.getDeclaredMethod("setHtml", String::class.java)
    }

    private val setCSSMethod by lazy {
      MarkdownHtmlPanel::class.java.getDeclaredMethod("setCSS", String::class.java, Array<String>::class.java)
    }

    private val renderMethod by lazy {
      MarkdownHtmlPanel::class.java.getDeclaredMethod("render")
    }

    private fun MarkdownHtmlPanel.setHtml(html: String) {
      setHtmlMethod.invoke(this, html)
    }

    private fun MarkdownHtmlPanel.setCSS(inlineCss: String?, vararg fileUris: String?) {
      setCSSMethod.invoke(this, inlineCss, fileUris)
    }

    private fun MarkdownHtmlPanel.render() {
      renderMethod.invoke(this)
    }
  }
}
