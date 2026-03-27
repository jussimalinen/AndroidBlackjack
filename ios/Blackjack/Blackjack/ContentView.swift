import SwiftUI

struct ContentView: View {
    @State private var gameViewModel = GameViewModel()
    @State private var settingsViewModel = SettingsViewModel()
    @State private var showGame = false

    var body: some View {
        NavigationStack {
            SettingsScreen(
                settingsViewModel: settingsViewModel,
                onStartGame: { rules in
                    gameViewModel.startGame(rules: rules)
                    showGame = true
                }
            )
            .navigationDestination(isPresented: $showGame) {
                GameScreen(
                    viewModel: gameViewModel,
                    onNavigateToSettings: {
                        showGame = false
                    }
                )
            }
        }
        .preferredColorScheme(.dark)
    }
}
