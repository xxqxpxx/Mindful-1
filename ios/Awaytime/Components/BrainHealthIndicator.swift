import SwiftUI

struct BrainHealthIndicator: View {
    let screenTimeHours: Double
    let motivationAlignment: Double // 0.0 to 1.0
    let brainStressLevel: Int // 1-5 scale
    @State private var animateProgress = false
    
    var body: some View {
        VStack(spacing: 20) {
            // Brain health status
            HStack(spacing: 12) {
                BrainMascotView(state: brainMascotState, size: 40)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Brain Health")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text(healthStatusText)
                        .font(.subheadline)
                        .foregroundColor(healthStatusColor)
                }
                
                Spacer()
                
                // Health score
                ZStack {
                    Circle()
                        .stroke(Color.gray.opacity(0.3), lineWidth: 4)
                        .frame(width: 50, height: 50)
                    
                    Circle()
                        .trim(from: 0, to: animateProgress ? healthScore : 0)
                        .stroke(healthStatusColor, lineWidth: 4)
                        .frame(width: 50, height: 50)
                        .rotationEffect(.degrees(-90))
                        .animation(.easeInOut(duration: 1.0), value: animateProgress)
                    
                    Text("\(Int(healthScore * 100))")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(healthStatusColor)
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(UIColor.secondarySystemBackground))
            )
            
            // Life percentage indicator
            if screenTimeHours > 4 {
                LifePercentageView(screenTimeHours: screenTimeHours)
            }
            
            // Brain health tips
            BrainHealthTipsView(brainStressLevel: brainStressLevel)
        }
        .onAppear {
            animateProgress = true
        }
    }
    
    private var brainMascotState: BrainMascotView.MascotState {
        switch brainStressLevel {
        case 1, 2:
            return .happy
        case 3:
            return .thinking
        case 4, 5:
            return .concerned
        default:
            return .happy
        }
    }
    
    private var healthStatusText: String {
        switch brainStressLevel {
        case 1:
            return "Excellent - Your brain is thriving!"
        case 2:
            return "Good - Healthy digital habits"
        case 3:
            return "Fair - Room for improvement"
        case 4:
            return "Poor - High digital stress"
        case 5:
            return "Critical - Immediate attention needed"
        default:
            return "Unknown"
        }
    }
    
    private var healthStatusColor: Color {
        switch brainStressLevel {
        case 1, 2:
            return .green
        case 3:
            return .orange
        case 4, 5:
            return .red
        default:
            return .gray
        }
    }
    
    private var healthScore: Double {
        let screenTimeScore = max(0, 1.0 - (screenTimeHours / 12.0)) // Normalize to 12 hours max
        let stressScore = Double(6 - brainStressLevel) / 5.0 // Invert stress level
        let motivationScore = motivationAlignment
        
        return (screenTimeScore + stressScore + motivationScore) / 3.0
    }
}

struct LifePercentageView: View {
    let screenTimeHours: Double
    
    var body: some View {
        VStack(spacing: 12) {
            HStack {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundColor(.orange)
                    .font(.title3)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("Screen Time Impact")
                        .font(.headline)
                        .fontWeight(.semibold)
                    
                    Text(impactMessage)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
            
            // Life percentage bar
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text("Daily life spent on screens")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Text("\(Int(lifePercentage))%")
                        .font(.caption)
                        .fontWeight(.bold)
                        .foregroundColor(.orange)
                }
                
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Rectangle()
                            .fill(Color.gray.opacity(0.3))
                            .frame(height: 8)
                            .cornerRadius(4)
                        
                        Rectangle()
                            .fill(Color.orange)
                            .frame(width: geometry.size.width * (lifePercentage / 100), height: 8)
                            .cornerRadius(4)
                    }
                }
                .frame(height: 8)
            }
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.orange.opacity(0.1))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.orange.opacity(0.3), lineWidth: 1)
                )
        )
    }
    
    private var lifePercentage: Double {
        // Assuming 16 waking hours per day
        return (screenTimeHours / 16.0) * 100
    }
    
    private var impactMessage: String {
        switch lifePercentage {
        case 0..<25:
            return "Healthy balance maintained"
        case 25..<40:
            return "Moderate usage - watch for trends"
        case 40..<60:
            return "High usage - significant life impact"
        case 60...:
            return "Critical - majority of waking hours"
        default:
            return "Unknown impact"
        }
    }
}

struct BrainHealthTipsView: View {
    let brainStressLevel: Int
    @State private var currentTipIndex = 0
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "lightbulb.fill")
                    .foregroundColor(.blue)
                    .font(.title3)
                
                Text("Brain Health Tip")
                    .font(.headline)
                    .fontWeight(.semibold)
                
                Spacer()
                
                if healthTips.count > 1 {
                    Button(action: nextTip) {
                        Image(systemName: "arrow.right.circle.fill")
                            .foregroundColor(.blue)
                            .font(.title3)
                    }
                }
            }
            
            Text(currentTip)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.leading)
                .lineLimit(nil)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color.blue.opacity(0.1))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color.blue.opacity(0.3), lineWidth: 1)
                )
        )
    }
    
    private var currentTip: String {
        guard !healthTips.isEmpty else { return "Keep up the great work!" }
        return healthTips[currentTipIndex]
    }
    
    private var healthTips: [String] {
        switch brainStressLevel {
        case 1, 2:
            return [
                "Great job! Your brain is in excellent condition. Keep maintaining these healthy digital habits.",
                "Consider using your extra mental energy for creative pursuits or learning new skills.",
                "You're a great example of healthy screen time management. Share your success with others!"
            ]
        case 3:
            return [
                "Try the 20-20-20 rule: Every 20 minutes, look at something 20 feet away for 20 seconds.",
                "Consider setting phone-free zones in your bedroom and during meals.",
                "Use app timers to become more aware of your usage patterns."
            ]
        case 4:
            return [
                "High screen time can reduce your brain's ability to focus deeply. Try scheduling tech-free periods.",
                "Excessive scrolling creates dopamine addiction. Replace one social app with a book or podcast.",
                "Your brain needs breaks to process information. Try meditation or short walks without your phone."
            ]
        case 5:
            return [
                "Critical levels of screen time can impact sleep, focus, and mental health. Consider professional guidance.",
                "Start small: try a 1-hour phone-free period each day and gradually increase.",
                "Replace mindless scrolling with intentional activities like exercise, reading, or socializing."
            ]
        default:
            return ["Keep monitoring your digital wellness for optimal brain health."]
        }
    }
    
    private func nextTip() {
        currentTipIndex = (currentTipIndex + 1) % healthTips.count
    }
}

struct BrainHealthIndicator_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 20) {
                BrainHealthIndicator(
                    screenTimeHours: 3.5,
                    motivationAlignment: 0.8,
                    brainStressLevel: 2
                )
                
                BrainHealthIndicator(
                    screenTimeHours: 6.5,
                    motivationAlignment: 0.4,
                    brainStressLevel: 4
                )
                
                BrainHealthIndicator(
                    screenTimeHours: 9.0,
                    motivationAlignment: 0.2,
                    brainStressLevel: 5
                )
            }
            .padding()
        }
        .background(Color(UIColor.systemGroupedBackground))
    }
}