/*
 * Copyright 2020 The Android Open Source Project
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

package com.example.jetnews.ui.interests

import androidx.compose.foundation.Box
import androidx.compose.foundation.Icon
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.DrawerValue
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberDrawerState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.savedinstancestate.savedInstanceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.viewModel
import androidx.ui.tooling.preview.Preview
import com.example.jetnews.R
import com.example.jetnews.data.Result
import com.example.jetnews.data.interests.InterestsRepository
import com.example.jetnews.data.interests.TopicSelection
import com.example.jetnews.data.interests.impl.FakeInterestsRepository
import com.example.jetnews.ui.AppDrawer
import com.example.jetnews.ui.Screen
import com.example.jetnews.ui.ThemedPreview
import com.example.jetnews.ui.state.UiState
import kotlinx.coroutines.runBlocking

enum class Sections(val title: String) {
    Topics("Topics"),
    People("People"),
    Publications("Publications")
}

/**
 * TabContent for a single tab of the screen.
 *
 * This is intended to encapsulate a tab & it's content as a single object. It was added to avoid
 * passing several parameters per-tab from the stateful composable to the composable that displays
 * the current tab.
 *
 * @param section the tab that this content is for
 * @param section content of the tab, a composable that describes the content
 */
class TabContent(val section: Sections, val content: @Composable () -> Unit)

/**
 * Stateful InterestsScreen uses a [InterestsViewModel] to manage state.
 *
 * @param navigateTo (event) request navigation to [Screen]
 * @param scaffoldState (state) state for screen Scaffold
 * @param interestsRepository data source for [InterestsViewModel]
 */
@Composable
fun InterestsScreen(
    navigateTo: (Screen) -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    interestsRepository: InterestsRepository
) {
    // viewModel() is scoped to the Application or Fragment Lifecycle that is displaying this
    // composable by default. Callers of this composable can modify this by providing a new scope
    // through [ViewModelStoreOwnerAmbient]. Navigation controller is expected to scope ViewModel in
    // this manner.
    val interestsViewModel = viewModel<InterestsViewModel>(
        factory = InterestsViewModelFactory(interestsRepository)
    )

    // Describe the screen sections here since each section needs 2 states and 1 event from the
    // ViewModel. Pass them to the stateless InterestsScreen using a tabSlot.
    val topicsSection = TabContent(Sections.Topics) {

        // observeAsState will read a [LiveData] in Compose
        val topics by interestsViewModel.topics.observeAsState(UiState())
        // collectAsState will read a [Flow] in Compose
        val selectedTopics by interestsViewModel.selectedTopics.collectAsState(setOf())
        val data = topics.data ?: return@TabContent
        TopicList(data, selectedTopics, interestsViewModel::onTopicSelect)
    }

    val peopleSection = TabContent(Sections.People) {
        val people by interestsViewModel.people.observeAsState(UiState())
        val selectedPeople by interestsViewModel.selectedPeople.collectAsState(setOf())
        val data = people.data ?: return@TabContent
        PeopleList(data, selectedPeople, interestsViewModel::onPersonSelect)
    }

    val publicationSection = TabContent(Sections.Publications) {
        val publications by interestsViewModel.publications.observeAsState(UiState())
        val selectedPublications by interestsViewModel.selectedPublications.collectAsState(setOf())
        val data = publications.data ?: return@TabContent
        PublicationList(data, selectedPublications, interestsViewModel::onPublicationSelect)
    }

    InterestsScreen(
        tabContent = listOf(topicsSection, peopleSection, publicationSection),
        navigateTo = navigateTo,
        scaffoldState = scaffoldState
    )
}

/**
 * Stateless interest screen displays the tabs specified in [tabContent]
 *
 * @param tabContent (slot) the tabs and their content to display on this screen, must be a non-empty
 * list, tabs are displayed in the order specified by this list
 * @param navigateTo (event) request navigation to [Screen]
 * @param scaffoldState (state) the state for the screen's [Scaffold]
 */
@Composable
fun InterestsScreen(
    tabContent: List<TabContent>,
    navigateTo: (Screen) -> Unit,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
) {
    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            AppDrawer(
                currentScreen = Screen.Interests,
                closeDrawer = { scaffoldState.drawerState.close() },
                navigateTo = navigateTo
            )
        },
        topBar = {
            TopAppBar(
                title = { Text("Interests") },
                navigationIcon = {
                    IconButton(onClick = { scaffoldState.drawerState.open() }) {
                        Icon(vectorResource(R.drawable.ic_jetnews_logo))
                    }
                }
            )
        },
        bodyContent = {
            val (currentSection, updateSection) = savedInstanceState { tabContent.first().section }
            TabContent(currentSection, updateSection, tabContent)
        }
    )
}

/**
 * Displays a tab row with [currentSection] selected and the body of the corresponding [tabContent].
 *
 * @param currentSection (state) the tab that is currently selected
 * @param updateSection (event) request a change in tab selection
 * @param tabContent (slot) tabs and their content to display, must be a non-empty list, tabs are
 * displayed in the order of this list
 */
@Composable
private fun TabContent(
    currentSection: Sections,
    updateSection: (Sections) -> Unit,
    tabContent: List<TabContent>
) {
    val selectedTabIndex = tabContent.indexOfFirst { it.section == currentSection }
    Column {
        TabRow(
            selectedTabIndex = selectedTabIndex
        ) {
            tabContent.forEachIndexed { index, tabContent ->
                Tab(
                    text = { Text(tabContent.section.title) },
                    selected = selectedTabIndex == index,
                    onClick = { updateSection(tabContent.section) }
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            // display the current tab content which is a @Composable () -> Unit
            tabContent[selectedTabIndex].content()
        }
    }
}

/**
 * Display the list for the topic tab
 *
 * @param topics (state) topics to display, mapped by section
 * @param selectedTopics (state) currently selected topics
 * @param onTopicSelect (event) request a topic selection be changed
 */
@Composable
private fun TopicList(
    topics: Map<String, List<String>>,
    selectedTopics: Set<TopicSelection>,
    onTopicSelect: (TopicSelection) -> Unit
) {
    TabWithSections(topics, selectedTopics, onTopicSelect)
}

/**
 * Display the list for people tab
 *
 * @param people (state) people to display
 * @param selectedPeople (state) currently selected people
 * @param onPersonSelect (event) request a person selection be changed
 */
@Composable
private fun PeopleList(
    people: List<String>,
    selectedPeople: Set<String>,
    onPersonSelect: (String) -> Unit
) {
    TabWithTopics(people, selectedPeople, onPersonSelect)
}

/**
 * Display a list for publications tab
 *
 * @param publications (state) publications to display
 * @param selectedPublications (state) currently selected publications
 * @param onPublicationSelect (event) request a publication selection be changed
 */
@Composable
private fun PublicationList(
    publications: List<String>,
    selectedPublications: Set<String>,
    onPublicationSelect: (String) -> Unit
) {
    TabWithTopics(publications, selectedPublications, onPublicationSelect)
}

/**
 * Display a simple list of topics
 *
 * @param topics (state) topics to display
 * @param selectedTopics (state) currently selected topics
 * @param onTopicSelect (event) request a topic selection be changed
 */
@Composable
private fun TabWithTopics(
    topics: List<String>,
    selectedTopics: Set<String>,
    onTopicSelect: (String) -> Unit
) {
    ScrollableColumn(modifier = Modifier.padding(top = 16.dp)) {
        topics.forEach { topic ->
            TopicItem(
                topic,
                selected = selectedTopics.contains(topic)
            ) { onTopicSelect(topic) }
            TopicDivider()
        }
    }
}

/**
 * Display a sectioned list of topics
 *
 * @param sections (state) topics to display, grouped by sections
 * @param selectedTopics (state) currently selected topics
 * @param onTopicSelect (event) request a topic+section selection be changed
 */
@Composable
private fun TabWithSections(
    sections: Map<String, List<String>>,
    selectedTopics: Set<TopicSelection>,
    onTopicSelect: (TopicSelection) -> Unit
) {
    ScrollableColumn {
        sections.forEach { (section, topics) ->
            Text(
                text = section,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.subtitle1
            )
            topics.forEach { topic ->
                TopicItem(
                    itemTitle = topic,
                    selected = selectedTopics.contains(TopicSelection(section, topic))
                ) { onTopicSelect(TopicSelection(section, topic)) }
                TopicDivider()
            }
        }
    }
}

/**
 * Display a full-width topic item
 *
 * @param itemTitle (state) topic title
 * @param selected (state) is topic currently selected
 * @param onToggle (event) toggle selection for topic
 */
@Composable
private fun TopicItem(itemTitle: String, selected: Boolean, onToggle: () -> Unit) {
    val image = imageResource(R.drawable.placeholder_1_1)
    Row(
        modifier = Modifier
            .toggleable(
                value = selected,
                onValueChange = { onToggle() }
            )
            .padding(horizontal = 16.dp)
    ) {
        Image(
            image,
            Modifier
                .gravity(Alignment.CenterVertically)
                .preferredSize(56.dp, 56.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Text(
            text = itemTitle,
            modifier = Modifier
                .weight(1f)
                .gravity(Alignment.CenterVertically)
                .padding(16.dp),
            style = MaterialTheme.typography.subtitle1
        )
        SelectTopicButton(
            modifier = Modifier.gravity(Alignment.CenterVertically),
            selected = selected
        )
    }
}

/**
 * Full-width divider for topics
 */
@Composable
private fun TopicDivider() {
    Divider(
        modifier = Modifier.padding(start = 72.dp, top = 8.dp, bottom = 8.dp),
        color = MaterialTheme.colors.surface.copy(alpha = 0.08f)
    )
}

@Preview("Interests screen")
@Composable
fun PreviewInterestsScreen() {
    ThemedPreview {
        InterestsScreen(
            navigateTo = {},
            interestsRepository = FakeInterestsRepository()
        )
    }
}

@Preview("Interests screen dark theme")
@Composable
fun PreviewInterestsScreenDark() {
    ThemedPreview(darkTheme = true) {
        val scaffoldState = rememberScaffoldState(
            drawerState = rememberDrawerState(DrawerValue.Open)
        )
        InterestsScreen(
            navigateTo = {},
            scaffoldState = scaffoldState,
            interestsRepository = FakeInterestsRepository()
        )
    }
}

@Preview("Interests screen drawer open")
@Composable
private fun PreviewDrawerOpen() {
    ThemedPreview {
        val scaffoldState = rememberScaffoldState(
            drawerState = rememberDrawerState(DrawerValue.Open)
        )
        InterestsScreen(
            navigateTo = {},
            scaffoldState = scaffoldState,
            interestsRepository = FakeInterestsRepository()
        )
    }
}

@Preview("Interests screen drawer open dark theme")
@Composable
private fun PreviewDrawerOpenDark() {
    ThemedPreview(darkTheme = true) {
        val scaffoldState = rememberScaffoldState(
            drawerState = rememberDrawerState(DrawerValue.Open)
        )
        InterestsScreen(
            navigateTo = {},
            scaffoldState = scaffoldState,
            interestsRepository = FakeInterestsRepository()
        )
    }
}

@Preview("Interests screen topics tab")
@Composable
fun PreviewTopicsTab() {
    ThemedPreview {
        TopicList(loadFakeTopics(), setOf(), {})
    }
}

@Preview("Interests screen topics tab dark theme")
@Composable
fun PreviewTopicsTabDark() {
    ThemedPreview(darkTheme = true) {
        TopicList(loadFakeTopics(), setOf(), {})
    }
}

@Composable
private fun loadFakeTopics(): Map<String, List<String>> {
    val topics = runBlocking {
        FakeInterestsRepository().getTopics()
    }
    return (topics as Result.Success).data
}

@Preview("Interests screen people tab")
@Composable
fun PreviewPeopleTab() {
    ThemedPreview {
        PeopleList(loadFakePeople(), setOf(), { })
    }
}

@Preview("Interests screen people tab dark theme")
@Composable
fun PreviewPeopleTabDark() {
    ThemedPreview(darkTheme = true) {
        PeopleList(loadFakePeople(), setOf(), { })
    }
}

@Composable
private fun loadFakePeople(): List<String> {
    val people = runBlocking {
        FakeInterestsRepository().getPeople()
    }
    return (people as Result.Success).data
}

@Preview("Interests screen publications tab")
@Composable
fun PreviewPublicationsTab() {
    ThemedPreview {
        PublicationList(loadFakePublications(), setOf(), { })
    }
}

@Preview("Interests screen publications tab dark theme")
@Composable
fun PreviewPublicationsTabDark() {
    ThemedPreview(darkTheme = true) {
        PublicationList(loadFakePublications(), setOf(), { })
    }
}

@Composable
private fun loadFakePublications(): List<String> {
    val publications = runBlocking {
        FakeInterestsRepository().getPublications()
    }
    return (publications as Result.Success).data
}

@Preview("Interests screen tab with topics")
@Composable
fun PreviewTabWithTopics() {
    ThemedPreview {
        TabWithTopics(topics = listOf("Hello", "Compose"), selectedTopics = setOf()) {}
    }
}

@Preview("Interests screen tab with topics dark theme")
@Composable
fun PreviewTabWithTopicsDark() {
    ThemedPreview(darkTheme = true) {
        TabWithTopics(topics = listOf("Hello", "Compose"), selectedTopics = setOf()) {}
    }
}
