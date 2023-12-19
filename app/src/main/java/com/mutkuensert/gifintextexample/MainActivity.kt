package com.mutkuensert.gifintextexample

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabPosition
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.Indicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.mutkuensert.gifintextexample.ui.theme.GifInTextExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GifInTextExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var text by remember { mutableStateOf("") }
                    var selectedTab by rememberSaveable { mutableStateOf(TextAndGifTab.RAW) }

                    Column {
                        Tabs(
                            tabs = TextAndGifTab.entries,
                            onTabSelected = { selectedTab = TextAndGifTab.valueOf(it) }
                        )

                        when (selectedTab) {
                            TextAndGifTab.PREVIEW -> {
                                TextWithGifRenderer(
                                    modifier = Modifier.fillMaxSize(),
                                    text = text
                                )
                            }

                            TextAndGifTab.RAW -> {
                                OutlinedTextField(
                                    modifier = Modifier.fillMaxSize(),
                                    value = text,
                                    onValueChange = { text = it })
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Tabs(
        modifier: Modifier = Modifier,
        tabs: List<TextAndGifTab>,
        onTabSelected: (String) -> Unit
    ) {
        var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

        TabRow(
            modifier = modifier,
            selectedTabIndex = selectedIndex,
            containerColor = Color.Transparent,
            contentColor = Color.Black,
            indicator = { tabPositions: List<TabPosition> ->
                Indicator(Modifier.tabIndicatorOffset(tabPositions[selectedIndex]))
            }) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    text = { Text(tab.name.uppercase()) },
                    selected = selectedIndex == index,
                    onClick = {
                        selectedIndex = index
                        onTabSelected(TextAndGifTab.valueOf(tab.name).name)
                    }
                )
            }

        }
    }

    @Composable
    private fun TextWithGifRenderer(modifier: Modifier = Modifier, text: String) {
        LazyColumn(modifier = modifier) {
            val regex = Regex("""!\[GIF\]\((.*?)\)""")

            val renderItems = regex.splitToGifsAndTexts(text)

            items(renderItems.size) { count ->
                when (val renderItem = renderItems[count]) {
                    is RenderItem.Text -> Text(text = renderItem.input)
                    is RenderItem.Gif -> {
                        val gifUrl = renderItem.input.substring(7, renderItem.input.lastIndex)
                        Image(
                            modifier = Modifier.height(100.dp),
                            painter = rememberGifPainter(url = gifUrl),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }

    private fun Regex.splitToGifsAndTexts(input: String): List<RenderItem> {
        val matches = findAll(input)

        val resultList = mutableListOf<RenderItem>()
        var lastIndex = 0

        for (match in matches) {
            val matchRange = match.range
            val matchText = match.value

            if (lastIndex < matchRange.first) {
                resultList.add(RenderItem.Text(input.substring(lastIndex, matchRange.first)))
            }

            resultList.add(RenderItem.Gif(matchText))

            lastIndex = matchRange.last + 1
        }

        if (lastIndex < input.length) {
            resultList.add(RenderItem.Text(input.substring(lastIndex)))
        }

        return resultList
    }

    private sealed interface RenderItem {
        class Gif(val input: String) : RenderItem
        class Text(val input: String) : RenderItem
    }

    private enum class TextAndGifTab {
        RAW, PREVIEW
    }

    @Preview
    @Composable
    private fun PreviewTextWithGifRenderer() {
        TextWithGifRenderer(
            text = "First gif: ![GIF](https://media.giphy.com/media/sJWNLTclcvVmw/giphy.gif?cid=82a1493bjtdunhufcaxp9f8rldtwvt3kv3qnhk12do1v7bmj&ep=v1_gifs_trending&rid=giphy.gif&ct=g) Second Gif: ![GIF](https://media.giphy.com/media/14uXQbPS73Y3qU/giphy.gif?cid=82a1493bjtdunhufcaxp9f8rldtwvt3kv3qnhk12do1v7bmj&ep=v1_gifs_trending&rid=giphy.gif&ct=g)"
        )
    }

    @Composable
    private fun rememberGifPainter(url: String): AsyncImagePainter {
        val context = LocalContext.current

        return rememberAsyncImagePainter(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(false)
                .build(),
            imageLoader = ImageLoader.Builder(context = context)
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        )
    }

    private fun Modifier.tabIndicatorOffset(
        currentTabPosition: TabPosition
    ): Modifier = composed(
        inspectorInfo = debugInspectorInfo {
            name = "tabIndicatorOffset"
            value = currentTabPosition
        }
    ) {
        val currentTabWidth by animateDpAsState(
            targetValue = currentTabPosition.width,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing), label = ""
        )
        val indicatorOffset by animateDpAsState(
            targetValue = currentTabPosition.left,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing), label = ""
        )
        fillMaxWidth()
            .wrapContentSize(Alignment.BottomStart)
            .offset(x = indicatorOffset)
            .width(currentTabWidth)
    }

    @Preview
    @Composable
    private fun PreviewRawPreview() {
        Tabs(tabs = TextAndGifTab.entries, onTabSelected = {})
    }
}