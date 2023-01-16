package me.saket.cascade.internal

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.window.PopupPositionProvider

@Composable
internal fun PositionPopupContent(
  modifier: Modifier = Modifier,
  positionProvider: PopupPositionProvider,
  anchorBounds: ScreenRelativeBounds?,
  anchorView: View,
  content: @Composable () -> Unit
) {
  val popupView = LocalView.current
  val layoutDirection = LocalLayoutDirection.current

  var popupPosition: ScreenRelativeOffset? by remember { mutableStateOf(null) }
  val rectBuffer = remember { android.graphics.Rect(0, 0, 0, 0) }

  Box(modifier) {
    Box(
      Modifier
        .onGloballyPositioned { coordinates ->
          val anchorWindowSizeWithInsets = run {
            anchorView.getWindowVisibleDisplayFrame(rectBuffer)
            IntSize(width = rectBuffer.width(), height = rectBuffer.height())
          }
          val popupContentBounds = ScreenRelativeBounds(coordinates, owner = popupView)
          val contentSize = coordinates.size

          if (anchorBounds != null) {
            popupPosition = positionProvider.calculatePosition(
              anchorBounds = anchorBounds.boundsInRoot.round(),
              windowSize = anchorWindowSizeWithInsets,
              layoutDirection = layoutDirection,
              popupContentSize = contentSize,
            )
            .let { position ->
              // Material3's DropdownMenuPositionProvider was written to calculate
              // a position in the anchor's window. Cascade will have to adjust the
              // position to use it inside the popup's window.
              val positionInAnchorWindow = ScreenRelativeOffset(
                positionInRoot = position.toOffset(),
                rootOffsetFromScreen = anchorBounds.rootOffsetFromScreen
              )
              positionInAnchorWindow.alignedWithWindowOf(popupContentBounds)
            }
          }
        }
        .absoluteOffset {
          popupPosition?.positionInRoot?.round() ?: IntOffset.Zero
        }
        // Hide the popup until it can be positioned.
        .alpha(if (popupPosition != null) 1f else 0f)
    ) {
      content()
    }
  }
}

private fun Rect.round(): IntRect {
  return IntRect(topLeft = topLeft.round(), bottomRight = bottomRight.round())
}