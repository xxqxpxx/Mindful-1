import SwiftUI

struct MotivationSelectionView: View {
    @StateObject private var viewModel = MotivationSelectionViewModel()
    @Binding var selectedMotivations: Set<UserMotivation.Reason>
    let onContinue: () -> Void
    
    var body: some View {
        ZStack {
            // Background
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Progress bar
                ProgressView(value: 0.4)
                    .progressViewStyle(LinearProgressViewStyle(tint: .blue))
                    .padding(.horizontal)
                    .padding(.top, 10)
                
                Spacer()
                
                // Brain mascot
                BrainMascotView(state: .encouraging, size: 140)
                    .padding(.bottom, 40)
                
                // Title and subtitle
                VStack(spacing: 16) {
                    Text("you're here for a reason")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("what is that reason?")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 40)
                
                // Motivation options
                LazyVStack(spacing: 16) {
                    ForEach(UserMotivation.Reason.allCases, id: \.self) { reason in
                        MotivationOptionRow(
                            reason: reason,
                            isSelected: selectedMotivations.contains(reason)
                        ) {
                            toggleMotivation(reason)
                        }
                    }
                }
                .padding(.horizontal, 32)
                
                Spacer()
                
                // Continue button
                Button(action: onContinue) {
                    Text("continue")
                        .font(.title3)
                        .fontWeight(.medium)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(
                            RoundedRectangle(cornerRadius: 28)
                                .fill(selectedMotivations.isEmpty ? Color.gray : Color.blue)
                        )
                }
                .disabled(selectedMotivations.isEmpty)
                .padding(.horizontal, 32)
                .padding(.bottom, 40)
            }
        }
    }
    
    private func toggleMotivation(_ reason: UserMotivation.Reason) {
        if selectedMotivations.contains(reason) {
            selectedMotivations.remove(reason)
        } else {
            selectedMotivations.insert(reason)
        }
    }
}

struct MotivationOptionRow: View {
    let reason: UserMotivation.Reason
    let isSelected: Bool
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            HStack {
                Text(reason.displayText)
                    .font(.body)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.leading)
                
                Spacer()
                
                Circle()
                    .stroke(isSelected ? Color.blue : Color.gray, lineWidth: 2)
                    .fill(isSelected ? Color.blue : Color.clear)
                    .frame(width: 24, height: 24)
                    .overlay(
                        Circle()
                            .fill(Color.white)
                            .frame(width: 8, height: 8)
                            .opacity(isSelected ? 1 : 0)
                    )
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(UIColor.secondarySystemBackground))
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
                    )
            )
        }
        .buttonStyle(PlainButtonStyle())
    }
}

class MotivationSelectionViewModel: ObservableObject {
    @Published var selectedMotivations: Set<UserMotivation.Reason> = []
    
    func saveMotivations() {
        // Save to Core Data when implemented
        UserDefaults.standard.set(
            selectedMotivations.map { $0.rawValue },
            forKey: "selectedMotivations"
        )
    }
}

extension UserMotivation {
    enum Reason: String, CaseIterable {
        case improveFocus = "improve_focus"
        case reduceMindlessScrolling = "reduce_mindless_scrolling"
        case sleepBetter = "sleep_better"
        case beMorePresent = "be_more_present"
        case beMoreProductive = "be_more_productive"
        case justCurious = "just_curious"
        
        var displayText: String {
            switch self {
            case .improveFocus:
                return "improve focus"
            case .reduceMindlessScrolling:
                return "reduce mindless scrolling"
            case .sleepBetter:
                return "sleep better"
            case .beMorePresent:
                return "be more present"
            case .beMoreProductive:
                return "be more productive"
            case .justCurious:
                return "just curious"
            }
        }
        
        var motivationalMessage: String {
            switch self {
            case .improveFocus:
                return "Great choice! Let's help you build laser focus by reducing digital distractions."
            case .reduceMindlessScrolling:
                return "Smart move! We'll help you break the scroll cycle and reclaim your time."
            case .sleepBetter:
                return "Excellent! Better screen time habits lead to much better sleep quality."
            case .beMorePresent:
                return "Wonderful! Being present is one of life's greatest gifts to yourself and others."
            case .beMoreProductive:
                return "Perfect! Let's turn your screen time into productive, intentional usage."
            case .justCurious:
                return "Curiosity is the first step to positive change! Let's explore together."
            }
        }
        
        var brainHealthTip: String {
            switch self {
            case .improveFocus:
                return "Constant app switching weakens your brain's ability to concentrate deeply."
            case .reduceMindlessScrolling:
                return "Mindless scrolling creates dopamine loops that make it harder to enjoy simple pleasures."
            case .sleepBetter:
                return "Blue light and mental stimulation before bed disrupt your natural sleep cycles."
            case .beMorePresent:
                return "Heavy phone use during social time weakens real-world connection skills."
            case .beMoreProductive:
                return "Task-switching between apps can reduce productivity by up to 40%."
            case .justCurious:
                return "The average person checks their phone 96 times per day - that's once every 10 minutes!"
            }
        }
    }
}

struct MotivationSelectionView_Previews: PreviewProvider {
    @State static private var selectedMotivations: Set<UserMotivation.Reason> = []
    
    static var previews: some View {
        MotivationSelectionView(
            selectedMotivations: $selectedMotivations,
            onContinue: {}
        )
    }
}