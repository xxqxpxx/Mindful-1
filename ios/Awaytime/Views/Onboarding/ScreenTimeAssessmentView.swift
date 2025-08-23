import SwiftUI

struct ScreenTimeAssessmentView: View {
    @Binding var selectedHours: Int
    let onContinue: () -> Void
    
    var body: some View {
        ZStack {
            Color(UIColor.systemBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Progress bar
                ProgressView(value: 0.6)
                    .progressViewStyle(LinearProgressViewStyle(tint: .blue))
                    .padding(.horizontal)
                    .padding(.top, 10)
                
                Spacer()
                
                // Brain mascot
                BrainMascotView(state: .thinking, size: 140)
                    .padding(.bottom, 40)
                
                // Title and subtitle
                VStack(spacing: 16) {
                    Text("how much time do you spend\non screens daily?")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text("you can tell the truth")
                        .font(.title3)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding(.horizontal, 40)
                .padding(.bottom, 40)
                
                // Hours selection
                VStack(spacing: 20) {
                    Text("\(selectedHours) hours")
                        .font(.system(size: 48, weight: .bold, design: .rounded))
                        .foregroundColor(hoursColor)
                        .animation(.easeInOut(duration: 0.2), value: selectedHours)
                    
                    // Slider
                    HStack {
                        Text("0h")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        
                        Slider(
                            value: Binding(
                                get: { Double(selectedHours) },
                                set: { selectedHours = Int($0.rounded()) }
                            ),
                            in: 0...12,
                            step: 1
                        )
                        .accentColor(.blue)
                        
                        Text("12h+")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    .padding(.horizontal, 20)
                    
                    // Impact message
                    if selectedHours > 0 {
                        Text(impactMessage)
                            .font(.subheadline)
                            .foregroundColor(impactColor)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 40)
                            .padding(.vertical, 16)
                            .background(
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(impactColor.opacity(0.1))
                            )
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
                                .fill(Color.blue)
                        )
                }
                .padding(.horizontal, 32)
                .padding(.bottom, 40)
            }
        }
    }
    
    private var hoursColor: Color {
        switch selectedHours {
        case 0...2:
            return .green
        case 3...4:
            return .blue
        case 5...6:
            return .orange
        case 7...8:
            return .red
        default:
            return .purple
        }
    }
    
    private var impactMessage: String {
        let lifePercentage = (Double(selectedHours) / 16.0) * 100 // Assuming 16 waking hours
        
        switch selectedHours {
        case 0...2:
            return "Great balance! You're using screens mindfully."
        case 3...4:
            return "Good usage level - \(Int(lifePercentage))% of your waking hours."
        case 5...6:
            return "Moderate usage - that's \(Int(lifePercentage))% of your day on screens."
        case 7...8:
            return "High usage - \(Int(lifePercentage))% of your waking life is spent on screens."
        case 9...10:
            return "Very high usage - over half your waking hours are on screens."
        case 11...12:
            return "Critical usage - most of your day is spent looking at screens."
        default:
            return "This is a significant percentage of your life."
        }
    }
    
    private var impactColor: Color {
        switch selectedHours {
        case 0...2:
            return .green
        case 3...4:
            return .blue
        case 5...6:
            return .orange
        default:
            return .red
        }
    }
}

struct ScreenTimeAssessmentView_Previews: PreviewProvider {
    @State static private var selectedHours = 6
    
    static var previews: some View {
        ScreenTimeAssessmentView(
            selectedHours: $selectedHours,
            onContinue: {}
        )
    }
}