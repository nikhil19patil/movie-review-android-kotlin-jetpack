# Movie Review App

A modern Android application for browsing movies, viewing details, and adding reviews. This project was developed using Cursor AI as a learning exercise to understand modern Android development concepts and best practices.

## 🎯 Learning Objectives

This project demonstrates the implementation of various modern Android development concepts:

- **Jetpack Compose**: Modern declarative UI toolkit
- **Clean Architecture**: Separation of concerns with Repository pattern
- **MVVM Architecture**: ViewModel and StateFlow for UI state management
- **Dependency Injection**: Using Hilt for dependency management
- **Paging 3**: Efficient loading and caching of movie lists
- **Room Database**: Local data persistence
- **Retrofit**: Network API calls
- **Coroutines & Flow**: Asynchronous programming
- **Material 3**: Modern Material Design implementation

## 🛠️ Libraries Used

### UI & Architecture
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern UI toolkit
- [Material 3](https://m3.material.io/) - Material Design components
- [Coil](https://coil-kt.github.io/coil/) - Image loading library
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) - Navigation component

### Architecture Components
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - UI state management
- [StateFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow) - State management
- [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) - Pagination library
- [Room](https://developer.android.com/training/data-storage/room) - Local database

### Dependency Injection
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) - Dependency injection framework

### Networking
- [Retrofit](https://square.github.io/retrofit/) - Type-safe HTTP client
- [OkHttp](https://square.github.io/okhttp/) - HTTP client
- [Gson](https://github.com/google/gson) - JSON parsing

### Asynchronous Programming
- [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - Asynchronous programming
- [Flow](https://kotlinlang.org/docs/flow.html) - Reactive streams

## 📱 Features

- Browse popular movies
- View detailed movie information
- Add and view movie reviews
- Offline support with local caching
- Modern Material 3 UI design
- Responsive and smooth scrolling
- Error handling and loading states

## 🎨 UI Components

- Movie list with pagination
- Movie detail screen
- Review system
- Loading indicators
- Error states
- Pull-to-refresh functionality

## 🔄 Data Flow

1. **Remote Data Source**
   - TMDB API integration
   - Movie details and list fetching
   - Error handling

2. **Local Data Source**
   - Room database for caching
   - Offline support
   - Review storage

3. **Repository Layer**
   - Data synchronization
   - Network state handling
   - Error management

## 📝 Note

This project was developed using Cursor AI as a learning exercise. It's intended to demonstrate various Android development concepts and best practices. The code structure and implementation choices are made to showcase these concepts rather than for production use.

## 🔑 API Key

To run this project, you'll need to:
1. Get an API key from [TMDB](https://www.themoviedb.org/documentation/api)
2. Replace the API key in `NetworkModule.kt`

## 🚀 Getting Started

1. Clone the repository
2. Add your TMDB API key
3. Build and run the project

## 📚 Learning Resources

- [Android Developer Documentation](https://developer.android.com/docs)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose/documentation)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Hilt Documentation](https://developer.android.com/training/dependency-injection/hilt-android) 