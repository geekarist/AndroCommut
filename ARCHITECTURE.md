## Architecture

### System

Here is a view of the interactions between the TNPR application and external components (web services, SDKs...):

![](README_assets/system.png)

For now, TNPR only uses one external component which is the Navitia web service.

Navitia provides two routes that TNPR uses:
1. Find places (`/places`). It is used by the [place suggestion feature](README.md#place-suggestion-feature).
2. Find journeys (`/journeys`). It is used by the [journey search feature](README.md#journey-search-feature).

### Layers

TNPR follows the MVVM architecture. Well, at leat it follows my interpretation of it. 

![](README_assets/layers.png)

In this diagram, you see:

- The user
- The app
  - The View: this is the UI code that is closely tied to the Android SDK and UI/widget library: `Activity`, `Fragment`, `Context`, `android.view.View`, XML layouts...
  - The ViewModel: as [the Presentation Model described by Martin Fowler](https://martinfowler.com/eaaDev/PresentationModel.html), this is "an abstract of the view that is not dependent on a specific GUI framework". 
    So theoretically, it could be reused in any app targeting any UI framework e.g. Android, iOS or the web using Kotlin Multiplatform, Android or the desktop using Compose...
  - The Model: this is where TNPR's "intelligence" resides.
    When the app has to process data, it does it by converting the data into internal representation that is not at all tied to the view (ViewModel → Model), does the processing (`processingModel = model(data)`), and then converts the data into a representation (`uiState.updatedWith(processingModel)`) that can be easily displayed by the View (Model → ViewModel).
    TNPR does not have a proper `model` package or classes, but the `model(data)` is defined as a pure function: it only converts data, applying computations in the process.
- The "cloud" (mainly Navitia web services)

To learn more about that, see [the MVVM experiment section](#mvvm-viewviewmodelmodel).

### Dependencies

The dependencies of TNPR can be represented in two ways: 
- Abstract, where each dependency is labeled in a generic way, independant of the framework: "standard library", "type-safe HTTP client"... 
    This exact representation of the dependencies could apply to the same application not targeting Android, but instead a web browser or a desktop OS.
- Concrete, where each dependency is labeled specifically as the implementation that is used in TNPR: "standard libary" → `kotlin.stdlib`, "type-safe HTTP client" → `com.squareup.retrofit2`. 

| Concrete                                 | Abstract                                 |
|------------------------------------------|------------------------------------------|
| ![](README_assets/deps-concrete.png) | ![](README_assets/deps-abstract.png) |

Each diagram is split into high-level packages: Application, Network, UI, Runtime environment and Language.

Read on to learn about these packages.

#### Application (`me.cpele`)

This package contains TNPR's source code (`tnpr` subpackage) and a custom framework (`afk` subpackage).

`afk` is a lightweight framework on which Android apps can be based. It also contains reusable classes and functions.

To learn more about AFK, see the [AFK custom framework experiment section](#afk-custom-framework)

#### Network

The network package is `com.squareup`, it contains the libraries provided by Square to manage HTTP connections: `retrofit2` and `okhttp3`.

These libraries have been chosen because they are well established and widely known.

#### UI

Some UI components come from [Material Components](https://material.io/components?platform=android).

Why? Material components are cross-platform, abundantly documented and commonly used.

## Language: `org.jetbrains`

The app depends on this package that provides the Kotlin standard library (`kotlin.stdlib`) and the Coroutines library (`kotlinx.coroutines`).

Coroutines are used to implement asynchronous operations like calls to web services.

Flows are used to implement asynchronous operations reperesented as streams, for example it allows to handle the user input in the Autosuggest screen.

## Runtime environment: `androidx`

Some AndroidX libraries bring Android related facilities (Jetpack), utilities that are provided as libraries.

The `core` subpackage is for extensions of the Android SDK, it brings e.g. `View.children` or `Context.getSystemService`.

`lifecycle` brings facilities to manage Android components that have a lifecycle like Activities or Fragments. This is the home of the `ViewModel` and `LiveData` classes. `LiveData` could be replaced by `Flow` but `LiveData` is still widely used and better suited to Android.

`appcompat` implements components that supersed those that come with the Android SDK, it has better backward and forward compatibiliy e.g. `ActivityCompat`.

`navigation` is the Jetpack component to implement navigation. It has drawbacks, mostly in the dependency relations it requires, but it was a simple way to design the app's navigation efficiently. It also provides editing tools that offer a global view of the app's UI, as you'll see [in the Features section](#features-diagram).

### Features

Thanks to Jetpack Navigation, here is a diagram showing all of the app's screens.

<img id="features-diagram" src="README_assets/features.png" width="800">

### Experiments

This app was a way for me to try a few things I could not try on my day job as and Android developer. In this section you'll find some of the experiments I tried.

#### Transmodel data model

Transmodel is the name of a reference data model to represent public transport data. I tried to use it as the internal data model of the application, but decided not to use it.

Transmodel is a complex model, its complexity comes from features which were not useful for the purpose of TNPR, notably:

- Separate concepts for the spatial and temporal aspects of transport data
- A representation for passengers, drivers and other types of "users" of public tranport modes

I choosed to stay with a projection of the Navitia data model. By "projection" I mean that I took the model as is, only keeping the resources and attributes I wanted TNPR to manage. Navitia's model is simpler and better suited for an application that is only targeted at passengers.

#### AFK custom framework

The `me.cpele.afk` is the first step of a custom lightweight framework for Android app development.

It contains classes that I used to rewrite or copy/paste each time I was developing a new Android application. So I thought it would be appropriate to code them once and reuse them later. NB: for now `me.cpele.afk` is part of TNPR, but in the future it will be distributed separately.

It contains:

- `Component`: this is an interface that can be implemented by the app's `ViewModel`s: 
  - It has a `dispatch(Action)` method which executes an `Action`. An `Action` could be e.g. `LoadData(id: Int)` to trigger a request to a web service and load data, or `SearchJourneys(origin: Place, destiation: Place)` to find a journey from a point A to a point B.
  - It has a `stateLive` property representing the "state" or "model" of a view, expressed in a way that should be independent of the UI framework.
  - It has an `eventLive` property that allows one-shot "events" or "effects" to come back to the UI without forcing the `Component` to retain a reference to the view. 

- Some utility classes or functions:
  - `ViewModelFactory`: a generic `ViewModelProvider.Factory` that is used to instantiate a `ViewModel` from an `Activity` or a `Fragment` like this:
    ```kotlin
    private val viewModel: AutosuggestViewModel by viewModels {
        ViewModelFactory {
            AutosuggestViewModel(
                CustomApp.instance.navitiaService
            )
        }
    }
    ```
  - `Event`: a data holder that can be "consumed". It is used by the `Component.eventLive` property to represent one-shot events that "bubble up" to the UI. One use case of this class is display a message only once using `Toast`.
  - `Outcome`: like a `kotlin.Result`, it represents an outcome that can be successful (`Outcome.success(obj)`) or a failure (`Outcome.failure(throwable)`). But unlike `Result`, `Outcome` can be returned by functions. It is used mainly to call web services.
  - `Dates.kt`: date parsing


#### Data `Flow`ing through functions

TODO (see AutosuggestViewModel)

#### Taking out effects

When starting this experiment, I had the idea of this function: `process(inputFlow: Flow<Input>): Flow<Output>`. It would be the heart of a `ViewModel` and would not depend on anything other than the `Flow` API, `Input` and `Output` data classes.

The `process()` top level function would have **no side effect**. For example instead of fetching data in a repository by calling `repository.findById(id)`, it would describe the "fetch" effect and let it be emitted to an output `Flow`: `Output.FetchData(id)`. 

The actual effects would be handled by methods in the `ViewModel`. These methods would be "normal" imperative code: it would have dependencies on the Android SDK, the data layer, web services, etc.

As an example, here is the code in `RoadmapViewModel.kt`. Keep in mind that `process` is a top level function, not a method of `RoadmapViewModel`.

```kotlin
private sealed class Input {
    object Default : Input()
    data class Start(val tripId: String) : Input()
    data class TripRecalled(val id: String, val journey: Journey?) : Input()
}

private sealed class Output {
    data class RecallTrip(val tripId: String) : Output()
    data class ChangeState(val journeyOutcome: Outcome<Journey>) : Output()
}

private fun process(inputFlow: Flow<Input>): Flow<Output> = merge(

    // Start ⇒ recall trip
    inputFlow.filterIsInstance<Input.Start>()
        .map {
            val tripId = it.tripId
            Output.RecallTrip(tripId)
        },

    // Trip recalled ⇒ change state
    inputFlow.filterIsInstance<Input.TripRecalled>()
        .map { recalled ->
            val trip = recalled.journey
            val tripId = recalled.id
            val outcome = if (trip == null) {
                Outcome.Failure(Exception("Trip not found: $tripId"))
            } else {
                Outcome.Success(trip)
            }
            Output.ChangeState(outcome)
        }
)
```

The idea behind this code is that all the intelligence ("decisions" or "logic") of the `ViewModel` is implemented in the `process` function. The `ViewModel`'s role is to send `Input`s to `process()` and then apply effects based on its `Output`s.

The beauty of this approach is that the `Input`s could be anything like geolocation updates, user steps (as in a podometer), microphone data, or any sensor input... The `Output`s could represent any effect like displaying information on the screen, playing audio, logging an error, or even triggering a robotic arm. 

Whatever device capabilities or SDKs the app is using, the `process` method would only manipulate kotlin `Flow`s, `Input`s or `Output`s. Is `process` a pure function? I don't think so, because `Flows` can have state or be impure themselves. But `Flow`s are a way to abstract your dependencies and use them without coupling.

How does it work? It all starts in `RoadmapViewModel` when the output Flow is initialized:

```kotlin
private val outputFlow: Flow<Output> = process(inputFlow)
```

Then the ViewModel sets up effect handlers:

```kotlin
init {
    outputFlow.onEach {
        processOutput(it)
    }.launchIn(viewModelScope)
}

private fun processOutput(output: Output) {
    when (output) {
        is Output.RecallTrip -> recallTrip(output.tripId)
        is Output.ChangeState -> changeState(output.journeyOutcome)
    }
}

private fun recallTrip(tripId: String) {
    val recalled = journeyCache.get(tripId)
    inputFlow.value = Input.TripRecalled(tripId, recalled)
}

private fun changeState(journeyOutcome: Outcome<Journey>) {
    _state.value = State(journeyOutcome)
}
```

And then `RoadmapFragment` loads the initial data:

```kotlin
override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    val args = arguments
        ?.let { RoadmapFragmentArgs.fromBundle(it) }
        ?: throw IllegalArgumentException("Invalid arguments: $arguments")
    viewModel.load(args.tripId)
}
```

And here is `RoadmapViewModel.load()`. It sends an `Input.Start`:

```kotlin
fun load(tripId: String) {
    inputFlow.value = Input.Start(tripId)
}
```

So, what's the result of this experiment about implementing all the intelligence in the `process` function?

In `RoadmapViewModel`, the code seems alright. But let's imagine the equivalent "usual" code:

```kotlin
fun load(tripId: String) {
    // Start(tripId)
    // Recall(tripId)
    val recalled = journeyCache.get(tripId)
    // TripRecalled(tripId, recalled)
    val trip = recalled.journey
    val tripId = recalled.id
    val outcome = if (trip == null) {
        Outcome.Failure(Exception("Trip not found: $tripId"))
    } else {
        Outcome.Success(trip)
    }
    // Output.ChangeState(outcome)
    _state.value = State(journeyOutcome)
}
```

😅

The experiment should be conducted in a more complex use case, but I have the intuition that it would not be worth it. Perhaps when the app has to interact with many third party SDKs or frameworks? I don't know.

#### What next?

There are a few other things I would like to explore:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [ReduxKotlin](https://reduxkotlin.org/)
- [Functional Android development using Arrow](https://medium.com/default-to-open/explaining-the-arrow-android-sample-ee5c8bdfe88a)